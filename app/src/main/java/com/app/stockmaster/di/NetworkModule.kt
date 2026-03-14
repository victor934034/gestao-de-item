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
    private const val BRIDGE_BASE_URL = "http://76.13.161.164:3000/"
    private const val API_TOKEN = "fdaefe9956da9074686debb15db59aea775b89b04ea74cffc5e269dd2df3ed5b"

    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor {
        return AuthInterceptor(API_TOKEN)
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
    fun provideBridgeRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BRIDGE_BASE_URL)
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
