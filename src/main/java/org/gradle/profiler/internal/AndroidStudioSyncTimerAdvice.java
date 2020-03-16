package org.gradle.profiler.internal;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

/**
 * Android studio has a different method than IntelliJ IDEA that performs the synchronizaton. We can directly hook into
 * this method with byte-buddy.
 */
public class AndroidStudioSyncTimerAdvice {

    private static final String SYNC_CLASS_NAME = "com.android.tools.idea.gradle.project.sync.GradleSyncState";
    private static final String SYNC_START_METHOD_NAME = "syncStarted";
    private static final String SYNC_STOP_METHOD_NAME = "syncFinished";

    public static void register(Instrumentation instrumentation) {
        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .type(ElementMatchers.nameContains(SYNC_CLASS_NAME))
                .transform((builder, typeDescription, classLoader, module) -> builder
                        .method(ElementMatchers.nameContains(SYNC_START_METHOD_NAME).or(ElementMatchers.nameContains(SYNC_STOP_METHOD_NAME)))
                        .intercept(Advice.to(AndroidStudioSyncTimerAdvice.class))
                ).installOn(instrumentation);
    }

    @Advice.OnMethodEnter
    static void enter(@Advice.Origin String method) {
        if (method.contains(SYNC_START_METHOD_NAME)) {
            IdeaSync.getInstance().syncStarted();
        }
    }

    @Advice.OnMethodExit
    static void exit(@Advice.Origin String method) {
        if (method.contains(SYNC_STOP_METHOD_NAME)) {
            IdeaSync.getInstance().syncFinished();
        }
    }
}
