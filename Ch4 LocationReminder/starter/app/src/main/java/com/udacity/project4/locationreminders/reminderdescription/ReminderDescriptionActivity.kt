package com.udacity.project4.locationreminders.reminderdescription

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.data.ReminderDataItem
import org.koin.android.ext.android.inject

/**
 * Activity that displays the reminder details after the user clicks on the notification,
 * or after the user clicks on an item in the reminder list
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        // Receive the reminder object after the user clicks on the notification,
        // or from the reminder lists
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    val viewModel: ReminderDescriptionViewModel by inject()
    private lateinit var binding: ActivityReminderDescriptionBinding
    private val TAG = "DescriptionActivity"
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminder = intent.getParcelableExtra<ReminderDataItem>(EXTRA_ReminderDataItem) ?: return
        viewModel.setReminder(reminder)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.deleteButton.setOnClickListener {
            removeGeofence()
        }

        viewModel.finishActivity.observe(this) {
            if (it == true) finish()
        }

        geofencingClient = LocationServices.getGeofencingClient(this)
    }

    /**
     * Don't seem to need permissions when removing geofences
     */
    private fun removeGeofence() {
        val reminderId = viewModel.reminder.value?.id ?: return

        geofencingClient.removeGeofences(listOf(reminderId))?.run {
            addOnSuccessListener {
                // If the geofence was successfully removed, update the local database
                viewModel.deleteReminder(reminderId)
            }
            addOnFailureListener {
                Log.d(TAG, getString(R.string.geofence_not_removed))
            }
        }
    }
}
