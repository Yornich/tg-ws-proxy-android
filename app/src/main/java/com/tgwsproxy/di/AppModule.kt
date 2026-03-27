package com.tgwsproxy.di

import com.tgwsproxy.data.LogBuffer
import com.tgwsproxy.data.ProxyRepository
import com.tgwsproxy.data.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideLogBuffer(): LogBuffer = LogBuffer()

    @Provides @Singleton
    fun provideProxyRepository(settingsDataStore: SettingsDataStore): ProxyRepository =
        ProxyRepository(settingsDataStore)
}
