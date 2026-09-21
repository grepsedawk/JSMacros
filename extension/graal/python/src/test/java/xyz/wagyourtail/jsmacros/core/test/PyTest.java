package xyz.wagyourtail.jsmacros.core.test;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.EventLockWatchdog;
import xyz.wagyourtail.jsmacros.core.event.IEventListener;
import xyz.wagyourtail.jsmacros.core.event.impl.EventCustom;
import xyz.wagyourtail.jsmacros.core.language.EventContainer;
import xyz.wagyourtail.jsmacros.test.BaseTest;
import xyz.wagyourtail.jsmacros.test.stubs.CoreInstanceCreator;
import xyz.wagyourtail.jsmacros.test.stubs.EventRegistryStub;
import xyz.wagyourtail.jsmacros.test.stubs.ProfileStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PyTest extends BaseTest {

    @Override
    public String getLang() {
        return "py";
    }

    @Language("py")
    private final String TEST_SCRIPT = """
            import json
            order = []
            
            def a():
                order.append(1)
                print(1)
                event.putString("test", json.dumps(order))
            
            JavaWrapper.methodToJavaAsync(5, a).run()
            
            def b():
                order.append(2)
                print(2)
                event.putString("test", json.dumps(order))
            
            JavaWrapper.methodToJavaAsync(5, b).run()
            
            def c():
                order.append(3)
                print(3)
                event.putString("test", json.dumps(order))
            
            JavaWrapper.methodToJavaAsync(6, c).run()
            order.append(0)
            print(0)
            JavaWrapper.deferCurrentTask(-2) # change priority of this thread from 5 -> 3
            event.putString("test", json.dumps(order))
            """;

    @Test
    public void test() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT);
        assertEquals("[0, 3, 1, 2]", custom.getString("test"));
    }

    @Language("py")
    private final String TEST_SCRIPT_2 = """
            import json
            j = []
            a_steps = []
            b_steps = []
            a_done = False
            b_done = False
            
            def a():
                global a_done
                for atime in range(0, 500, 100):
                    a_steps.append(atime)
                    j.append(f'a {atime}')
                    Time.sleep(100)
                a_done = True
            
            JavaWrapper.methodToJavaAsync(a).run()

            def b():
                global b_done
                for btime in range(0, 550, 110):
                    b_steps.append(btime)
                    j.append(f'b {btime}')
                    Time.sleep(110)
                b_done = True

            JavaWrapper.methodToJavaAsync(b).run()
            JavaWrapper.deferCurrentTask(-1)

            while not a_done or not b_done:
                JavaWrapper.deferCurrentTask()

            j.append('c')
            event.putString("test", json.dumps(j))
            event.putString("aSteps", json.dumps(a_steps))
            event.putString("bSteps", json.dumps(b_steps))
            """;

    @Test
    public void test2() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT_2, 5000);
        assertEquals("[0, 100, 200, 300, 400]", custom.getString("aSteps"));
        assertEquals("[0, 110, 220, 330, 440]", custom.getString("bSteps"));
        assertEquals(11, custom.getString("test").split(",").length);
        assertTrue(custom.getString("test").endsWith("\"c\"]"));
    }

    @Language("py")
    private final String TEST_SCRIPT_3 = """
            import json

            start = Time.time()
            a = []
            def long():
                a.append("long started")
                Time.sleep(5000)
                a.append('long finished')
                done()
            
            def rapid():
                a.append('rapid 1')
                Time.sleep(500)
                a.append('rapid 2')
                Time.sleep(500)
                a.append('rapid 3')
                Time.sleep(500)
                a.append('rapid 4')
                done()
            
            isDone = False
            def done():
                global isDone
                if len(a) == 6:
                    event.putString("test", json.dumps(a))
                    event.putDouble("time", Time.time() - start)
                    isDone = True
            
            def runAsync(fn):
                JavaWrapper.methodToJavaAsync(fn).run()
            
            runAsync(long)
            while len(a) == 0:
                JavaWrapper.deferCurrentTask()
            runAsync(rapid)
            while not isDone:
                JavaWrapper.deferCurrentTask()
            """;

    @Test
    public void test3() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT_3);
        System.out.println("Time: " + custom.getDouble("time"));
        assertEquals("[\"long started\", \"rapid 1\", \"rapid 2\", \"rapid 3\", \"rapid 4\", \"long finished\"]", custom.getString("test"));
        assertTrue(custom.getDouble("time") > 5000);
        assertTrue(custom.getDouble("time") < 7000);
    }

}
