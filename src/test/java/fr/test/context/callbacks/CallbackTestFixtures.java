package fr.test.context.callbacks;

import com.github.oxal.annotation.context.AfterContextLoad;
import com.github.oxal.annotation.context.BeforeContextLoad;
import com.github.oxal.context.Context;
import fr.test.context.base.Bean1;
import io.github.classgraph.ScanResult;

import java.util.ArrayList;
import java.util.List;

public class CallbackTestFixtures {

    // Static list to track execution of before callbacks
    public static final List<String> beforeCallbackExecutionOrder = new ArrayList<>();
    // Static list to track execution of after callbacks
    public static final List<String> afterCallbackExecutionOrder = new ArrayList<>();
    public static boolean beforeScanResultInjected = false;
    public static boolean afterBeanInjected = false;
    public static boolean afterContextInjected = false;

    /**
     * Resets all static trackers before each test.
     */
    public static void reset() {
        beforeCallbackExecutionOrder.clear();
        beforeScanResultInjected = false;
        afterCallbackExecutionOrder.clear();
        afterBeanInjected = false;
        afterContextInjected = false;
    }

    // --- Class with @BeforeContextLoad methods ---
    public static class BeforeCallbackTester {

        @BeforeContextLoad(order = 20)
        public void secondBefore() {
            beforeCallbackExecutionOrder.add("secondBefore");
        }

        @BeforeContextLoad(order = 10)
        public void firstBefore(ScanResult scanResult) {
            beforeCallbackExecutionOrder.add("firstBefore");
            if (scanResult != null) {
                beforeScanResultInjected = true;
            }
        }
    }

    // --- Class with @AfterContextLoad methods ---
    public static class AfterCallbackTester {

        @AfterContextLoad(order = 1)
        public void firstAfter(Context context, Bean1 bean1) {
            afterCallbackExecutionOrder.add("firstAfter");
            if (context != null) {
                afterContextInjected = true;
            }
            if (bean1 != null) {
                afterBeanInjected = true;
            }
        }

        @AfterContextLoad(order = 100)
        public void secondAfter() {
            afterCallbackExecutionOrder.add("secondAfter");
        }
    }
}
