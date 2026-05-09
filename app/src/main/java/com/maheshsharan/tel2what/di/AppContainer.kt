package com.maheshsharan.tel2what.di

import android.content.Context
import com.maheshsharan.tel2what.BuildConfig
import com.maheshsharan.tel2what.data.local.AppDatabase
import com.maheshsharan.tel2what.data.network.FileDownloader
import com.maheshsharan.tel2what.data.network.TelegramBotApi
import com.maheshsharan.tel2what.data.repository.StickerRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual DI container scoped to the Application lifetime.
 *
 * All singletons that must survive Fragment recreation live here:
 *   - OkHttpClient  (shared thread pool / connection pool)
 *   - AppDatabase   (Room singleton)
 *   - StickerRepository
 *
 * Access via: (application as Tel2WhatApplication).appContainer
 *
 * NOTE: This is intentionally a lightweight manual container. Migrate to Hilt
 * when the team is ready — all bindings are already interface-separable.
 */
class AppContainer(context: Context) {

    /**
     * Single shared OkHttpClient for the entire app.
     * Creating multiple instances leaks ThreadPoolExecutors and ConnectionPools.
     */
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)   // Large sticker packs can be slow on 3G
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val database: AppDatabase = AppDatabase.getDatabase(context)

    /**
     * Bot token sourced exclusively from BuildConfig (injected at compile time
     * from local.properties — never committed to source control).
     */
    val repository: StickerRepository = StickerRepository(
        stickerDao = database.stickerDao(),
        telegramBotApi = TelegramBotApi(
            botToken = BuildConfig.TELEGRAM_BOT_TOKEN,
            client = okHttpClient
        ),
        fileDownloader = FileDownloader(client = okHttpClient)
    )
}
