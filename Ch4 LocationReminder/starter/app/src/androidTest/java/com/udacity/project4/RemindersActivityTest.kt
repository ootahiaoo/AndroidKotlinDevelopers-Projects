package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderdescription.ReminderDescriptionViewModel
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LargeTest
// END TO END test to black box test the app
class RemindersActivityTest :
    KoinTest {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    // https://knowledge.udacity.com/questions/505362
    @get:Rule
    val activityTestRule =
        object : ActivityTestRule<RemindersActivity>(RemindersActivity::class.java) {
            override fun beforeActivityLaunched() {
                super.beforeActivityLaunched()
                init()
            }
        }

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private val testReminder = ReminderDTO(
        title = "title",
        description = "description",
        location = "location",
        latitude = 1.23,
        longitude = 1.23,
        id = "123"
    )

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    fun init() {
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }

        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            viewModel {
                ReminderDescriptionViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        // Declare a new koin module
        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(myModule))
        }
        // Get our real repository
        repository = get()

        // Clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun cleanUp() = runTest {
        repository.deleteAllReminders()
        stopKoin()
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun oneReminder_deleteReminder_displayEmptyList() = runBlocking {
        // 1. Start activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 2. Add a reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        // (SaveReminderFragment)
        onView(withId(R.id.selectLocation)).perform(click())
        // (SelectLocationFragment)
        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.save_button)).perform(click())
        // (SaveReminderFragment)
        onView(withId(R.id.reminderTitle)).perform(replaceText(testReminder.title))
        onView(withId(R.id.reminderDescription)).perform(replaceText(testReminder.description))
        onView(withId(R.id.saveReminder)).perform(click())

        // 3. Open the newly created reminder
        onView(withText(testReminder.title)).perform(click())

        // 4. Delete the reminder
        onView(withId(R.id.delete_button)).perform(click())

        // 5. Assert that the reminder is not displayed anymore in the reminder list
        onView(withText(testReminder.title)).check(doesNotExist())
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun saveReminder_showConfirmationToast() = runBlocking {
        // 1. Start activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 2. Add a reminder
        onView(withId(R.id.addReminderFAB)).perform(click())
        // (SaveReminderFragment)
        onView(withId(R.id.selectLocation)).perform(click())
        // (SelectLocationFragment)
        // Wait for the "Select a point of interest" Toast to disappear
        // (or it would still be on when we return to the reminder list)
        runBlocking {
            delay(2000)
        }
        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.save_button)).perform(click())
        // (SaveReminderFragment)
        onView(withId(R.id.reminderTitle)).perform(replaceText(testReminder.title))
        onView(withId(R.id.reminderDescription)).perform(replaceText(testReminder.description))
        onView(withId(R.id.saveReminder)).perform(click())

        // 3. Assert that the confirmation Toast is displayed
        onView(withText(R.string.reminder_saved))
            .inRoot(withDecorView(not(activityTestRule.activity.window.decorView)))
            .check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun saveEmptyReminder_showErrorSnackBar() = runBlocking {
        // 1. Start activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 2. Open SaveReminderFragment
        onView(withId(R.id.addReminderFAB)).perform(click())

        // 3. (Try to) save an empty reminder
        onView(withId(R.id.reminderDescription)).perform(replaceText(testReminder.description))
        onView(withId(R.id.saveReminder)).perform(click())

        // 4. Assert that the error SnackBar is displayed
        onView(withText(R.string.err_enter_title)).check(matches(isDisplayed()))

        activityScenario.close()
    }
}
