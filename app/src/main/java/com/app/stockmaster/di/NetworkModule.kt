package com.app.stockmaster.di

import com.app.stockmaster.data.remote.AuthInterceptor
import com.app.stockmaster.data.remote.ErpApi
import com.app.stockmaster.data.remote.BridgeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TINY_BASE_URL = "https://api.tiny.com.br/api2/"
    // Unified Backend URL (Entrega + Gestão)
    private const val BRIDGE_BASE_URL = "https://app-backend.zdc13k.easypanel.host/api/stock/"
    private const val TINY_API_TOKEN = "fdaefe9956da9074686debb15db59aea775b89b04ea74cffc5e269dd2df3ed5b"
    
    // Supabase Credentials
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJoZ21pdHJ5Ymhtd3dpaHpub3BqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkyMTMwNjcsImV4cCI6MjA4NDc4OTA2N30.hCu-znNbVmvuGujepYkKMuDHX28pC69YzZ3-zYMHU7c"

    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor {
        return AuthInterceptor(TINY_API_TOKEN)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("SupabaseHttpClient")
    fun provideSupabaseHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Unified backend doesn't need Supabase headers, it handles it internally
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TINY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("BridgeRetrofit")
    fun provideBridgeRetrofit(@Named("SupabaseHttpClient") supabaseHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BRIDGE_BASE_URL)
            .client(supabaseHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideErpApi(retrofit: Retrofit): com.app.stockmaster.data.remote.ErpApi {
        return retrofit.create(com.app.stockmaster.data.remote.ErpApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBridgeApi(@Named("BridgeRetrofit") bridgeRetrofit: Retrofit): com.app.stockmaster.data.remote.BridgeApi {
        return bridgeRetrofit.create(com.app.stockmaster.data.remote.BridgeApi::class.java)
    }
}
