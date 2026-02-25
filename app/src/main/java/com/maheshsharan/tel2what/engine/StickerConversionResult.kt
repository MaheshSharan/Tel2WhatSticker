package com.maheshsharan.tel2what.engine

import java.io.File

sealed class StickerConversionResult {
    data class Success(
        val outputFile: File,
        val width: Int,
        val height: Int,
        val sizeBytes: Long,
        val isAnimated: Boolean
    ) : StickerConversionResult()

    data class Failed(
        val reason: String,
        val exception: Throwable? = null
    ) : StickerConversionResult()

    data class ValidationFailed(
        val reason: String
    ) : StickerConversionResult()
}
