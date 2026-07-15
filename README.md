# JsMacros Reloaded

A Fabric mod for scripting Minecraft. Write scripts in JavaScript (or Ruby, see below)
that react to game events: chat messages, ticks, keybinds, whatever. The original
[JsMacros](https://github.com/wagyourtail/JsMacros) is no longer actively maintained,
so this fork carries it forward on current Minecraft versions.

## Downloads

Grab the jar from [GitHub releases](https://github.com/grepsedawk/JSMacros/releases).
That's it, releases live there and only there.

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

Prefer Ruby? [jsmacros-ruby](https://github.com/grepsedawk/jsmacros-ruby) is an
extension that runs JRuby scripts with the same API in snake_case
(`Player.get_player`, `Client.wait_tick`, and so on).

## Development

You need a JDK to launch Gradle (21 works) and the build utilizes a JDK 25
toolchain, which Gradle will provision if it can't find one.

```sh
./gradlew build            # compile and assemble the jars
./gradlew fabricRunClient  # launch a dev Minecraft client with the mod loaded
./gradlew createDist       # build the distributable jars (what CI runs)
```

There is no unit test suite currently, `build` plus a `fabricRunClient` smoke test
is the verification story. CI runs `createDist` on every push.

Releases are cut by tagging `vX.Y.Z+<mc version>` on `main` and publishing a GitHub
release; CI attaches the jars and publishes to Modrinth.

## Credits

Fork of [JsMacros](https://github.com/wagyourtail/JsMacros) by WagYourTail. The
Minecraft 26.1 port is based on work by Pablete1234, and the rendering subsystem
is by Jack Manning.

## License

[MPL-2.0](LICENSE)
