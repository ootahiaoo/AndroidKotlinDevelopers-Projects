package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 43
const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 44
const val REQUEST_TURN_DEVICE_LOCATION_ON = 45
const val LOCATION_PERMISSION_INDEX = 0
const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
const val ACTION_GEOFENCE_EVENT =
    "SaveReminderFragment.action.ACTION_GEOFENCE_EVENT"

class SaveReminderFragment : BaseFragment() {

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var reminder: ReminderDataItem
    private val TAG = "SaveReminderFragment"

    // The location permission process is slightly different depending on the SDK version
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    private lateinit var geofencingClient: GeofencingClient
    private val geofenceRadiusInMeter = 100f
    private val geofenceExpirationDuration = TimeUnit.DAYS.toMillis(30)

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
        binding.lifecycleOwner = this
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
        if (isForegroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
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
            .setExpirationDuration(geofenceExpirationDuration)
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
        // Request to check the device's location setting current status
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        // If the location setting is set to OFF, prompt and help the user to turn it ON
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    // Prompt user to turn on location services
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, getString(R.string.location_setting_error, sendEx.message))
                }
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
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofence()
            }
        }
    }

    /**
     * When we get the result from asking the user to turn on device location,
     * call checkDeviceLocationSettingsAndStartGeofence() again to make sure it's actually on,
     * but don't resolve the check (in order to keep the user from seeing an endless loop)
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // If the user denied the device location permission, ask again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }
    //endregion

    //region Permissions
    /**
     * In all cases, we need to have the location permission
     * On Android 10+ (Q) we need to have the background permission as well
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        val requestInterrupted = grantResults.isEmpty()
        val missingOneLocationPermission =
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED

        // Only when running on API 29+
        val backgroundLocationPermissionDenied = (requestCode ==
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                PackageManager.PERMISSION_DENIED)

        if (requestInterrupted ||
            missingOneLocationPermission ||
            backgroundLocationPermissionDenied
        ) {
            // A permission is still missing, prompt the user to change the location setting
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    // Open the device settings
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            // All permissions are already granted
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    /**
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @TargetApi(29)
    private fun isForegroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /**
     *  Requests ACCESS_FINE_LOCATION and on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        // Permissions are already granted
        if (isForegroundAndBackgroundLocationPermissionApproved()) return

        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = if (runningQOrLater) {
            permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
            REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
        } else {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Request foreground only location permission")

        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )
    }
    //endregion


    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
