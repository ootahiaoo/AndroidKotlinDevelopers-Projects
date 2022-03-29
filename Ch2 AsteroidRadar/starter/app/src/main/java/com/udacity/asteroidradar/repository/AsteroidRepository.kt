package com.udacity.asteroidradar.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.udacity.asteroidradar.database.AsteroidDatabase
import com.udacity.asteroidradar.database.toModel
import com.udacity.asteroidradar.model.Asteroid
import com.udacity.asteroidradar.model.PictureOfDay
import com.udacity.asteroidradar.model.toDatabaseEntity
import com.udacity.asteroidradar.network.AsteroidApi
import com.udacity.asteroidradar.network.parseAsteroidsJsonResult
import com.udacity.asteroidradar.utils.Constants.API_QUERY_DATE_FORMAT
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AsteroidRepository(private val database: AsteroidDatabase) {

    // Receive LiveData type from database, so we need to use LiveData for this even in repository
    val asteroidList: LiveData<List<Asteroid>> =
        Transformations.map(database.asteroidDao.getAsteroidList()) {
            it.toModel()
        }

    suspend fun refreshAsteroidList() {
        // Default end date is 7 days later, so we don't add it as a parameter in the query
        val startDate = getToday()
        try {
            database.asteroidDao.deleteOutdatedCache(today = startDate)
            val response = AsteroidApi.retrofitService.getAsteroidList(startDate)
            val list = parseAsteroidsJsonResult(JSONObject(response))
            database.asteroidDao.insertAll(*list.toDatabaseEntity())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getImageOfTheDay(): PictureOfDay? {
        return try {
            AsteroidApi.retrofitService.getImageOfTheDay()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getToday(): String {
        val date = SimpleDateFormat(API_QUERY_DATE_FORMAT, Locale.getDefault())
        return date.format(Date())
    }
}