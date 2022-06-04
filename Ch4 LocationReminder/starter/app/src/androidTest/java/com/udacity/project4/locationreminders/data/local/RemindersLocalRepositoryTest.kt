package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    private val testReminder = ReminderDTO(
        title = "title",
        description = "description",
        location = "location",
        latitude = 1.23,
        longitude = 1.23,
        id = "123"
    )

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminder_retrieveReminder_reminderIsNotNull() = runTest {
        repository.saveReminder(testReminder)

        val result = repository.getReminder(testReminder.id)

        assertThat(result, `is`(Result.Success(testReminder)))
    }

    @Test
    fun emptyDb_getReminders_returnEmptyList() = runTest {
        val result = repository.getReminders()

        assertThat(result, `is`(Result.Success(emptyList())))
    }

    @Test
    fun emptyDb_getReminderById_returnError() = runTest {
        val result = repository.getReminder(testReminder.id)

        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }
}