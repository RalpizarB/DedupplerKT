package com.deduppler.utils

import com.deduppler.model.FileInfo
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

object CsvReportGenerator {
    /**
     * Generate report from completed list of files (batch mode)
     */
    fun generateReport(files: List<FileInfo>, outputPath: String) {
        val csvFile = File(outputPath)
        
        FileWriter(csvFile).use { writer ->
            val csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("FullPath", "FileExt", "Hash", "Disk", "Size", "Type")
                .build()
            
            CSVPrinter(writer, csvFormat).use { printer ->
                files.forEach { file ->
                    writeFileRecord(printer, file)
                }
            }
        }
    }
    
    /**
     * Create a CSV writer for streaming/incremental writing with new flow:
     * 1. Write header
     * 2. Write paths only (with empty other fields)
     * 3. Keep data in memory to rewrite CSV with complete data later
     */
    fun createStreamingWriter(outputPath: String): StreamingCsvWriter {
        return StreamingCsvWriter(outputPath)
    }
    
    /**
     * Write a single file record to the CSV
     */
    private fun writeFileRecord(printer: CSVPrinter, file: FileInfo) {
        // Convert size from bytes to MB, display as number only for better sorting
        val sizeInMB = String.format("%.2f", file.size / (1024.0 * 1024.0))
        
        printer.printRecord(
            file.fullPath,
            file.fileExt,
            file.hash,
            file.disk,
            sizeInMB,
            file.type.name.lowercase()
        )
    }
    
    /**
     * Streaming CSV writer for incremental writing during scan
     * New flow: Paths first, then complete data
     */
    class StreamingCsvWriter(
        private val outputPath: String
    ) {
        private val fileData = mutableMapOf<String, FileInfo>()
        private val pathsInOrder = mutableListOf<String>()
        private var pathsPhaseComplete = false
        private var currentWriter: BufferedWriter? = null
        private var currentPrinter: CSVPrinter? = null
        
        init {
            // Create CSV and write header immediately
            val csvFile = File(outputPath)
            currentWriter = BufferedWriter(FileWriter(csvFile))
            
            val csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("FullPath", "FileExt", "Hash", "Disk", "Size", "Type")
                .build()
            
            currentPrinter = CSVPrinter(currentWriter, csvFormat)
        }
        
        /**
         * Phase 1: Write only the file path (with empty other fields)
         * This is called immediately when a file is found
         */
        fun writePathOnly(fullPath: String) {
            if (pathsPhaseComplete) {
                throw IllegalStateException("Cannot write paths after paths phase is complete")
            }
            
            // Write path with empty fields for now
            currentPrinter?.printRecord(
                fullPath,      // FullPath
                "",            // FileExt (empty for now)
                "",            // Hash (empty for now)
                "",            // Disk (empty for now)
                "",            // Size (empty for now)
                ""             // Type (empty for now)
            )
            
            pathsInOrder.add(fullPath)
            
            // Flush after each write for restart capability
            currentPrinter?.flush()
            currentWriter?.flush()
        }
        
        /**
         * Call this when all paths have been written
         * Closes the paths-only CSV and prepares to rewrite with complete data
         */
        fun completePathsPhase() {
            pathsPhaseComplete = true
            
            // Close the paths-only CSV
            currentPrinter?.close()
            currentWriter?.close()
            
            // Prepare to write complete data
            currentWriter = null
            currentPrinter = null
        }
        
        /**
         * Phase 2: Store complete file data
         * We collect all data first, then rewrite the entire CSV
         */
        fun updateFileData(file: FileInfo) {
            if (!pathsPhaseComplete) {
                throw IllegalStateException("Must complete paths phase before updating data")
            }
            
            // Store the file data
            fileData[file.fullPath] = file
            
            // Rewrite the entire CSV with all data collected so far
            rewriteCsvWithData()
        }
        
        /**
         * Rewrite the entire CSV file with complete data
         */
        private fun rewriteCsvWithData() {
            val csvFile = File(outputPath)
            FileWriter(csvFile).use { writer ->
                val csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader("FullPath", "FileExt", "Hash", "Disk", "Size", "Type")
                    .build()
                
                CSVPrinter(writer, csvFormat).use { printer ->
                    // Write all paths in order
                    pathsInOrder.forEach { path ->
                        val file = fileData[path]
                        if (file != null) {
                            // Write complete data
                            val sizeInMB = String.format("%.2f", file.size / (1024.0 * 1024.0))
                            printer.printRecord(
                                file.fullPath,
                                file.fileExt,
                                file.hash,
                                file.disk,
                                sizeInMB,
                                file.type.name.lowercase()
                            )
                        } else {
                            // Path found but no data yet - write empty fields
                            printer.printRecord(path, "", "", "", "", "")
                        }
                    }
                }
            }
        }
        
        /**
         * Write a complete file record (used for backward compatibility)
         */
        fun writeFile(file: FileInfo) {
            val sizeInMB = String.format("%.2f", file.size / (1024.0 * 1024.0))
            
            currentPrinter?.printRecord(
                file.fullPath,
                file.fileExt,
                file.hash,
                file.disk,
                sizeInMB,
                file.type.name.lowercase()
            )
            
            // Flush after each write for restart capability
            currentPrinter?.flush()
            currentWriter?.flush()
        }
        
        /**
         * Close the writer
         */
        fun close() {
            try {
                // Final rewrite with all collected data
                if (pathsPhaseComplete && fileData.isNotEmpty()) {
                    rewriteCsvWithData()
                }
                
                currentPrinter?.close()
                currentWriter?.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }
}

