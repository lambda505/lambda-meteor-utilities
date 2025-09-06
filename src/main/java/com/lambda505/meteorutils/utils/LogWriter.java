package com.lambda505.meteorutils.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogWriter {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Writes a log entry to a file with proper error handling and directory creation
     * @param logFile The file to write to
     * @param logEntry The entry to write
     * @return true if write was successful
     */
    public static boolean writeLogEntry(File logFile, String logEntry) {
        try {
            // Create all necessary directories
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    return false;
                }
            }

            // Append to file with proper error handling
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(logEntry);
                writer.flush();
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Creates a formatted log entry with timestamp
     * @param content The main content to log
     * @return Formatted log entry with timestamp
     */
    public static String createTimestampedEntry(String content) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        return String.format("[%s] %s%n", timestamp, content);
    }

    /**
     * Creates a formatted log entry with custom format
     * @param format The format string
     * @param args The arguments for the format
     * @return Formatted log entry
     */
    public static String createFormattedEntry(String format, Object... args) {
        try {
            return String.format(format + "%n", args);
        } catch (Exception e) {
            // Fallback: just concatenate if formatting fails
            StringBuilder sb = new StringBuilder();
            sb.append(format);
            for (Object arg : args) {
                sb.append(" ").append(arg);
            }
            sb.append(System.lineSeparator());
            return sb.toString();
        }
    }

    /**
     * Creates a formatted log entry with timestamp and custom format
     * @param format The format string (timestamp will be prepended)
     * @param args The arguments for the format
     * @return Formatted log entry with timestamp
     */
    public static String createTimestampedFormattedEntry(String format, Object... args) {
        try {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String content = String.format(format, args);
            return String.format("[%s] %s%n", timestamp, content);
        } catch (Exception e) {
            // Fallback: use simple concatenation
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(timestamp).append("] ").append(format);
            for (Object arg : args) {
                sb.append(" ").append(arg);
            }
            sb.append(System.lineSeparator());
            return sb.toString();
        }
    }

    /**
     * Safe method to create timestamped entries without format strings
     * @param content The content to log
     * @return Formatted entry with timestamp
     */
    public static String createSafeTimestampedEntry(String content) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        return "[" + timestamp + "] " + content + System.lineSeparator();
    }

    /**
     * Creates a session separator entry
     * @param sessionInfo Information about the session
     * @return Formatted session separator
     */
    public static String createSessionSeparator(String sessionInfo) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String separator = "=" + "=".repeat(50) + "=";
        return separator + System.lineSeparator() +
            "[" + timestamp + "] SESSION: " + sessionInfo + System.lineSeparator() +
            separator + System.lineSeparator();
    }
}
