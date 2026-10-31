package com.maty.rocola

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.maty.rocola.art.ArtworkFetcher
import com.maty.rocola.art.ArtworkKeyer
import com.maty.rocola.data.MusicRepository
import com.maty.rocola.data.UserStore
import com.maty.rocola.playback.EqualizerState

class RocolaApp : Application(), SingletonImageLoader.Factory {

    lateinit var repository: MusicRepository
        private set
    lateinit var userStore: UserStore
        private set

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(this)
        userStore = UserStore(this)
        EqualizerState.init(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(ArtworkKeyer())
                add(ArtworkFetcher.Factory(this@RocolaApp))
            }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.2).build() }
            .crossfade(200)
            .build()
}

val Context.rocola: RocolaApp
    get() = applicationContext as RocolaApp
