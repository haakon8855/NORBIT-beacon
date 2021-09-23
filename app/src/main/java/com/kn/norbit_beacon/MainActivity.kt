package com.kn.norbit_beacon

import android.location.Location
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.kn.norbit_beacon.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), LocationProvider.Listener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var lastLocation: Location
    private lateinit var locationFetcher: FusedLocationFetcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Set up location manager
        locationFetcher = FusedLocationFetcher(this)
        initializeLocationFetcher()
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
        }
    }

    private fun initializeLocationFetcher() {
        locationFetcher.setListener(this)
        locationFetcher.startUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onNewLocationUpdate(location: Location) {
        onLocationUpdate(location)
    }

    override fun onLocationUpdate(location: Location?) {
        if (location != null) {
            lastLocation = location
        }
    }

}