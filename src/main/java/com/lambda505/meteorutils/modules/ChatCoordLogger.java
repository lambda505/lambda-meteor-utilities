package com.lambda505.meteorutils.modules;

import com.lambda505.meteorutils.LambdaUtilities;
import com.lambda505.meteorutils.utils.*;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatCoordLogger extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgFilters = this.settings.createGroup("Filters");

    // Fixed paths - not configurable
    private static final String BASE_PATH = "LambdaMeteorUtilities";
    private static final String SUB_FOLDER = "ChatCoordLeaks";

    // Display-only path information
    private final Setting<String> pathInfo = sgGeneral.add(new StringSetting.Builder()
        .name("log-path")
        .description("Current log directory (read-only)")
        .defaultValue(BASE_PATH + File.separator + SUB_FOLDER)
        .build()
    );

    // Filter settings
    private final Setting<Boolean> ignoreSpawnRadius = sgFilters.add(new BoolSetting.Builder()
        .name("ignore-spawn-radius")
        .description("Don't log coordinates within spawn radius")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> spawnRadius = sgFilters.add(new IntSetting.Builder()
        .name("spawn-radius")
        .description("Radius around spawn (0,0) to ignore coordinates")
        .defaultValue(1500)
        .min(0)
        .max(50000)
        .sliderMax(10000)
        .visible(ignoreSpawnRadius::get)
        .build()
    );

    private final Setting<Boolean> logOwnCoords = sgGeneral.add(new BoolSetting.Builder()
        .name("log-own-coordinates")
        .description("Log your own coordinate leaks")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> detectXZCoords = sgGeneral.add(new BoolSetting.Builder()
        .name("detect-xz-coordinates")
        .description("Detect X-Z coordinate pairs without Y component")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minCoordValue = sgGeneral.add(new IntSetting.Builder()
        .name("min-coord-value")
        .description("Minimum absolute coordinate value to consider valid (helps filter out non-coordinates)")
        .defaultValue(100)
        .min(1)
        .max(10000)
        .sliderMax(5000)
        .visible(detectXZCoords::get)
        .build()
    );

    // Enhanced regex pattern to match coordinates in various formats
    private static final Pattern COORD_PATTERN_XYZ = Pattern.compile(
        "(?i)(?:" +
            // Format: x: 123, y: 64, z: -456
            "(?:.*(?:x|pos)\\s*[:=]?\\s*(-?\\d+).*(?:y|height)\\s*[:=]?\\s*(-?\\d+).*(?:z)\\s*[:=]?\\s*(-?\\d+).*)|" +
            // Format: (123, 64, -456)
            "(?:.*\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\).*)|" +
            // Format: 123 64 -456 (three space-separated numbers)
            "(?:.*\\b(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\b.*)" +
            ")"
    );

    // Pattern for X-Z coordinates (two numbers that look like coordinates)
    private static final Pattern COORD_PATTERN_XZ = Pattern.compile(
        "(?i)(?:" +
            // Format: x: 123, z: -456
            "(?:.*(?:x|pos)\\s*[:=]?\\s*(-?\\d+).*(?:z)\\s*[:=]?\\s*(-?\\d+).*)|" +
            // Format: (123, -456) - two numbers in parentheses
            "(?:.*\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\).*)|" +
            // Format: 123 -456 (two space-separated large numbers)
            "(?:.*\\b(-?\\d{3,})\\s+(-?\\d{3,})\\b.*)" +
            ")"
    );

    public ChatCoordLogger() {
        super(LambdaUtilities.CATEGORY, "chat-coord-logger", "Logs coordinate leaks found in chat messages to server-specific files.");
        updatePathInfo();
    }

    private void updatePathInfo() {
        pathInfo.set(FileUtils.getDisplayPath(BASE_PATH, SUB_FOLDER));
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (event.getMessage() == null) return;

        String messageText = event.getMessage().getString();
        String senderName = ChatMessageUtils.extractPlayerName(event.getMessage());

        // Skip if it's our own message and we don't want to log our coordinates
        if (!logOwnCoords.get() && mc.player != null &&
            ChatMessageUtils.isOwnMessage(event.getMessage(), mc.player.getName().getString())) {
            return;
        }

        // First try to match XYZ coordinates
        if (checkXYZCoordinates(messageText, senderName)) {
            return; // Found XYZ coordinates, don't check for XZ
        }

        // If XZ detection is enabled and no XYZ found, try XZ coordinates
        if (detectXZCoords.get()) {
            checkXZCoordinates(messageText, senderName);
        }
    }

    private boolean checkXYZCoordinates(String messageText, String senderName) {
        Matcher matcher = COORD_PATTERN_XYZ.matcher(messageText);
        if (matcher.find()) {
            String x = null, y = null, z = null;

            // Check which group matched and extract coordinates accordingly
            if (matcher.group(1) != null) {
                // Format: x: 123, y: 64, z: -456
                x = matcher.group(1);
                y = matcher.group(2);
                z = matcher.group(3);
            } else if (matcher.group(4) != null) {
                // Format: (123, 64, -456)
                x = matcher.group(4);
                y = matcher.group(5);
                z = matcher.group(6);
            } else if (matcher.group(7) != null) {
                // Format: 123 64 -456
                x = matcher.group(7);
                y = matcher.group(8);
                z = matcher.group(9);
            }

            if (x != null && y != null && z != null) {
                try {
                    int coordX = Integer.parseInt(x);
                    int coordZ = Integer.parseInt(z);

                    // Check spawn radius filter
                    if (ignoreSpawnRadius.get() && isWithinSpawnRadius(coordX, coordZ)) {
                        return true; // Found coords but filtered out
                    }

                    logCoordinates(senderName, x, y, z, messageText, "XYZ");
                    return true;
                } catch (NumberFormatException e) {
                    // Invalid coordinates, continue
                }
            }
        }
        return false;
    }

    private boolean checkXZCoordinates(String messageText, String senderName) {
        Matcher matcher = COORD_PATTERN_XZ.matcher(messageText);
        if (matcher.find()) {
            String x = null, z = null;

            // Check which group matched and extract coordinates accordingly
            if (matcher.group(1) != null) {
                // Format: x: 123, z: -456
                x = matcher.group(1);
                z = matcher.group(2);
            } else if (matcher.group(3) != null) {
                // Format: (123, -456)
                x = matcher.group(3);
                z = matcher.group(4);
            } else if (matcher.group(5) != null) {
                // Format: 123 -456 (two large numbers)
                x = matcher.group(5);
                z = matcher.group(6);
            }

            if (x != null && z != null) {
                try {
                    int coordX = Integer.parseInt(x);
                    int coordZ = Integer.parseInt(z);

                    // Check if coordinates are large enough to be actual coordinates
                    if (Math.abs(coordX) < minCoordValue.get() && Math.abs(coordZ) < minCoordValue.get()) {
                        return false; // Numbers too small, probably not coordinates
                    }

                    // Check spawn radius filter
                    if (ignoreSpawnRadius.get() && isWithinSpawnRadius(coordX, coordZ)) {
                        return true; // Found coords but filtered out
                    }

                    logCoordinates(senderName, x, "?", z, messageText, "XZ");
                    return true;
                } catch (NumberFormatException e) {
                    // Invalid coordinates, continue
                }
            }
        }
        return false;
    }

    private boolean isWithinSpawnRadius(int x, int z) {
        double distance = Math.sqrt(x * x + z * z);
        return distance <= spawnRadius.get();
    }

    private void logCoordinates(String playerName, String x, String y, String z, String fullMessage, String coordType) {
        File logFile = getLogFile();

        // Create the log entry with coordinate type information
        String logEntry = LogWriter.createTimestampedFormattedEntry(
            "Player: %s | Coords (%s): %s, %s, %s | Message: %s",
            playerName, coordType, x, y, z, fullMessage);

        if (LogWriter.writeLogEntry(logFile, logEntry)) {
            info("Logged " + coordType + " coordinates from " + playerName + " to " + logFile.getName());
        } else {
            error("Failed to write coordinate log");
        }
    }

    private File getLogFile() {
        String serverName = ServerUtils.getServerName();
        String fileName = "ccl_" + serverName + ".txt";
        return FileUtils.getLogFile(BASE_PATH, SUB_FOLDER, fileName);
    }

    @Override
    public void onActivate() {
        updatePathInfo();

        if (FileUtils.createDirectoryStructure(BASE_PATH, SUB_FOLDER)) {
            String currentLogFile = getLogFile().getName();
            info("Chat Coordinate Logger activated. Current log file: " + currentLogFile);
        } else {
            error("Failed to create directory structure");
        }
    }
}
