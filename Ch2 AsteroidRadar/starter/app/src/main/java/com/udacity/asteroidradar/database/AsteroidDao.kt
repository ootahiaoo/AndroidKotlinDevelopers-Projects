package com.udacity.asteroidradar.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AsteroidDao {
    @Query("SELECT * FROM asteroids ORDER BY close_approach_date")
    fun getAsteroidList(): LiveData<List<AsteroidEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg asteroidList: AsteroidEntity)

    @Query("DELETE FROM asteroids WHERE close_approach_date < :today")
    suspend fun deleteOutdatedCache(today: String)
}