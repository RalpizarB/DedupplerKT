# DedupplerKT Implementation Summary

## Project Overview
A Kotlin Compose Desktop application for finding duplicate files using Blake2b hashing algorithm.

## Technology Stack
- **Build System**: Gradle 8.4 (with wrapper)
- **Language**: Kotlin 1.9.20
- **UI Framework**: Jetpack Compose Desktop 1.5.10
- **Hashing**: BouncyCastle Blake2b-512
- **CSV Export**: Apache Commons CSV 1.10.0
- **Concurrency**: Kotlin Coroutines 1.7.3

## Project Structure

```
DedupplerKT/
├── build.gradle.kts          # Gradle build configuration
├── settings.gradle.kts        # Gradle settings
├── gradlew                    # Gradle wrapper script
├── gradle/wrapper/            # Gradle wrapper files
├── src/main/kotlin/com/deduppler/
│   ├── Main.kt                # Application entry point
│   ├── model/
│   │   └── FileInfo.kt        # Data model for file information
│   ├── scanner/
│   │   └── FileScanner.kt     # File scanning logic
│   ├── ui/
│   │   └── DedupplerApp.kt    # Compose Desktop UI
│   └── utils/
│       ├── CsvReportGenerator.kt  # CSV export functionality
│       ├── FileTypeDetector.kt    # File type detection
│       └── HashUtils.kt           # Blake2b hashing utility
└── README.md                  # Documentation
```

## Key Features Implemented

### 1. File Scanning
- Scan a specific directory
- Scan all available disks on the system
- Skip system directories (Windows, Program Files, AppData, etc.)
- Real-time progress updates

### 2. File Type Filtering
- **Media files only mode** (default): Music, Video, Image
- Configurable filters for each type
- Option to include other file types
- Supported formats:
  - Music: mp3, wav, flac, aac, ogg, m4a, wma, aiff, ape, opus
  - Video: mp4, avi, mkv, mov, wmv, flv, webm, m4v, mpg, mpeg, 3gp
  - Image: jpg, jpeg, png, gif, bmp, tiff, tif, webp, svg, ico, heic, heif

### 3. Blake2b Hashing
- Uses Blake2b-512 algorithm via BouncyCastle
- Secure and fast file comparison
- Full hash stored in results

### 4. CSV Report Export
Format: `FullPath, FileExt, Hash, Disk, Size, Type`

**Important**: Size is displayed in MB as a numeric value only (e.g., "5.00") for better sorting in spreadsheet applications.

Example CSV output:
```csv
FullPath,FileExt,Hash,Disk,Size,Type
/home/user/music/song.mp3,mp3,abc123...,C:,5.25,music
/home/user/videos/movie.mp4,mp4,def456...,C:,125.50,video
```

### 5. Compose Desktop UI
- Modern Material Design interface
- Directory selection with file browser
- Checkboxes for scan options and file type filters
- Real-time progress indication
- Scrollable results list showing:
  - Full file path
  - File type
  - Size in MB
  - Hash preview (first 16 characters)
- Export button to save CSV reports

### 6. Duplicate Detection
- Groups files by hash value
- Displays count of duplicate groups
- All files with same hash are duplicates

## Building and Running

### Build
```bash
./gradlew build
```

### Run
```bash
./gradlew run
```

### Create Native Package
```bash
# Current platform
./gradlew packageDistributionForCurrentOS

# Specific platforms
./gradlew packageDeb    # Linux
./gradlew packageMsi    # Windows
./gradlew packageDmg    # macOS
```

## Code Highlights

### Blake2b Hashing (HashUtils.kt)
```kotlin
fun calculateBlake2bHash(file: File): String {
    val digest = MessageDigest.getInstance("BLAKE2B-512", "BC")
    FileInputStream(file).use { fis ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
```

### CSV Size Formatting (CsvReportGenerator.kt)
```kotlin
// Convert size from bytes to MB, display as number only for better sorting
val sizeInMB = String.format("%.2f", file.size / (1024.0 * 1024.0))
```

### File Scanner (FileScanner.kt)
- Recursive directory traversal
- Excluded directory filtering
- File type detection
- Progress callbacks
- Coroutine-based for async operation

### Compose UI (DedupplerApp.kt)
- Reactive state management with `remember` and `mutableStateOf`
- Coroutine scopes for background tasks
- Material Design components
- File chooser dialogs using Swing interop

## Testing

The application includes test files in `/tmp/test_deduppler/` for quick verification:
- Music files (.mp3)
- Images (.jpg)
- Videos (.mp4)
- Duplicate files with same content

## Requirements Met

✅ Kotlin Compose Desktop application
✅ Gradle build system (switched from Maven for better Compose support)
✅ Scan directories or all disks
✅ Media file filtering (music, video, image)
✅ System directory exclusion
✅ Blake2b hashing algorithm
✅ CSV export with format: FullPath, FileExt, Hash, Disk, Size (MB), Type
✅ Size in MB as numeric value only for better sorting
✅ Modern UI with progress indication
✅ Duplicate detection and reporting

## Future Enhancements (Not Implemented)
- Command-line interface option
- Batch delete duplicates
- File preview
- Compare by file content vs. name
- Save/load scan sessions
- Multi-threaded scanning for better performance
- Progress percentage indicator
