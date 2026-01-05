package com.deduppler.scanner

import com.deduppler.model.FileInfo
import com.deduppler.model.FileType
import com.deduppler.utils.FileTypeDetector
import com.deduppler.utils.HashUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class FileScanner(
    private val onProgress: (String) -> Unit = {},
    private val onFileFound: (FileInfo) -> Unit = {},
    private val onFileHashed: (FileInfo) -> Unit = {}
) {
    private val excludedDirNames = setOf(
        "appdata", "program files", "program files (x86)", "windows", "system32",
        "programdata", "\$recycle.bin", "system volume information", "recovery",
        "boot", "winnt", "windows.old", "perflogs", "msocache"
    )
    
    data class ScanOptions(
        val scanAllDisks: Boolean = false,
        val mediaOnly: Boolean = true,
        val includeMusic: Boolean = true,
        val includeVideo: Boolean = true,
        val includeImage: Boolean = true,
        val includeOther: Boolean = false
    )
    
    /**
     * Two-phase scanning: First collect all file metadata, then hash from smallest to largest
     */
    fun scanDirectory(rootPath: String, options: ScanOptions): List<FileInfo> {
        val rootFile = File(rootPath)
        if (!rootFile.exists() || !rootFile.isDirectory) {
            onProgress("Error: Invalid directory path")
            return emptyList()
        }
        
        onProgress("Starting scan of $rootPath")
        
        // Phase 1: Collect file metadata (no hashing yet)
        val filesWithoutHash = mutableListOf<FileInfo>()
        onProgress("Phase 1: Collecting file metadata...")
        collectFileMetadata(rootFile, options, filesWithoutHash)
        
        // Phase 2: Hash files from smallest to largest
        onProgress("Phase 2: Hashing files (smallest to largest)...")
        val hashedFiles = hashFilesInOrder(filesWithoutHash)
        
        return hashedFiles
    }
    
    fun scanAllDisks(options: ScanOptions): List<FileInfo> {
        val roots = File.listRoots()
        onProgress("Found ${roots.size} disk(s) to scan")
        
        // Phase 1: Collect metadata from all disks
        val filesWithoutHash = mutableListOf<FileInfo>()
        onProgress("Phase 1: Collecting file metadata from all disks...")
        
        for (root in roots) {
            if (root.exists() && root.canRead()) {
                onProgress("Scanning disk: ${root.absolutePath}")
                collectFileMetadata(root, options, filesWithoutHash)
            }
        }
        
        // Phase 2: Hash files from smallest to largest
        onProgress("Phase 2: Hashing files (smallest to largest)...")
        val hashedFiles = hashFilesInOrder(filesWithoutHash)
        
        return hashedFiles
    }
    
    /**
     * Phase 1: Recursively collect file metadata without hashing
     */
    private fun collectFileMetadata(
        directory: File,
        options: ScanOptions,
        allFiles: MutableList<FileInfo>
    ) {
        try {
            val entries = directory.listFiles() ?: return
            
            for (entry in entries) {
                try {
                    if (entry.isDirectory) {
                        if (shouldSkipDirectory(entry)) {
                            continue
                        }
                        collectFileMetadata(entry, options, allFiles)
                    } else if (entry.isFile) {
                        if (shouldProcessFile(entry, options)) {
                            val fileInfo = createFileInfoWithoutHash(entry)
                            allFiles.add(fileInfo)
                            onFileFound(fileInfo)  // Report file found immediately
                            onProgress("Found: ${entry.name} (${formatSize(entry.length())})")
                        }
                    }
                } catch (e: Exception) {
                    // Skip files/directories that can't be accessed
                }
            }
        } catch (e: Exception) {
            // Skip directories that can't be accessed
        }
    }
    
    /**
     * Create FileInfo with metadata but without hash
     */
    private fun createFileInfoWithoutHash(file: File): FileInfo {
        val fileType = FileTypeDetector.detectFileType(file)
        val disk = getDiskFromPath(file.absolutePath)
        
        return FileInfo(
            fullPath = file.absolutePath,
            fileExt = file.extension,
            hash = "",  // No hash yet
            disk = disk,
            size = file.length(),
            type = fileType,
            isHashed = false
        )
    }
    
    /**
     * Phase 2: Hash files sorted from smallest to largest
     */
    private fun hashFilesInOrder(filesWithoutHash: List<FileInfo>): List<FileInfo> {
        val hashedFiles = mutableListOf<FileInfo>()
        
        // Sort by size (smallest first)
        val sortedFiles = filesWithoutHash.sortedBy { it.size }
        
        onProgress("Hashing ${sortedFiles.size} files from smallest to largest...")
        
        sortedFiles.forEachIndexed { index, fileInfo ->
            try {
                val file = File(fileInfo.fullPath)
                if (file.exists()) {
                    val hash = HashUtils.calculateBlake2bHash(file)
                    val hashedFileInfo = fileInfo.copy(hash = hash, isHashed = true)
                    hashedFiles.add(hashedFileInfo)
                    onFileHashed(hashedFileInfo)  // Notify that file has been hashed
                    
                    val progress = ((index + 1) * 100) / sortedFiles.size
                    onProgress("Hashing [$progress%]: ${file.name} (${formatSize(fileInfo.size)})")
                } else {
                    // File no longer exists, add without hash
                    hashedFiles.add(fileInfo)
                }
            } catch (e: Exception) {
                // If hashing fails, add file without hash
                hashedFiles.add(fileInfo)
                onProgress("Error hashing ${fileInfo.fullPath}: ${e.message}")
            }
        }
        
        return hashedFiles
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.2f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    private fun shouldSkipDirectory(directory: File): Boolean {
        val dirNameLower = directory.name.lowercase()
        return excludedDirNames.any { excluded ->
            dirNameLower.contains(excluded)
        }
    }
    
    private fun shouldProcessFile(file: File, options: ScanOptions): Boolean {
        val fileType = FileTypeDetector.detectFileType(file)
        
        if (options.mediaOnly && fileType == FileType.OTHER) {
            return false
        }
        
        return when (fileType) {
            FileType.MUSIC -> options.includeMusic
            FileType.VIDEO -> options.includeVideo
            FileType.IMAGE -> options.includeImage
            FileType.OTHER -> options.includeOther
        }
    }
    
    private fun getDiskFromPath(path: String): String {
        val normalized = path.replace('\\', '/')
        
        // Windows: C:/ format
        if (normalized.length >= 2 && normalized[1] == ':') {
            return normalized.substring(0, 2)
        }
        
        // Unix: /mount/point format
        if (normalized.startsWith("/")) {
            return "/"
        }
        
        return "Unknown"
    }
}
