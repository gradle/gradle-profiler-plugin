/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.profiler.internal;

import org.gradle.api.InvalidUserCodeException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Arrays;

public class GradleProfilerPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        if (!project.equals(project.getRootProject())) {
            throw new InvalidUserCodeException("The 'org.gradle.gradle-profiler' plugin should be applied on the root project only");
        }

        ProfilerConfigurationExtension configuration = project.getExtensions().create("profiler", ProfilerConfigurationExtension.class, project.getObjects());
        String asyncProfilerHome = System.getProperty("async.profiler.home", System.getProperty("user.home") + "/async-profiler");
        // TODO: It would be nice to use convention here and for the profiler output location
        configuration.getAsyncProfilerLocation().set(new File(asyncProfilerHome));
        configuration.getAsyncProfilerParameters().convention(Arrays.asList("-e", "cpu", "-i", "10ms", "-t"));
        configuration.getProfilerOutputLocation().set(new File(project.getLayout().getProjectDirectory().getAsFile(), ".profiles"));

        String group = "profiler";

        project.getTasks().register("enableProfiling", EnableProfilingTask.class, task -> {
            task.setGroup(group);
            Provider<RegularFile> initScript = project.getLayout().file(project.provider(() -> new File(project.getGradle().getGradleUserHomeDir(), Constants.LOCATION_CUSTOM_INIT_SCRIPT)));
            task.getProfilingInitScriptFile().convention(initScript);

            Provider<RegularFile> preferences = project.getLayout().file(project.provider(() -> new File(Constants.LOCATION_GLOBAL_PREFERENCES_FILE)));
            task.getProfilingPreferencesFile().convention(preferences);
            task.getAsyncProfilerLocation().convention(configuration.getAsyncProfilerLocation());
            task.getAsyncProfilerParameters().convention(configuration.getAsyncProfilerParameters());
            task.getProfilerOutputLocation().convention(configuration.getProfilerOutputLocation());
        });
        project.afterEvaluate(p -> p.getTasks().withType(EnableProfilingTask.class).forEach(t -> t.getProfilerOutputLocation().getAsFile().get().mkdirs()));

        project.getTasks().register("disableProfiling", DisableProfilingTask.class, task -> {
            task.setGroup(group);
        });
    }
}
