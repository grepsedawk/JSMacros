# JsMacros Reloaded

A Fabric mod for scripting Minecraft. Write scripts in JavaScript (or Ruby, see below)
that react to game events: chat messages, ticks, keybinds, whatever. The original
[JsMacros](https://github.com/wagyourtail/JsMacros) is no longer actively maintained,
so this fork carries it forward on current Minecraft versions.

## Downloads

Download from [GitHub releases](https://github.com/grepsedawk/JsMacros/releases) or
[Modrinth](https://modrinth.com/mod/jsmacros-reloaded). Install one main jar for your
Minecraft version:

| Minecraft | Jar group |
| --- | --- |
| 26.1, 26.1.1, 26.1.2 | 26.1 |
| 26.2 | 26.2 |
| 26.3 | 26.3 |

[![Build Release](https://github.com/grepsedawk/JSMacros/actions/workflows/release.yml/badge.svg)](https://github.com/grepsedawk/JSMacros/actions/workflows/release.yml)

## Getting started

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version.
2. Drop the JsMacros jar into your `mods` folder.
3. Launch the game and press `K` (default) to open the JsMacros hub.
4. Create a script and bind it to an event.

Scripts live in `config/jsmacros/Macros`. A first script can be as small as this,
like so:

```js
Chat.log("Hello from JsMacros!");
```

Bind that to the `Key` event and every keypress logs to chat. From there the
[documentation site](https://jsmacros.wagyourtail.xyz) covers the full API: events,
the `Player`/`World`/`Chat` libraries, services, and everything else.

## Ruby scripting

Prefer Ruby? [jsmacros-ruby](https://github.com/grepsedawk/jsmacros-ruby) is a
companion Fabric mod that runs JRuby scripts with the same API in snake_case
(`Player.get_player`, `Client.wait_tick`, and so on). Install it and
[Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) in `mods`
alongside JsMacros.

## Development

You need a JDK to launch Gradle (21 works) and the build uses a JDK 25
toolchain, which Gradle will provision if it can't find one.

```sh
./gradlew build            # compile and assemble the jars
./gradlew fabricRunClient  # launch a dev Minecraft client with the mod loaded
./gradlew createDist       # build the distributable jars (what CI runs)
```

The build has separate targets for Minecraft 26.1, 26.2, and 26.3. Select one with
`-PmcProfile`; omitting it selects 26.3. Each target writes its build products to
`build/<profile>` and its distributable jars to `dist/<profile>`.

```sh
./gradlew check createDist checkProfileArtifacts -PmcProfile=26.1
./gradlew check createDist checkProfileArtifacts -PmcProfile=26.2
./gradlew check createDist checkProfileArtifacts -PmcProfile=26.3
```

The 26.1 target packages the 26.1, 26.1.1, and 26.1.2 compatibility group. These
are build targets, not a claim that every target has passed a client smoke test.
CI runs the extension tests, packages every target, and checks the resulting
artifacts on pushes and pull requests. Run `fabricRunClient` against the target you
are changing for the graphical smoke test.

To prepare an isolated client smoke test for a packaged jar, pass both the target
profile and the Minecraft version it declares. This example exercises the 26.1.1
entry in the 26.1 compatibility group:

```sh
python3 scripts/smoke-client.py \
  --profile 26.1 \
  --minecraft-version 26.1.1 \
  --jar dist/26.1/jsmacros-26.1-<version>-fabric.jar \
  --prepare-only
```

Without `--prepare-only`, it launches with the offline `JsMacrosSmoke` account in
an isolated `run/<profile>/<minecraft-version>/game` directory. The launcher checks
the jar's Fabric metadata before it prepares the client.

For a scripting smoke test, copy `scripts/compatibility-smoke.js` to the isolated
client's `config/jsmacros/Macros/index.js` before launch. It logs script startup,
tick and key events, then checks world/inventory helpers and registers 2D/3D
drawings when you enter a world.

Minecraft 26.3 no longer sends full brewing recipes to multiplayer clients.
Exact recipe checks and potion previews throw `UnsupportedOperationException`
on remote servers when the answer cannot be determined from synced data.
Single-player uses the integrated server's recipes. `FurnaceInventory.getFuelValues()`
also throws on 26.3: fuel durations now depend on server-context item components.
Fuel eligibility and the open furnace's synced timing methods remain available.

For layered, depth-tested Draw3D surfaces, use distinct z-index values. Exactly
coplanar elements can still z-fight at some camera angles. See the
[drawing smoke fixture](scripts/drawing-smoke.md) for rendering coverage.

Releases are cut by tagging `vX.Y.Z` (or the older `vX.Y.Z+<mc version>` form) and
publishing a GitHub release. CI builds every target from that tag, attaches one main
jar per compatibility group, and publishes the corresponding Modrinth versions.

## Credits

Fork of [JsMacros](https://github.com/wagyourtail/JsMacros) by WagYourTail. The
Minecraft 26.1 port is based on work by Pablete1234, and the rendering subsystem
is by Jack Manning.

## License

[MPL-2.0](LICENSE)
