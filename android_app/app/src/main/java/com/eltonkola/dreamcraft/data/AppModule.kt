package com.eltonkola.dreamcraft.data

import android.content.Context
import com.eltonkola.dreamcraft.core.data.FileManager
import com.eltonkola.dreamcraft.core.data.RemoteTaskFileSource
import com.eltonkola.dreamcraft.core.data.StaticRemoteSource
import com.eltonkola.dreamcraft.data.local.FileManagerImpl
import com.eltonkola.dreamcraft.data.remote.AiRepositoryImpl
import com.eltonkola.dreamcraft.remote.data.AiApiServiceFactory
import com.eltonkola.dreamcraft.remote.data.AiPreferencesRepository
import com.eltonkola.dreamcraft.remote.data.DataStoreAiPreferencesRepository
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
    fun provideFileManager(
        @ApplicationContext context: Context
    ): FileManager {
        return FileManagerImpl(context)
    }

    @Provides
    @Singleton
    fun provideGroqRepository(
        aiApiServiceFactory: AiApiServiceFactory,
        fileManager: FileManager
    ): AiRepository {
        return AiRepositoryImpl(aiApiServiceFactory, fileManager)
    }

    @Provides
    @Singleton
    fun provideAiApiServiceFactory(
        @ApplicationContext context: Context,
        client: OkHttpClient
    ): AiApiServiceFactory {
        return AiApiServiceFactory(context, client)
    }

    @Provides
    @Singleton
    fun provideRemoteTaskFileSource(
    ): RemoteTaskFileSource {
        return StaticRemoteSource()
    }

    @Provides
    @Singleton
    fun provideAiPreferencesRepository(
        @ApplicationContext context: Context,
    ): AiPreferencesRepository {
        return DataStoreAiPreferencesRepository(context)
    }

}
