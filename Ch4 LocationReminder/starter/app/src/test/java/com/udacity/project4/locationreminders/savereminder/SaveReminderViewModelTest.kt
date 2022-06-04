package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataItem
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private lateinit var dataSource: FakeDataSource

    private val application: Application = ApplicationProvider.getApplicationContext()

    private val testReminder = ReminderDataItem(
        title = "title",
        description = "description",
        location = "location",
        latitude = 1.23,
        longitude = 1.23
    )

    @Before
    fun setupViewModel() {
        dataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(application, dataSource)
    }

    // https://stackoverflow.com/a/57041568
    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun onClear_reminderLiveDataSetToNull() {
        saveReminderViewModel.onClear()

        assertThat(
            saveReminderViewModel.reminderTitle.getOrAwaitValue(),
            `is`(nullValue())
        )
        assertThat(
            saveReminderViewModel.reminderDescription.getOrAwaitValue(),
            `is`(nullValue())
        )
        assertThat(
            saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(),
            `is`(nullValue())
        )
        assertThat(
            saveReminderViewModel.selectedPOI.getOrAwaitValue(),
            `is`(nullValue())
        )
        assertThat(
            saveReminderViewModel.latitude.getOrAwaitValue(),
            `is`(nullValue())
        )
        assertThat(
            saveReminderViewModel.longitude.getOrAwaitValue(),
            `is`(nullValue())
        )
    }

    @Test
    fun savePOI_reminderLiveDataChanged() {
        val poi = PointOfInterest(LatLng(1.23, 1.23), "placeId", "name")

        saveReminderViewModel.savePOI(poi)

        assertThat(
            saveReminderViewModel.selectedPOI.getOrAwaitValue(),
            `is`(poi)
        )
        assertThat(
            saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(),
            `is`(poi.name)
        )
        assertThat(
            saveReminderViewModel.latitude.getOrAwaitValue(),
            `is`(poi.latLng.latitude)
        )
        assertThat(
            saveReminderViewModel.longitude.getOrAwaitValue(),
            `is`(poi.latLng.longitude)
        )
    }

    @Test
    fun saveReminder_savedReminderToDatabase() = mainCoroutineRule.runBlockingTest {
        saveReminderViewModel.saveReminder(testReminder)

        val result = dataSource.getReminder(testReminder.id) as Result.Success
        val resultData = result.data

        assertThat(resultData.title, `is`(testReminder.title))
        assertThat(resultData.description, `is`(testReminder.description))
        assertThat(resultData.location, `is`(testReminder.location))
        assertThat(resultData.latitude, `is`(testReminder.latitude))
        assertThat(resultData.longitude, `is`(testReminder.longitude))
        assertThat(resultData.id, `is`(testReminder.id))
    }

    @Test
    fun emptyOrNullTitle_validateEnteredData_returnFalse() {
        val emptyTitleReminder = testReminder.copy(title = "")
        val nullTitleReminder = testReminder.copy(title = null)

        val emptyTitleResult = saveReminderViewModel.validateEnteredData(emptyTitleReminder)
        val nullTitleResult = saveReminderViewModel.validateEnteredData(nullTitleReminder)

        assertThat(emptyTitleResult, `is`(false))
        assertThat(nullTitleResult, `is`(false))
    }

    @Test
    fun emptyLocation_validateEnteredData_returnFalseAndDisplaySnackBar() {
        val emptyLocationReminder = testReminder.copy(location = "")

        val result = saveReminderViewModel.validateEnteredData(emptyLocationReminder)

        // Result is false, show SnackBar
        assertThat(result, `is`(false))
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location)
        )
    }

    @Test
    fun nonNullData_validateEnteredData_returnTrue() {
        val result = saveReminderViewModel.validateEnteredData(testReminder)

        assertThat(result, `is`(true))
    }
}