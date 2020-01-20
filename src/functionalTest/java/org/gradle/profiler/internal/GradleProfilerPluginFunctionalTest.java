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

import com.google.common.base.Charsets;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.Assert.assertTrue;

public class GradleProfilerPluginFunctionalTest {

    private File projectDir;

    @Before
    public void setUp() throws IOException {
        projectDir = new File("build/functionalTest/GradleIdeSyncPerformancePluginFunctionalTest");
        projectDir.mkdirs();

        Files.asCharSink(new File(projectDir, "settings.gradle"), Charsets.UTF_8, FileWriteMode.APPEND)
                .write("");
        Files.asCharSink(new File(projectDir, "build.gradle"), Charsets.UTF_8, FileWriteMode.APPEND)
                .write("plugins { id 'org.gradle.gradle-profiler' }");
    }

    @After
    public void tearDown() throws IOException {
        runTask("disableProfiling");
        java.nio.file.Files.walk(projectDir.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void can_enable_profiling() throws Exception {
        // setup:
        runTask("enableProfiling");

        // when:
        BuildResult result = runTask("help");

        // then:
        assertTrue(result.getOutput().contains("Started profiling process"));
        assertTrue(result.getOutput().contains("Finished profiling process"));
        assertTrue(result.getOutput().contains("Results saved at " + projectDir.getAbsolutePath() + "/.gradle-profiles/profile-1.collapsed"));
    }

    @Test
    public void can_disable_profiling() throws Exception {
        // setup:
        runTask("enableProfiling");
        runTask("disableProfiling");

        // when:
        BuildResult result = runTask("help");

        // then:
        assertTrue(!result.getOutput().contains("Started profiling process"));
        assertTrue(!result.getOutput().contains("Finished profiling process"));
    }

    @Test
    public void disabling_profiling_wont_generate_another_profile() {
        // setup:
        runTask("enableProfiling");
        BuildResult result = runTask("disableProfiling");

        // then:
        assertTrue(!result.getOutput().contains("Started profiling process"));
        assertTrue(!result.getOutput().contains("Finished profiling process"));
    }

    private BuildResult runTask(String task) {
        return GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(task)
                .withProjectDir(projectDir)
                .build();
    }
}
