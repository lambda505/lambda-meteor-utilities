package com.lambda505.meteorutils.modules;

import com.lambda505.meteorutils.LambdaUtilities;
import com.lambda505.meteorutils.utils.*;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PrivateMessageArchiver extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgSession = this.settings.createGroup("Session Management");
    private final SettingGroup sgDebug = this.settings.createGroup("Debug");

    private static final String BASE_PATH = "LambdaMeteorUtilities";
    private static final String SUB_FOLDER = "PrivateMessageArchiver";
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Settings
    private final Setting<String> pathInfo = sgGeneral.add(new StringSetting.Builder()
        .name("archive-path").description("Current archive directory (read-only)")
        .defaultValue(BASE_PATH + File.separator + SUB_FOLDER).build());

    private final Setting<Boolean> logOwnMessages = sgGeneral.add(new BoolSetting.Builder()
        .name("log-own-messages").description("Archive messages you send as well")
        .defaultValue(true).build());

    private final Setting<Boolean> includeTimestamps = sgGeneral.add(new BoolSetting.Builder()
        .name("include-timestamps").description("Include timestamps in archived messages")
        .defaultValue(true).build());

    private final Setting<Integer> maxMessagesPerFile = sgGeneral.add(new IntSetting.Builder()
        .name("max-messages-per-file").description("Maximum messages per file before creating a new one")
        .defaultValue(1000).min(100).max(10000).sliderMax(5000).build());

    private final Setting<Boolean> endOnDisconnect = sgSession.add(new BoolSetting.Builder()
        .name("end-on-disconnect").description("Mark end of discussion when disconnecting from server")
        .defaultValue(true).build());

    private final Setting<Integer> sessionTimeoutMinutes = sgSession.add(new IntSetting.Builder()
        .name("session-timeout").description("Minutes of inactivity before ending discussion session")
        .defaultValue(30).min(5).max(480).sliderMax(120).build());

    private final Setting<Boolean> logSessionMarkers = sgSession.add(new BoolSetting.Builder()
        .name("log-session-markers").description("Add session start/end markers to files")
        .defaultValue(true).build());

    private final Setting<Boolean> enableDebugLogging = sgDebug.add(new BoolSetting.Builder()
        .name("debug-to-file").description("Log debug info to debug file (safe, won't crash)")
        .defaultValue(false).build());

    // Pattern matching
    private static final Pattern TIMESTAMP_PREFIX = Pattern.compile("^<\\d{1,2}:\\d{2}>\\s*");
    private static final List<MessagePattern> PATTERNS = Arrays.asList(
        // Incoming patterns
        new MessagePattern(" whispers to you", true, true),
        new MessagePattern(" tells you", true, true),
        new MessagePattern(" messages you", true, true),
        new MessagePattern(" -> you", true, true),
        new MessagePattern(" -> YOU", true, true), // Added for case variations
        new MessagePattern(" whispers:", true, true),
        new MessagePattern("From ", false, true, 5),
        new MessagePattern("from ", false, true, 5),
        new MessagePattern("FROM ", false, true, 5),
        new MessagePattern("[", false, true) { // Special case for [Player -> You]
            @Override
            public MatchResult tryMatch(String msg) {
                if (!msg.startsWith("[") || !msg.contains(" -> You]")) return null;
                int arrowIndex = msg.indexOf(" -> You]");
                if (arrowIndex <= 1) return null;
                String player = msg.substring(1, arrowIndex).trim();
                String content = extractContent(msg, msg.indexOf("]") + 1);
                return new MatchResult(player, content, true);
            }
        },

        // Outgoing patterns
        new MessagePattern("You whisper to ", false, false, "You whisper to ".length()),
        new MessagePattern("You tell ", false, false, "You tell ".length()),
        new MessagePattern("You message ", false, false, "You message ".length()),
        new MessagePattern("To ", false, false, 3),
        new MessagePattern("to ", false, false, 3),
        new MessagePattern("TO ", false, false, 3),
        new MessagePattern("you -> ", false, false, "you -> ".length()),
        new MessagePattern("YOU -> ", false, false, "YOU -> ".length()), // Added for uppercase case
        new MessagePattern("[You -> ", false, false) { // Special case for [You -> Player]
            @Override
            public MatchResult tryMatch(String msg) {
                if (!msg.startsWith("[You -> ")) return null;
                int bracketIndex = msg.indexOf("]");
                if (bracketIndex <= "[You -> ".length()) return null;
                String player = msg.substring("[You -> ".length(), bracketIndex).trim();
                String content = extractContent(msg, bracketIndex + 1);
                return new MatchResult(player, content, false);
            }
        },
        new MessagePattern("You -> ", false, false, "You -> ".length()),
        new MessagePattern("Reply to ", false, false, "Reply to ".length()),

        // Custom patterns for Name -> YOU and YOU -> Name format
        new MessagePattern(null, false, false) { // Custom outgoing pattern for "YOU -> Name"
            @Override
            public MatchResult tryMatch(String msg) {
                if (!msg.startsWith("YOU -> ")) return null;
                int colonIndex = msg.indexOf(":");
                if (colonIndex <= "YOU -> ".length()) return null;
                String player = msg.substring("YOU -> ".length(), colonIndex).trim();
                String content = extractContent(msg, colonIndex);
                return new MatchResult(player, content, false);
            }
        },
        new MessagePattern(null, false, true) { // Custom incoming pattern for "Name -> YOU"
            @Override
            public MatchResult tryMatch(String msg) {
                if (!msg.contains(" -> YOU:")) return null;
                int arrowIndex = msg.indexOf(" -> YOU:");
                if (arrowIndex <= 0) return null;
                String player = msg.substring(0, arrowIndex).trim();
                String content = extractContent(msg, arrowIndex + " -> YOU:".length());
                return new MatchResult(player, content, true);
            }
        }
    );

    // State tracking
    private final Map<String, Integer> messageCountsPerPlayer = new HashMap<>();
    private final Map<String, Long> lastActivityPerPlayer = new HashMap<>();
    private final Map<String, Boolean> activeSessionsPerPlayer = new HashMap<>();
    private final List<String> debugQueue = new ArrayList<>();
    private String currentPlayerName;
    private int tickCounter = 0;

    public PrivateMessageArchiver() {
        super(LambdaUtilities.CATEGORY, "private-message-archiver",
            "Archives private messages/discussions in separate files per player.");
        updatePathInfo();
    }

    private void updatePathInfo() {
        pathInfo.set(FileUtils.getDisplayPath(BASE_PATH, SUB_FOLDER));
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (++tickCounter >= 20) {
            tickCounter = 0;
            checkSessionTimeouts();
            processDebugQueue();
        }
    }

    @EventHandler private void onGameLeft(GameLeftEvent event) {
        if (endOnDisconnect.get()) endAllSessions("DISCONNECTED");
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        try {
            if (event.getMessage() == null) return;
            String messageText = event.getMessage().getString();
            if (messageText == null || messageText.trim().isEmpty()) return;

            if (currentPlayerName == null) {
                currentPlayerName = ServerUtils.getCurrentPlayerName();
            }

            String cleanMessage = removeTimestamp(messageText);
            MatchResult result = tryMatchPatterns(cleanMessage);

            if (result != null) {
                if (!result.isIncoming && !logOwnMessages.get()) return;

                String sanitizedPlayerName = FileUtils.sanitizeFileName(result.playerName);
                archiveMessage(sanitizedPlayerName, result.content, result.isIncoming);

                if (enableDebugLogging.get()) {
                    queueDebugMessage("MATCHED: " + (result.isIncoming ? "IN" : "OUT") +
                        " | " + result.playerName + " | " + messageText);
                }
            } else if (enableDebugLogging.get() && containsPrivateMessageKeywords(cleanMessage)) {
                queueDebugMessage("UNMATCHED: " + messageText);
            }

        } catch (Exception e) {
            if (enableDebugLogging.get()) {
                queueDebugMessage("ERROR: " + e.getMessage());
            }
        }
    }

    private String removeTimestamp(String messageText) {
        Matcher matcher = TIMESTAMP_PREFIX.matcher(messageText);
        return matcher.find() ? messageText.substring(matcher.end()) : messageText;
    }

    private MatchResult tryMatchPatterns(String message) {
        for (MessagePattern pattern : PATTERNS) {
            MatchResult result = pattern.tryMatch(message);
            if (result != null) {
                result.playerName = cleanPlayerName(result.playerName);
                if (result.playerName != null && !result.playerName.isEmpty() &&
                    result.content != null && !result.content.isEmpty()) {
                    return result;
                }
            }
        }
        return null;
    }

    private String cleanPlayerName(String playerName) {
        if (playerName == null) return null;
        return playerName.replaceAll("^\\[.*?\\]\\s*", "")
            .replaceAll("\\s*\\[.*?\\]$", "")
            .replaceAll("^\\*+\\s*", "")
            .replaceAll("\\s*\\*+$", "")
            .trim();
    }

    private boolean containsPrivateMessageKeywords(String message) {
        String lower = message.toLowerCase();
        return lower.contains("whisper") || lower.contains("tell") || lower.contains("message") ||
            lower.contains(" -> ") || lower.contains("from ") || lower.contains("to ") ||
            lower.contains("reply");
    }

    private void archiveMessage(String playerName, String content, boolean isIncoming) {
        try {
            if (!activeSessionsPerPlayer.getOrDefault(playerName, false) && logSessionMarkers.get()) {
                startSession(playerName);
            }

            File archiveFile = getArchiveFile(playerName);
            String direction = isIncoming ? "FROM" : "TO";
            String messageContent = direction + " " + playerName + ": " + content;

            String logEntry = includeTimestamps.get() ?
                LogWriter.createSafeTimestampedEntry(messageContent) :
                messageContent + System.lineSeparator();

            if (LogWriter.writeLogEntry(archiveFile, logEntry)) {
                messageCountsPerPlayer.put(playerName, messageCountsPerPlayer.getOrDefault(playerName, 0) + 1);
                lastActivityPerPlayer.put(playerName, System.currentTimeMillis());
                activeSessionsPerPlayer.put(playerName, true);
            }
        } catch (Exception ignored) {}
    }

    private void startSession(String playerName) {
        try {
            if (logSessionMarkers.get()) {
                File archiveFile = getArchiveFile(playerName);
                String sessionInfo = "CONVERSATION STARTED WITH " + playerName.toUpperCase();
                String sessionEntry = LogWriter.createSessionSeparator(sessionInfo);
                LogWriter.writeLogEntry(archiveFile, sessionEntry);
            }
        } catch (Exception ignored) {}
    }

    private void endSession(String playerName, String reason) {
        try {
            if (logSessionMarkers.get()) {
                File archiveFile = getArchiveFile(playerName);
                String sessionInfo = "CONVERSATION ENDED WITH " + playerName.toUpperCase() + " - " + reason;
                LogWriter.writeLogEntry(archiveFile, LogWriter.createSessionSeparator(sessionInfo));
            }
            activeSessionsPerPlayer.put(playerName, false);
        } catch (Exception ignored) {}
    }

    private void checkSessionTimeouts() {
        try {
            long currentTime = System.currentTimeMillis();
            long timeoutMillis = sessionTimeoutMinutes.get() * 60 * 1000L;

            lastActivityPerPlayer.entrySet().removeIf(entry -> {
                String playerName = entry.getKey();
                if (activeSessionsPerPlayer.getOrDefault(playerName, false) &&
                    (currentTime - entry.getValue()) > timeoutMillis) {
                    endSession(playerName, "TIMEOUT (" + sessionTimeoutMinutes.get() + " minutes)");
                    return false;
                }
                return false;
            });
        } catch (Exception ignored) {}
    }

    private void endAllSessions(String reason) {
        activeSessionsPerPlayer.keySet().forEach(playerName -> {
            if (activeSessionsPerPlayer.get(playerName)) {
                endSession(playerName, reason);
            }
        });
    }

    private File getArchiveFile(String playerName) {
        String serverName = ServerUtils.getServerName();
        int currentCount = messageCountsPerPlayer.getOrDefault(playerName, 0);
        String timestamp = (currentCount >= maxMessagesPerFile.get()) ?
            "_" + LocalDateTime.now().format(FILE_TIMESTAMP) : "";

        if (currentCount >= maxMessagesPerFile.get()) {
            messageCountsPerPlayer.put(playerName, 0);
        }

        String fileName = playerName + timestamp + ".txt";
        return FileUtils.getServerLogFile(BASE_PATH, SUB_FOLDER, serverName, fileName);
    }

    private void queueDebugMessage(String message) {
        synchronized (debugQueue) {
            debugQueue.add(LocalDateTime.now() + " - " + message);
            if (debugQueue.size() > 1000) debugQueue.remove(0);
        }
    }

    private void processDebugQueue() {
        if (!enableDebugLogging.get() || debugQueue.isEmpty()) return;

        synchronized (debugQueue) {
            if (!debugQueue.isEmpty()) {
                StringBuilder batch = new StringBuilder();
                for (int i = 0; i < Math.min(10, debugQueue.size()); i++) {
                    batch.append(debugQueue.remove(0)).append(System.lineSeparator());
                }
                if (batch.length() > 0) {
                    File debugFile = FileUtils.getServerLogFile(BASE_PATH, SUB_FOLDER,
                        ServerUtils.getServerName(), "pma_debug_" + ServerUtils.getServerName() + ".txt");
                    LogWriter.writeLogEntry(debugFile, batch.toString());
                }
            }
        }
    }

    @Override public void onActivate() {
        try {
            updatePathInfo();
            String serverName = ServerUtils.getServerName();
            currentPlayerName = ServerUtils.getCurrentPlayerName();
            FileUtils.createServerDirectoryStructure(BASE_PATH, SUB_FOLDER, serverName);
        } catch (Exception ignored) {}
    }

    @Override public void onDeactivate() {
        if (endOnDisconnect.get()) endAllSessions("MODULE DEACTIVATED");
        if (enableDebugLogging.get()) processDebugQueue();
    }

    // Helper classes
    private static class MatchResult {
        String playerName, content;
        boolean isIncoming;

        MatchResult(String playerName, String content, boolean isIncoming) {
            this.playerName = playerName;
            this.content = content;
            this.isIncoming = isIncoming;
        }
    }

    private static class MessagePattern {
        final String pattern;
        final boolean contains, isIncoming;
        final int prefixLength;

        MessagePattern(String pattern, boolean contains, boolean isIncoming) {
            this(pattern, contains, isIncoming, 0);
        }

        MessagePattern(String pattern, boolean contains, boolean isIncoming, int prefixLength) {
            this.pattern = pattern;
            this.contains = contains;
            this.isIncoming = isIncoming;
            this.prefixLength = prefixLength;
        }

        MatchResult tryMatch(String message) {
            if (pattern == null) return null; // For custom patterns

            boolean matches = contains ? message.contains(pattern) : message.startsWith(pattern);
            if (!matches) return null;

            int patternIndex = contains ? message.indexOf(pattern) : 0;
            String playerName = message.substring(prefixLength,
                contains ? patternIndex : message.indexOf(":")).trim();

            int contentStart = contains ? patternIndex + pattern.length() : message.indexOf(":") + 1;
            String content = extractContent(message, contentStart);

            return new MatchResult(playerName, content, isIncoming);
        }

        protected String extractContent(String message, int start) {
            if (start >= message.length()) return "";
            if (message.charAt(start) == ':') start++;
            return message.substring(start).trim();
        }
    }
}
