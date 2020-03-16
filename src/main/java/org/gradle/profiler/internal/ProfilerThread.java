package org.gradle.profiler.internal;

public class ProfilerThread extends Thread {

    public static boolean syncAlreadyDetected = false;
    private static int daemonPid = 1;

    @Override
    public void run() {
        while (true) {
            try {
                if (!syncAlreadyDetected && isIntelliJSyncInProgress()) {
                    syncAlreadyDetected = true;
                    IdeaSync.syncStarted();
                } else if (syncAlreadyDetected && !isIntelliJSyncInProgress()) {
                    syncAlreadyDetected = false;
                    IdeaSync.syncFinished();
                }

                if (syncAlreadyDetected && daemonPid == -1) {
                    int gP = ProcessUtils.findGradleProcess();
                    if (gP > 0) {
                        daemonPid = gP;
                        IdeaProfilerLogger.log("Gradle daemon started with PID: " + gP);
                        // TODO start Gradle profiling
                    }
                }

                if (!syncAlreadyDetected && daemonPid > 0) {
                    // TODO stop Gradle profiling
                    IdeaProfilerLogger.log("Gradle daemon finished sync");
                    daemonPid = -1;
                }


                Thread.sleep(200);
            } catch (Exception e) {

                IdeaProfilerLogger.log("IntelliJ Platform sync time profiler stopped");
                IdeaProfilerLogger.log(e);
                if (e instanceof InterruptedException) {
                    break;
                }
            }
        }
        IdeaProfilerLogger.close();
    }

    public static boolean isIntelliJSyncInProgress() {
        return Thread.getAllStackTraces().keySet().stream().anyMatch(t -> t.getName().contains("Importing") && t.getName().contains("Gradle project"));
    }
}
