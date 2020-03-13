package org.gradle.profiler.internal;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

/**
 * This is an agent for get execution time of all running methods
 */
public class IdeaProfilerAgent {

    public static void premain(String arguments, Instrumentation instrumentation) {

        try {
            IdeaProfilerLogger.log("Gradle sync time profiler loaded");
            new IdeaProfilerThread().start();

            new AgentBuilder.Default()
                    .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                    .type(ElementMatchers.nameContains("GradleSyncState"))
                    .transform((builder, typeDescription, classLoader, module) -> builder
                            .method(ElementMatchers.nameContains("syncStarted").or(ElementMatchers.nameContains("syncFinished")))
                            .intercept(Advice.to(IdeaProfilerAgentTimerAdvice.class))
                    ).installOn(instrumentation);
        } finally {
            IdeaProfilerLogger.close();
        }
    }
}
