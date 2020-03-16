package org.gradle.profiler.internal;

import java.lang.instrument.Instrumentation;

public class IdeaProfilerAgent {

    public static void premain(String arguments, Instrumentation instrumentation) {

        try {
            IdeaProfilerLogger.log("IntelliJ Platform sync time profiler started");
            IdeaProfilerLogger.log("To profile the Gradle daemon, append the following entry to the JVM arguments: -Dgradle.profiler=enabled. You can do that by adding the following entry to the gradle.properties file: org.gradle.jvmargs=-Dgradle.profiler.enabled");
            IdeaSync.init();

            new IdeaProfilerThread().start();
            AndroidStudioSyncTimerAdvice.register(instrumentation);

        } finally {
        }
    }
}
