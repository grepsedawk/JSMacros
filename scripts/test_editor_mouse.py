"""Run the 26.3 editor's actual click handler without starting Minecraft.

Only font lookup, cursor storage, superclass dispatch, and menu display are
stubbed. Button IDs follow Minecraft 26.3 / SDL: left=1, middle=2, right=3.
The existing CI unittest discovery runs this with its installed JDK.
"""
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EDITOR = ROOT / "src/versions/26.3/client/java/xyz/wagyourtail/jsmacros/client/gui/screens/EditorScreen.java"

JAVA_PREFIX = """
class InputConstants {
    static final int MOUSE_BUTTON_LEFT = 1, MOUSE_BUTTON_MIDDLE = 2, MOUSE_BUTTON_RIGHT = 3;
}
record MouseButtonEvent(double x, double y, int button, boolean hasShiftDown) {}
class BaseScreenStub {
    Object minecraft = new Object(), overlay;
    boolean handled;
    void setFocused(Object focused) {}
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { return handled; }
}
public class EditorClickProbe extends BaseScreenStub {
    static class Cursor {
        int startIndex = 10, endIndex = 20, dragStartIndex = 10;
        int startLineIndex, endLineIndex, arrowLineIndex;
        boolean arrowEnd;
        void updateStartIndex(int index, String text) { startIndex = index; }
        void updateEndIndex(int index, String text) { endIndex = index; }
    }
    static class History { String current = "a selection in the editor"; }
    Cursor cursor = new Cursor();
    History history = new History();
    int index, menus;
    int getIndexPosition(double x, double y) { return index; }
    void selectWordAtCursor() { throw new AssertionError("Unexpected word selection"); }
    void openRClickMenu(int index, int x, int y) { menus++; }
"""

JAVA_SUFFIX = """
    public static void main(String[] args) {
        EditorClickProbe probe = new EditorClickProbe();
        int button = Integer.parseInt(args[0]);
        boolean shift = Boolean.parseBoolean(args[1]);
        probe.index = Integer.parseInt(args[2]);
        probe.handled = Boolean.parseBoolean(args[3]);
        if (Boolean.parseBoolean(args[4])) probe.overlay = new Object();
        probe.mouseClicked(new MouseButtonEvent(50, 50, button, shift), false);
        System.out.println(probe.cursor.startIndex + "," + probe.cursor.endIndex + "," + probe.menus);
    }
}
"""


class EditorMouseTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.java = shutil.which("java")
        javac = shutil.which("javac")
        if not cls.java or not javac:
            raise RuntimeError("Editor mouse tests require a JDK on PATH")
        temporary = tempfile.TemporaryDirectory()
        cls.addClassCleanup(temporary.cleanup)
        cls.directory = Path(temporary.name)
        source = EDITOR.read_text()
        start = source.index("    @Override\n    public boolean mouseClicked(")
        end = source.index("\n    private void openRClickMenu", start)
        # Compile the source handler verbatim so either right-click check can regress.
        probe = cls.directory / "EditorClickProbe.java"
        probe.write_text(JAVA_PREFIX + source[start:end] + JAVA_SUFFIX)
        subprocess.run([javac, "-d", str(cls.directory), str(probe)],
                       check=True, capture_output=True, text=True, timeout=30)

    def click(self, button, *, shift=False, index=15, handled=False, overlay=False):
        result = subprocess.run(
            [self.java, "-ea", "-cp", str(self.directory), "EditorClickProbe",
             str(button), str(shift).lower(), str(index), str(handled).lower(), str(overlay).lower()],
            check=True, capture_output=True, text=True, timeout=15,
        )
        return tuple(int(value) for value in result.stdout.strip().split(","))

    def test_left_click_collapses_selection_without_menu(self):
        self.assertEqual(self.click(1), (15, 15, 0))

    def test_right_click_inside_selection_preserves_it_and_opens_menu(self):
        self.assertEqual(self.click(3), (10, 20, 1))

    def test_right_click_outside_selection_moves_cursor_and_opens_menu(self):
        self.assertEqual(self.click(3, index=25), (25, 25, 1))

    def test_shift_left_click_extends_selection_without_menu(self):
        self.assertEqual(self.click(1, shift=True), (10, 15, 0))

    def test_middle_click_does_not_open_menu(self):
        self.assertEqual(self.click(2), (15, 15, 0))

    def test_widget_handled_click_bypasses_editor(self):
        self.assertEqual(self.click(1, handled=True), (10, 20, 0))

    def test_existing_overlay_bypasses_editor(self):
        self.assertEqual(self.click(3, overlay=True), (10, 20, 0))


if __name__ == "__main__":
    unittest.main()
