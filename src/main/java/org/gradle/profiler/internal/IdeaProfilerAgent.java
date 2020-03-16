package org.gradle.profiler.internal;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class IdeaProfilerAgent {

    public static void premain(String arguments, Instrumentation instrumentation) {
        File asyncProfiler = new File(System.getProperty("user.home") + "/async-profiler/profiler.sh");
        if (!asyncProfiler.exists()) {
            IdeaProfilerLogger.log("ERROR async profiler not available at " + asyncProfiler.getParentFile().getAbsolutePath());
            return;
        }

        IdeaProfilerLogger.log("IntelliJ Platform sync time profiler started");
        new ProfilerThread().start();
        AndroidStudioSyncTimerAdvice.register(instrumentation);
    }
}
