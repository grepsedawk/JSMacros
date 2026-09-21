const System = Java.type('java.lang.System');
const File = Java.type('java.io.File');
const AtomicBoolean = Java.type('java.util.concurrent.atomic.AtomicBoolean');
const Minecraft = Java.type('net.minecraft.client.Minecraft');
const mc = Minecraft.getInstance();
const records = [];
const install2D = eval(FS.open('drawing-smoke-2d.js').read() + '\ninstallDrawingSmoke2D;');
const log = message => System.out.println('[Drawing smoke] ' + message);
function report(id, detail) {
    records.push({ id: id, detail: String(detail) });
    log(id + ': ' + detail);
}
function check(id, condition) {
    if (!condition) throw new Error('Assertion failed: ' + id);
    report(id, 'API assertion passed; visual verification separate');
}
function main(action) {
    let failure;
    Client.runOnMainThread(JavaWrapper.methodToJava(() => {
        try { action(); } catch (error) { failure = error; }
    }), true, 10000);
    if (failure) throw failure;
}
function commands(lines) {
    const server = mc.getSingleplayerServer();
    const done = new AtomicBoolean(false);
    server.execute(JavaWrapper.methodToJava(() => {
        try {
            for (const line of lines) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), line.replaceAll('JsMacrosSmoke', String(Player.getPlayer().getName().getString())));
        } finally {
            done.set(true);
        }
    }));
    for (let i = 0; !done.get() && i < 400; i++) Client.waitTick();
    check('fixture.server-commands-completed', done.get());
}
function capture(name) {
    Client.waitTick(30);
    const done = new AtomicBoolean(false);
    main(() => Player.takeScreenshot('drawing-smoke-output', name + '.png', JavaWrapper.methodToJava(result => {
        report('capture.' + name, result.getString());
        done.set(true);
    })));
    for (let i = 0; !done.get() && i < 200; i++) Client.waitTick();
    check('capture-completed.' + name, done.get());
}

// This fixture edits terrain and game mode, so it must never run in a normal world.
if (!new File(mc.gameDirectory, '.jsmacros-drawing-smoke').isFile()) {
    throw new Error('Run this fixture only in the isolated drawing-smoke client.');
}
Hud.clearDraw2Ds();
Hud.clearDraw3Ds();
while (!World.isWorldLoaded()) Client.waitTick();
if (mc.getSingleplayerServer() === null) throw new Error('Drawing fixture requires an isolated integrated server.');
commands([
    'gamemode creative JsMacrosSmoke',
    'difficulty peaceful',
    'time set noon',
    'weather clear',
    'tp JsMacrosSmoke 0.5 199 -6.5 0 0',
    'kill @e[type=minecraft:armor_stand,tag=jsmacros_render_smoke]',
    'kill @e[type=minecraft:wandering_trader]',
    'kill @e[type=minecraft:trader_llama]'
]);
Client.waitTick(60);
commands([
    'fill -12 198 -12 12 198 24 minecraft:white_concrete',
    'fill -12 199 -12 12 210 24 minecraft:air',
    'tp JsMacrosSmoke 0.5 199 -6.5 0 0',
    'summon minecraft:armor_stand 4 199 5 {Tags:["jsmacros_render_smoke"],NoGravity:1b}'
]);
Client.waitTick(20);

const hud = Hud.createDraw2D();
let hudState;
hud.setOnInit(JavaWrapper.methodToJava(draw => {
    hudState = install2D(draw, (id, detail) => report('hud.' + id, detail));
}));
hud.setOnFailInit(JavaWrapper.methodToJava(error => report('FAIL.hud-init', error)));
main(() => hud.register());
capture('01-hud-elements');
main(() => {
    hudState.updateCheckerboard();
    mc.player.getCooldowns().addCooldown(hudState.elements.worn.getItem().getRaw(), 200);
});
capture('02-hud-dynamic-texture');
main(() => mc.player.getCooldowns().removeCooldown(mc.player.getCooldowns().getCooldownGroup(hudState.elements.worn.getItem().getRaw())));
const originalHudWidth = hud.getWidth();
main(() => Client.getGameOptions().setGuiScale(3));
Client.waitTick(10);
check('hud.logical-resize', hud.getWidth() !== originalHudWidth);
capture('03-hud-resize');
main(() => Client.getGameOptions().setGuiScale(2));

const lower = Hud.createDraw2D();
const upper = Hud.createDraw2D();
lower.setZIndex(-20);
upper.setZIndex(20);
lower.setOnInit(JavaWrapper.methodToJava(d => d.addRect(20, 20, 100, 90, 0xFF4444)));
upper.setOnInit(JavaWrapper.methodToJava(d => d.addRect(60, 50, 140, 120, 0x44FFFF)));
main(() => { hud.unregister(); upper.register(); lower.register(); });
report('hud.cross-overlay-z', 'cyan z=20 must cover red z=-20 regardless of reverse registration order');
capture('04-cross-overlay-order');
main(() => { lower.unregister(); upper.unregister(); });

const screen = Hud.createScreen('Drawing smoke: script screen', false);
screen.setOnInit(JavaWrapper.methodToJava(d => {
    install2D(d, (id, detail) => report('screen.' + id, detail));
    const y = d.getHeight() - 24;
    const noop = JavaWrapper.methodToJava(() => {});
    d.addButton(8, y, 75, 20, 'Button', noop);
    d.addTextInput(90, y, 100, 20, 'Input', noop).setText('SDL text');
    d.addSlider(200, y, 120, 20, 'Slider', 0.65, noop);
    d.addCheckbox(335, y, 100, 20, 'Checked', true, noop);
}));
main(() => Hud.openScreen(screen));
capture('05-script-screen');
main(() => Hud.openScreen(null));

const world = Hud.createDraw3D();
function surface(id, x, y, z, configure, content) {
    let builder = world.surfaceBuilder().pos(x + 3, y, z).rotation(0, 180, 0).size(3, 1.8).minSubdivisions(180).renderBack(true).cull(false);
    if (configure) builder = configure(builder);
    const result = builder.buildAndAdd();
    result.setOnInit(JavaWrapper.methodToJava(d => {
        d.addRect(0, 0, d.getWidth(), d.getHeight(), 0x182030, 200);
        d.addText(id, 6, 5, 0xFFDD66, true);
        content(d);
    }));
    result.setOnFailInit(JavaWrapper.methodToJava(error => report('FAIL.surface.' + id, error)));
    result.init();
    report('world.surface.' + id, 'submitted; verify against labeled screenshot');
    return result;
}
const surfaces = [];
let worldTool;
function zOrderSurface(id, x, y, z, cull) {
    const result = world.surfaceBuilder().pos(x + 3, y, z).rotation(0, 180, 0).size(3, 1.8).minSubdivisions(180).renderBack(true).cull(cull).buildAndAdd();
    result.setOnInit(JavaWrapper.methodToJava(d => {
        const white = 0xFFFFFFFF | 0;
        d.addRect(0, 0, d.getWidth(), d.getHeight(), 0x102038, 160, 0, 0);
        d.addText(id, 10, 8, 0xFFDD66, 9, true);
        d.addImage(18, 30, 115, 58, 1, 220, white, 'minecraft:textures/block/stone.png', 0, 0, 16, 16, 16, 16, 0);
        d.addText('text z2 over image z1', 25, 52, white, 2, true);
        d.addText('lower text z3', 25, 98, white, 3, true);
        d.addRect(18, 88, 150, 112, 0xFF3366, 240, 0, 4);
        d.addRect(170, 30, 265, 92, 0xFF3333, 255, 0, 5);
        const orderedImage = d.addImage(182, 38, 72, 46, 5, 255, white, 'minecraft:textures/block/stone.png', 0, 0, 16, 16, 16, 16, 0);
        check('world.image-constructor-z.' + id, orderedImage.getZIndex() === 5);
        d.addLine(174, 61, 260, 61, 0x66FF66, 5, 4, 0);
        d.addText('same z: line > image > rect', 160, 98, 0xFFFF66, 6, true);
        d.addText('hidden text', 20, 126, white, 7, false);
        d.addRect(16, 118, 145, 142, 0x3366FF, 255, 0, 7);
        d.addRect(160, 118, 285, 142, 0x3366FF, 255, 0, 8);
        d.addText('rect then text', 165, 126, white, 8, false);
        d.addText('same z no-shadow: text->rect hidden; rect->text visible', 12, 154, 0xFFFF66, 9, false);
    }));
    result.setOnFailInit(JavaWrapper.methodToJava(error => report('FAIL.surface.' + id, error)));
    result.init();
    report('world.surface.' + id, 'z0 translucent background; mixed text/image and text/rect z directions; same-z rect/image/line and text/rect insertion fixtures');
    return result;
}
function zTextOrderSurface() {
    const result = world.surfaceBuilder().pos(5, 205.4, 9).rotation(0, 180, 0).size(3, 1.8).minSubdivisions(180).renderBack(true).cull(true).buildAndAdd();
    result.setOnInit(JavaWrapper.methodToJava(d => {
        const white = 0xFFFFFFFF | 0;
        d.addRect(0, 0, d.getWidth(), d.getHeight(), 0x102038, 160, 0, 0);
        d.addText('same-z shadow regression', 10, 8, 0xFFDD66, 20, true);
        d.addText('shadow hidden', 16, 34, white, 10, true);
        d.addRect(12, 27, 142, 51, 0xCC3366, 255, 0, 10);
        d.addText(Chat.createTextHelperFromJSON('{"text":"bold hidden","bold":true}'), 16, 67, white, 11, true);
        d.addRect(12, 60, 142, 84, 0xCC3366, 255, 0, 11);
        d.addText(Chat.createTextHelperFromJSON('{"text":"underline hidden","underlined":true}'), 16, 100, white, 12, true);
        d.addRect(12, 93, 142, 117, 0xCC3366, 255, 0, 12);
        d.addRect(158, 27, 286, 51, 0x3366CC, 255, 0, 13);
        d.addText('visible text', 162, 34, white, 13, false);
        d.addText('hidden text', 162, 67, white, 14, false);
        d.addRect(158, 60, 286, 84, 0x3366CC, 255, 0, 14);
        d.addText('opaque rectangles must fully hide preceding text', 12, 136, 0xFFFF66, 21, false);
        d.addText('including shadow, bold, and underline glyphs', 12, 152, 0xFFFF66, 21, false);
    }));
    result.setOnFailInit(JavaWrapper.methodToJava(error => report('FAIL.surface.shadow-z-order', error)));
    result.init();
    report('world.surface.shadow-z-order', 'same-z opaque Text->Rect regressions for shadow, bold shadow, and underline; plus no-shadow directions');
    return result;
}
function cloudSurface() {
    const result = world.surfaceBuilder().pos(5, 193, 9).rotation(0, 180, 0).size(3, 1.8).minSubdivisions(180).renderBack(true).cull(false).buildAndAdd();
    result.setOnInit(JavaWrapper.methodToJava(d => {
        const white = 0xFFFFFFFF | 0;
        d.addRect(0, 0, d.getWidth(), d.getHeight(), 0x182030, 180, 0, 0);
        d.addText('cloud / item-image order', 8, 8, 0xFFDD66, 20, true);
        d.addImage(22, 36, 105, 105, 10, 255, white, 'minecraft:textures/block/stone.png', 0, 0, 16, 16, 16, 16, 0);
        d.addItem(60, 72, 11, 'minecraft:diamond', false);
        d.addItem(170, 72, 12, 'minecraft:diamond', false);
        d.addImage(145, 36, 105, 105, 13, 220, white, 'minecraft:textures/block/stone.png', 0, 0, 16, 16, 16, 16, 0);
        d.addText('image z10 / item z11', 16, 150, white, 21, true);
        d.addText('item z12 / image z13', 145, 150, white, 21, true);
    }));
    result.setOnFailInit(JavaWrapper.methodToJava(error => report('FAIL.surface.cloud-order', error)));
    result.init();
    report('world.surface.cloud-order', 'cull=false surface compares both image/item z directions while crossing the cloud layer');
    return result;
}
surfaces.push(surface('text', -5, 203, 9, null, d => {
    d.addText('Plain / shadow', 12, 30, 0xFFFFFF, true, 2, 0);
    d.addText(Chat.createTextHelperFromJSON('{"text":"Formatted","bold":true,"color":"aqua"}'), 12, 62, 0xFFFFFF, 4, false, 2, -8);
}));
surfaces.push(surface('rect / line', -1.5, 203, 9, null, d => {
    d.addRect(15, 35, 140, 120, 0xFF4444, 150, 15, 1);
    d.addRect(65, 60, 210, 145, 0x44FFFF, 150, -12, 2);
    d.addLine(10, 35, 260, 145, 0xFFFF66, 3, 4, 0);
}));
surfaces.push(surface('image UV / alpha', 2, 203, 9, null, d => {
    d.addImage(20, 30, 100, 100, 1, 255, 0xFFFFFF, 'minecraft:textures/block/stone.png', 0, 0, 16, 16, 16, 16, 0);
    d.addImage(140, 45, 90, 90, 2, 128, 0x66FFFF, 'minecraft:textures/block/stone.png', 0, 0, 8, 8, 16, 16, 20);
}));
surfaces.push(surface('items / overlay', -5, 200.7, 9, null, d => {
    const tool = d.addItem(30, 40, 'minecraft:diamond_pickaxe', true, 4, 0);
    worldTool = tool;
    tool.getItem().getCreative().setDamage(1000);
    tool.getItem().getCreative().addEnchantment('minecraft:efficiency', 3);
    d.addItem(230, 42, 'minecraft:chest', true, 3, 0);
    d.itemBuilder().pos(150, 40).item('minecraft:grass_block', 27).overlayVisible(true).scale(4).rotation(25).buildAndAdd();
}));
surfaces.push(surface('nested drawing', -1.5, 200.7, 9, null, d => {
    const child = Hud.createDraw2D();
    child.setOnInit(JavaWrapper.methodToJava(c => {
        c.addRect(0, 0, 80, 55, 0xAA44FF, 160);
        c.addText('nested', 5, 20, 0xFFFFFF, true);
    }));
    d.addDraw2D(child, 40, 40, 150, 100).setRotation(20);
}));
surfaces.push(surface('billboard / pivot', 2, 200.7, 9, b => b.rotation(20, 30, 15).rotateCenter(true).rotateToPlayer(true), d => {
    d.addText('Faces camera', 15, 45, 0xFFFFFF, true, 2, 0);
}));

world.addBox(-5, 199, 5, -4, 200, 6, 0x55FF55, 0x55FF55, false, false);
const filled = world.boxBuilder().pos(-3, 199, 5, -2, 200, 6).color(0xFF5555, 255).fillColor(0xFF5555, 100).fill(true).cull(true).buildAndAdd();
const alphaBox = world.boxBuilder().pos(0, 0, 0, 1, 1, 1).color(0x00112233, 0x40).fillColor(0x7F112233, 0x40).build();
check('world.box-builder-explicit-alpha', alphaBox.color === (0x40112233 | 0) && alphaBox.fillColor === (0x40112233 | 0));
alphaBox.setFillColor(0x7F112233, 0x40);
check('world.box-set-fill-alpha', alphaBox.fillColor === (0x40112233 | 0));
const point = world.addPoint(0, 199.7, 5, 0.12, 0xFFFF55, 255, false);
check('world.point-color-alpha', point.color === (0xFFFFFF55 | 0) && point.fillColor === (0xFFFFFF55 | 0));
world.lineBuilder().pos(-5, 201, 6, 5, 201, 6).color(0x44FFFF, 190).cull(false).buildAndAdd();
world.addLine(-5, 201.5, 6, 5, 201.5, 6, 0xFF55FF, 160, true);
check('world.trace-builder.always-on-top', world.traceLineBuilder().alwaysOnTop(true).build().isAlwaysOnTop());
const trace = world.traceLineBuilder().pos(3, 201, 7).color(0xFFFF55, 180).alwaysOnTop(true).buildAndAdd();
const target = Array.from(World.getEntities()).find(entity => entity.getType() === 'minecraft:armor_stand');
check('world.entity-trace-target', target !== undefined);
if (target) world.entityTraceLineBuilder().entity(target).yOffset(1.5).color(0xFF8800, 255).alwaysOnTop(false).buildAndAdd();
report('world.geometry', 'outline/fill boxes, point, alpha lines, depth variants, positional and entity traces submitted');
main(() => world.register());
capture('06-world-front');
main(() => mc.player.getCooldowns().addCooldown(worldTool.getItem().getRaw(), 200));
capture('06a-world-cooldown');
main(() => mc.player.getCooldowns().removeCooldown(mc.player.getCooldowns().getCooldownGroup(worldTool.getItem().getRaw())));

const bound = surface('entity offset', 0, 0, 0, b => b.bindToEntity(Player.getPlayer()).boundOffset(5, 6.4, 15.5), d => d.addText('Offset above player', 10, 50, 0xFFFFFF, true, 1.5, 0));
const frontOnly = surface('front only', -5, 205.4, 9, b => b.rotation(0, 180, 0).renderBack(false), d => d.addText('Hidden from back', 10, 45, 0xFFFFFF, true, 1.5, 0));
const twoSided = surface('two sided', -1.5, 205.4, 9, b => b.rotation(0, 180, 0).renderBack(true), d => d.addText('Visible from back', 10, 45, 0xFFFFFF, true, 1.5, 0));
surfaces[0].setWorldLight();
surfaces[1].setLight(0, 0);
surfaces[2].setFullBrightLight();
report('world.surface.light-modes', 'world light, zero light, full bright must be compared');
capture('07-surface-options');
surfaces[0].cull = true;
surfaces[3].cull = true;
commands(['fill -6 199 6 6 204 6 minecraft:stone']);
report('world.depth-occlusion', 'text and item surfaces cull=true must be hidden; other see-through surfaces remain visible');
capture('07a-depth-tested');
surfaces[0].cull = false;
surfaces[3].cull = false;
capture('07b-see-through');
commands(['fill -6 199 6 6 204 6 minecraft:air']);
commands(['tp JsMacrosSmoke 0.5 199 23.5 180 0']);
capture('08-world-backfaces');
commands(['tp JsMacrosSmoke 0.5 199 -6.5 0 0']);

world.removeDraw2D(bound);
world.removeDraw2D(frontOnly);
world.removeDraw2D(twoSided);
const zOrderSeeThrough = zOrderSurface('z order see through', -5, 205.4, 9, false);
const zOrderDepthTested = zOrderSurface('z order depth tested', -1.5, 205.4, 9, true);
zTextOrderSurface();
report('world.surface-z-order', 'verify mixed-type directions, same-z geometry insertion, no-shadow text/rect directions, and opaque shadow-text regressions');
capture('13-surface-z-order');
commands(['tp JsMacrosSmoke 0.5 199 23.5 180 0']);
report('world.surface-z-order-back', 'renderBack=true z-order panels must preserve the labeled equal-z cases from the reverse view');
capture('13a-surface-z-order-back');
commands(['tp JsMacrosSmoke 0.5 199 -6.5 0 0']);
commands(['fill -12 199 6 12 210 6 minecraft:stone']);
report('world.surface-z-order-cull', 'see-through panel remains visible; cull=true panel is hidden by stone');
capture('14-surface-z-order-cull');
commands(['fill -12 199 6 12 210 6 minecraft:air']);

cloudSurface();
world.boxBuilder().pos(-4, 191, 5, -3, 192, 6).color(0x55FF55, 220).fillColor(0x55FF55, 90).fill(true).cull(false).buildAndAdd();
world.lineBuilder().pos(-5, 192, 6, 5, 192, 6).color(0xFF55FF, 220).cull(false).buildAndAdd();
world.traceLineBuilder().pos(3, 192, 8).color(0xFFFF55, 255).alwaysOnTop(false).buildAndAdd();
commands([
    'fill -12 190 -12 12 210 24 minecraft:air',
    'fill -12 189 -12 12 189 24 minecraft:white_concrete',
    'tp JsMacrosSmoke 0.5 190 -6.5 0 0'
]);
const videoOptions = Client.getGameOptions().getVideoOptions();
const originalCloudsMode = videoOptions.getCloudsMode();
try {
    main(() => videoOptions.setCloudsMode('fancy'));
    check('world.clouds.fancy-mode', videoOptions.getCloudsMode() === 'fancy');
    report('world.clouds.fancy', 'cloud layer must be compared against cull=false surface, box, line, and depth-tested trace');
    Client.waitTick(100);
    capture('15-clouds-fancy');
    main(() => videoOptions.setCloudsMode('off'));
    check('world.clouds.off-mode', videoOptions.getCloudsMode() === 'off');
    report('world.clouds.off', 'same camera and Draw3D scene with clouds disabled');
    Client.waitTick(20);
    capture('16-clouds-off');
} finally {
    main(() => videoOptions.setCloudsMode(originalCloudsMode));
    report('world.clouds.restore', 'restored cloud mode ' + originalCloudsMode);
}
commands(['tp JsMacrosSmoke 0.5 199 -6.5 0 0']);

world.removeBox(filled);
world.reAddElement(filled);
world.removeTraceLine(trace);
world.reAddElement(trace);
check('world.lifecycle.remove-readd', world.getBoxes().contains(filled) && world.getTraceLines().contains(trace));
filled.setFillColor(0x44FF44, 200);
surfaces[0].setRotations(10, 20, 5);
surfaces[0].setSizes(3.3, 1.9);
surfaces[0].setMinSubdivisions(220);
surfaces[0].init();
report('world.dynamic-mutators', 'fill color, surface rotation, dimensions and subdivisions changed');
capture('09-world-mutated');
main(() => world.unregister());
capture('10-world-unregistered');
main(() => world.register());
capture('11-world-reregistered');
world.clear();
check('world.lifecycle.clear', world.getBoxes().isEmpty() && world.getLines().isEmpty() && world.getDraw2Ds().isEmpty() && world.getTraceLines().isEmpty() && world.getEntityTraceLines().isEmpty());
capture('12-world-cleared');
main(() => hud.register());
check('no-initialization-failures', !records.some(record => record.id.startsWith('FAIL.')));
log('COMPLETE: inspect all PNGs and render logs; API success alone is not visual success.');
FS.open('drawing-smoke-output/cases.json').write(JSON.stringify(records, null, 2));
