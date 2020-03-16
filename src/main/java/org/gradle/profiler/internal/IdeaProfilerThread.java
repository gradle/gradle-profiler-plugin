package org.gradle.profiler.internal;

public class IdeaProfilerThread extends Thread {

    public static boolean syncAlreadyDetected = false;

    @Override
    public void run() {
        // TODO delete
        int gP = ProcessUtils.findGradleProcess();
        System.out.println("gradle daemon PID: " + gP);
        while (true) {
            try {
                if (!syncAlreadyDetected && isIntelliJSyncInProgress()) {
                    syncAlreadyDetected = true;
                    IdeaSync.syncStarted();
                } else if (syncAlreadyDetected && !isIntelliJSyncInProgress()) {
                    syncAlreadyDetected = false;
                    IdeaSync.syncFinished();
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
