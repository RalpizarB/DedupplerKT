package com.deduppler

import com.deduppler.ui.DedupplerFrame
import com.formdev.flatlaf.FlatLightLaf
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    // Set modern look and feel
    try {
        UIManager.setLookAndFeel(FlatLightLaf())
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    SwingUtilities.invokeLater {
        val frame = DedupplerFrame()
        frame.isVisible = true
    }
}
