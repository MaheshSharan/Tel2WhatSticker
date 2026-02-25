package com.maheshsharan.tel2what.engine

import java.io.File

interface StickerConverter {
    /**
     * Converts a raw input file (image, tgs, webm) into a WhatsApp compliant WebP sticker.
     * 
     * @param inputFile The downloaded raw Telegram sticker file.
     * @param outputFile The destination file where the .webp should be saved.
     * @param config The WhatsApp constraints for the current conversion.
     * @return StickerConversionResult (Success, Failed, ValidationFailed)
     */
    suspend fun convert(
        inputFile: File, 
        outputFile: File, 
        config: ConversionConfig
    ): StickerConversionResult
}
