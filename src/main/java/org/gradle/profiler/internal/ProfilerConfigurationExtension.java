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

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;

import javax.inject.Inject;

public class ProfilerConfigurationExtension {
    // TODO: Would be nice to use managed properties for this, but this is only supported in newer versions of Gradle.
    private final DirectoryProperty asyncProfilerLocation;
    private final ListProperty<String> asyncProfilerParameters;

    @Inject
    public ProfilerConfigurationExtension(ObjectFactory objects) {
        this.asyncProfilerLocation = objects.directoryProperty();
        this.asyncProfilerParameters = objects.listProperty(String.class);
    }

    public DirectoryProperty getAsyncProfilerLocation() {
        return asyncProfilerLocation;
    }

    public ListProperty<String> getAsyncProfilerParameters() {
        return asyncProfilerParameters;
    }
}
