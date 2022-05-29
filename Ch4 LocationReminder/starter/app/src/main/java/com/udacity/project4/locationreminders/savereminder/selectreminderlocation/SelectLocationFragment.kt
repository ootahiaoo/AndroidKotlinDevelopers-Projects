package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.*
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val TAG = "SelectLocationFragment"

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Used to request an update, in case it is the first time the user uses location services
    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            locationResult?.locations ?: return

            for (location in locationResult.locations) {
                if (location == null) return
                zoomToUserLocation(location)
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
    }

    // The location the user selected displayed as a marker on the map
    // Only one can be selected at a time
    private var poiMarker: Marker? = null

    // Currently selected but not saved yet
    private var currentlySelectedPoi: PointOfInterest? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        if (this::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun onLocationSelected() {
        currentlySelectedPoi?.let {
            _viewModel.savePOI(it)
        }
        findNavController().popBackStack()
    }

    //region Map
    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap ?: return
        map = googleMap
        // Zoom to the user's location if permission is granted
        checkPermissionsToUserLocation()
        setPoiClick(map)
        setMapStyle(map)

        Toast.makeText(requireContext(), R.string.select_poi, Toast.LENGTH_LONG).show()
    }

    fun zoomToUserLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        val zoomLevel = 18f
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
    }

    /**
     * Add a marker on the map when the user selects a point of interest (shop, etc)
     * Only allow one marker to be shown at a time
     */
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            poiMarker?.remove()

            val marker = MarkerOptions()
                .position(poi.latLng)
                .title(poi.name)

            poiMarker = map.addMarker(marker)
            poiMarker?.showInfoWindow()
            currentlySelectedPoi = poi
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                // Custom style made with https://mapstyle.withgoogle.com/
                // Retro theme, POI icon and text color are in the app's primary dark color (#00574B)
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }
    //endregion

    /**
     * Get the user's last known location to zoom there on the map
     * This method is called after getting the location permission and switching the location
     * setting to ON if necessary
     */
    @SuppressLint("MissingPermission")
    private fun getUserLastKnownLocation() {
        map.isMyLocationEnabled = true
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            // If there is no last known location, request an update
            // (For example, if this was the first time the location setting is set to ON)
            if (location == null) {
                requestLocationUpdate()
                return@addOnSuccessListener
            }
            zoomToUserLocation(location)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdate() {
        // https://blog.teamtreehouse.com/beginners-guide-location-android
        // https://stackoverflow.com/a/68869344
        val request = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
    }

    //region Location permissions & setting
    private fun checkPermissionsToUserLocation() {
        if (isLocationPermissionApproved(requireActivity(), foregroundOnly = true)) {
            checkDeviceLocationSettingsAndZoomToUserLocation()
        } else {
            requestLocationPermissions(requireActivity(), foregroundOnly = true)
        }
    }

    /**
     * Handle the foreground location permission result, before checking the device location setting
     *
     * Callback result from RemindersActivity's onRequestPermissionsResult()
     */
    fun checkPermissionResult(isMissingPermission: Boolean) {
        if (isMissingPermission) {
            // A permission is still missing, prompt the user to change the location setting
            // and do not display the MyLocation button
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                // Don't make the SnackBar stay on indefinitely
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.settings) {
                    requireContext().openAppSettings()
                }.show()
        } else {
            // Check the location setting (if it is OFF, the location won't be displayed even
            // with permission granted)
            checkDeviceLocationSettingsAndZoomToUserLocation()
        }
    }

    /**
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndZoomToUserLocation(resolve: Boolean = true) {
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
                    R.string.location_required_for_map,
                    // Don't make the SnackBar stay on indefinitely
                    Snackbar.LENGTH_LONG
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndZoomToUserLocation()
                }.show()
            }
        }

        // The location setting is already ON, zoom to user location on the map
        locationSettingsResponse.addOnCompleteListener {
            if (it.isSuccessful) {
                getUserLastKnownLocation()
            }
        }
    }

    /**
     * When we get the result from asking the user to turn on device location,
     * call checkDeviceLocationSettingsAndZoomToUserLocation() again to make sure it's actually on,
     * but don't resolve the check (in order to keep the user from seeing an endless loop)
     *
     * Callback result from RemindersActivity's onActivityResult()
     */
    fun checkDeviceLocationRequestResult() {
        checkDeviceLocationSettingsAndZoomToUserLocation(false)
    }
    //endregion

    //region Menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
    //endregion
}
