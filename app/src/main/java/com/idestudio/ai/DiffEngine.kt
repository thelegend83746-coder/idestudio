package com.idestudio.ai

data class DiffLine(
    val type: DiffType,
    val text: String,
    val oldLineNumber: Int?,
    val newLineNumber: Int?
)

enum class DiffType {
    UNCHANGED,
    ADDED,
    DELETED
}

object DiffEngine {

    fun computeDiff(oldText: String, newText: String): List<DiffLine> {
        val oldLines = oldText.lines()
        val newLines = newText.lines()

        val result = mutableListOf<DiffLine>()
        var oldIdx = 0
        var newIdx = 0

        while (oldIdx < oldLines.size || newIdx < newLines.size) {
            if (oldIdx < oldLines.size && newIdx < newLines.size && oldLines[oldIdx] == newLines[newIdx]) {
                result.add(
                    DiffLine(
                        type = DiffType.UNCHANGED,
                        text = oldLines[oldIdx],
                        oldLineNumber = oldIdx + 1,
                        newLineNumber = newIdx + 1
                    )
                )
                oldIdx++
                newIdx++
            } else if (newIdx < newLines.size && (oldIdx >= oldLines.size || !oldLines.contains(newLines[newIdx]))) {
                result.add(
                    DiffLine(
                        type = DiffType.ADDED,
                        text = newLines[newIdx],
                        oldLineNumber = null,
                        newLineNumber = newIdx + 1
                    )
                )
                newIdx++
            } else if (oldIdx < oldLines.size) {
                result.add(
                    DiffLine(
                        type = DiffType.DELETED,
                        text = oldLines[oldIdx],
                        oldLineNumber = oldIdx + 1,
                        newLineNumber = null
                    )
                )
                oldIdx++
            }
        }

        return result
    }
}
