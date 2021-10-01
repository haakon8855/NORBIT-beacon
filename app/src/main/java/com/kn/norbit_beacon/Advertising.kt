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
import java.util.*
import kotlin.math.roundToLong


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class Advertising : Fragment(), LocationProvider.Listener {

    private var _binding: FragmentSecondBinding? = null
    private val LOG_TAG: String = "Bluetooth callback"
    private lateinit var advertiser : BluetoothLeAdvertiser
    private lateinit var currentAdvertisingSet : AdvertisingSet
    private lateinit var callback : AdvertisingSetCallback
    private lateinit var locationFetcher: FusedLocationFetcher
    private lateinit var lastLocation: Location
    private var manufacturerId: Int = 0xD109
    private var protocolId: ByteArray = byteArrayOfInts(0xDF, 0x02)
    private var ids: ByteArray = byteArrayOfInts(0x01, 0x00, 0xEE, 0x00, 0x00, 0x01)
    private lateinit var manufacturerData: ByteArray
    private val accuracyThreshold: Int = 10
    private var isAdvertising: Boolean = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        // Set up location manager
        locationFetcher = FusedLocationFetcher(requireActivity(), this)
        initializeLocationFetcher()

        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

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
                currentAdvertisingSet.setAdvertisingData(
                    AdvertiseData.Builder().setIncludeDeviceName(false)
                        .addManufacturerData(manufacturerId, manufacturerData)
                        .setIncludeTxPowerLevel(true).build()
                )

                // Wait for onAdvertisingDataSet callback...
                currentAdvertisingSet.setScanResponseData(
                    AdvertiseData.Builder().addServiceUuid(ParcelUuid(UUID.randomUUID())).build()
                )
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

    private fun setManufacturerData(protocol: ByteArray, ids: ByteArray, gps: ByteArray) {
        manufacturerData = protocol + ids + gps
    }

    private fun startAdvertising() {
        if (!isAdvertising) {
            val parameters: AdvertisingSetParameters = (AdvertisingSetParameters.Builder())
                .setLegacyMode(true)
                .setConnectable(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                .build()

            val data: AdvertiseData = AdvertiseData.Builder().setIncludeDeviceName(false).build()

            advertiser.startAdvertisingSet(parameters, data, null, null, null, callback);
            isAdvertising = true
        }
    }

    private fun initializeLocationFetcher() {
        locationFetcher.setListener(this)
        locationFetcher.startUpdates()
    }

    override fun onNewLocationUpdate(location: Location) {
        onLocationUpdate(location)
    }

    private fun longToLittleEndian4B(number: Long): ByteArray {
        val b = ByteArray(4)
        b[0] = (number and 0xFF).toByte()
        b[1] = (number shr 8 and 0xFF).toByte()
        b[2] = (number shr 16 and 0xFF).toByte()
        b[3] = (number shr 24 and 0xFF).toByte()
        return b
    }

    private fun longToLittleEndian2B(number: Long): ByteArray {
        // TODO: Merge this method into longToLittleEndian4B using a for loop
        val b = ByteArray(2)
        b[0] = (number and 0xFF).toByte()
        b[1] = (number shr 8 and 0xFF).toByte()
        return b
    }

    override fun onLocationUpdate(location: Location?) {
        if (location != null) {
            if (location.accuracy <= accuracyThreshold) {
                lastLocation = location
                val seconds = longToLittleEndian4B(lastLocation.time/1000)
                val lat = longToLittleEndian4B(((lastLocation.latitude + 180) * 100000).roundToLong())
                val lng = longToLittleEndian4B(((lastLocation.longitude + 90) * 100000).roundToLong())
                val alt = longToLittleEndian2B(lastLocation.altitude.toLong())
                val acc = ByteArray(1)
                acc[0] = (lastLocation.accuracy.roundToLong() and 0xFF).toByte()
                val gps = seconds + lat + lng + alt + acc
                setManufacturerData(protocolId, ids, gps)
                startAdvertising()
            }
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
        isAdvertising = false

        super.onDestroyView()
        _binding = null
    }
}