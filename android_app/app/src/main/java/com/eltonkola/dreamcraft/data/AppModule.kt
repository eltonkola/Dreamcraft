package com.eltonkola.dreamcraft.data

import android.content.Context
import com.eltonkola.dreamcraft.BuildConfig
import com.eltonkola.dreamcraft.data.local.FileManagerImpl
import com.eltonkola.dreamcraft.data.remote.GroqApiServiceImpl
import com.eltonkola.dreamcraft.data.remote.GroqRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGroqApiService(
        client: OkHttpClient
    ): GroqApiService {
        return GroqApiServiceImpl(
            client = client,
            apiKey = BuildConfig.GROQ_API_KEY //TODO - check if PreferencesManager is using custom key
        )
    }

    @Provides
    @Singleton
    fun provideFileManager(
        @ApplicationContext context: Context
    ): FileManager {
        return FileManagerImpl(context)
    }

    @Provides
    @Singleton
    fun provideGroqRepository(
        apiService: GroqApiService,
        fileManager: FileManager
    ): GroqRepository {
        return GroqRepositoryImpl(apiService, fileManager)
    }
}

