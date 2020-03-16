package org.gradle.profiler.internal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessUtils {
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

    public static int findGradleProcess() {
        List<Integer> processIds = new ArrayList<>();
        try {

            List<String> jpsOutput = exec("jps");
            for (String s : jpsOutput) {
                if (s.contains("GradleDaemon")) {
                    String pid = s.split(" ")[0];

                    List<String> systemProps = exec("jcmd " + pid + " VM.system_properties");
                    for (String s2 : systemProps) {
                        if (s2.contains("sync.profiler=enabled")) {
                            processIds.add(Integer.parseInt(pid));
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            IdeaProfilerLogger.log(e);
        }

        if (processIds.isEmpty()) {
            return -1;
        } else if (processIds.size() > 1) {
            IdeaProfilerLogger.log("WARN More than one Gradle Daemon is running with gradle.profiler=enabled system property: " + processIds + ". " + processIds.get(0) + " will be profiled.");
        }
        return processIds.get(0);
    }
    private static List<String> exec(String command) {
        List<String> result = new ArrayList<>();
        try {
            String line;
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                result.add(line);
            }
            input.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
