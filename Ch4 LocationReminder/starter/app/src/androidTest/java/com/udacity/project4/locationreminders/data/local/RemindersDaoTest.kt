package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

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
        // Database will be deleted once the process is killed
        // (never actually stored on disk)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminder_retrieveReminder_reminderIsNotNull() = runTest {
        // Insert data
        database.reminderDao().saveReminder(testReminder)

        // Retrieve data
        val result = database.reminderDao().getReminderById(testReminder.id)

        assertThat(result as ReminderDTO, notNullValue())
        assertThat(result, IsEqual(testReminder))
    }


    @Test
    fun deleteAllReminders_getReminders_returnEmptyList() = runTest {
        database.reminderDao().deleteAllReminders()

        val result = database.reminderDao().getReminders()

        // No data found
        assertThat(result, `is`(emptyList()))
    }
}