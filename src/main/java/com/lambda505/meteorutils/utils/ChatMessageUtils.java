package com.lambda505.meteorutils.utils;

import net.minecraft.text.Text;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatMessageUtils {

    /**
     * Extracts player name from various chat message formats
     * @param message The chat message Text object
     * @return Player name or "Unknown" if not found
     */
    public static String extractPlayerName(Text message) {
        String fullText = message.getString();

        // Common chat formats: <PlayerName> message, [PlayerName] message, PlayerName: message
        Pattern namePattern = Pattern.compile("^(?:<([^>]+)>|\\[([^\\]]+)\\]|([^:]+):)");
        Matcher nameMatcher = namePattern.matcher(fullText);

        if (nameMatcher.find()) {
            for (int i = 1; i <= nameMatcher.groupCount(); i++) {
                if (nameMatcher.group(i) != null) {
                    return nameMatcher.group(i).trim();
                }
            }
        }

        return "Unknown";
    }

    /**
     * Checks if a message is from the current player
     * @param message The chat message Text object
     * @param playerName The current player's name
     * @return true if the message is from the current player
     */
    public static boolean isOwnMessage(Text message, String playerName) {
        if (playerName == null) return false;
        String senderName = extractPlayerName(message);
        return senderName.equals(playerName);
    }
}
