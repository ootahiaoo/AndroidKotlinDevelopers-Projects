package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataItem
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var remindersListViewModel: RemindersListViewModel

    private lateinit var dataSource: FakeDataSource

    private val application: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setupViewModel() {
        dataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(application, dataSource)
    }

    // https://stackoverflow.com/a/57041568
    @After
    fun clearSetup() = mainCoroutineRule.runBlockingTest {
        dataSource.deleteAllReminders()
        stopKoin()
    }

    @Test
    fun loadReminders_savedListAsDataItemLiveData() = mainCoroutineRule.runBlockingTest {
        val reminder1 = ReminderDataItem(
            title = "title1",
            description = "description1",
            location = "location1",
            latitude = 1.11,
            longitude = 1.11
        )
        val reminder2 = ReminderDataItem(
            title = "title2",
            description = "description2",
            location = "location2",
            latitude = 2.22,
            longitude = 2.22
        )
        val reminderList = listOf(reminder1, reminder2)
        dataSource.saveReminder(reminder1.toDTO())
        dataSource.saveReminder(reminder2.toDTO())

        remindersListViewModel.loadReminders()

        assertThat(
            remindersListViewModel.remindersList.getOrAwaitValue(),
            IsEqual(reminderList)
        )
        assertThat(
            remindersListViewModel.remindersList.getOrAwaitValue().size,
            `is`(2)
        )
    }

    @Test
    fun loadReminders_checkLoading() {
        // Pause before coroutine so we can verify initial value
        mainCoroutineRule.dispatcher.pauseDispatcher()

        // Load the reminders
        remindersListViewModel.loadReminders()

        // The loading indicator is displayed
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines
        mainCoroutineRule.dispatcher.resumeDispatcher()

        // The progress indicator is now hidden
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun setShouldReturnError_loadReminders_returnError() {
        dataSource.setReturnError(true)

        remindersListViewModel.loadReminders()

        // An error message is shown
        assertThat(
            remindersListViewModel.showSnackBar.getOrAwaitValue(),
            `is`("Reminders not found!")
        )
        assertThat(
            remindersListViewModel.showNoData.getOrAwaitValue(),
            `is`(true)
        )
    }
}