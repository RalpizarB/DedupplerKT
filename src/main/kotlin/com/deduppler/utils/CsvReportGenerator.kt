package com.deduppler.utils

import com.deduppler.model.FileInfo
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileWriter

object CsvReportGenerator {
    fun generateReport(files: List<FileInfo>, outputPath: String) {
        val csvFile = File(outputPath)
        
        FileWriter(csvFile).use { writer ->
            val csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("FullPath", "FileExt", "Hash", "Disk", "Size", "Type")
                .build()
            
            CSVPrinter(writer, csvFormat).use { printer ->
                files.forEach { file ->
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
            }
        }
    }
}
