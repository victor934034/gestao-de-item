package com.app.stockmaster.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        
        val url = originalUrl.newBuilder()
            .addQueryParameter("token", token)
            .addQueryParameter("formato", "JSON")
            .build()
            
        val request = originalRequest.newBuilder()
            .url(url)
            // Still keeping header in case it's needed for other parts of the system
            .addHeader("Authorization", "Bearer $token")
            .build()
            
        return chain.proceed(request)
    }
}
