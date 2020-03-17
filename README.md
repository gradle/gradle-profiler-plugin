# Gradle profiler plugin

Gradle plugin profiling profile the Gradle build process with [async-profiler](https://github.com/jvm-profiling-tools/async-profiler). 

**Warning** This plugin is an early prototype (hence the 0. version number). 
Please use it with caution and do profiling on one project at a time.

The main purpose of this plugin is to help profiling the Gradle and the IDE process simultaneously when the IDE synchronization is running.

## How it works

The plugin attaches async-profiler to the Gradle and the IDE processes during the project synchronization.
On the Gradle side, the plugin deploys a custom init script to the gradle user home. 
This script starts the profiler at the beginning of the build and stops it in a `buildFinished` hook.
This is - obviously - an interpolation as the measurement misses the time between the daemon startup and until the init scripts being evaluated.
This is acceptable though, as we have other means to investigate what happens in that build stage.

The IDE - IntelliJ IDEA or Android Studio - is instrumented with a Java agent, which is part of this plugin's implementation. 
Android Studio has a specific method that runs the project sync. The java agent hooks into that method's entry and exit
event and starts/stops the async profiler respectively.
In the case of IntelliJ Idea, the Ultimate version has code obfuscated, so the plugin can't hook into any specific method. 
To work around that, the agent periodically polls the running threads. 
If a thread name matches the `importing XXX Gradle project` string then the profiling starts
Similarly, when the thread disappears he profiling is stopped.

## Getting started

#### Apply the plugin in the root project

    plugins {
        id 'org.gradle.gradle-profiler' version '0.0.1'
    }

If the legacy plugin mechanism is used, use the following instead:

    buildscript {
        dependencies {
            classpath 'org.gradle.profiler:gradle-profiler-plugin:0.0.1'
        }
    }
    apply plugin: 'org.gradle.gradle-profiler'

#### Enable profiling

    ./gradlew :enableProfiling

After executing the task, each subsequent Gradle invocations will generate a [`.collapsed`](https://github.com/brendangregg/FlameGraph) file in the `$rootProject/.profiles` folder.
You can open those files IntelliJ Ultimate: click Menu > Run > Open Profiler Snapshot.
 
Along with a `.collapsed` file, a `.details` file is generated that contains the following information:
 - PID of the profiled process
 - Duration of the execution
 - Profiler configuration 

To enable the IDE you need to adjust the VM settings: Menu > Open Menu > Help > Edit Custom VM Options. In the editor, add the following entry to a new line:

    -javaagent:/path/to/gradle-profiler-plugin-VERSION.jar


#### Disable profiling

    ./gradlew :disableProfiling
    
#### Sanitize the results

    ./gradlew :sanitizeResults
    
The task will take all `profile-\d+.collapsed` files in the `$rootProject/.gradle-profiles` folder, merges it into a single file, cleans up its content and writes it in the same folder.
 The cleanup consists of removing duplicate frames, replacing groovy-related frames with `dynamic invocation` entries and more. 
 For the details, check the `SanitizeResultsTask` implementation.
## Configuration

The following configuration options are available for the plugin via the `profiler` extension:
```
profiler {
    asyncProfilerLocation = file('/path/to/async-profiler')
    asyncProfilerParameters = [ '-e', 'mem' ]
    profilerOutputLocation = file("profiler-output-directory") 
}
```

## Compatibility

The plugin is known to work with the following versions:
- Java 1.8 and above
- Gradle 5.1 and above
- IntelliJ IDEA 2019.3.3
- Android Studio 3.6.1

## FAQ

#### I applied this plugin on one of my projects and now I can't execute _any_ builds

In order to start the profiler as early as possible within Gradle, the profiler plugin places a script in the Gradle user home (i.e. to the `.gradle` in your user home).
That script might be broken for some unforeseen reason.
Delete the `${gradleUserHome}/init.d/profiling.gradle` file and your builds should go back to normal.
Also, please open a bug report on the problem.

#### "(jattach|libasyncProfiler.so)" cannot be opened because the developers cannot be verified.

Upon first usage, macOS might complain about binaries from unknown sources. You can [work around that in the preferences](https://support.apple.com/guide/mac-help/open-a-mac-app-from-an-unidentified-developer-mh40616/mac).

#### My output mentions `frame_buffer_overflow`

You might need to add
```
profiler {
    asyncProfilerParameters = [ '-b', '5000000' ]
}
```
or another suitably large number. See [async-profiler troubleshooting](https://github.com/jvm-profiling-tools/async-profiler#troubleshooting) for more information.
