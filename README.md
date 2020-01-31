# Gradle profiler plugin

Gradle plugin profiling the Gradle build process with [async-profiler](https://github.com/jvm-profiling-tools/async-profiler). 

**Warning** This plugin is an early prototype (hence the 0. version number). 
Please use it with caution and do profiling on one project at a time.

## Getting started

#### Apply the plugin

    plugins {
        id 'org.gradle.gradle-profiler' version '0.0.1'
    }
    
#### Enable profiling

    ./gradlew enableProfiling

After executing the task, each subsequent Gradle invocations will generate a [`.collapsed`](https://github.com/brendangregg/FlameGraph) file in the `$rootProject/.gradle-profiles` folder.
The profiling starts when Gradle executes the scripts in the `.init.d` folder and finishes in a `gradle.buildFinished` hook. 
The file can be opened with IntelliJ Ultimate: Menu > Run > Open Profiler Snapshot.  

#### Disable profiling

    ./gradlew disableProfiling
    
#### Sanitize the results

    ./gradlew sanitizeResults
    
The task will take all `profile-\d+.collapsed` files from the `$rootProject/.gradle-profiles` folder merges it into a single file and cleans up the content.
 The cleanup consists of removing duplicate frames, replacing groovy-related frames with `dynamic invocation` entries and more. 
 For the details, check the `SanitizeResultsTask` implementation.

## Configuration

The following configuration options are available for the plugin via the `profiler` extension:
```
profiler {
    asyncProfilerLocation = file("/path/to/async-profiler")
    asyncProfilerParameters = [ '-e', 'mem' ]
}
```

## FAQ

#### I applied this plugin on one of my projects and now I can't execute _any_ builds

In order to start the profiler as early as possible within Gradle, the profiler plugin places a script in the Gradle user home (i.e. to the `.gradle` in your user home).
That script might be broken for some unforeseen reason.
Delete the `${gradleUserHome}/init.d/profiling.gradle` file and your builds should go back to normal.
Also, please open a bug report on the problem.

#### "(jattach|libasyncProfiler.so)" cannot be opened because the developers cannot be verified.

Upon first usage, macOS might complain about binaries from unknown sources. You can [work around that in the preferences](https://support.apple.com/guide/mac-help/open-a-mac-app-from-an-unidentified-developer-mh40616/mac).
