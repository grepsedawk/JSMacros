#!/usr/bin/env python3
import importlib.util
import json
import tempfile
import unittest
import zipfile
from pathlib import Path


SPEC = importlib.util.spec_from_file_location("smoke_client", Path(__file__).with_name("smoke-client.py"))
smoke_client = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(smoke_client)


class VersionPredicateTests(unittest.TestCase):
    def test_exact_versions_from_generated_metadata(self):
        self.assertTrue(smoke_client.version_predicate_supports(["26.1", "26.1.1", "26.1.2"], "26.1.1"))
        self.assertFalse(smoke_client.version_predicate_supports(["26.1"], "26.2"))

    def test_common_bounds_and_wildcards(self):
        self.assertTrue(smoke_client.version_predicate_supports(">=26.1 <26.3", "26.2"))
        self.assertFalse(smoke_client.version_predicate_supports(">=26.1 <26.2", "26.2"))
        self.assertTrue(smoke_client.version_predicate_supports("26.1.x", "26.1.2"))


class ArgumentTests(unittest.TestCase):
    def test_rule_and_placeholder_expansion(self):
        arguments = ["--gameDir", "${game_directory}", {"rules": [{"action": "allow"}], "value": "--demo"}]
        self.assertEqual(
            smoke_client.expand_arguments(arguments, {"game_directory": "/tmp/game"}),
            ["--gameDir", "/tmp/game", "--demo"],
        )

    def test_feature_rule_is_not_enabled(self):
        arguments = [{"rules": [{"action": "allow", "features": {"is_demo_user": True}}], "value": "--demo"}]
        self.assertEqual(smoke_client.expand_arguments(arguments, {}), [])


class RuntimeTests(unittest.TestCase):
    def test_java_major_parses_openjdk_output(self):
        class Completed:
            stdout = 'openjdk version "25.0.3" 2025-04-15\n'
        original_run = smoke_client.subprocess.run
        smoke_client.subprocess.run = lambda *args, **kwargs: Completed()
        try:
            self.assertEqual(smoke_client.java_major(Path("/tmp/java")), 25)
        finally:
            smoke_client.subprocess.run = original_run

    def test_jar_selection_ignores_non_jsmacros_distribution_jars(self):
        original_root = smoke_client.ROOT
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            dist = root / "dist" / "26.1"
            dist.mkdir(parents=True)
            for name, mod_id in (("python.jar", "python"), ("jsmacros.jar", "jsmacros")):
                with zipfile.ZipFile(dist / name, "w") as archive:
                    archive.writestr("fabric.mod.json", json.dumps({"id": mod_id}))
            smoke_client.ROOT = root
            self.assertEqual(smoke_client.resolve_jar("26.1", None).name, "jsmacros.jar")
        smoke_client.ROOT = original_root

    def test_marker_owned_runtime_can_resume(self):
        with tempfile.TemporaryDirectory() as temporary:
            runtime = Path(temporary) / "runtime"
            game_dir = runtime / "game"
            jar = Path("/tmp/jsmacros.jar")
            marker = {"profile": "26.1", "minecraft": "26.1", "jar": str(jar), "mod_id": "jsmacros", "fabric_loader": "0.19.3"}
            smoke_client.prepare_owned_runtime(runtime, game_dir, marker, jar)
            smoke_client.prepare_owned_runtime(runtime, game_dir, marker, jar)


if __name__ == "__main__":
    unittest.main()
