# Drawing smoke test

`drawing-smoke.js` runs an in-game visual fixture. `drawing-smoke-2d.js` supplies the shared HUD and script-screen cases.

## Isolation

Run this only in a disposable single-player world and a separate game directory. The script refuses to run without a `.jsmacros-drawing-smoke` file in that game directory. It changes terrain, game mode, time, weather, and player position in the fixture world.

Copy both scripts into that instance's macro directory, create `drawing-smoke-output` there, and run `drawing-smoke.js` after joining the world. The local player name is resolved at runtime. No remote server is needed.

## Evidence

The script writes `drawing-smoke-output/cases.json` and numbered PNGs under `drawing-smoke-output/screenshots`. A `COMPLETE` log means the script reached every phase and its API assertions passed. It does not mean the images have passed visual review.

| Area | Cases |
| --- | --- |
| HUD and script screens | Plain and formatted text, shadow, scale, rotation, rectangles, alpha blending, thin/thick lines, texture UVs/tint, item models/counts/durability/glint, nested drawings, builders and alignment |
| Dynamic state | Texture uploads and source cropping, distinct unnamed textures, GUI-scale resize/reinitialization, cooldown overlays |
| Ordering | Element z-index, independent HUD z-index with reverse registration order, mixed text/image/rectangle and item/image surface ordering, equal-z geometry and text/rectangle insertion |
| Screen widgets | Button, text field, slider, checkbox appearance |
| World primitives | Outline/filled boxes, explicit-alpha color replacement, colored point, lines, positional and entity traces, depth modes |
| World surfaces | Text, rectangles, lines, images, items including a chest special model, nested drawings, rotation/pivots, billboard, entity binding/offset |
| Surface options | Front/back visibility, world/custom/full-bright lighting, real wall occlusion versus see-through, cloud-layer cull comparisons |
| Lifecycle | Remove/re-add, mutate, unregister/re-register, clear |

The surface ordering pair checks both depth-tested and see-through panels. In the equal-z geometry case, the later image should cover the rectangle and the final green line should cover the image. The opaque no-shadow cases make both directions unambiguous: the later rectangle must hide the preceding text, while text added after the rectangle must remain visible. A third panel retains the shadow-sensitive regressions: shadow, bold-shadow, and formatted-underlined text are each added before an opaque equal-z rectangle and must be fully hidden. `13a-surface-z-order-back.png` repeats those panels from the back before the wall-occlusion pair. Compare mixed text/custom-geometry and item/image directions in their labeled panels rather than treating API construction as visual proof.

`15-clouds-fancy.png` and `16-clouds-off.png` are a matched camera/scene pair at the cloud layer. The scene contains a cull=false surface, a cull=false filled box and line, and a depth-tested trace. The cloud mode is restored immediately after the pair, including if capture fails. Inspect whether clouds cross or hide each primitive consistently, and compare the item/image overlap in the labeled surface between the two frames.

Review every screenshot and search the client log for shader, mixin, rendering, and script errors. In the wall pair, depth-tested text/items should disappear and reappear when see-through is enabled. In the back view, the front-only surface should disappear while the two-sided surface remains. Unregister and clear frames should contain no JsMacros world drawings.

This is feature-family coverage, not an exhaustive permutation of every overload, resource pack, graphics backend, or other mod. Widget interaction and multiplayer behavior are separate tests.
