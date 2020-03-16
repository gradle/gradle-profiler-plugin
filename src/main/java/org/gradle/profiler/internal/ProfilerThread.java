package org.gradle.profiler.internal;

public class ProfilerThread extends Thread {

    public static boolean syncAlreadyDetected = false;

    @Override
    public void run() {
        while (true) {
            try {
                if (!syncAlreadyDetected && isIntelliJSyncInProgress()) {
                    syncAlreadyDetected = true;
                    IdeaSync.getInstance().syncStarted();
                } else if (syncAlreadyDetected && !isIntelliJSyncInProgress()) {
                    syncAlreadyDetected = false;
                    IdeaSync.getInstance().syncFinished();
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
