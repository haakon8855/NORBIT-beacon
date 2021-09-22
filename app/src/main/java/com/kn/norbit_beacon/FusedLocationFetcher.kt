package com.kn.norbit_beacon

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

const val BEACON_LOCATION_REQUEST_CODE = 99

class FusedLocationFetcher(activity: MainActivity) : LocationProvider {
    companion object {
        //How fast can your application process location updates requested by other applications
        private val DEFAULT_FASTEST_INTERVAL: Long = 500 // 1 Second

        //Interval to ask for location updates until one has been determined
        private val DEFAULT_SHORT_INTERVAL: Long = 500 // 1/2 Second

        //Interval that is used AFTER a non-null location is received
        private val DEFAULT_LONG_INTERVAL: Long = 500 //20 * 60 * 1000 //20 Minutes

        //The accuracy of the location updates
        private val LOCATION_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY

        private fun isSameLatLong(l1: Location, l2: Location): Boolean =
            (l1.latitude == l2.latitude && l1.longitude == l2.longitude)
    }

    private val client = LocationServices.getFusedLocationProviderClient(activity)
    private val activity = activity
    private val locationCallbackHelper = LocationCallbackHelper()
    private var lastKnownLocation: Location? = null
    private var listener: LocationProvider.Listener? = null
    private var locationRequest = LocationRequest.create()
    private var running: Boolean = false

    override fun getCurrentLocation(): Location? = lastKnownLocation

    override fun getRunning(): Boolean {
        return running
    }

    public fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            BEACON_LOCATION_REQUEST_CODE
        )
    }

    public fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Access not granted
            running = false
            stopUpdates()
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                AlertDialog.Builder(activity)
                    .setTitle("Location service required")
                    .setMessage("This app requires location services in order to funciton properly.")
                    .setPositiveButton("OK") { _, _ ->
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                requestLocationPermission()
            }
        }
    }


    override fun setListener(listener: LocationProvider.Listener?) {
        this.listener = listener
    }

    override fun startUpdates() {
        stopUpdates()
        running = true
        if (this.listener != null) {
            checkLocationPermission()
            client.lastLocation
                .addOnSuccessListener { location -> handleLocationUpdate(location) }
                .addOnCompleteListener {
                    val interval =
                        if (lastKnownLocation == null) DEFAULT_SHORT_INTERVAL else DEFAULT_LONG_INTERVAL
                    requestLocationUpdates(interval, LOCATION_PRIORITY)
                }

        }
    }

    override fun stopUpdates() {
        client.removeLocationUpdates(locationCallbackHelper)
        reset()
    }

    //Permissions and location services should be enabled by this point, make sure you do it!!
    private fun requestLocationUpdates(interval: Long, priority: Int) {
        locationRequest.setFastestInterval(DEFAULT_FASTEST_INTERVAL)
            .setInterval(interval)
            .setPriority(priority)
        checkLocationPermission()
        client.requestLocationUpdates(locationRequest, locationCallbackHelper, null)
    }

    private fun handleLocationUpdate(location: Location?) {
        val lastLocation = lastKnownLocation
        lastKnownLocation = location

        //Update to a longer interval, also will trigger a location update using the last one
        if (location != null) {
            if (lastLocation == null || !isSameLatLong(
                    lastLocation,
                    location
                )
            ) listener?.onNewLocationUpdate(location)
            if (locationRequest.interval == DEFAULT_SHORT_INTERVAL) {
                requestLocationUpdates(DEFAULT_LONG_INTERVAL, LOCATION_PRIORITY)
                return
            }
        }
        listener?.onLocationUpdate(location)
    }

    private fun reset() {
        lastKnownLocation = null
    }

    private inner class LocationCallbackHelper : LocationCallback() {
        override fun onLocationResult(lr: LocationResult) {
            handleLocationUpdate(lr.lastLocation)
        }

        override fun onLocationAvailability(la: LocationAvailability) {
            if (lastKnownLocation == null && la.isLocationAvailable) {
                checkLocationPermission()
                client.lastLocation.addOnSuccessListener { location ->
                    handleLocationUpdate(location)
                }
            }
        }
    }
}
