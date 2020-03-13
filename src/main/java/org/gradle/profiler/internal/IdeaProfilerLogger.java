package org.gradle.profiler.internal;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class IdeaProfilerLogger {

    private static PrintStream out;
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        try {
            String profilerLogPath = "/tmp/idea-profiler-log";
            System.out.println("Profiler log available at " + profilerLogPath);
            out = new PrintStream(new FileOutputStream(profilerLogPath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void log(String message) {
        out.println(String.format("%s %s", dateFormat.format(LocalDateTime.now()), message));
    }

    public static void close() {
        out.close();
    }
}
