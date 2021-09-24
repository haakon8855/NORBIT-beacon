package com.kn.norbit_beacon

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
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
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.fragment.findNavController
import com.kn.norbit_beacon.databinding.FragmentSecondBinding
import android.bluetooth.le.AdvertisingSet

import android.bluetooth.le.AdvertisingSetCallback
import android.content.pm.PackageManager
import android.util.Log
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import java.util.*


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class Advertising : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private lateinit var advertiser : BluetoothLeAdvertiser
    private val LOG_TAG : String = "noe"
    private lateinit var currentAdvertisingSet : AdvertisingSet
    private lateinit var callback : AdvertisingSetCallback

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        val parameters: AdvertisingSetParameters = (AdvertisingSetParameters.Builder())
            .setLegacyMode(true)
            .setConnectable(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            .build()

        val data: AdvertiseData = AdvertiseData.Builder().setIncludeDeviceName(true).build()

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
                    AdvertiseData.Builder().setIncludeDeviceName(true).setIncludeTxPowerLevel(true).build()
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

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }


    override fun onDestroyView() {

        // When done with the advertising:
        advertiser.stopAdvertisingSet(callback)

        super.onDestroyView()
        _binding = null
    }
}