package org.gradle.profiler.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class IdeaSync {

    private static int pid;
    private static long syncStartTime = 0;

    private static final AtomicBoolean alreadyStarted = new AtomicBoolean(false);

    public static void init() {
        pid = ProcessUtils.getCurrentPid();
        if (pid <= 0) {
            IdeaProfilerLogger.log("ERROR cannot determine IDEA process ID");
        } else {
            File asyncProfiler = new File(System.getProperty("user.home") + "/async-profiler/profiler.sh");
            if (!asyncProfiler.exists()) {
                IdeaProfilerLogger.log("ERROR async profiler not available at " + asyncProfiler.getParentFile().getAbsolutePath());
            }
        }
    }

    public static void syncStarted() {
        if (!alreadyStarted.compareAndSet(false, true)) {
            return;
        }

        try {
            new ProcessBuilder(System.getProperty("user.home") + "/async-profiler/profiler.sh", "start", "-e", "cpu", "-i", "10ms", "-t", String.valueOf(pid)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        IdeaProfilerLogger.log("Gradle sync started");
    }

    public static void syncFinished() {
        if (!alreadyStarted.compareAndSet(true, false)) {
            IdeaProfilerLogger.log("WARN sync already finished");
            return;
        }

        long now = System.nanoTime();
        long diff = (now - syncStartTime) / 1000000;
        syncStartTime = 0;

        File profilesDir = new File("/tmp/profiles");
        profilesDir.mkdirs();
        int i = 1;
        File detailsFile;
        while ((detailsFile = new File(profilesDir, "idea-profile-" + i + ".details")).exists()) {
            i++;
        }

        int pid = ProcessUtils.getCurrentPid();
        if (pid > 0) {
            File profileFile = new File(profilesDir, "idea-profile-" + i + ".collapsed");
            Process profilerStopProcess = null;
            try {
                profilerStopProcess = new ProcessBuilder(System.getProperty("user.home") + "/async-profiler/profiler.sh", "stop", "-f", profileFile.getAbsolutePath(), String.valueOf(pid)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                profilerStopProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String details = "Idea finished in " + diff + " milliseconds\n" +
                "pid: " + pid + "\n";

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(detailsFile));
            writer.write(details);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignore) {
                }
            }
        }

        IdeaProfilerLogger.log("Gradle sync finished");
    }
}
