# Lambda Meteor Utilities - Utility Classes

This document describes the utility classes used in the Lambda Meteor Utilities addon to reduce code duplication and provide common functionality across modules.

## Overview

The utility classes are located in the `com.lambda505.meteorutils.utils` package and provide shared functionality for file operations, server detection, chat message processing, and logging.

## Utility Classes

### FileUtils.java

Handles file and directory operations with proper error handling.

#### Key Methods:
- **`createDirectoryStructure(String basePath, String subFolder)`**
    - Creates base directory and subfolder structure
    - Returns `true` if directories exist or were created successfully

- **`createServerDirectoryStructure(String basePath, String subFolder, String serverName)`**
    - Creates directory structure with server-specific folder
    - Used for organizing logs by server

- **`getLogFile(String basePath, String subFolder, String fileName)`**
    - Returns File object for a log file path
    - For simple two-level directory structure

- **`getServerLogFile(String basePath, String subFolder, String serverName, String fileName)`**
    - Returns File object for server-specific log file
    - Creates three-level directory structure

- **`getDisplayPath(String basePath, String subFolder)`**
    - Returns formatted path string for UI display
    - Includes actual filesystem path in parentheses

- **`sanitizeFileName(String fileName)`**
    - Removes invalid filename characters
    - Converts to lowercase for consistency

#### Example Usage:
```java
// Create directories
FileUtils.createDirectoryStructure("LambdaMeteorUtilities", "ChatCoordLeaks");

// Get log file
File logFile = FileUtils.getLogFile("LambdaMeteorUtilities", "ChatCoordLeaks", "server.txt");

// Sanitize filename
String safe = FileUtils.sanitizeFileName("2b2t.org:25565"); // Returns "2b2t_org_25565"
```

### ServerUtils.java

Provides server identification and player information utilities.

#### Key Methods:
- **`getServerName()`**
    - Returns current server name/IP for file organization
    - Handles null safety and fallback to "unknown"

- **`getCurrentPlayerName()`**
    - Gets current player's username
    - Returns null-safe string for player identification

#### Example Usage:
```java
String server = ServerUtils.getServerName(); // "2b2t.org"
String player = ServerUtils.getCurrentPlayerName(); // "lambda505"
```

### LogWriter.java

Centralized logging functionality with consistent formatting and error handling.

#### Key Methods:
- **`writeLogEntry(File logFile, String entry)`**
    - Writes log entry to specified file
    - Handles file creation and error cases
    - Returns `true` if successful

- **`createTimestampedFormattedEntry(String format, Object... args)`**
    - Creates formatted log entry with timestamp
    - Uses String.format() for safe formatting

- **`createFormattedEntry(String format, Object... args)`**
    - Creates formatted log entry without timestamp
    - Safe formatting with error handling

- **`createSafeTimestampedEntry(String content)`**
    - Creates timestamped entry with safe content handling
    - Handles null and special characters

- **`createSessionSeparator(String sessionInfo)`**
    - Creates visual separator for session boundaries
    - Used in conversation logging

#### Example Usage:
```java
File logFile = new File("coords.txt");
String entry = LogWriter.createTimestampedFormattedEntry("Player: %s | Coords: %d, %d, %d", 
    playerName, x, y, z);
LogWriter.writeLogEntry(logFile, entry);
```

### ChatMessageUtils.java

Utilities for processing and analyzing chat messages.

#### Key Methods:
- **`extractPlayerName(Text message)`**
    - Extracts player name from various chat message formats
    - Handles common chat prefixes: `<PlayerName>`, `[PlayerName]`, `PlayerName:`
    - Returns "Unknown" if no player name found

- **`isOwnMessage(Text message, String playerName)`**
    - Checks if a message is from the current player
    - Used for filtering own messages in logging

#### Example Usage:
```java
Text chatMessage = event.getMessage();
String sender = ChatMessageUtils.extractPlayerName(chatMessage);
boolean isMyMessage = ChatMessageUtils.isOwnMessage(chatMessage, "lambda505");
```

## Directory Structure

All utilities create consistent directory structures:
```
.minecraft/
└── LambdaMeteorUtilities/
    ├── ChatCoordLeaks/
    │   └── server_logs.txt
    └── PrivateMessageArchiver/
        └── [server_name]/
            ├── player1.txt
            ├── player2.txt
            └── debug_logs.txt
```

## Error Handling

All utility classes implement safe error handling:
- Silent failures to prevent crashes
- Null safety checks
- Graceful degradation when file operations fail
- Debug logging available for troubleshooting
