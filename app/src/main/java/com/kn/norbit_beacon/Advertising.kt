package com.kn.norbit_beacon

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.kn.norbit_beacon.databinding.FragmentSecondBinding
import android.bluetooth.le.AdvertisingSet

import android.bluetooth.le.AdvertisingSetCallback
import android.location.Location
import android.util.Log
import android.os.ParcelUuid
import com.google.android.material.snackbar.Snackbar
import java.util.*


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class Advertising : Fragment(), LocationProvider.Listener {

    private var _binding: FragmentSecondBinding? = null
    private lateinit var advertiser : BluetoothLeAdvertiser
    private val LOG_TAG : String = "noe"
    private lateinit var currentAdvertisingSet : AdvertisingSet
    private lateinit var callback : AdvertisingSetCallback
    private lateinit var locationFetcher: FusedLocationFetcher
    private lateinit var lastLocation: Location

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.i("Created", "created")
        // Set up location manager
        locationFetcher = FusedLocationFetcher(requireActivity(), this)
        initializeLocationFetcher()

        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        //val byteArray = byteArrayOfInts(0x0101FF0000001200F8002A000000000001000000000000)

        val parameters: AdvertisingSetParameters = (AdvertisingSetParameters.Builder())
            .setLegacyMode(true)
            .setConnectable(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            .build()

        val data: AdvertiseData = AdvertiseData.Builder().setIncludeDeviceName(false).build()

        advertiser = bluetoothManager.adapter.bluetoothLeAdvertiser;

        callback = object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet,
                txPower: Int,
                status: Int
            ) {
                Log.i(
                    LOG_TAG, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                            + status
                )
                currentAdvertisingSet = advertisingSet

                // After onAdvertisingSetStarted callback is called, you can modify the
                // advertising data and scan response data:
                val manufactuererData = byteArrayOfInts(0x01, 0x01, 0xFF, 0x00, 0x00, 0x00, 0x12, 0x00, 0xF8, 0x00, 0x2A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
                currentAdvertisingSet.setAdvertisingData(
                    AdvertiseData.Builder().setIncludeDeviceName(false)
                        .addManufacturerData(0xD109, manufactuererData)
                        .setIncludeTxPowerLevel(true).build()
                )

                // Wait for onAdvertisingDataSet callback...
                currentAdvertisingSet.setScanResponseData(
                    AdvertiseData.Builder().addServiceUuid(ParcelUuid(UUID.randomUUID())).build()
                )
                // Wait for onScanResponseDataSet callback...
            }

            override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
                Log.i(LOG_TAG, "onAdvertisingDataSet() :status:$status")
            }

            override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
                Log.i(LOG_TAG, "onScanResponseDataSet(): status:$status")
            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
                Log.i(LOG_TAG, "onAdvertisingSetStopped():")
            }
        }

        advertiser.startAdvertisingSet(parameters, data, null, null, null, callback);


        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root


        /*
        //The mail button.
        binding.fab.setOnClickListener { view ->
            // Should now have last known location in 'this.lastLocation'
            var snackbarText = ""
            if (!locationFetcher.getRunning()) {
                initializeLocationFetcher()
            }
            if (this::lastLocation.isInitialized) {
                if (lastLocation != null) {
                    val lat = lastLocation.latitude
                    val lng = lastLocation.longitude
                    val acc = lastLocation.accuracy
                    val age = (System.currentTimeMillis()-lastLocation.time)/1000
                    snackbarText = "Lat: $lat, Lng: $lng, Acc: $acc\nAge(sec): $age"
                } else {
                    snackbarText = "Location is null, missing permissions?"
                }
            } else{
                snackbarText = "Location is uninitialized, missing permissions?"
            }
            Snackbar.make(view, snackbarText, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }*/
    }

    private fun initializeLocationFetcher() {
        locationFetcher.setListener(this)
        locationFetcher.startUpdates()
    }

    override fun onNewLocationUpdate(location: Location) {
        onLocationUpdate(location)
    }

    override fun onLocationUpdate(location: Location?) {
        Log.i("Updated", "updated")
        if (location != null) {
            // TODO: Dont update if accuracy is worse than earlier and/or greater than some threshold
            lastLocation = location
            Log.i("Location:", "d"+location.accuracy)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    public fun moveToHome() {
        findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
    }

    override fun onDestroyView() {
        locationFetcher.stopUpdates()

        // When done with the advertising:
        advertiser.stopAdvertisingSet(callback)

        super.onDestroyView()
        _binding = null
    }
}