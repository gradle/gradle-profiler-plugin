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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GradleProfilerPluginFunctionalTest {

    @TempDir
    File projectDir;

    @BeforeEach
    public void setUp() throws IOException {
        writeSettingsFile("");
        writeBuildFile("" +
                "plugins {\n" +
                "   id 'org.gradle.gradle-profiler'\n" +
                "}\n");
    }

    @AfterEach
    public void tearDown() throws IOException {
        runTask("disableProfiling");
    }

    @ParameterizedTest(name = "Can enable profiling with Gradle {0}")
    @MethodSource("getTestedGradleVersions")
    public void can_enable_profiling(String gradleVersion) throws IOException {
        // setup:
        runTaskWithVersion(gradleVersion, "enableProfiling");

        // when:
        BuildResult result = runTaskWithVersion(gradleVersion, "help");

        // then:
        assertOutputContains(result, "BUILD SUCCESSFUL");
        assertOutputContains(result, "Started profiling process");
        assertOutputContains(result, "Finished profiling process");
        assertOutputContains(result, "Results saved at " + projectDir.getCanonicalPath() + "/.gradle-profiles/profile-1.collapsed");
        assertFileCreatedAsync(projectDir.getAbsolutePath() + "/.gradle-profiles/profile-1.collapsed");
    }

    @ParameterizedTest(name = "Can disable profiling with Gradle {0}")
    @MethodSource("getTestedGradleVersions")
    public void can_disable_profiling(String gradleVersion) {
        // setup:
        runTaskWithVersion(gradleVersion, "enableProfiling");
        runTaskWithVersion(gradleVersion, "disableProfiling");

        // when:
        BuildResult result = runTaskWithVersion(gradleVersion, "help");

        // then:
        assertOutputContains(result, "BUILD SUCCESSFUL");
        assertOutputNotContains(result, "Started profiling process");
        assertOutputNotContains(result,"Finished profiling process");
    }

    @Test
    public void disabling_profiling_wont_generate_another_profile() {
        // setup:
        runTask("enableProfiling");
        BuildResult result = runTask("disableProfiling");

        // then:
        assertOutputContains(result, "BUILD SUCCESSFUL");
        assertOutputNotContains(result, "Started profiling process");
        assertOutputNotContains(result, "Finished profiling process");
    }

    @Test
    public void can_configure_async_profiler() throws IOException {
        // setup:
        writeBuildFile("" +
                "profiler {\n" +
                "    asyncProfilerParameters '-e', 'mem' \n" +
                "}\n");

        // then:
        runTask("enableProfiling");
        BuildResult result = runTask("help", "--info");

        // then:
        assertOutputContains(result, "BUILD SUCCESSFUL");
        assertOutputContains(result, "profiler.sh start -e mem");
    }

    private void writeBuildFile(String content) throws IOException {
        writeProjectFile("build.gradle", content);
    }

    private void writeSettingsFile(String content) throws IOException {
        writeProjectFile("settings.gradle", content);
    }

    private void writeProjectFile(String fileName, String content) throws IOException {
        Files.asCharSink(new File(projectDir, fileName), Charsets.UTF_8, FileWriteMode.APPEND).write(content);
    }

    private BuildResult runTask(String... arguments) {
        return GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments)
                .withProjectDir(projectDir)
                .build();
    }

    private BuildResult runTaskWithVersion(String version, String... arguments) {
        return GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments)
                .withProjectDir(projectDir)
                .withGradleVersion(version)
                .build();
    }

    private static Stream<String> getTestedGradleVersions() {
        return Stream.of("6.1", "5.6.2", "5.1");
    }

    private static void assertOutputContains(BuildResult result, String s) {
        assertTrue(result.getOutput().contains(s));
    }

    private static void assertOutputNotContains(BuildResult result, String s) {
        assertTrue(!result.getOutput().contains(s));
    }

    private void assertFileCreatedAsync(String path) {
        assertTrue(waitFor(() -> new File(path).exists()));
    }
    private static <T> boolean waitFor(Supplier<Boolean> condition) {
        long start = System.currentTimeMillis();
        while(!condition.get()) {
            if ((System.currentTimeMillis() - start) > 10000) {
                return false;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
        }
        return true;
    }
}
