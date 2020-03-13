package org.gradle.profiler.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.FieldValue;

public class IdeaProfilerThread extends Thread {

    public static long start = 0;

    @Override
    public void run() {
        while (true) {
            try {
                if (start == 0 && isGradleSyncInProgress()) {
                    syncStarted();
                } else if (start > 0 && !isGradleSyncInProgress()) {
                    syncFinished();
                }
                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof InterruptedException) {
                    break;
                }
            }
        }
    }

    public static boolean isGradleSyncInProgress() {
        return Thread.getAllStackTraces().keySet().stream().anyMatch(t -> t.getName().contains("Importing") && t.getName().contains("Gradle project"));
    }

    static void syncStarted() throws Exception {
        start = System.nanoTime();
        int pid = OsUtils.getCurrentPid();

        if (pid > 0) {
            System.out.println("current pid=" + pid);
            // TODO save profiler arguments in preferences file and load it from there
            new ProcessBuilder(System.getProperty("user.home") + "/async-profiler/profiler.sh", "start", "-e", "cpu", "-i", "10ms", "-t", String.valueOf(pid)).start();

        }
        System.out.println("Gradle sync started");
    }



    static void syncFinished() throws Exception {
        long now = System.nanoTime();
        long diff = (now - start) / 1000000;
        start = 0;

        File profilesDir = new File("/tmp/profiles");
        profilesDir.mkdirs();
        int i = 1;
        File detailsFile;
        while ((detailsFile = new File(profilesDir, "idea-profile-" + i + ".details")).exists()) {
            i++;
        }

        int pid = OsUtils.getCurrentPid();
        if (pid > 0) {
            File profileFile = new File(profilesDir, "idea-profile-" + i + ".collapsed");
            Process profilerStopProcess = new ProcessBuilder(System.getProperty("user.home") + "/async-profiler/profiler.sh", "stop", "-f", profileFile.getAbsolutePath(), String.valueOf(pid)).start();
            profilerStopProcess.waitFor();
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

        System.out.println("Gradle sync finished");
    }
}
