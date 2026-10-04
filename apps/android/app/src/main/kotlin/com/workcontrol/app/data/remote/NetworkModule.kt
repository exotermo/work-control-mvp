package com.workcontrol.app.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.workcontrol.app.BuildConfig
import com.workcontrol.app.data.auth.AccessTokenProvider
import com.workcontrol.app.data.auth.InMemoryAccessTokenStore
import com.workcontrol.app.data.prelo.PreloApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiBaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PreloRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAccessTokenProvider(store: InMemoryAccessTokenStore): AccessTokenProvider = store

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    @ApiBaseUrl
    fun provideApiBaseUrl(): HttpUrl = BuildConfig.API_BASE_URL.toHttpUrl()

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: BearerAuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (BuildConfig.ENABLE_HTTP_LOGGING) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
                redactHeader("Cookie")
                redactHeader("Set-Cookie")
                redactHeader("CF-Access-Client-Id")
                redactHeader("CF-Access-Client-Secret")
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        @ApiBaseUrl baseUrl: HttpUrl,
        client: OkHttpClient,
        gson: Gson,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideWorkControlApi(retrofit: Retrofit): WorkControlApi =
        retrofit.create(WorkControlApi::class.java)

    @Provides
    @Singleton
    @PreloRetrofit
    fun providePreloRetrofit(client: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.PRELO_BASE_URL.toHttpUrl())
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun providePreloApi(@PreloRetrofit retrofit: Retrofit): PreloApi = retrofit.create(PreloApi::class.java)
}
