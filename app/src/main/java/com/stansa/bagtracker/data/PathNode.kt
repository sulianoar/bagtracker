package com.stansa.bagtracker.data

data class PathNode(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val depth: Int
)