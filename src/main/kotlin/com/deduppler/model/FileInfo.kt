package com.deduppler.model

data class FileInfo(
    val fullPath: String,
    val fileExt: String,
    val hash: String,
    val disk: String,
    val size: Long,
    val type: FileType
)

enum class FileType {
    MUSIC,
    VIDEO,
    IMAGE,
    OTHER
}
