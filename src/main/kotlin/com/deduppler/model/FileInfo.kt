package com.deduppler.model

data class FileInfo(
    val fullPath: String,
    val fileExt: String,
    val hash: String = "",  // Hash is optional initially, computed later
    val disk: String,
    val size: Long,
    val type: FileType,
    val isHashed: Boolean = false  // Track if file has been hashed
)

enum class FileType {
    MUSIC,
    VIDEO,
    IMAGE,
    OTHER
}
