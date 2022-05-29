package com.udacity.project4.utils

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task
import com.udacity.project4.BuildConfig
import com.udacity.project4.R

const val REQUEST_TURN_DEVICE_LOCATION_ON = 45
const val LOCATION_PERMISSION_INDEX = 0
const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 43
const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 44

// The location permission process is slightly different depending on the SDK version
val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
        android.os.Build.VERSION_CODES.Q

//region Location setting
/**
 * Build the request to check the device's location setting current status
 */
fun Context.requestLocationSettingStatus(): Task<LocationSettingsResponse> {
    val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_LOW_POWER
    }
    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    val settingsClient = LocationServices.getSettingsClient(this)
    return settingsClient.checkLocationSettings(builder.build())
}

/**
 * Prompt user to turn on location services
 *
 * Override onActivityResult() to handle result
 */
fun ResolvableApiException.requestToTurnOnLocationSetting(activity: Activity) {
    try {
        startResolutionForResult(
            activity,
            REQUEST_TURN_DEVICE_LOCATION_ON
        )
    } catch (sendEx: IntentSender.SendIntentException) {
        Log.d(
            activity.javaClass.simpleName, activity.getString(
                R.string.location_setting_error,
                sendEx.message
            )
        )
    }
}
//endregion

//region Location permission
fun Context.openAppSettings() {
    startActivity(Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}

/**
 * Determines whether the app has the appropriate permissions across Android 10+ and all other
 * Android versions.
 */
@TargetApi(29)
fun isLocationPermissionApproved(
    activity: Activity,
    foregroundOnly: Boolean = false
): Boolean {
    val foregroundLocationApproved = (
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ))
    val backgroundPermissionApproved =
        if (runningQOrLater && !foregroundOnly) {
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
        }
    return foregroundLocationApproved && backgroundPermissionApproved
}

/**
 * Requests ACCESS_FINE_LOCATION and on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
 *
 * Override onRequestPermissionsResult() to handle result
 */
@TargetApi(29)
fun requestLocationPermissions(
    activity: Activity,
    foregroundOnly: Boolean = false
) {
    // Permissions are already granted
    if (isLocationPermissionApproved(activity, foregroundOnly)) return

    var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    val resultCode = if (runningQOrLater && !foregroundOnly) {
        permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
        REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
    } else {
        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
    }

    ActivityCompat.requestPermissions(
        activity,
        permissionsArray,
        resultCode
    )
}

/**
 * Check the result of onRequestPermissionsResult() after requesting permissions
 * On Android 10+ (Q) we need to have the background permission as well
 */
fun isStillMissingAPermission(
    requestCode: Int,
    grantResults: IntArray
): Boolean {
    val requestInterrupted = grantResults.isEmpty()
    val missingOneLocationPermission =
        grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED

    // Only when running on API 29+
    val backgroundLocationPermissionDenied =
        (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)

    return (requestInterrupted || missingOneLocationPermission || backgroundLocationPermissionDenied)
}
//endregion
