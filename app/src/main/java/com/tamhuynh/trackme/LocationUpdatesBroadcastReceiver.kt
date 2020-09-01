
package com.tamhuynh.trackme

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.tamhuynh.trackme.data.LocationRepository
import com.tamhuynh.trackme.data.MyLocationManager
import com.tamhuynh.trackme.MyLocationEntity
import java.util.Date
import java.util.concurrent.Executors

private const val TAG = "LUBroadcastReceiver"

/**
 * Receiver for handling location updates.
 *
 * For apps targeting API level O and above
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)} should be used when
 * requesting location updates in the background. Due to limits on background services,
 * {@link android.app.PendingIntent#getService(Context, int, Intent, int)} should NOT be used.
 *
 *  Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 *  less frequently than the interval specified in the
 *  {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 *  foreground.
 */
class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() context:$context, intent:$intent")
        intent.action?.let {
            if (it.contains(ACTION_PROCESS_UPDATES)) {
                val workoutRecordId : String = it.split("#")[1]
                LocationResult.extractResult(intent)?.let { locationResult ->
                    val locations = locationResult.locations.map { location ->
                        MyLocationEntity(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                foreground = isAppInForeground(context),
                                date = Date(location.time),
                                workoutRecordId = workoutRecordId.toInt()
                        )
                    }
                    if (locations.isNotEmpty()) {
                        LocationRepository.getInstance(context, Executors.newSingleThreadExecutor())
                                .addLocations(locations)
                    }
                }
            }
        }
    }

    // Note: This function's implementation is only for debugging purposes. If you are going to do
    // this in a production app, you should instead track the state of all your activities in a
    // process via android.app.Application.ActivityLifecycleCallbacks's
    // unregisterActivityLifecycleCallbacks(). For more information, check out the link:
    // https://developer.android.com/reference/android/app/Application.html#unregisterActivityLifecycleCallbacks(android.app.Application.ActivityLifecycleCallbacks
    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false

        appProcesses.forEach { appProcess ->
            if (appProcess.importance ==
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == context.packageName) {
                return true
            }
        }
        return false
    }

    companion object {
        const val ACTION_PROCESS_UPDATES =
            "com.tamhuynh.trackme.action." +
                    "PROCESS_UPDATES"
    }
}
