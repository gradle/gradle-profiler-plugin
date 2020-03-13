package org.gradle.profiler.internal;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public class IdeaProfilerAgent {

    public static void premain(String arguments, Instrumentation instrumentation) {

        try {
            IdeaProfilerLogger.log("IntelliJ idea started with Gradle sync time profiler");
            IdeaProfilerLogger.log("To profile the Gradle daemon, append the following entry to the JVM arguments: -Dgradle.profiler=enabled. You can do that by adding the following entry to the gradle.properies file: `org.org.gradle.jvmargs=-Dgradle.profiler.enabled");
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
