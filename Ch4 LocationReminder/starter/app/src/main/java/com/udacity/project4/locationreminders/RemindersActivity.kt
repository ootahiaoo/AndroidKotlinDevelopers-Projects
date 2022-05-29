package com.udacity.project4.locationreminders

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.udacity.project4.R
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import com.udacity.project4.utils.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.utils.isStillMissingAPermission
import kotlinx.android.synthetic.main.activity_reminders.*

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                (nav_host_fragment as NavHostFragment).navController.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val permissionResult = isStillMissingAPermission(
            requestCode = requestCode,
            grantResults = grantResults
        )

        val fragment = getCurrentlyVisibleFragment() ?: return
        when (fragment) {
            is SelectLocationFragment -> fragment.checkPermissionResult(permissionResult)
            is SaveReminderFragment -> fragment.checkPermissionResult(permissionResult)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            val fragment = getCurrentlyVisibleFragment() ?: return
            when (fragment) {
                is SelectLocationFragment -> fragment.checkDeviceLocationRequestResult()
                is SaveReminderFragment -> fragment.checkDeviceLocationRequestResult()
            }
        }
    }

    private fun getCurrentlyVisibleFragment(): Fragment? {
        val stack = (nav_host_fragment as NavHostFragment).childFragmentManager.fragments
        for (fragment in stack) {
            if (fragment != null && fragment.isVisible) {
                return fragment
            }
        }
        return null
    }
}
