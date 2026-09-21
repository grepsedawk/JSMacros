const System = Java.type('java.lang.System');
const log = message => System.out.println('[JsMacros compatibility] ' + message);

log('JavaScript engine loaded');

JsMacros.once('Tick', JavaWrapper.methodToJava(() => {
    log('Tick event received');
}));

JsMacros.on('Key', JavaWrapper.methodToJava(event => {
    log('Key: ' + event.key + ', action=' + event.action + ', modifiers=' + event.mods);
}));

JsMacros.once('JoinServer', JavaWrapper.methodToJava(() => {
    const player = Player.getPlayer();
    const position = player.getPos();
    log('World/player helpers: time=' + World.getTime() + ', position=' + position);
    log('Inventory slots: ' + Player.openInventory().getTotalSlots());

    const overlay = Hud.createDraw2D();
    overlay.setOnInit(JavaWrapper.methodToJava(draw => {
        draw.addText('JsMacros compatibility smoke', 8, 8, 0x55FF55, true);
    }));
    overlay.register();

    const draw = Hud.createDraw3D();
    draw.addBox(position.x - 1, position.y, position.z - 1,
        position.x + 1, position.y + 2, position.z + 1,
        0x55FF55, 0x2255FF55, false);
    draw.register();
    log('2D and 3D smoke drawings registered');
}));
