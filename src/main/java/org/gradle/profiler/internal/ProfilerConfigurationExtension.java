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

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProfilerConfigurationExtension {

    private final Property<String> asyncProfilerLocation;
    private final ListProperty<String> asyncProfilerParameters;

    public ProfilerConfigurationExtension(Project project) {
        asyncProfilerLocation = project.getObjects().property(String.class).convention(System.getProperty("user.home") + "/async-profiler");
        asyncProfilerParameters = project.getObjects().listProperty(String.class).convention(Arrays.asList("-e", "cpu", "-i", "5ms"));
    }

    public Property<String> getAsyncProfilerLocation() {
        return asyncProfilerLocation;
    }

    public void asyncProfilerLocation(String location) {
        asyncProfilerLocation.set(location);
    }

    public ListProperty<String> getAsyncProfilerParameters() {
        return asyncProfilerParameters;
    }

    public void asyncProfilerParameters(String... parameters) {
        asyncProfilerParameters.set(Arrays.asList(parameters));
    }
}
