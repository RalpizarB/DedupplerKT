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
     * Create a CSV writer for streaming/incremental writing
     * Returns a CSVWriter that should be used with the write and close methods
     */
    fun createStreamingWriter(outputPath: String): StreamingCsvWriter {
        val csvFile = File(outputPath)
        val writer = BufferedWriter(FileWriter(csvFile))
        
        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader("FullPath", "FileExt", "Hash", "Disk", "Size", "Type")
            .build()
        
        val printer = CSVPrinter(writer, csvFormat)
        return StreamingCsvWriter(printer, writer)
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
     */
    class StreamingCsvWriter(
        private val printer: CSVPrinter,
        private val writer: BufferedWriter
    ) {
        /**
         * Write a file record and flush to disk (for restart capability)
         */
        fun writeFile(file: FileInfo) {
            val sizeInMB = String.format("%.2f", file.size / (1024.0 * 1024.0))
            
            printer.printRecord(
                file.fullPath,
                file.fileExt,
                file.hash,
                file.disk,
                sizeInMB,
                file.type.name.lowercase()
            )
            
            // Flush after each write for restart capability
            printer.flush()
            writer.flush()
        }
        
        /**
         * Close the writer
         */
        fun close() {
            try {
                printer.close()
                writer.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }
}

