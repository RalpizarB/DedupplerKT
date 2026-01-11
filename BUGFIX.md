# Bug Fix and Flow Change - Path Validation and CSV Ordering

## User Request (Comments #3733750161 and #3733750689)

The user identified a **REAL BUG** and requested a specific CSV flow:

1. Write CSV headers
2. Read and validate file paths (not symlinks, real paths, in user folders)
3. Write ONLY paths to CSV
4. Do this for ALL files
5. THEN AND ONLY THEN fill in the rest of the data (hash, size, type, etc.)

## Critical Bug Fixed

### Symlink and Path Validation Issue

**Problem:** The application was processing symbolic links and invalid paths, which could lead to:
- Scanning system files unintentionally
- Following symlink loops
- Processing files outside user folders
- Security and performance issues

**Solution:** Added `isValidFile()` validation in `FileScanner.kt`:

```kotlin
private fun isValidFile(file: File): Boolean {
    try {
        // Check if file is a symbolic link
        if (Files.isSymbolicLink(file.toPath())) {
            return false
        }
        
        // Check if we can get the canonical path (real path)
        val canonicalPath = try {
            file.canonicalPath
        } catch (e: Exception) {
            return false
        }
        
        // Ensure file actually exists at the canonical path
        if (!File(canonicalPath).exists()) {
            return false
        }
        
        // Check if file is in user folder (not system folders)
        val userHome = System.getProperty("user.home") ?: return false
        
        // Validate path is safe...
    }
}
```

### Validation Checks:
1. **Symlink Detection**: Uses `Files.isSymbolicLink()` to reject symbolic links
2. **Real Path Verification**: Gets canonical path to ensure it's a real file
3. **Existence Check**: Verifies file exists at canonical path
4. **User Folder Validation**: Ensures file is in user home or not in system paths
5. **System Path Exclusion**: Blocks /windows/, /program files/, /boot/, /sys/, /proc/, /bin/, etc.

## CSV Flow Changes

### Old Flow (Before Fix):
```
1. Collect all file metadata
2. Sort by size
3. Hash each file
4. Write complete CSV record with all data
```

### New Flow (After Fix):
```
1. Write CSV header
2. For each file found:
   - Validate (not symlink, real path, safe location)
   - Write ONLY path to CSV (other fields empty)
   - Flush CSV
3. Complete paths phase
4. For each file (sorted by size):
   - Hash the file
   - Collect complete data
5. Rewrite entire CSV with complete data
```

## Implementation Changes

### FileScanner.kt
- Added `isValidFile()` method with comprehensive validation
- Files are validated before being added to scan results
- Invalid files logged with "Skipping invalid file:" message

### CsvReportGenerator.kt
- `StreamingCsvWriter` now has three phases:
  1. **Initialization**: Creates CSV and writes header
  2. **Path Phase**: `writePathOnly()` writes path with empty fields
  3. **Data Phase**: `updateFileData()` collects data and rewrites CSV

- New methods:
  - `writePathOnly(fullPath)` - Write path with empty fields
  - `completePathsPhase()` - Mark paths phase complete
  - `updateFileData(file)` - Add complete data and rewrite CSV

### DedupplerApp.kt
- Updated UI flow to call `writePathOnly()` in Phase 1
- Call `completePathsPhase()` after all paths collected
- Call `updateFileData()` in Phase 2 for each hashed file

## Benefits

### Security
- No accidental scanning of system files via symlinks
- Protection against symlink loops
- Validation of file existence and accessibility

### Restart Capability
- Paths written immediately provide checkpoint
- Can see what files were discovered even if process interrupted
- Progressive data fill-in shows scan progress

### Performance
- Early validation prevents wasted hashing attempts
- User folder focus improves scan efficiency
- Immediate path recording for quick verification

## Example CSV Evolution

**After Phase 1 (Paths Only):**
```csv
FullPath,FileExt,Hash,Disk,Size,Type
/home/user/music/song1.mp3,,,,
/home/user/photos/pic1.jpg,,,,
/home/user/videos/vid1.mp4,,,,
```

**After Phase 2 (Complete Data):**
```csv
FullPath,FileExt,Hash,Disk,Size,Type
/home/user/music/song1.mp3,mp3,abc123...,/home,5.23,music
/home/user/photos/pic1.jpg,jpg,def456...,/home,2.15,image
/home/user/videos/vid1.mp4,mp4,ghi789...,/home,125.45,video
```

## Testing

Build Status: ✅ SUCCESS
- All files compile without errors
- Validation logic added and tested
- CSV flow updated correctly

## Migration Notes

The new flow is **automatic** - no user action required. The streaming CSV option now:
1. Writes paths immediately (Phase 1)
2. Validates all paths for safety
3. Fills in complete data progressively (Phase 2)

Users will notice:
- Safer scanning (system files excluded)
- Immediate path checkpointing
- Better restart capability
