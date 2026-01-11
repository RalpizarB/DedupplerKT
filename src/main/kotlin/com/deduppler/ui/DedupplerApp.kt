package com.deduppler.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deduppler.model.FileInfo
import com.deduppler.scanner.FileScanner
import com.deduppler.utils.CsvReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser

@Composable
@Preview
fun DedupplerApp() {
    var selectedDirectory by remember { mutableStateOf("") }
    var scanAllDisks by remember { mutableStateOf(false) }
    var mediaOnly by remember { mutableStateOf(true) }
    var includeMusic by remember { mutableStateOf(true) }
    var includeVideo by remember { mutableStateOf(true) }
    var includeImage by remember { mutableStateOf(true) }
    var includeOther by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var scannedFiles by remember { mutableStateOf<List<FileInfo>>(emptyList()) }
    var duplicateCount by remember { mutableStateOf(0) }
    var streamingCsvEnabled by remember { mutableStateOf(true) }  // Enable streaming CSV by default
    var csvOutputPath by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DedupplerKT - Duplicate File Finder",
                    style = MaterialTheme.typography.h4
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Directory Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedDirectory,
                        onValueChange = { selectedDirectory = it },
                        label = { Text("Directory to scan") },
                        modifier = Modifier.weight(1f),
                        enabled = !scanAllDisks && !isScanning
                    )
                    
                    Button(
                        onClick = {
                            val fileChooser = JFileChooser()
                            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            val result = fileChooser.showOpenDialog(null)
                            if (result == JFileChooser.APPROVE_OPTION) {
                                selectedDirectory = fileChooser.selectedFile.absolutePath
                            }
                        },
                        enabled = !scanAllDisks && !isScanning
                    ) {
                        Text("Browse")
                    }
                }
                
                // Scan All Disks Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = scanAllDisks,
                        onCheckedChange = { scanAllDisks = it },
                        enabled = !isScanning
                    )
                    Text("Scan all disks")
                }
                
                // Streaming CSV Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = streamingCsvEnabled,
                        onCheckedChange = { streamingCsvEnabled = it },
                        enabled = !isScanning
                    )
                    Text("Write CSV during scan (enables restart capability)")
                }
                
                // CSV Output Path Selection (when streaming is enabled)
                if (streamingCsvEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = csvOutputPath,
                            onValueChange = { csvOutputPath = it },
                            label = { Text("CSV output path (optional)") },
                            modifier = Modifier.weight(1f),
                            enabled = !isScanning,
                            placeholder = { Text("duplicates_report.csv") }
                        )
                        
                        Button(
                            onClick = {
                                val fileChooser = JFileChooser()
                                fileChooser.dialogTitle = "Select CSV Output Path"
                                fileChooser.selectedFile = File("duplicates_report.csv")
                                val result = fileChooser.showSaveDialog(null)
                                if (result == JFileChooser.APPROVE_OPTION) {
                                    csvOutputPath = fileChooser.selectedFile.absolutePath
                                }
                            },
                            enabled = !isScanning
                        ) {
                            Text("Browse")
                        }
                    }
                }
                
                // File Type Filters
                Text("File Types:", style = MaterialTheme.typography.h6)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = mediaOnly,
                        onCheckedChange = { 
                            mediaOnly = it
                            if (it) {
                                includeOther = false
                            }
                        },
                        enabled = !isScanning
                    )
                    Text("Media files only (default)")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeMusic,
                            onCheckedChange = { includeMusic = it },
                            enabled = !isScanning
                        )
                        Text("Music")
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeVideo,
                            onCheckedChange = { includeVideo = it },
                            enabled = !isScanning
                        )
                        Text("Video")
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeImage,
                            onCheckedChange = { includeImage = it },
                            enabled = !isScanning
                        )
                        Text("Image")
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = includeOther,
                            onCheckedChange = { 
                                includeOther = it
                                if (it) {
                                    mediaOnly = false
                                }
                            },
                            enabled = !isScanning
                        )
                        Text("Other")
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Scan and Export Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isScanning = true
                                scannedFiles = emptyList()
                                duplicateCount = 0
                                progressMessage = "Starting scan..."
                                
                                withContext(Dispatchers.IO) {
                                    // Prepare streaming CSV writer if enabled
                                    val csvWriter = if (streamingCsvEnabled) {
                                        val outputPath = csvOutputPath.ifBlank { "duplicates_report.csv" }
                                        try {
                                            CsvReportGenerator.createStreamingWriter(outputPath)
                                        } catch (e: Exception) {
                                            progressMessage = "Error creating CSV: ${e.message}"
                                            null
                                        }
                                    } else null
                                    
                                    try {
                                        val scanner = FileScanner(
                                            onProgress = { msg -> progressMessage = msg },
                                            onFileFound = { fileInfo ->
                                                // File found (Phase 1) - write path only to CSV immediately
                                                csvWriter?.writePathOnly(fileInfo.fullPath)
                                            },
                                            onFileHashed = { hashedFileInfo ->
                                                // File has been hashed (Phase 2) - write complete data
                                                csvWriter?.updateFileData(hashedFileInfo)
                                            }
                                        )
                                        
                                        val options = FileScanner.ScanOptions(
                                            scanAllDisks = scanAllDisks,
                                            mediaOnly = mediaOnly,
                                            includeMusic = includeMusic,
                                            includeVideo = includeVideo,
                                            includeImage = includeImage,
                                            includeOther = includeOther
                                        )
                                        
                                        val results = if (scanAllDisks) {
                                            scanner.scanAllDisks(options)
                                        } else {
                                            scanner.scanDirectory(selectedDirectory, options)
                                        }
                                        
                                        // Complete paths phase before hashing starts
                                        csvWriter?.completePathsPhase()
                                        
                                        scannedFiles = results
                                        
                                        // Count duplicates (only from hashed files)
                                        val hashedFiles = results.filter { it.isHashed && it.hash.isNotBlank() }
                                        val hashGroups = hashedFiles.groupBy { it.hash }
                                        duplicateCount = hashGroups.values.count { it.size > 1 }
                                        
                                        val csvMessage = if (csvWriter != null) {
                                            " CSV written to: ${csvOutputPath.ifBlank { "duplicates_report.csv" }}"
                                        } else ""
                                        
                                        progressMessage = "Scan complete! Found ${results.size} files, ${duplicateCount} duplicate groups.$csvMessage"
                                    } finally {
                                        // Close CSV writer
                                        csvWriter?.close()
                                    }
                                }
                                
                                isScanning = false
                            }
                        },
                        enabled = !isScanning && (scanAllDisks || selectedDirectory.isNotBlank()),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isScanning) "Scanning..." else "Start Scan")
                    }
                    
                    Button(
                        onClick = {
                            val fileChooser = JFileChooser()
                            fileChooser.dialogTitle = "Save CSV Report"
                            fileChooser.selectedFile = File("duplicates_report.csv")
                            val result = fileChooser.showSaveDialog(null)
                            
                            if (result == JFileChooser.APPROVE_OPTION) {
                                val outputPath = fileChooser.selectedFile.absolutePath
                                CsvReportGenerator.generateReport(scannedFiles, outputPath)
                                progressMessage = "Report saved to: $outputPath"
                            }
                        },
                        enabled = scannedFiles.isNotEmpty() && !isScanning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export CSV Report")
                    }
                }
                
                // Progress Message
                if (progressMessage.isNotBlank()) {
                    Text(
                        text = progressMessage,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary
                    )
                }
                
                if (isScanning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Results Display
                Text(
                    text = "Results (${scannedFiles.size} files found, $duplicateCount duplicate groups)",
                    style = MaterialTheme.typography.h6
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(scannedFiles) { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = file.fullPath,
                                    style = MaterialTheme.typography.body1
                                )
                                val hashDisplay = if (file.isHashed && file.hash.isNotBlank()) {
                                    "Hash: ${file.hash.take(16)}..."
                                } else {
                                    "Hash: (not computed)"
                                }
                                Text(
                                    text = "Type: ${file.type.name} | Size: ${"%.2f".format(file.size / (1024.0 * 1024.0))} MB | $hashDisplay",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
