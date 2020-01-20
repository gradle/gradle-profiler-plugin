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
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

public class SanitizeResultsTask extends DefaultTask {

    File sourceDir;

    @InputFile
    public File getSourceDir() {
        return sourceDir;
    }

    File target;

    @InputFile
    public File getTarget() {
        return target;
    }

    private static final Splitter STACKTRACE_SPLITTER = Splitter.on(";").omitEmptyStrings();
    private static final Joiner STACKTRACE_JOINER = Joiner.on(";");

    @TaskAction
    public void sanitize() {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            getProject().getLogger().warn("Profiling directory does not exist: " + sourceDir.getAbsolutePath());
            return;
        }

        File[] profiles = sourceDir.listFiles((containerDir, fileName) -> fileName.matches("profile-\\d+\\.collapsed"));
        if (profiles == null || profiles.length == 0) {
            getProject().getLogger().warn("No profiles found in in: " + sourceDir.getAbsolutePath());
            return;
        }

        SanitizeFunction sanitizeFunction = createSanitizeFunction();
        target.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(target))) {
            for (File source : profiles) {
                for (String line : Files.asCharSource(source, Charsets.UTF_8).readLines()) {
                    int endOfStack = line.lastIndexOf(" ");
                    if (endOfStack <= 0) {
                        continue;
                    }
                    String stackTrace = line.substring(0, endOfStack);
                    String invocationCount = line.substring(endOfStack + 1);
                    List<String> stackTraceElements = STACKTRACE_SPLITTER.splitToList(stackTrace);
                    List<String> sanitizedStackElements = sanitizeFunction.map(stackTraceElements);
                    if (!sanitizedStackElements.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.setLength(0);
                        STACKTRACE_JOINER.appendTo(sb, sanitizedStackElements);
                        sb.append(" ");
                        sb.append(invocationCount);
                        sb.append("\n");
                        writer.write(sb.toString());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Copied from https://github.com/gradle/gradle-profiler

    SanitizeFunction createSanitizeFunction() {
        return new CompositeSanitizeFunction(Arrays.asList(
            new RemoveSystemThreads(),
            new ReplaceRegex(
                    ImmutableMap.of(
                            Pattern.compile("build_[a-z0-9]+"), "build script",
                            Pattern.compile("settings_[a-z0-9]+"), "settings script"
                    )
            ),
            new CompositeSanitizeFunction(
                    new ChopPrefix("loadSettings"),
                    new ChopPrefix("configureBuild"),
                    new ChopPrefix("constructTaskGraph"),
                    new ChopPrefix("executeTasks"),
                    new ChopPrefix("org.gradle.api.internal.tasks.execution"),
                    new ReplaceContainment(asList("DynamicObject", "Closure.call", "MetaClass", "MetaMethod", "CallSite", "ConfigureDelegate", "Method.invoke", "MethodAccessor", "Proxy", "ConfigureUtil", "Script.invoke", "ClosureBackedAction", "getProperty("), "dynamic invocation_[j]"),
                    new CollapseDuplicateFrames()
            )
        ));
    }

    interface SanitizeFunction {
        List<String> map(List<String> stack);
    }

    private static final class CompositeSanitizeFunction implements SanitizeFunction {

        private final List<SanitizeFunction> sanitizeFunctions;

        private CompositeSanitizeFunction(SanitizeFunction... sanitizeFunctions) {
            this(Arrays.asList(sanitizeFunctions));
        }

        private CompositeSanitizeFunction(Collection<SanitizeFunction> sanitizeFunctions) {
            this.sanitizeFunctions = ImmutableList.copyOf(sanitizeFunctions);
        }

        @Override
        public List<String> map(List<String> stack) {
            List<String> result = stack;
            for (SanitizeFunction sanitizeFunction : sanitizeFunctions) {
                result = sanitizeFunction.map(result);
            }
            return result;
        }
    }

    private static final class RemoveSystemThreads implements SanitizeFunction {

        @Override
        public List<String> map(List<String> stack) {
            for (String frame : stack) {
                if (frame.contains("GCTaskThread") || frame.contains("JavaThread")) {
                    return Collections.emptyList();
                }
            }
            return stack;
        }
    }

    private static abstract class FrameWiseSanitizeFunction implements SanitizeFunction {
        @Override
        public final List<String> map(List<String> stack) {
            List<String> result = Lists.newArrayListWithCapacity(stack.size());
            for (String frame : stack) {
                result.add(mapFrame(frame));
            }
            return result;
        }

        protected abstract String mapFrame(String frame);
    }

    private static class ReplaceContainment extends FrameWiseSanitizeFunction {
        private final Collection<String> keyWords;
        private final String replacement;

        private ReplaceContainment(Collection<String> keyWords, String replacement) {
            this.keyWords = keyWords;
            this.replacement = replacement;
        }

        @Override
        protected String mapFrame(String frame) {
            for (String keyWord : keyWords) {
                if (frame.contains(keyWord)) {
                    return replacement;
                }
            }
            return frame;
        }
    }

    private static class ReplaceRegex extends FrameWiseSanitizeFunction {
        private final Map<Pattern, String> replacements;

        private ReplaceRegex(Map<Pattern, String> replacements) {
            this.replacements = replacements;
        }

        @Override
        protected String mapFrame(String frame) {
            for (Map.Entry<Pattern, String> replacement : replacements.entrySet()) {
                Matcher matcher = replacement.getKey().matcher(frame);
                String value = replacement.getValue();
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    matcher.appendReplacement(sb, value);
                }
                matcher.appendTail(sb);
                if (sb.length() > 0) {
                    frame = sb.toString();
                }
            }
            return frame;
        }
    }

    private static class CollapseDuplicateFrames implements SanitizeFunction {
        @Override
        public List<String> map(List<String> stack) {
            List<String> result = Lists.newArrayList(stack);
            ListIterator<String> iterator = result.listIterator();
            String previous = null;
            while (iterator.hasNext()) {
                String next = iterator.next();
                if (next.equals(previous)) {
                    iterator.remove();
                }
                previous = next;
            }
            return result;
        }
    }

    private static class ChopPrefix implements SanitizeFunction {
        private final String stopToken;

        private ChopPrefix(String stopToken) {
            this.stopToken = stopToken;
        }

        @Override
        public List<String> map(List<String> stack) {
            for (int i = 0; i < stack.size(); i++) {
                String frame = stack.get(i);
                if (frame.contains(stopToken)) {
                    return stack.subList(i, stack.size());
                }
            }
            return stack;
        }
    }
}
