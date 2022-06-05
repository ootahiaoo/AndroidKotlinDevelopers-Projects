package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.data.ReminderDataItem
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.utils.*
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

const val ACTION_GEOFENCE_EVENT =
    "SaveReminderFragment.action.ACTION_GEOFENCE_EVENT"

class SaveReminderFragment : BaseFragment() {

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var reminder: ReminderDataItem
    private val TAG = "SaveReminderFragment"

    private lateinit var geofencingClient: GeofencingClient
    private val geofenceRadiusInMeter = 100f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminder = ReminderDataItem(
                title = title,
                description = description,
                location = location,
                latitude = latitude,
                longitude = longitude
            )

            // The error message SnackBar is displayed by the BaseViewModel
            if (!_viewModel.validateEnteredData(reminder)) return@setOnClickListener

            // Save the reminder only after having successfully added the geofence
            checkPermissionsBeforeStartingGeofencing()
        }
    }

    private fun checkPermissionsBeforeStartingGeofencing() {
        if (isLocationPermissionApproved(requireActivity())) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestLocationPermissions(requireActivity())
        }
    }

    /**
     * Handle the background/foreground location permission result
     * The result for the device location setting change request is handled with onActivityResult()
     *
     * Callback result from RemindersActtivity's onRequestPermissionsResult()
     */
    fun checkPermissionResult(isMissingPermission: Boolean) {
        if (isMissingPermission) {
            // A permission is still missing, prompt the user to change the location setting
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    requireContext().openAppSettings()
                }.show()
        } else {
            // All permissions are already granted
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    //region Geofencing
    /**
     * Add the geofence, then save the reminder in the database
     *
     * This method is called only after the user has granted the location permission
     */
    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        val geofencingRequest = createGeofenceRequest()

        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        val geofencePendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            addOnSuccessListener {
                // The navigation back to the list screen (after saving the reminder) is handled
                // inside the ViewModel
                _viewModel.saveReminder(reminder)
            }

            addOnFailureListener {
                Toast.makeText(requireContext(), R.string.geofences_not_added, Toast.LENGTH_SHORT)
                    .show()
                val errorMessage = it.message ?: getString(R.string.error_adding_geofence)
                Log.w(TAG, errorMessage)
            }
        }
    }

    // Helper method for addGeofence()
    private fun createGeofenceRequest(): GeofencingRequest {
        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                geofenceRadiusInMeter
            )
            .setExpirationDuration(NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }
    //endregion

    //region Location setting
    /**
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationSettingsResponse = requireContext().requestLocationSettingStatus()

        // If the location setting is set to OFF, prompt and help the user to turn it ON
        locationSettingsResponse.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                exception.requestToTurnOnLocationSetting(requireActivity())
            } else {
                // Explain that the location setting needs to be turned ON
                // If the SnackBar is tapped, starts the permission process again
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }

        // The location setting is already ON, proceed with the geofencing
        locationSettingsResponse.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofence()
            }
        }
    }

    /**
     * When we get the result from asking the user to turn on device location,
     * call checkDeviceLocationSettingsAndStartGeofence() again to make sure it's actually on,
     * but don't resolve the check (in order to keep the user from seeing an endless loop)
     *
     * Callback result from RemindersActivity's onActivityResult()
     */
    fun checkDeviceLocationRequestResult() {
        checkDeviceLocationSettingsAndStartGeofence(false)
    }
    //endregion

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
