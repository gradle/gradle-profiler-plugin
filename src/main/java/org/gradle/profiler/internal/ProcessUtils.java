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

    public static String findIdeaProcess() {
        String process = null;
        try {
            String line;
            Process p = Runtime.getRuntime().exec("jps -v");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                if (line.trim().contains("idea.home.path")) {
                    process = line;
                    break;
                }
            }
            input.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return process;
    }
}
