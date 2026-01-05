# Changes Summary - Two-Phase Scanning Implementation

## User Request (Comment #3711080546)

The user requested:
1. Report generation should be done in parallel with analysis
2. Enable restart capability in case of disconnection
3. Fill all file stats first, leave hashes for last
4. Hash files from smaller to bigger

## Implementation Details

### 1. Modified FileInfo Model
- Added optional `hash` field (defaults to empty string)
- Added `isHashed` boolean flag to track hashing status
- Enables files to exist without hash during Phase 1

### 2. Two-Phase Scanning Process

**Phase 1: Metadata Collection**
```
Scan directories → Collect file info (path, size, type, disk) → No hashing yet
```
- Fast recursive directory traversal
- Collects all file metadata without computing hashes
- Files displayed immediately as found

**Phase 2: Hashing (Smallest to Largest)**
```
Sort files by size → Hash from smallest first → Write to CSV immediately
```
- Files sorted by size before hashing
- Smaller files hashed first (better perceived performance)
- Progress percentage displayed
- Each hashed file written to CSV immediately (if streaming enabled)

### 3. Streaming CSV Export

**New Features:**
- Checkbox: "Write CSV during scan (enables restart capability)"
- Output path selection before scan starts
- CSV writer flushes after each file record
- Enables restart capability - partial results saved if interrupted

**StreamingCsvWriter Class:**
- `writeFile()` - Writes single file and flushes immediately
- `close()` - Properly closes resources
- Buffered writing with explicit flush for data safety

### 4. UI Enhancements

**New Controls:**
- Streaming CSV checkbox (enabled by default)
- CSV output path text field with browse button
- Phase indicators in progress messages
- Hash status shown in results ("Hash: (not computed)" vs actual hash)

**Progress Messages:**
- "Phase 1: Collecting file metadata..."
- "Phase 2: Hashing files (smallest to largest)..."
- "Hashing [45%]: filename.jpg (2.5 MB)"
- Completion message includes CSV path if streaming enabled

## Benefits

1. **Faster Initial Feedback**: Phase 1 completes quickly, showing all files found
2. **Better Progress Tracking**: Percentage progress during Phase 2 hashing
3. **Restart Capability**: CSV flushed after each file - can resume after interruption
4. **Efficient Processing**: Smaller files hashed first provide quick results
5. **Data Safety**: Streaming CSV ensures partial results are saved

## Code Changes

### Modified Files:
1. `FileInfo.kt` - Added optional hash and isHashed flag
2. `FileScanner.kt` - Implemented two-phase scanning with size-based sorting
3. `CsvReportGenerator.kt` - Added StreamingCsvWriter for incremental writing
4. `DedupplerApp.kt` - Updated UI with streaming CSV controls
5. `README.md` - Documented new features and usage

### New Callbacks:
- `onFileFound` - Called when file metadata collected (Phase 1)
- `onFileHashed` - Called when file has been hashed (Phase 2)

## Testing

Build status: ✅ SUCCESS
- All files compile without errors
- Warning fixed (unused parameter renamed to `_`)
- Gradle wrapper updated

## Performance Characteristics

**Before:**
- Single-phase: Find file → Hash immediately → Process next file
- Blocking: Each file must be hashed before continuing
- No checkpoint capability

**After:**
- Phase 1: Scan all files quickly (no hashing)
- Phase 2: Sort by size, hash smallest first
- Streaming: Save results as we go
- Restart: Can resume from partial CSV

**User Experience:**
- Fast initial results (Phase 1 shows all files quickly)
- Progressive updates during hashing with percentage
- Smaller files complete first (better perceived performance)
- CSV available during scan (not just at end)
