package com.lambda505.meteorutils.utils;

import net.minecraft.client.MinecraftClient;

public class ServerUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Gets the current server name, sanitized for use in filenames
     * @return Sanitized server name or appropriate fallback
     */
    public static String getServerName() {
        try {
            if (mc.getCurrentServerEntry() != null) {
                String serverAddress = mc.getCurrentServerEntry().address;
                // Remove port if present and sanitize for filename
                String serverName = serverAddress.split(":")[0];
                return FileUtils.sanitizeFileName(serverName);
            } else if (mc.isInSingleplayer()) {
                return getSingleplayerWorldName();
            } else {
                return "unknown_server";
            }
        } catch (Exception e) {
            return "unknown_server";
        }
    }

    /**
     * Gets the singleplayer world name with various fallback strategies
     * @return Sanitized world name or appropriate fallback
     */
    public static String getSingleplayerWorldName() {
        try {
            // Try to get the world name from the integrated server
            if (mc.getServer() != null && mc.getServer().getSaveProperties() != null) {
                String worldName = mc.getServer().getSaveProperties().getLevelName();
                if (worldName != null && !worldName.trim().isEmpty()) {
                    return FileUtils.sanitizeFileName(worldName);
                }
            }

            // Fallback: try to get from world directory name
            if (mc.world != null) {
                String worldDir = mc.world.getRegistryKey().getValue().getPath();
                if (worldDir != null && !worldDir.trim().isEmpty() && !worldDir.equals("overworld")) {
                    return FileUtils.sanitizeFileName(worldDir);
                }
            }

            // Last fallback: try session
            if (mc.getSession() != null) {
                String sessionName = mc.getSession().getUsername() + "_world";
                return FileUtils.sanitizeFileName(sessionName);
            }

        } catch (Exception e) {
            // If all else fails, use a timestamp-based name
            return "singleplayer_" + System.currentTimeMillis();
        }

        return "singleplayer_unknown";
    }

    /**
     * Gets the current player's username
     * @return Player username or "unknown"
     */
    public static String getCurrentPlayerName() {
        try {
            if (mc.player != null) {
                return mc.player.getName().getString();
            } else if (mc.getSession() != null) {
                return mc.getSession().getUsername();
            }
        } catch (Exception e) {
            // Fall through to unknown
        }
        return "unknown";
    }
}
