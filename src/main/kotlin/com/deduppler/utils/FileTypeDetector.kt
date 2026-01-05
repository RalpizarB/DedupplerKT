package com.deduppler.utils

import com.deduppler.model.FileType
import java.io.File

object FileTypeDetector {
    private val musicExtensions = setOf(
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "aiff", "ape", "opus"
    )
    
    private val videoExtensions = setOf(
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "3gp"
    )
    
    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp", "svg", "ico", "heic", "heif"
    )
    
    fun detectFileType(file: File): FileType {
        val extension = file.extension.lowercase()
        
        return when {
            musicExtensions.contains(extension) -> FileType.MUSIC
            videoExtensions.contains(extension) -> FileType.VIDEO
            imageExtensions.contains(extension) -> FileType.IMAGE
            else -> FileType.OTHER
        }
    }
    
    fun isMediaFile(file: File): Boolean {
        val type = detectFileType(file)
        return type in setOf(FileType.MUSIC, FileType.VIDEO, FileType.IMAGE)
    }
}
