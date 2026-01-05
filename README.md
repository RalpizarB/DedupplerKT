# DedupplerKT

Kotlin Compose Desktop application that finds duplicate files using Blake2b hashing.

## Features

- **Compose Desktop UI** - Modern, native desktop application built with Jetpack Compose
- **Two-phase scanning** - First collects file metadata, then hashes from smallest to largest
- **Streaming CSV export** - Write results during scan for restart capability
- **Scan directories** for duplicate files
- **Scan all disks** on the system
- **Filter by file type**: Music, Video, Image, or Other
- **Media files only mode** (default) - scans only media files
- **Smart directory skipping** - automatically skips system directories (Windows, AppData, Program Files, etc.)
- **Blake2b hashing** - uses secure Blake2b-512 algorithm for file comparison
- **Efficient hashing** - Files are hashed from smallest to largest for better performance
- **CSV export** - generates detailed reports with:
  - Full file path
  - File extension
  - Blake2b hash
  - Disk location
  - File size in MB (numeric only for better sorting)
  - File type (music, video, image, other)

## Requirements

- Java 17 or higher
- Gradle 8.4 or higher (wrapper included)

## Building

```bash
./gradlew build
```

This creates an executable JAR in `build/libs/DedupplerKT-1.0.0.jar`

## Running

```bash
./gradlew run
```

Or run the packaged application:

```bash
java -jar build/libs/DedupplerKT-1.0.0.jar
```

## Creating Native Packages

Create platform-specific packages:

```bash
# For your current platform
./gradlew packageDistributionForCurrentOS

# Specific platforms
./gradlew packageDeb    # Linux .deb
./gradlew packageMsi    # Windows .msi
./gradlew packageDmg    # macOS .dmg
```

## Usage

### Scanning Process

The application uses a **two-phase scanning approach**:

**Phase 1: Metadata Collection**
- Recursively scans directories
- Collects file information (path, size, type, disk)
- No hashing yet - very fast
- Files are displayed as they're found

**Phase 2: Hashing**
- Files are sorted by size (smallest first)
- Blake2b-512 hash computed for each file
- Progress shown with percentage complete
- Smaller files hash faster, providing quick feedback

### Streaming CSV Export

Enable "Write CSV during scan" to:
- Write results incrementally as files are hashed
- Enable restart capability if process is interrupted
- CSV is flushed after each file for data safety
- Specify output path before starting scan

### Step-by-Step Guide

1. **Choose scanning mode**:
   - Enter a directory path or use "Browse" to select a folder
   - Or check "Scan all disks" to scan all available drives

2. **Configure streaming CSV (recommended)**:
   - Check "Write CSV during scan" for restart capability
   - Browse to select output CSV path
   - CSV will be written as files are processed

3. **Select file types**:
   - "Media files only" (default): Scans only music, video, and image files
   - Uncheck to include other file types
   - Select specific media types to scan (Music, Video, Image)

4. **Start scanning**:
   - Click "Start Scan" to begin
   - Phase 1: File metadata collection (fast)
   - Phase 2: Hashing from smallest to largest (shows progress %)
   - Results appear in the list as files are processed

5. **Export results** (if not using streaming CSV):
   - Click "Export CSV Report" to save results
   - CSV includes all file information with size in MB for easy sorting

## CSV Report Format

The exported CSV contains the following columns:

| Column | Description |
|--------|-------------|
| FullPath | Complete file path |
| FileExt | File extension |
| Hash | Blake2b-512 hash (full) |
| Disk | Disk/drive location |
| Size | File size in MB (numeric only) |
| Type | File type (music, video, image, other) |

## Supported File Types

### Music
mp3, wav, flac, aac, ogg, m4a, wma, aiff, ape, opus

### Video
mp4, avi, mkv, mov, wmv, flv, webm, m4v, mpg, mpeg, 3gp

### Image
jpg, jpeg, png, gif, bmp, tiff, tif, webp, svg, ico, heic, heif

## Excluded Directories

The scanner automatically skips these system directories:
- AppData
- Program Files / Program Files (x86)
- Windows / System32
- ProgramData
- $Recycle.Bin
- System Volume Information
- Recovery
- Boot
- WinNT
- Windows.old
- PerfLogs
- MSOCache

## Technology Stack

- **Language**: Kotlin 1.9.20
- **Build Tool**: Gradle 8.4
- **UI**: Jetpack Compose Desktop 1.5.10
- **Hashing**: BouncyCastle (Blake2b-512)
- **CSV Export**: Apache Commons CSV
- **Concurrency**: Kotlin Coroutines

## License

MIT License

