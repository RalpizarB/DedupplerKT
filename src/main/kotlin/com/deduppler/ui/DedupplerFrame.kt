package com.deduppler.ui

import com.deduppler.model.FileInfo
import com.deduppler.scanner.FileScanner
import com.deduppler.utils.CsvReportGenerator
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel

class DedupplerFrame : JFrame("DedupplerKT - Duplicate File Finder") {
    private val directoryField = JTextField(40)
    private val browseButton = JButton("Browse")
    private val scanAllDisksCheckbox = JCheckBox("Scan all disks")
    private val mediaOnlyCheckbox = JCheckBox("Media files only (default)", true)
    private val musicCheckbox = JCheckBox("Music", true)
    private val videoCheckbox = JCheckBox("Video", true)
    private val imageCheckbox = JCheckBox("Image", true)
    private val otherCheckbox = JCheckBox("Other", false)
    private val scanButton = JButton("Start Scan")
    private val exportButton = JButton("Export CSV Report")
    private val progressLabel = JLabel(" ")
    private val progressBar = JProgressBar()
    private val tableModel = DefaultTableModel(
        arrayOf("File Path", "Extension", "Type", "Size (MB)", "Hash (partial)"),
        0
    )
    private val resultsTable = JTable(tableModel)
    
    private var scannedFiles = listOf<FileInfo>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        setupUI()
        setupListeners()
        
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1200, 700)
        setLocationRelativeTo(null)
    }
    
    private fun setupUI() {
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.border = EmptyBorder(10, 10, 10, 10)
        
        // Top panel - Directory selection
        val topPanel = JPanel(BorderLayout(5, 5))
        val dirPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        dirPanel.add(JLabel("Directory:"))
        dirPanel.add(directoryField)
        dirPanel.add(browseButton)
        topPanel.add(dirPanel, BorderLayout.NORTH)
        topPanel.add(scanAllDisksCheckbox, BorderLayout.CENTER)
        
        // Options panel
        val optionsPanel = JPanel()
        optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)
        optionsPanel.border = BorderFactory.createTitledBorder("File Type Options")
        
        val mediaPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        mediaPanel.add(mediaOnlyCheckbox)
        optionsPanel.add(mediaPanel)
        
        val typesPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        typesPanel.add(musicCheckbox)
        typesPanel.add(videoCheckbox)
        typesPanel.add(imageCheckbox)
        typesPanel.add(otherCheckbox)
        optionsPanel.add(typesPanel)
        
        topPanel.add(optionsPanel, BorderLayout.SOUTH)
        
        // Control panel
        val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        controlPanel.add(scanButton)
        controlPanel.add(exportButton)
        exportButton.isEnabled = false
        
        // Progress panel
        val progressPanel = JPanel(BorderLayout(5, 5))
        progressPanel.add(progressLabel, BorderLayout.NORTH)
        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        progressPanel.add(progressBar, BorderLayout.CENTER)
        
        // Results table
        resultsTable.autoResizeMode = JTable.AUTO_RESIZE_OFF
        resultsTable.getColumnModel().getColumn(0).preferredWidth = 400
        resultsTable.getColumnModel().getColumn(1).preferredWidth = 80
        resultsTable.getColumnModel().getColumn(2).preferredWidth = 80
        resultsTable.getColumnModel().getColumn(3).preferredWidth = 100
        resultsTable.getColumnModel().getColumn(4).preferredWidth = 200
        val scrollPane = JScrollPane(resultsTable)
        
        // Assemble panels
        val northPanel = JPanel(BorderLayout())
        northPanel.add(topPanel, BorderLayout.NORTH)
        northPanel.add(controlPanel, BorderLayout.CENTER)
        northPanel.add(progressPanel, BorderLayout.SOUTH)
        
        mainPanel.add(northPanel, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        
        contentPane = mainPanel
    }
    
    private fun setupListeners() {
        browseButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                directoryField.text = fileChooser.selectedFile.absolutePath
            }
        }
        
        scanAllDisksCheckbox.addActionListener {
            directoryField.isEnabled = !scanAllDisksCheckbox.isSelected
            browseButton.isEnabled = !scanAllDisksCheckbox.isSelected
        }
        
        mediaOnlyCheckbox.addActionListener {
            if (mediaOnlyCheckbox.isSelected) {
                otherCheckbox.isSelected = false
            }
        }
        
        otherCheckbox.addActionListener {
            if (otherCheckbox.isSelected) {
                mediaOnlyCheckbox.isSelected = false
            }
        }
        
        scanButton.addActionListener {
            startScan()
        }
        
        exportButton.addActionListener {
            exportReport()
        }
    }
    
    private fun startScan() {
        if (!scanAllDisksCheckbox.isSelected && directoryField.text.isBlank()) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a directory or choose to scan all disks",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        // Disable controls during scan
        scanButton.isEnabled = false
        exportButton.isEnabled = false
        directoryField.isEnabled = false
        browseButton.isEnabled = false
        scanAllDisksCheckbox.isEnabled = false
        mediaOnlyCheckbox.isEnabled = false
        musicCheckbox.isEnabled = false
        videoCheckbox.isEnabled = false
        imageCheckbox.isEnabled = false
        otherCheckbox.isEnabled = false
        
        progressBar.isVisible = true
        progressBar.isIndeterminate = true
        progressLabel.text = "Starting scan..."
        tableModel.rowCount = 0
        
        scope.launch {
            try {
                val scanner = FileScanner(
                    onProgress = { msg ->
                        SwingUtilities.invokeLater {
                            progressLabel.text = msg
                        }
                    }
                )
                
                val options = FileScanner.ScanOptions(
                    scanAllDisks = scanAllDisksCheckbox.isSelected,
                    mediaOnly = mediaOnlyCheckbox.isSelected,
                    includeMusic = musicCheckbox.isSelected,
                    includeVideo = videoCheckbox.isSelected,
                    includeImage = imageCheckbox.isSelected,
                    includeOther = otherCheckbox.isSelected
                )
                
                val results = if (scanAllDisksCheckbox.isSelected) {
                    scanner.scanAllDisks(options)
                } else {
                    scanner.scanDirectory(directoryField.text, options)
                }
                
                scannedFiles = results
                
                withContext(Dispatchers.Swing) {
                    displayResults(results)
                    
                    val hashGroups = results.groupBy { it.hash }
                    val duplicateCount = hashGroups.values.count { it.size > 1 }
                    
                    progressLabel.text = "Scan complete! Found ${results.size} files, $duplicateCount duplicate groups"
                    progressBar.isVisible = false
                    exportButton.isEnabled = results.isNotEmpty()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Swing) {
                    progressLabel.text = "Error: ${e.message}"
                    progressBar.isVisible = false
                    JOptionPane.showMessageDialog(
                        this@DedupplerFrame,
                        "Error during scan: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            } finally {
                withContext(Dispatchers.Swing) {
                    // Re-enable controls
                    scanButton.isEnabled = true
                    directoryField.isEnabled = !scanAllDisksCheckbox.isSelected
                    browseButton.isEnabled = !scanAllDisksCheckbox.isSelected
                    scanAllDisksCheckbox.isEnabled = true
                    mediaOnlyCheckbox.isEnabled = true
                    musicCheckbox.isEnabled = true
                    videoCheckbox.isEnabled = true
                    imageCheckbox.isEnabled = true
                    otherCheckbox.isEnabled = true
                }
            }
        }
    }
    
    private fun displayResults(files: List<FileInfo>) {
        tableModel.rowCount = 0
        for (file in files) {
            val sizeInMB = String.format("%.2f", file.size / (1024.0 * 1024.0))
            val hashPreview = file.hash.take(16) + "..."
            tableModel.addRow(
                arrayOf(
                    file.fullPath,
                    file.fileExt,
                    file.type.name.lowercase(),
                    sizeInMB,
                    hashPreview
                )
            )
        }
    }
    
    private fun exportReport() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Save CSV Report"
        fileChooser.selectedFile = File("duplicates_report.csv")
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                val outputPath = fileChooser.selectedFile.absolutePath
                CsvReportGenerator.generateReport(scannedFiles, outputPath)
                progressLabel.text = "Report saved to: $outputPath"
                JOptionPane.showMessageDialog(
                    this,
                    "Report successfully saved to:\n$outputPath",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this,
                    "Error saving report: ${e.message}",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }
    
    override fun dispose() {
        scope.cancel()
        super.dispose()
    }
}
