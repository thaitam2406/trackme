package com.tamhuynh.trackme.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

class UtilsHelper {

    fun checkLocationServiceEnable(activity: Activity) : Boolean{
        var locationManager: LocationManager? = null
        var gps_enabled = false
        var network_enabled = false
        if (locationManager == null) {
            locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
        try {
            gps_enabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }
        try {
            network_enabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }
        if (!gps_enabled && !network_enabled) {
            val dialog: AlertDialog.Builder = AlertDialog.Builder(activity)
            dialog.setMessage("GPS not enabled")
            dialog.setPositiveButton("Ok"
            ) { _, _ -> //this will navigate user to the device location settings screen
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                activity.startActivity(intent)
            }
            val alert: AlertDialog = dialog.create()
            alert.show()
            return false
        } else {
            return true
        }
    }
}