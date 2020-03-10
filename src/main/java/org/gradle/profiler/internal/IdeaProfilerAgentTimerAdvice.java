package org.gradle.profiler.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.FieldValue;

public class IdeaProfilerAgentTimerAdvice {

    public static long start = 0;

    @Advice.OnMethodEnter
    static void enter(@Advice.Origin String method, @FieldValue("myTitle") String myTitle) throws Exception {
        System.out.println("debug_method_start=" + method);

        if (isGradleSyncInProgress(myTitle) && method.contains("start")) {
            start = System.nanoTime();
            int pid = getCurrentPid();

            if (pid > 0) {
                System.out.println("current pid=" + pid);
                // TODO save profiler arguments in preferences file and load it from there
                new ProcessBuilder(System.getProperty("user.home") + "/async-profiler/profiler.sh", "start", "-e", "cpu", "-i", "10ms", "-e", "wall", "-t", String.valueOf(pid)).start();
                
            }
            System.out.println("profile_start=" + method);
        }
    }

    public static boolean isGradleSyncInProgress(String title) {
        return title != null && title.contains("Importing") && title.contains("Gradle project");
    }

    @Advice.OnMethodExit
    static void exit(@Advice.Origin String method, @FieldValue("myTitle") String myTitle) throws Exception {
        System.out.println("debug_method_finish=" + method);

        if (isGradleSyncInProgress(myTitle) && method.contains("stop") && start > 0) {
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

            int pid = getCurrentPid();
            if (pid > 0) {
                File profileFile = new File(profilesDir, "idea-profile-" + i + ".collapsed");
                Process profilerStopProcess = new ProcessBuilder(System.getProperty("user.home") + "/async-profiler/profiler.sh", "stop", "-f", profileFile.getAbsolutePath(), String.valueOf(pid)).start();
                profilerStopProcess.waitFor();
            }

            String simpleName = simpleName(method);
            String details = "Idea finished in " + diff + " milliseconds\n" +
                    "pid: " + getCurrentPid() + "\n";

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

    public static int getCurrentPid() {
        try {
            // Workaround to obtain the PID. The ProcessHandle class is available since Java 9 only
            java.lang.management.RuntimeMXBean runtime = java.lang.management.ManagementFactory.getRuntimeMXBean();
            java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
            jvm.setAccessible(true);
            sun.management.VMManagement mgmt = (sun.management.VMManagement) jvm.get(runtime);
            java.lang.reflect.Method pid_method = mgmt.getClass().getDeclaredMethod("getProcessId");
            pid_method.setAccessible(true);

            return (Integer) pid_method.invoke(mgmt);
        } catch (Exception e) {
            return -1;
        }
    }

    public static String simpleName(String method) {
        method = method.substring(0, method.indexOf("("));
        int lastDot =  method.lastIndexOf(".");
        String methodName = method.substring(lastDot + 1, method.length());
        String classFqName = method.substring(0, lastDot);
        String className = classFqName.substring(classFqName.lastIndexOf(".") + 1, classFqName.length());
        return className + "." + methodName;
    }
}
