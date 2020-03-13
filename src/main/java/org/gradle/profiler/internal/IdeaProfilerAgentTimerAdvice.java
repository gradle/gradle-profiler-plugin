package org.gradle.profiler.internal;

import net.bytebuddy.asm.Advice;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class IdeaProfilerAgentTimerAdvice {

    public static long start = 0;

    @Advice.OnMethodEnter
    static void enter(@Advice.Origin String method) throws Exception {
        System.out.println("debug_method_start=" + method);

        if (method.contains("syncStarted")) {
            start = System.nanoTime();
            int pid = OsUtils.getCurrentPid();

            if (pid > 0) {
                System.out.println("current pid=" + pid);
                // TODO save profiler arguments in preferences file and load it from there
                new ProcessBuilder(System.getProperty("user.home") + "/async-profiler/profiler.sh", "start", "-e", "cpu", "-i", "10ms", "-t", String.valueOf(pid)).start();
                
            }
            System.out.println("profile_start=" + method);
        }
    }

    @Advice.OnMethodExit
    static void exit(@Advice.Origin String method) throws Exception {
        System.out.println("debug_method_finish=" + method);

        if (method.contains("syncFinished") && start > 0) {
            long now = System.nanoTime();
            long diff = (now - start) / 1000000;
            start = 0;
            System.out.println("profile_finish=" + method + ", diff=" + diff);

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
        }
    }
}
