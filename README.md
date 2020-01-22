# Gradle profiler plugin

Gradle plugin profiling the Gradle build process with [async-profiler](https://github.com/jvm-profiling-tools/async-profiler). 

## Getting started

#### Applying the plugin

    plugins {
        id 'org.gradle.gradle-profiler' version '0.0.1'
    }
  
The plugin requires async-profiler to be present in the `{user.home}/async-profiler` directory.
    
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