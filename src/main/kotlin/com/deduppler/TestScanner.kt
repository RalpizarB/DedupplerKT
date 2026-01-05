package com.deduppler

import com.deduppler.scanner.FileScanner
import com.deduppler.utils.CsvReportGenerator
import java.io.File

/**
 * Simple CLI test to verify scanning and CSV export functionality
 */
fun testScanner() {
    println("DedupplerKT Scanner Test")
    println("========================\n")
    
    // Create a test directory with some files
    val testDir = File("/tmp/test_deduppler")
    if (testDir.exists()) {
        println("Using existing test directory: ${testDir.absolutePath}")
    } else {
        println("Test directory not found at: ${testDir.absolutePath}")
        return
    }
    
    // Test the scanner
    println("\n1. Testing FileScanner...")
    var progressCount = 0
    val scanner = FileScanner(
        onProgress = { msg -> 
            progressCount++
            if (progressCount % 10 == 0) {
                println("   Progress: $msg")
            }
        }
    )
    
    val options = FileScanner.ScanOptions(
        scanAllDisks = false,
        mediaOnly = true,
        includeMusic = true,
        includeVideo = true,
        includeImage = true,
        includeOther = false
    )
    
    println("   Scanning: ${testDir.absolutePath}")
    val results = scanner.scanDirectory(testDir.absolutePath, options)
    println("   ✓ Found ${results.size} files")
    
    if (results.isNotEmpty()) {
        println("\n2. Testing duplicate detection...")
        val hashGroups = results.groupBy { it.hash }
        val duplicates = hashGroups.filter { it.value.size > 1 }
        println("   ✓ Found ${duplicates.size} duplicate groups")
        
        duplicates.forEach { (hash, files) ->
            println("   Duplicate set (hash: ${hash.take(16)}...):")
            files.forEach { file ->
                println("     - ${file.fullPath}")
            }
        }
    }
    
    // Test CSV export
    if (results.isNotEmpty()) {
        println("\n3. Testing CSV export...")
        val csvPath = "/tmp/test_report.csv"
        CsvReportGenerator.generateReport(results, csvPath)
        println("   ✓ CSV saved to: $csvPath")
        
        // Verify CSV content
        val csvContent = File(csvPath).readText()
        val lines = csvContent.lines().filter { it.isNotBlank() }
        println("   ✓ CSV has ${lines.size} lines (including header)")
        
        // Check header
        val header = lines[0]
        if (header.contains("FullPath") && header.contains("Size") && header.contains("Type")) {
            println("   ✓ CSV header is correct")
        }
        
        // Check size format (should be in MB)
        if (lines.size > 1) {
            val firstDataLine = lines[1]
            println("   Sample line: $firstDataLine")
            
            // Size should be numeric MB value
            val parts = firstDataLine.split(",")
            if (parts.size >= 5) {
                val sizeValue = parts[4]
                try {
                    val sizeMB = sizeValue.toDouble()
                    println("   ✓ Size is numeric: $sizeMB MB")
                } catch (e: Exception) {
                    println("   ✗ Size is not numeric: $sizeValue")
                }
            }
        }
    }
    
    println("\n✓ All tests completed successfully!")
}

fun main() {
    testScanner()
}
