package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
    }

    val TAG = SelectLocationFragment::class.java.simpleName

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap

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
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {
        currentlySelectedPoi?.let {
            _viewModel.savePOI(it)
        }
        findNavController().popBackStack()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap ?: return
        map = googleMap
        // Zoom to the user's location if permission is granted
        askPermissionForLocation()
        setPoiClick(map)
        setMapStyle(map)

        Toast.makeText(requireContext(), R.string.select_poi, Toast.LENGTH_LONG).show()
    }

    /**
     * Get the user's last known location and zoom there on the map
     * This method is called after getting the location permission
     */
    @SuppressLint("MissingPermission")
    private fun zoomToUserLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                // Got last known location. In some rare situations this can be null.
                location ?: return@addOnSuccessListener

                val latLng = LatLng(location.latitude, location.longitude)
                val zoomLevel = 18f
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
            }
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

    //region Permission for location
    @SuppressLint("MissingPermission")
    private fun askPermissionForLocation() {
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true
            zoomToUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                Companion.REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) === PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if location permissions are granted.
        // If so enable the location data layer.
        if (requestCode == Companion.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                askPermissionForLocation()
            }
        }
        // If the request was denied, do not display the MyLocation button
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
