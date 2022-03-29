package com.udacity.asteroidradar.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.lang.reflect.Type

/**
 * Use Scalars Converter for the asteroid list because the response needs to be a String
 * (in order to use the parseAsteroidsJsonResult() helper method)
 *
 * https://knowledge.udacity.com/questions/380481
 * https://criticalgnome.com/2018/10/29/two-converters-in-one-retrofit-project/
 */

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention
internal annotation class ScalarsConverter

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention
internal annotation class MoshiConverter

class ScalarsOrMoshiConverter : Converter.Factory() {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        annotations.forEach { annotation ->
            return when (annotation) {
                is ScalarsConverter -> ScalarsConverterFactory.create()
                    .responseBodyConverter(type, annotations, retrofit)
                is MoshiConverter -> MoshiConverterFactory.create(moshi)
                    .responseBodyConverter(type, annotations, retrofit)
                else -> null
            }
        }
        return null
    }

    companion object {
        fun create() = ScalarsOrMoshiConverter()
    }
}