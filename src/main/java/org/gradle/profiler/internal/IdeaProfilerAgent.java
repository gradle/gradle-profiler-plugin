package org.gradle.profiler.internal;

import java.lang.instrument.Instrumentation;

public class IdeaProfilerAgent {

    public static void premain(String arguments, Instrumentation instrumentation) {
        IdeaProfilerLogger.log("IntelliJ Platform sync time profiler started");
        new ProfilerThread().start();
        AndroidStudioSyncTimerAdvice.register(instrumentation);
    }
}
