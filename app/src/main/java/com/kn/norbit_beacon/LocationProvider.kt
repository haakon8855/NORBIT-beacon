package com.kn.norbit_beacon

import android.location.Location

interface LocationProvider {

    /**
     * Returns the running state of the location provider
     */
    fun getRunning(): Boolean

    /**
     * Subscribe to notifications for location updates
     * @param listener
     */
    fun setListener(listener: Listener?)

    /**
     * Gets the current location known to the [LocationProvider]
     */
    fun getCurrentLocation(): Location?

    /**
     * Starts the location provider's services
     */
    fun startUpdates()

    /**
     * Stops the location provider's services
     */
    fun stopUpdates()

    /**
     * Listener to receive updates from a [LocationProvider]
     */
    interface Listener {
        /**
         * Triggered when a new location is received
         */
        fun onNewLocationUpdate(location: Location)

        /**
         * Triggered when any location update is received
         */
        fun onLocationUpdate(location: Location?)
    }
}
