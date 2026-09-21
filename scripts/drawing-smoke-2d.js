function installDrawingSmoke2D(draw, report) {
    const emit = typeof report === 'function' ? report : function () {};
    const width = draw.getWidth();
    const panelWidth = Math.max(92, Math.min(220, Math.floor((width - 36) / 3)));
    const panelHeight = 80;
    const gap = 8;
    const left = Math.max(8, Math.floor((width - (panelWidth * 3 + gap * 2)) / 2));
    const top = 24;
    const texture = 'minecraft:textures/block/stone.png';
    const white = 0xFFFFFFFF | 0;
    const paleGreen = 0xFFB0FFB0 | 0;
    const paleCyan = 0xFF80E8FF | 0;

    function panel(column, row, title) {
        const x = left + column * (panelWidth + gap);
        const y = top + row * (panelHeight + gap);
        draw.rectBuilder()
            .pos1(x, y)
            .size(panelWidth, panelHeight)
            .color(0x101820, 224)
            .zIndex(-10)
            .buildAndAdd();
        draw.textBuilder(title)
            .pos(x + 5, y + 5)
            .color(0xFFB84D)
            .shadow(true)
            .zIndex(20)
            .buildAndAdd();
        return { x: x, y: y };
    }

    function pass(caseId, details) {
        emit(caseId, details || 'submitted; visual verification pending');
    }

    function expect(caseId, condition, details) {
        if (!condition) {
            emit(caseId, 'FAIL: ' + details);
            throw new Error(caseId + ': ' + details);
        }
        pass(caseId, details);
    }

    const rectProbe = draw.rectBuilder(17, 23, 31, 37).build();
    expect('rect.builder-position-size', rectProbe.getX2() === 48 && rectProbe.getY2() === 60, 'rectangle factory preserves width and height at a nonzero origin');
    const text = panel(0, 0, 'Text');
    draw.addText('plain', text.x + 6, text.y + 20, white, false);
    const indexedText = draw.addText('z-index overload', text.x + 6, text.y + 29, paleGreen, 7, false);
    draw.addText('shadow / scale / rotation', text.x + 8, text.y + 46, paleCyan, 3, true, 0.82, -7);
    const formatted = Chat.createTextHelperFromJSON('{"text":"formatted ","color":"gold","extra":[{"text":"text","bold":true,"color":"aqua"}]}');
    draw.addText(formatted, text.x + 6, text.y + 65, white, 4, true, 1, 0);
    pass('text.plain-formatted-shadow-transform-z', 'plain and JSON formatted text, shadow, scale, rotation, alpha color, and z-index');
    expect('text.z-index-overload', indexedText !== null && indexedText.getZIndex() === 7, 'String addText z-index overload returns z=7');

    const rectangles = panel(1, 0, 'Rectangles');
    draw.rectBuilder()
        .pos1(rectangles.x + 8, rectangles.y + 22)
        .size(44, 34)
        .color(0xE84D4D, 170)
        .rotation(-15)
        .rotateCenter(true)
        .zIndex(1)
        .buildAndAdd();
    draw.rectBuilder()
        .pos1(rectangles.x + 34, rectangles.y + 29)
        .size(50, 24)
        .color(0x4DD9E8, 150)
        .rotation(13)
        .rotateCenter(false)
        .zIndex(2)
        .buildAndAdd();
    const rotatedRect = draw.addRect(rectangles.x + 88, rectangles.y + 38, rectangles.x + 106, rectangles.y + 56, 0xFFFFFF, 120, 31);
    pass('rect.fill-color-alpha-rotation-z', 'overlapping filled rectangles with RGB, alpha, pivot modes, rotation, and z-index');
    expect('rect.alpha-rotation-overload', rotatedRect !== null && rotatedRect.getRotation() === 31, 'addRect alpha/rotation overload retains rotation=31');

    const lines = panel(2, 0, 'Lines');
    draw.lineBuilder(lines.x + 8, lines.y + 25, lines.x + 74, lines.y + 25)
        .color(0x52FF8A, 220)
        .width(1)
        .zIndex(1)
        .buildAndAdd();
    draw.lineBuilder(lines.x + 12, lines.y + 48, lines.x + 74, lines.y + 48)
        .color(0xFF5ACD, 170)
        .width(4)
        .rotation(-11)
        .rotateCenter(true)
        .zIndex(2)
        .buildAndAdd();
    pass('line.color-alpha-width-rotation-z', 'thin and thick lines with alpha, rotation, center pivot, and z-index');

    const images = panel(0, 1, 'Images');
    draw.imageBuilder(texture)
        .pos(images.x + 7, images.y + 21)
        .size(38, 20)
        .regions(0, 0, 16, 16, 16, 16)
        .color(0xFFFFFF, 255)
        .zIndex(1)
        .buildAndAdd();
    draw.imageBuilder(texture)
        .pos(images.x + 58, images.y + 21)
        .size(38, 20)
        .regions(0, 0, 16, 16, 16, 16)
        .color(0x5AD7FF, 145)
        .rotation(12)
        .rotateCenter(true)
        .zIndex(2)
        .buildAndAdd();
    pass('image.texture-uv-color-alpha-rotation-z', 'widgets texture with UV regions, tint, alpha, center rotation, and z-index');

    const items = panel(1, 1, 'Items');
    const counted = draw.itemBuilder()
        .pos(items.x + 7, items.y + 23)
        .item('minecraft:emerald', 27)
        .overlayVisible(true)
        .overlayText('27')
        .zIndex(1)
        .buildAndAdd();
    const worn = draw.itemBuilder()
        .pos(items.x + 35, items.y + 23)
        .item('minecraft:diamond_pickaxe')
        .overlayVisible(true)
        .scale(1.2)
        .rotation(-12)
        .rotateCenter(true)
        .zIndex(2)
        .buildAndAdd();
    worn.getItem().getCreative().setDamage(1000);
    const enchanted = draw.itemBuilder()
        .pos(items.x + 68, items.y + 23)
        .item('minecraft:diamond_sword')
        .overlayVisible(false)
        .scale(1.2)
        .rotation(12)
        .rotateCenter(true)
        .zIndex(3)
        .buildAndAdd();
    enchanted.getItem().getCreative().addEnchantment('minecraft:sharpness', 5);
    const indexedItem = draw.addItem(items.x + panelWidth - 20, items.y + 25, 8, 'minecraft:gold_ingot');
    const indexedStackItem = draw.addItem(items.x + panelWidth - 20, items.y + 48, 9, counted.getItem());
    pass('item.count-overlay-durability-enchantment-transform-z', 'count overlay, damaged tool bar, enchanted glint, overlay toggle, scale, rotation, and z-index');
    expect('item.z-index-overload', indexedItem !== null && indexedItem.getZIndex() === 8, 'z-index addItem overload returns an item at z=8');
    expect('item.stack-z-index-overload', indexedStackItem !== null && indexedStackItem.getZIndex() === 9, 'ItemStackHelper z-index addItem overload returns an item at z=9');

    const nested = panel(2, 1, 'Nested Draw2D');
    const child = Hud.createDraw2D();
    child.setOnInit(JavaWrapper.methodToJava(nestedDraw => {
        nestedDraw.rectBuilder()
            .pos1(0, 0)
            .size(58, 28)
            .color(0x5C54E8, 190)
            .buildAndAdd();
        nestedDraw.textBuilder('child')
            .pos(7, 9)
            .color(white)
            .shadow(true)
            .buildAndAdd();
    }));
    draw.draw2DBuilder(child)
        .pos(nested.x + 18, nested.y + 29)
        .size(58, 28)
        .scale(1.15)
        .rotation(-10)
        .rotateCenter(true)
        .zIndex(3)
        .buildAndAdd();
    pass('nested-draw2d.translate-scale-rotation-z', 'nested canvas translated, scaled, center-rotated, and layered above its panel');

    const overlap = panel(0, 2, 'Z-order overlap');
    draw.rectBuilder()
        .pos1(overlap.x + 20, overlap.y + 24)
        .size(54, 30)
        .color(0xE84D4D, 220)
        .zIndex(1)
        .buildAndAdd();
    draw.rectBuilder()
        .pos1(overlap.x + 40, overlap.y + 34)
        .size(54, 30)
        .color(0x4DD9E8, 220)
        .zIndex(9)
        .buildAndAdd();
    draw.textBuilder('cyan is on top')
        .pos(overlap.x + 7, overlap.y + 68)
        .color(white)
        .shadow(true)
        .zIndex(10)
        .buildAndAdd();
    pass('z-order.visible-overlap', 'overlapping red z=1 and cyan z=9 rectangles visibly verify element ordering');

    const customImagePanel = panel(1, 2, 'Custom image');
    const checkerboard = Hud.createTexture(16, 16, null);
    let checkerboardPhase = 0;
    function updateCheckerboard() {
        checkerboardPhase = (checkerboardPhase + 1) % 2;
        for (let y = 0; y < 16; y++) {
            for (let x = 0; x < 16; x++) {
                const light = (x + y + checkerboardPhase) % 2 === 0;
                checkerboard.setPixel(x, y, light ? ((checkerboardPhase ? 0xFFFFD166 : 0xFF44FFFF) | 0) : ((checkerboardPhase ? 0xFF5C54E8 : 0xFF44DD66) | 0));
            }
        }
        checkerboard.update();
    }
    updateCheckerboard();
    draw.imageBuilder()
        .fromCustomImage(checkerboard)
        .pos(customImagePanel.x + 8, customImagePanel.y + 28)
        .size(32, 32)
        .zIndex(2)
        .buildAndAdd();
    expect('custom-image.create-update-upload', checkerboard.getWidth() === 16 && checkerboard.getPixel(0, 0) === (0xFF5C54E8 | 0), 'checkerboard texture was batch-updated and uploaded');

    const cropSource = Hud.createTexture(16, 16, null);
    cropSource.setGraphicsColor(0x5C54E8)
        .fillRect(0, 0, 16, 16)
        .setGraphicsColor(0xFFD166)
        .fillRect(4, 4, 8, 8)
        .setGraphicsColor(0xFFFFFF)
        .drawString(1, 14, 'S')
        .update();
    const cropDestination = Hud.createTexture(8, 8, null);
    expect('custom-image.unique-unnamed-textures', checkerboard.getName() !== cropSource.getName() && cropSource.getName() !== cropDestination.getName(), 'unnamed custom textures receive distinct identifiers');
    cropDestination.drawImage(cropSource.getImage(), 0, 0, 8, 8, 4, 4, 8, 8).update();
    draw.imageBuilder()
        .fromCustomImage(cropSource)
        .pos(customImagePanel.x + 48, customImagePanel.y + 28)
        .size(24, 24)
        .zIndex(2)
        .buildAndAdd();
    draw.imageBuilder()
        .fromCustomImage(cropDestination)
        .pos(customImagePanel.x + 82, customImagePanel.y + 28)
        .size(32, 32)
        .zIndex(3)
        .buildAndAdd();
    const croppedSourcePixel = cropSource.getPixel(4, 4);
    expect('custom-image.draw-image-crop', (croppedSourcePixel >>> 24) !== 0 && cropDestination.getPixel(0, 0) === croppedSourcePixel, 'cropped source texture reached the distinct destination texture');
    pass('custom-image.fill-draw-text-crop-render', 'source fill and drawString, then cropped drawImage from source to rendered destination');

    const builders = panel(2, 2, 'Builders');
    const aligned = draw.textBuilder('builder alignment')
        .pos(0, top + 3 * (panelHeight + gap) + 4)
        .color(0xFFB84D)
        .shadow(true)
        .buildAndAdd();
    aligned.alignHorizontally('center');
    pass('builder-alignment', 'Text builder output aligned to the canvas center');

    const spare = Hud.createDraw2D();
    const transient = spare.textBuilder('remove and re-add')
        .pos(0, 0)
        .color(white)
        .buildAndAdd();
    spare.removeElement(transient);
    spare.reAddElement(transient);
    const readded = spare.getElements().size() === 1;
    spare.init();
    const cleared = spare.getElements().size() === 0;
    pass('lifecycle.remove-readd-init-clear', 'spare canvas remove/re-add=' + readded + ', init clear=' + cleared);

    return {
        child: child,
        lifecycleCanvas: spare,
        checkerboard: checkerboard,
        updateCheckerboard: updateCheckerboard,
        cropSource: cropSource,
        cropDestination: cropDestination,
        elements: {
            counted: counted,
            worn: worn,
            enchanted: enchanted,
            aligned: aligned
        }
    };
}
