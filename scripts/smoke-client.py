#!/usr/bin/env python3
"""Prepare an isolated, offline Fabric client smoke-test installation."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import platform
import re
import shutil
import ssl
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
MOJANG_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
FABRIC_PROFILE = "https://meta.fabricmc.net/v2/versions/loader/{minecraft}/{loader}/profile/json"
MARKER = ".jsmacros-smoke-client.json"
CACHE_ROOT = Path(tempfile.gettempdir()) / "jsmacros-client-cache"
DEFAULT_CACHE_SOURCES = (
    Path.home() / "Library/Application Support/PrismLauncher",
    Path.home() / "Library/Application Support/minecraft",
)
CACHE_SOURCES: tuple[Path, ...] = ()


def tls_context() -> ssl.SSLContext:
    try:
        import certifi
        return ssl.create_default_context(cafile=certifi.where())
    except ImportError:
        return ssl.create_default_context()


class SmokeError(RuntimeError):
    pass


def sha1(path: Path) -> str:
    digest = hashlib.sha1()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def fetch_json(url: str) -> dict[str, Any]:
    with urllib.request.urlopen(url, timeout=30, context=tls_context()) as response:
        return json.load(response)


def download(url: str, destination: Path, expected_sha1: str | None = None) -> Path:
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.is_file() and (not expected_sha1 or sha1(destination) == expected_sha1):
        return destination
    cache_key = expected_sha1 or hashlib.sha1(url.encode()).hexdigest()
    cached = CACHE_ROOT / "downloads" / cache_key
    cached.parent.mkdir(parents=True, exist_ok=True)
    if not cached.is_file() and expected_sha1:
        for source in cache_candidates(destination, expected_sha1):
            if source.is_file() and sha1(source) == expected_sha1:
                shutil.copy2(source, cached)
                break
    if cached.is_file() and (not expected_sha1 or sha1(cached) == expected_sha1):
        shutil.copy2(cached, destination)
        return destination
    temporary = cached.with_suffix(".part")
    try:
        for attempt in range(3):
            try:
                with urllib.request.urlopen(url, timeout=60, context=tls_context()) as response, temporary.open("wb") as output:
                    shutil.copyfileobj(response, output)
                if expected_sha1 and sha1(temporary) != expected_sha1:
                    raise SmokeError(f"Checksum mismatch for {url}")
                temporary.replace(cached)
                shutil.copy2(cached, destination)
                return destination
            except (OSError, urllib.error.URLError) as error:
                temporary.unlink(missing_ok=True)
                if attempt == 2:
                    raise SmokeError(f"Download failed for {url}: {error}") from error
                time.sleep(1 + attempt)
    finally:
        temporary.unlink(missing_ok=True)
    raise AssertionError("unreachable")


def cache_candidates(destination: Path, digest: str) -> Iterable[Path]:
    for source in CACHE_SOURCES:
        yield source / "assets" / "objects" / digest[:2] / digest
        yield source / "objects" / digest[:2] / digest
        for segment in ("libraries", "versions"):
            if segment in destination.parts:
                relative = Path(*destination.parts[destination.parts.index(segment) + 1:])
                yield source / segment / relative


def read_properties(path: Path) -> dict[str, str]:
    values = {}
    for line in path.read_text().splitlines():
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def java_major(java: Path) -> int | None:
    try:
        output = subprocess.run(
            [str(java), "-version"], text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            timeout=15, check=False,
        ).stdout
    except OSError:
        return None
    match = re.search(r'(?:version|openjdk) "?(\d+)', output, re.IGNORECASE)
    return int(match.group(1)) if match else None


def resolve_java(requested: str | None, required_major: int) -> str:
    candidates: list[Path] = []
    if requested:
        candidates.append(Path(requested).expanduser())
    else:
        java_home = os.environ.get("JAVA_HOME")
        if java_home:
            candidates.append(Path(java_home) / "bin" / "java")
        candidates.extend((
            Path.home() / ".local/share/mise/installs/java/temurin-25/bin/java",
            Path("/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java"),
        ))
        path_java = shutil.which("java")
        if path_java:
            candidates.append(Path(path_java))
    checked: list[str] = []
    for candidate in candidates:
        candidate = candidate.resolve()
        major = java_major(candidate)
        checked.append(f"{candidate} ({major if major is not None else 'unavailable'})")
        if major == required_major:
            return str(candidate)
    source = "--java" if requested else "JAVA_HOME, mise, Homebrew, and PATH"
    raise SmokeError(
        f"Minecraft requires Java {required_major}; no matching runtime from {source}. Checked: {', '.join(checked)}"
    )


def profile_for_version(version: str) -> tuple[str, dict[str, str]]:
    for path in sorted((ROOT / "versions").glob("*.properties")):
        properties = read_properties(path)
        supported = [value.strip() for value in properties.get("minecraft_versions", "").split(",")]
        if version in supported:
            return path.stem, properties
    raise SmokeError(f"No version profile supports Minecraft {version}")


def resolve_jar(profile: str, requested: str | None) -> Path:
    if requested:
        jar = Path(requested).expanduser().resolve()
        if not jar.is_file():
            raise SmokeError(f"Mod jar does not exist: {jar}")
        return jar
    candidates = [
        path for path in (ROOT / "dist" / profile).glob("*.jar")
        if not path.name.endswith(("-sources.jar", "-dev.jar"))
    ]
    def is_jsmacros_jar(path: Path) -> bool:
        try:
            return mod_metadata(path).get("id") == "jsmacros"
        except SmokeError:
            return False
    candidates = [path for path in candidates if is_jsmacros_jar(path)]
    if len(candidates) != 1:
        found = ", ".join(str(path) for path in candidates) or "none"
        raise SmokeError(f"Expected one packaged jar in dist/{profile}; found {found}. Pass --jar to select it.")
    return candidates[0]


def mod_metadata(jar: Path) -> dict[str, Any]:
    try:
        with zipfile.ZipFile(jar) as archive:
            return json.loads(archive.read("fabric.mod.json"))
    except (KeyError, zipfile.BadZipFile, json.JSONDecodeError) as error:
        raise SmokeError(f"{jar} is not a packaged Fabric mod jar: {error}") from error


def version_predicate_supports(predicate: Any, version: str) -> bool:
    """Handle the predicates emitted by this build and common Fabric bounds."""
    if isinstance(predicate, list):
        return any(version_predicate_supports(item, version) for item in predicate)
    if not isinstance(predicate, str):
        return False
    if predicate == "*":
        return True
    for alternative in predicate.split("||"):
        terms = alternative.strip().split()
        if terms and all(version_term_supports(term, version) for term in terms):
            return True
    return False


def version_key(value: str) -> tuple[Any, ...]:
    return tuple(int(part) if part.isdigit() else part for part in re.split(r"[.+-]", value))


def version_term_supports(term: str, version: str) -> bool:
    match = re.fullmatch(r"(>=|<=|>|<|=|~|\^)?\s*(.+)", term)
    if not match:
        return False
    operator, expected = match.groups()
    operator = operator or "="
    if expected in {"*", "x", "X"}:
        return True
    if "x" in expected.lower() or "*" in expected:
        prefix = re.split(r"[xX*]", expected, maxsplit=1)[0].rstrip(".")
        return version == prefix or version.startswith(prefix + ".")
    actual_key, expected_key = version_key(version), version_key(expected)
    if operator == "=":
        return actual_key == expected_key
    if operator == ">=":
        return actual_key >= expected_key
    if operator == "<=":
        return actual_key <= expected_key
    if operator == ">":
        return actual_key > expected_key
    if operator == "<":
        return actual_key < expected_key
    if operator == "~":
        return actual_key[:2] == expected_key[:2] and actual_key >= expected_key
    if operator == "^":
        return actual_key[:1] == expected_key[:1] and actual_key >= expected_key
    return False


def assert_mod_supports(jar: Path, minecraft_version: str) -> dict[str, Any]:
    metadata = mod_metadata(jar)
    predicate = metadata.get("depends", {}).get("minecraft")
    if not version_predicate_supports(predicate, minecraft_version):
        raise SmokeError(
            f"{jar.name} declares minecraft {predicate!r}, which does not support {minecraft_version}"
        )
    return metadata


def packaged_mod_ids(jar: Path) -> list[str]:
    """Return the root and Fabric-declared nested mod ids from one packaged jar."""
    try:
        with zipfile.ZipFile(jar) as archive:
            metadata = json.loads(archive.read("fabric.mod.json"))
            ids = [metadata["id"]]
            for nested in metadata.get("jars", []):
                nested_path = nested["file"]
                try:
                    nested_bytes = archive.read(nested_path)
                    with zipfile.ZipFile(io.BytesIO(nested_bytes)) as nested_archive:
                        ids.append(json.loads(nested_archive.read("fabric.mod.json"))["id"])
                except KeyError as error:
                    raise SmokeError(f"{jar.name} declares missing nested mod {nested_path}") from error
            return ids
    except (KeyError, zipfile.BadZipFile, json.JSONDecodeError) as error:
        raise SmokeError(f"Cannot read packaged Fabric mods from {jar}: {error}") from error


def assert_unique_mod_ids(mods_dir: Path) -> None:
    ids: list[str] = []
    for jar in mods_dir.glob("*.jar"):
        ids.extend(packaged_mod_ids(jar))
    duplicates = sorted({mod_id for mod_id in ids if ids.count(mod_id) > 1})
    if duplicates:
        raise SmokeError("Duplicate Fabric mod IDs: " + ", ".join(duplicates))


def os_name() -> str:
    return {"Darwin": "osx", "Windows": "windows", "Linux": "linux"}.get(platform.system(), platform.system().lower())


def rules_allow(rules: list[dict[str, Any]]) -> bool:
    allowed = not rules
    for rule in rules:
        matches = True
        os_rule = rule.get("os", {})
        if "name" in os_rule and os_rule["name"] != os_name():
            matches = False
        if "arch" in os_rule and not re.search(os_rule["arch"], platform.machine()):
            matches = False
        for feature, expected in rule.get("features", {}).items():
            if expected:
                matches = False
        if matches:
            allowed = rule.get("action") == "allow"
    return allowed


def library_allowed(library: dict[str, Any]) -> bool:
    return rules_allow(library.get("rules", []))


def library_path(library: dict[str, Any]) -> Path:
    name = library["name"]
    group, artifact, version = name.split(":", 2)
    return Path(*group.split(".")) / artifact / version / f"{artifact}-{version}.jar"


def native_classifier(library: dict[str, Any]) -> str | None:
    classifier = library.get("natives", {}).get(os_name())
    if classifier:
        return classifier.replace("${arch}", "arm64" if platform.machine() == "arm64" else "64")
    return None


def library_downloads(library: dict[str, Any]) -> Iterable[tuple[Path, dict[str, Any], bool]]:
    if not library_allowed(library):
        return
    downloads = library.get("downloads", {})
    artifact = downloads.get("artifact")
    if artifact:
        yield Path(artifact["path"]), artifact, False
    elif library.get("url"):
        yield library_path(library), {
            "url": library["url"].rstrip("/") + "/" + str(library_path(library)).replace(os.sep, "/")
        }, False
    classifier = native_classifier(library)
    native = downloads.get("classifiers", {}).get(classifier) if classifier else None
    if native:
        yield Path(native["path"]), native, True


def merge_libraries(*sets: list[dict[str, Any]]) -> list[dict[str, Any]]:
    merged: dict[str, dict[str, Any]] = {}
    for libraries in sets:
        for library in libraries:
            merged[library["name"]] = library
    return list(merged.values())


def expand_arguments(arguments: list[Any] | str, values: dict[str, str]) -> list[str]:
    if isinstance(arguments, str):
        arguments = [arguments]
    expanded = []
    for argument in arguments:
        if isinstance(argument, str):
            expanded.append(re.sub(r"\$\{([^}]+)\}", lambda match: values.get(match.group(1), match.group(0)), argument))
        elif library_allowed(argument):
            expanded.extend(expand_arguments(argument.get("value", []), values))
    return expanded


def prepare_owned_runtime(runtime: Path, game_dir: Path, marker: dict[str, str], jar: Path) -> None:
    marker_path = runtime / MARKER
    if marker_path.is_file():
        try:
            existing = json.loads(marker_path.read_text())
        except json.JSONDecodeError as error:
            raise SmokeError(f"Invalid smoke marker: {marker_path}") from error
        for key in ("profile", "minecraft", "jar", "mod_id", "fabric_loader"):
            if existing.get(key) != marker[key]:
                raise SmokeError(f"Smoke runtime belongs to a different {key}: {runtime}")
        prohibited = ("saves", "config", "options.txt", "level.dat")
        if any((game_dir / name).exists() for name in prohibited):
            raise SmokeError(f"Smoke game directory has instance state: {game_dir}")
        allowed = {"mods"} if game_dir != runtime else {"mods", MARKER, "launch.json"}
        unexpected = [path.name for path in game_dir.iterdir() if path.name not in allowed]
        if unexpected:
            raise SmokeError(f"Smoke game directory has unrelated files: {', '.join(unexpected)}")
        mods = game_dir / "mods"
        if mods.exists() and {path.name for path in mods.iterdir()} - {jar.name}:
            raise SmokeError(f"Smoke game directory has unrelated mods: {mods}")
    else:
        if runtime.exists() and any(runtime.iterdir()):
            raise SmokeError(f"Runtime must be new or marker-owned: {runtime}")
        runtime.mkdir(parents=True, exist_ok=True)
        marker_path.write_text(json.dumps(marker, indent=2) + "\n")
    game_dir.mkdir(parents=True, exist_ok=True)
    (game_dir / "mods").mkdir(exist_ok=True)


def extract_natives(native_archives: list[Path], natives_dir: Path) -> None:
    natives_dir.mkdir(parents=True, exist_ok=True)
    for archive_path in native_archives:
        with zipfile.ZipFile(archive_path) as archive:
            for member in archive.infolist():
                if member.is_dir() or member.filename.startswith("META-INF/"):
                    continue
                target = (natives_dir / member.filename).resolve()
                if not target.is_relative_to(natives_dir.resolve()):
                    raise SmokeError(f"Native archive attempts path traversal: {member.filename}")
                target.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(member) as source, target.open("wb") as output:
                    shutil.copyfileobj(source, output)


def asset_files(asset_index: dict[str, Any]) -> Iterable[tuple[str, str]]:
    for asset in asset_index.get("objects", {}).values():
        digest = asset["hash"]
        yield digest, f"https://resources.download.minecraft.net/{digest[:2]}/{digest}"


def download_assets(asset_index: dict[str, Any], assets_dir: Path) -> None:
    def fetch(asset: tuple[str, str]) -> None:
        digest, url = asset
        download(url, assets_dir / "objects" / digest[:2] / digest, digest)
    assets = {digest: url for digest, url in asset_files(asset_index)}
    with ThreadPoolExecutor(max_workers=12) as pool:
        list(pool.map(fetch, assets.items()))


def prepare(args: argparse.Namespace) -> tuple[list[str], Path]:
    profile, properties = profile_for_version(args.minecraft_version)
    if args.profile and args.profile != profile:
        raise SmokeError(f"Minecraft {args.minecraft_version} belongs to profile {profile}, not {args.profile}")
    jar = resolve_jar(profile, args.jar)
    metadata = assert_mod_supports(jar, args.minecraft_version)
    runtime = Path(args.game_dir).expanduser().resolve() if args.game_dir else ROOT / "run" / profile / args.minecraft_version
    game_dir = runtime / "game" if not args.game_dir else runtime
    loader = properties["fabric_loader_version"]
    marker = {
        "profile": profile,
        "minecraft": args.minecraft_version,
        "jar": str(jar),
        "mod_id": metadata["id"],
        "fabric_loader": loader,
    }
    prepare_owned_runtime(runtime, game_dir, marker, jar)
    shutil.copy2(jar, game_dir / "mods" / jar.name)
    assert_unique_mod_ids(game_dir / "mods")

    manifest = fetch_json(MOJANG_MANIFEST)
    entry = next((item for item in manifest["versions"] if item["id"] == args.minecraft_version), None)
    if not entry:
        raise SmokeError(f"Minecraft {args.minecraft_version} is absent from the Mojang version manifest")
    version_path = runtime / "versions" / f"{args.minecraft_version}.json"
    download(entry["url"], version_path, entry.get("sha1"))
    version = json.loads(version_path.read_text())
    required_java = version.get("javaVersion", {}).get("majorVersion")
    if not isinstance(required_java, int):
        raise SmokeError(f"Minecraft {args.minecraft_version} metadata has no Java major version")
    java = resolve_java(args.java, required_java)
    fabric = fetch_json(FABRIC_PROFILE.format(minecraft=args.minecraft_version, loader=loader))
    if fabric.get("mainClass") is None:
        raise SmokeError("Fabric loader profile did not contain a launch main class")

    client = version["downloads"]["client"]
    client_path = runtime / "versions" / args.minecraft_version / f"{args.minecraft_version}.jar"
    download(client["url"], client_path, client.get("sha1"))
    libraries_dir = runtime / "libraries"
    native_archives: list[Path] = []
    classpath: list[Path] = []
    for library in merge_libraries(version.get("libraries", []), fabric.get("libraries", [])):
        for relative, details, is_native in library_downloads(library):
            path = download(details["url"], libraries_dir / relative, details.get("sha1"))
            if is_native:
                native_archives.append(path)
            else:
                classpath.append(path)
    classpath.append(client_path)
    natives_dir = runtime / "natives"
    extract_natives(native_archives, natives_dir)

    assets_dir = runtime / "assets"
    asset = version["assetIndex"]
    asset_index_path = assets_dir / "indexes" / f"{asset['id']}.json"
    download(asset["url"], asset_index_path, asset.get("sha1"))
    asset_index = json.loads(asset_index_path.read_text())
    download_assets(asset_index, assets_dir)

    values = {
        "auth_player_name": "JsMacrosSmoke",
        "version_name": args.minecraft_version,
        "game_directory": str(game_dir),
        "assets_root": str(assets_dir),
        "assets_index_name": asset["id"],
        "auth_uuid": "00000000000000000000000000000000",
        "auth_access_token": "0",
        "user_type": "legacy",
        "version_type": "smoke",
        "natives_directory": str(natives_dir),
        "classpath": os.pathsep.join(str(path) for path in classpath),
        "launcher_name": "jsmacros-smoke-client",
        "launcher_version": "1",
        "user_properties": "{}",
        "clientid": "",
        "auth_xuid": "",
    }
    jvm = expand_arguments(version.get("arguments", {}).get("jvm", []), values)
    jvm += expand_arguments(fabric.get("arguments", {}).get("jvm", []), values)
    game = expand_arguments(version.get("arguments", {}).get("game", []), values)
    game += expand_arguments(fabric.get("arguments", {}).get("game", []), values)
    command = [java, *jvm, fabric["mainClass"], *game]
    (runtime / "launch.json").write_text(json.dumps({
        "command": command,
        "game_dir": str(game_dir),
        "offline_username": values["auth_player_name"],
    }, indent=2) + "\n")
    return command, runtime


def main() -> int:
    global CACHE_SOURCES
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", required=True, help="Version profile, such as 26.1")
    parser.add_argument("--minecraft-version", required=True)
    parser.add_argument("--jar", help="Packaged mod jar; defaults to the sole dist/<profile> jar")
    parser.add_argument("--prepare-only", action="store_true", help="Prepare and print the command without launching")
    parser.add_argument("--java", help="Java executable; must match Minecraft metadata's Java major")
    parser.add_argument("--game-dir", help="An empty, isolated game directory instead of run/<profile>/<version>/game")
    parser.add_argument(
        "--asset-cache", action="append", default=[], metavar="DIR",
        help="Read-only Minecraft or Prism cache root to reuse after SHA-1 verification",
    )
    args = parser.parse_args()
    configured = [Path(path).expanduser() for path in args.asset_cache]
    CACHE_SOURCES = tuple(path for path in (*configured, *DEFAULT_CACHE_SOURCES) if path.is_dir())
    try:
        command, runtime = prepare(args)
    except SmokeError as error:
        parser.error(str(error))
    print(f"runtime={runtime}")
    print(f"launch_json={runtime / 'launch.json'}")
    if not args.prepare_only:
        return subprocess.run(command, cwd=runtime).returncode
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
