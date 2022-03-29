package com.udacity.asteroidradar.ui.main

import android.app.Application
import androidx.lifecycle.*
import com.udacity.asteroidradar.database.getDatabase
import com.udacity.asteroidradar.model.PictureOfDay
import com.udacity.asteroidradar.repository.AsteroidRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : ViewModel() {

    private val database = getDatabase(application)
    private val repository = AsteroidRepository(database)

    val asteroidList = repository.asteroidList

    private val _imageOfTheDay = MutableLiveData<PictureOfDay>()
    val imageOfTheDay: LiveData<PictureOfDay>
        get() = _imageOfTheDay

    val contentDescription = Transformations.map(_imageOfTheDay) {
        if (it.mediaType == "image") it.title else null
    }

    init {
        refreshAsteroidCacheList()
        getImageOfTheDay()
    }

    private fun getImageOfTheDay() {
        viewModelScope.launch {
            val image = repository.getImageOfTheDay() ?: return@launch
            _imageOfTheDay.value = image
        }
    }

    private fun refreshAsteroidCacheList() {
        viewModelScope.launch {
            repository.refreshAsteroidList()
        }
    }

    /**
     * Factory for constructing DevByteViewModel with parameter
     */
    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}