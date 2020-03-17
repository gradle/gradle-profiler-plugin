package org.gradle.profiler.internal;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class IdeaSync {

    private static final IdeaSync INSTANCE = new IdeaSync();
    private final int pid = ProcessUtils.getCurrentPid();
    private final AtomicBoolean alreadyStarted = new AtomicBoolean(false);

    private long syncStartTime = 0;
    private String asyncProfilerPath = null;
    private List<String> asyncProfilerParameters = null;
    private String profilesDir = null;

    public static IdeaSync getInstance() {
        return INSTANCE;
    }

    public void setAsyncProfilerPath(String asyncProfilerPath) {
        this.asyncProfilerPath = asyncProfilerPath;
    }

    public void setAsyncProfilerParameters(List<String> asynchPofilerParameters) {
        this.asyncProfilerParameters = asynchPofilerParameters;
    }

    public void setProfilesDir(String profilesDir) {
        this.profilesDir = profilesDir;
    }

    private boolean readPreferences() {
        File prefsFile = new File(Constants.LOCATION_GLOBAL_PREFERENCES_FILE);
        if (!prefsFile.exists()) {
            IdeaProfilerLogger.log("Profiling not enabled");
            return false;
        }

        Properties prefs = new Properties();
        try {
            prefs.load(new FileInputStream(prefsFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String asyncProfilerPath = (String) prefs.get("async.profiler.location");
        if (asyncProfilerPath == null) {
            IdeaProfilerLogger.log("Cannot read async profiler location");
            return false;
        }

        Object params = prefs.get("async.profiler.parameters");
        if (params == null) {
            IdeaProfilerLogger.log("Cannot read async profiler parameters");
            return false;
        }

        String profilesDir = (String) prefs.get("profiler.output.location");
        if (profilesDir == null) {
            IdeaProfilerLogger.log("Cannot read profiles dir");
            return false;
        }

        List<String> asyncProfilerParameters = Arrays.asList(((String)params).split(" "));
        setAsyncProfilerPath(asyncProfilerPath + "/profiler.sh");
        setAsyncProfilerParameters(asyncProfilerParameters);
        setProfilesDir(profilesDir);
        return true;
    }

    public void syncStarted() {
        if (!alreadyStarted.compareAndSet(false, true)) {
            return;
        }

        try {
            if (!readPreferences()) {
                return;
            }
            syncStartTime = System.nanoTime();
            startProfiler();
        } catch (IOException e) {
            e.printStackTrace();
        }
        IdeaProfilerLogger.log("Idea sync started");
    }

    public void syncFinished() {
        if (!alreadyStarted.compareAndSet(true, false)) {
            return;
        }

        long now = System.nanoTime();
        long diff = (now - syncStartTime) / 1000000;
        syncStartTime = 0;

        int i = 1;
        File detailsFile;
        while ((detailsFile = new File(profilesDir, "idea-profile-" + i + ".details")).exists()) {
            i++;
        }

        File profileFile = new File(profilesDir,  "idea-profile-" + i + ".collapsed");
        stopProfiler(profileFile);

        StringBuilder details = new StringBuilder();
        details.append("Idea finished in ").append(diff).append(" milliseconds\n");
        details.append("PID: ").append(pid).append("\n");
        details.append("Async profiler parameters: ").append(asyncProfilerParameters.stream().collect(Collectors.joining(" "))).append("\n");
        writeFile(detailsFile, details.toString());

        IdeaProfilerLogger.log("Idea sync finished");
    }

    private void startProfiler() throws IOException {
        List<String> command = new ArrayList<>(3 + asyncProfilerParameters.size());
        command.add(asyncProfilerPath);
        command.add("start");
        command.addAll(asyncProfilerParameters);
        command.add(String.valueOf(pid));
        new ProcessBuilder(command).start();
    }

    private void stopProfiler(File outputFile) {
        Process profilerStopProcess = null;
        try {
            profilerStopProcess = new ProcessBuilder(asyncProfilerPath, "stop", "-f", outputFile.getAbsolutePath(), String.valueOf(pid)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            profilerStopProcess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeFile(File detailsFile, String details) {
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
