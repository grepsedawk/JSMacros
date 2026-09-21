package xyz.wagyourtail.jsmacros.core.test;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import xyz.wagyourtail.jsmacros.core.event.impl.EventCustom;
import xyz.wagyourtail.jsmacros.test.BaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsTest extends BaseTest {

    @Override
    public String getLang() {
        return "js";
    }

    @Language("js")
    private final String TEST_SCRIPT = """
            var order = []
            JavaWrapper.methodToJavaAsync(5, () => {
                order.push(1);
                event.putString("test", JSON.stringify(order));
            }).run();
            JavaWrapper.methodToJavaAsync(5, () => {
                order.push(2);
                event.putString("test", JSON.stringify(order));
            }).run();
            JavaWrapper.methodToJavaAsync(6, () => {
                order.push(3);
                event.putString("test", JSON.stringify(order));
            }).run();
            order.push(0);
            JavaWrapper.deferCurrentTask(-2) // change priority of this thread from 5 -> 3
            event.putString("test", JSON.stringify(order));
        """;

    @Test
    public void test() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT);
        assertEquals("[0,3,1,2]", custom.getString("test"));
    }

    @Language("js")
    private final String TEST_SCRIPT_2 = """
            var j = []
            var aSteps = []
            var bSteps = []
            var aDone = false
            var bDone = false
            JavaWrapper.methodToJavaAsync(() => {
                for (let atime = 0; atime < 500; atime += 100) {
                    aSteps.push(atime)
                    j.push(`a ${atime}`)
                    Time.sleep(100)
                }
                aDone = true
            }).run();
            JavaWrapper.methodToJavaAsync(() => {
                for (let btime = 0; btime < 550; btime += 110) {
                    bSteps.push(btime)
                    j.push(`b ${btime}`)
                    Time.sleep(110)
                }
                bDone = true
            }).run();
            JavaWrapper.deferCurrentTask(-1)
            while (!aDone || !bDone) {
                JavaWrapper.deferCurrentTask()
            }
            j.push('c');
            event.putString("test", JSON.stringify(j))
            event.putString("aSteps", JSON.stringify(aSteps))
            event.putString("bSteps", JSON.stringify(bSteps))
            """;

    @Test
    public void test2() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT_2);
        assertEquals("[0,100,200,300,400]", custom.getString("aSteps"));
        assertEquals("[0,110,220,330,440]", custom.getString("bSteps"));
        assertEquals(11, custom.getString("test").split(",").length);
        assertTrue(custom.getString("test").endsWith("\"c\"]"));
    }

    @Language("js")
    private final String TEST_SCRIPT_3 = """
            const start = Time.time()
            let a = []
            function long() {
                a.push("long started");
                Time.sleep(5000);
                a.push('long finished');
                done()
            }
            function rapid() {
                a.push('rapid 1')
                Time.sleep(500);
                a.push('rapid 2')
                Time.sleep(500);
                a.push('rapid 3')
                Time.sleep(500);
                a.push('rapid 4')
                done()
            }
            var isDone = false
            function done() {
                if (a.length == 6) {
                    event.putString("test", JSON.stringify(a))
                    event.putDouble("time", Time.time() - start)
                    isDone = true
                }
            }
            function runAsync(fn) {
                JavaWrapper.methodToJavaAsync(fn).run()
            }
            runAsync(long)
            while (a.length === 0) {
                JavaWrapper.deferCurrentTask()
            }
            runAsync(rapid)
            while (!isDone) {
                JavaWrapper.deferCurrentTask()
            }
            """;

    @Test
    public void test3() throws InterruptedException {
        EventCustom custom = runTestScript(TEST_SCRIPT_3, 7000);
        System.out.println("Time: " + custom.getDouble("time"));
        assertEquals("[\"long started\",\"rapid 1\",\"rapid 2\",\"rapid 3\",\"rapid 4\",\"long finished\"]", custom.getString("test"));
        assertTrue(custom.getDouble("time") > 5000);
        assertTrue(custom.getDouble("time") < 7000);
    }

}
