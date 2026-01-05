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
    private val onFileFound: (FileInfo) -> Unit = {}
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
    
    fun scanDirectory(rootPath: String, options: ScanOptions): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        val filesByHash = mutableMapOf<String, MutableList<FileInfo>>()
        
        val rootFile = File(rootPath)
        if (!rootFile.exists() || !rootFile.isDirectory) {
            onProgress("Error: Invalid directory path")
            return emptyList()
        }
        
        onProgress("Starting scan of $rootPath")
        scanDirectoryRecursive(rootFile, options, files, filesByHash)
        
        return files
    }
    
    fun scanAllDisks(options: ScanOptions): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        val filesByHash = mutableMapOf<String, MutableList<FileInfo>>()
        
        val roots = File.listRoots()
        onProgress("Found ${roots.size} disk(s) to scan")
        
        for (root in roots) {
            if (root.exists() && root.canRead()) {
                onProgress("Scanning disk: ${root.absolutePath}")
                scanDirectoryRecursive(root, options, files, filesByHash)
            }
        }
        
        return files
    }
    
    private fun scanDirectoryRecursive(
        directory: File,
        options: ScanOptions,
        allFiles: MutableList<FileInfo>,
        filesByHash: MutableMap<String, MutableList<FileInfo>>
    ) {
        try {
            val entries = directory.listFiles() ?: return
            
            for (entry in entries) {
                try {
                    if (entry.isDirectory) {
                        if (shouldSkipDirectory(entry)) {
                            continue
                        }
                        scanDirectoryRecursive(entry, options, allFiles, filesByHash)
                    } else if (entry.isFile) {
                        if (shouldProcessFile(entry, options)) {
                            processFile(entry, allFiles, filesByHash)
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
    
    private fun processFile(
        file: File,
        allFiles: MutableList<FileInfo>,
        filesByHash: MutableMap<String, MutableList<FileInfo>>
    ) {
        try {
            onProgress("Processing: ${file.name}")
            
            val hash = HashUtils.calculateBlake2bHash(file)
            val fileType = FileTypeDetector.detectFileType(file)
            val disk = getDiskFromPath(file.absolutePath)
            
            val fileInfo = FileInfo(
                fullPath = file.absolutePath,
                fileExt = file.extension,
                hash = hash,
                disk = disk,
                size = file.length(),
                type = fileType
            )
            
            allFiles.add(fileInfo)
            onFileFound(fileInfo)
            
            // Track duplicates
            filesByHash.getOrPut(hash) { mutableListOf() }.add(fileInfo)
            
        } catch (e: Exception) {
            // Skip files that can't be hashed
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
