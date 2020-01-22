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
import org.gradle.api.file.Directory;

import java.io.File;

public class GradleProfilerPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        if (!project.equals(project.getRootProject())) {
            throw new InvalidUserCodeException("The 'org.gradle.gradle-profiler' plugin should be applied on the root project only");
        }

        ProfilerConfigurationExtension configuration = project.getExtensions().create("profiler", ProfilerConfigurationExtension.class, project);
        String group = "profiler";

        project.getTasks().register("enableProfiling", EnableProfilingTask.class, task -> {
            task.setGroup(group);
            task.apply(configuration);
        });

        project.getTasks().register("disableProfiling", DisableProfilingTask.class, task -> {
            task.setGroup(group);
        });

        project.getTasks().register("sanitizeResults", SanitizeResultsTask.class, task -> {
            task.setGroup(group);
            Directory rootDir = task.getProject().getRootProject().getLayout().getProjectDirectory();
            task.getSourceDirectory().convention(rootDir.dir(Constants.LOCATION_PROFILES));
            task.getTargetFile().convention(rootDir.file(Constants.LOCATION_SANITIZED_PROFILE));
        });
    }
}
