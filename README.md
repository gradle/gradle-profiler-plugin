# Gradle profiler plugin

Gradle plugin profiling the Gradle build process with [async-profiler](https://github.com/jvm-profiling-tools/async-profiler). 

**Warning** This plugin is an early prototype (hence the 0. version number). 
Please use it with caution and do profiling on one project at a time.

## Getting started

#### Applying the plugin

    plugins {
        id 'org.gradle.gradle-profiler' version '0.0.1'
    }
  
The plugin requires async-profiler to be installed in the `{user.home}/async-profiler` directory or in the [configured](#configuration) location.
    
Note: the plugin is not yet released to the plugin portal.
Right now if you want to use this plugin you need to clone the project and add the following statement to your settings file:

    includeBuild 'path/to/gradle-profiler/plugin' 
    
#### Enable profiling

    ./gradlew enableProfiling

After executing the task, each subsequent Gradle invocations will generate a [`.collapsed`](https://github.com/brendangregg/FlameGraph) file in the `$rootProject/.gradle-profiles` folder.
The profiling starts when Gradle executes the scripts in the `.init.d` folder and finishes in a `gradle.buildFinished` hook. 
The file can be opened with IntelliJ Ultimate: Menu > Run > Open Profiler Snapshot.  

#### Disable profiling

    ./gradlew disableProfiling
    
#### Sanitizing the results

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
