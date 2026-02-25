package com.maheshsharan.tel2what.engine

data class ConversionConfig(
    val targetWidth: Int = 512,
    val targetHeight: Int = 512,
    val maxStaticSizeBytes: Long = 100 * 1024L,       // 100 KB
    val maxAnimatedSizeBytes: Long = 500 * 1024L,     // 500 KB
    val maxTraySizeBytes: Long = 50 * 1024L,          // 50 KB
    val trayDimension: Int = 96,
    val maxDurationMs: Long = 10_000L,                // 10 Seconds
    val minFrameDurationMs: Long = 8L,                // ~125 FPS Max
    val targetFps: Int = 10                           // Reduced to 10 FPS for maximum speed
)
