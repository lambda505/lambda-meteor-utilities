package com.lambda505.meteorutils.utils;

import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

public class FileUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /**
     * Creates the full directory structure for a given base path and sub folder
     * @param basePath The base directory name
     * @param subFolder The sub folder name
     * @return true if directories exist or were created successfully
     */
    public static boolean createDirectoryStructure(String basePath, String subFolder) {
        try {
            // Create base directory
            Path baseDirPath = Paths.get(mc.runDirectory.getAbsolutePath(), basePath);
            if (!Files.exists(baseDirPath)) {
                Files.createDirectories(baseDirPath);
            }

            // Create sub folder
            Path subDirPath = Paths.get(mc.runDirectory.getAbsolutePath(), basePath, subFolder);
            if (!Files.exists(subDirPath)) {
                Files.createDirectories(subDirPath);
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Creates a directory structure with an additional server-specific folder
     * @param basePath The base directory name
     * @param subFolder The sub folder name
     * @param serverName The server-specific folder name
     * @return true if directories exist or were created successfully
     */
    public static boolean createServerDirectoryStructure(String basePath, String subFolder, String serverName) {
        try {
            // Create base and sub directories first
            if (!createDirectoryStructure(basePath, subFolder)) {
                return false;
            }

            // Create server-specific directory
            Path serverDirPath = Paths.get(mc.runDirectory.getAbsolutePath(), basePath, subFolder, serverName);
            if (!Files.exists(serverDirPath)) {
                Files.createDirectories(serverDirPath);
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Gets a file path for a given base path, sub folder, and filename
     * @param basePath The base directory name
     * @param subFolder The sub folder name
     * @param fileName The file name
     * @return File object representing the path
     */
    public static File getLogFile(String basePath, String subFolder, String fileName) {
        Path logPath = Paths.get(mc.runDirectory.getAbsolutePath(), basePath, subFolder, fileName);
        return logPath.toFile();
    }

    /**
     * Gets a file path for a server-specific file
     * @param basePath The base directory name
     * @param subFolder The sub folder name
     * @param serverName The server name
     * @param fileName The file name
     * @return File object representing the path
     */
    public static File getServerLogFile(String basePath, String subFolder, String serverName, String fileName) {
        Path logPath = Paths.get(mc.runDirectory.getAbsolutePath(), basePath, subFolder, serverName, fileName);
        return logPath.toFile();
    }

    /**
     * Gets the full path string for display purposes
     * @param basePath The base directory name
     * @param subFolder The sub folder name
     * @return Formatted path string with actual file system path
     */
    public static String getDisplayPath(String basePath, String subFolder) {
        try {
            Path fullPath = Paths.get(mc.runDirectory.getAbsolutePath(), basePath, subFolder);
            return basePath + File.separator + subFolder + " (" + fullPath.toString() + ")";
        } catch (Exception e) {
            return basePath + File.separator + subFolder;
        }
    }

    /**
     * Sanitizes a filename by replacing invalid characters with underscores
     * @param fileName The filename to sanitize
     * @return Sanitized filename in lowercase
     */
    public static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[<>:\"/\\\\|?*]", "_").toLowerCase();
    }
}
