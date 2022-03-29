package com.udacity.asteroidradar.network

import com.udacity.asteroidradar.BuildConfig
import com.udacity.asteroidradar.model.PictureOfDay
import com.udacity.asteroidradar.utils.Constants.API_QUERY_CONNECT_TIME_OUT
import com.udacity.asteroidradar.utils.Constants.API_QUERY_READ_TIME_OUT
import com.udacity.asteroidradar.utils.Constants.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface AsteroidApiService {
    @ScalarsConverter
    @GET("neo/rest/v1/feed")
    suspend fun getAsteroidList(@Query("start_date") startDate: String): String

    @MoshiConverter
    @GET("planetary/apod")
    suspend fun getImageOfTheDay(): PictureOfDay
}

val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    // Fetching the asteroid list takes a long time and often reaches the default timeout value,
    // so we give a bit more time
    .connectTimeout(API_QUERY_CONNECT_TIME_OUT, TimeUnit.MINUTES)
    .readTimeout(API_QUERY_READ_TIME_OUT, TimeUnit.SECONDS)
    // Add the API key to every request (instead of adding it as a parameter each time)
    // https://futurestud.io/tutorials/retrofit-2-how-to-add-query-parameters-to-every-request
    .addInterceptor { chain ->
        val original = chain.request()
        val originalUrl = original.url()
        val newUrl =
            originalUrl.newBuilder().addQueryParameter("api_key", BuildConfig.API_KEY).build()
        val request = original.newBuilder().url(newUrl).build()
        chain.proceed(request)
    }
    .build()

private val retrofit = Retrofit.Builder()
    .client(okHttpClient)
    .baseUrl(BASE_URL)
    .addConverterFactory(ScalarsOrMoshiConverter.create())
    .build()

object AsteroidApi {
    val retrofitService: AsteroidApiService by lazy {
        retrofit.create(AsteroidApiService::class.java)
    }
}