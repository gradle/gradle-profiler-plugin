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
        System.out.println("Gradle sync time profiler loaded");

        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .type(
                        ElementMatchers.nameContains("com.intellij.openapi.progress.util.ProgressWindow").or(
                                ElementMatchers.any()
                        )

                        //     ElementMatchers.nameContains("ExternalSystemResolveProjectTask")
                        //    .or(ElementMatchers.nameContains("ExternalProjectRefreshCallback"))
                        //    .or(ElementMatchers.nameContains("org.jetbrains.plugins.gradle.service.project.data.ExternalAnnotationsModuleLibrariesService"))
                        //    .or(ElementMatchers.nameContains("ExternalProjectRefreshCallback"))
                )

                //.type((ElementMatchers.nameContains("gradle")))
                .transform((builder, typeDescription, classLoader, module) -> builder
                        // .method(ElementMatchers.nameContains("doExecute")
                        //     .or(ElementMatchers.nameContains("onSuccess"))
                        //     .or(ElementMatchers.nameContains("onSuccessImport"))
                        //     )
                        .method(ElementMatchers.nameContains("start")
                                .or(ElementMatchers.nameContains("stopSystemActivity"))
                                .or(ElementMatchers.nameContains("stop"))
                                .or(ElementMatchers.nameContains("execute"))
                                .or(ElementMatchers.any())
                        )
                        .intercept(Advice.to(IdeaProfilerAgentTimerAdvice.class))
                ).installOn(instrumentation);
    }

}
