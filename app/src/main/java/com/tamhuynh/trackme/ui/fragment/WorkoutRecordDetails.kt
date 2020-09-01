
package com.tamhuynh.trackme.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.tamhuynh.trackme.R
import com.tamhuynh.trackme.WorkoutRecordData
import com.tamhuynh.trackme.databinding.FragmentLocationUpdateBinding
import com.tamhuynh.trackme.hasPermission
import com.tamhuynh.trackme.viewmodels.LocationUpdateViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.ui.IconGenerator
import com.tamhuynh.trackme.ui.UtilsHelper


private const val TAG = "WorkoutRecordDetails"

/**
 * Displays location information via PendingIntent after permissions are approved.
 *
 * Will suggest "enhanced feature" to enable background location requests if not approved.
 */
class WorkoutRecordDetails : Fragment() , OnMapReadyCallback {

    private var isMapReady: Boolean = false
    private var activityListener: Callbacks? = null

    private lateinit var binding: FragmentLocationUpdateBinding
    private val points : ArrayList<LatLng> = ArrayList()
    var line : Polyline? = null
    private var googleMap : GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var firstLocation: Location? = null

    private val locationUpdateViewModel by lazy {
        ViewModelProvider(this).get(LocationUpdateViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentLocationUpdateBinding.inflate(inflater, container, false)

        activity?.let {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(it)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            var workoutRecordData: WorkoutRecordData = it.getSerializable(WORKOUT_RECORD_DATA) as WorkoutRecordData
            workoutRecordData.locations.forEach {
                var latln = LatLng(it.latitude, it.longitude)
                points.add(latln)

                val fm: SupportMapFragment = childFragmentManager
                        .findFragmentById(R.id.googleMap) as SupportMapFragment
                fm.getMapAsync(this)
            }

            binding.distance.text = workoutRecordData.listWorkoutRecordEntity.distance.toString() + " m"
            binding.speed.text = workoutRecordData.listWorkoutRecordEntity.v.toString() + " ${getString(R.string.km_per_hour)}"
            binding.timer.text = workoutRecordData.listWorkoutRecordEntity.totalTime
        }
        binding.startOrStopLocationUpdatesButton.visibility = View.GONE
    }

    private fun reDrawLine(){
        googleMap?.clear() //clears all Markers and Polylines
        val options = PolylineOptions().width(5f).color(Color.BLUE).geodesic(true)
        for (i in points.indices) {
            val point = points[i]
            options.add(point)
        }
//        addMarker() //add Marker in current position
        line = googleMap?.addPolyline(options) //add Polyline
        firstLocation?.let {
//            addMarker(it, true)
        }
        centerMapOnMyLocation()
    }

    private fun addMarker(location: LatLng, isStart : Boolean) {
        val options = MarkerOptions()

        // following four lines requires 'Google Maps Android API Utility Library'
        // https://developers.google.com/maps/documentation/android/utility/
        // I have used this to display the time as title for location markers
        // you can safely comment the following four lines but for this info
        val iconFactory = IconGenerator(LocationUpdateFragment@this.activity?.applicationContext)
        iconFactory.setStyle(if(isStart) IconGenerator.STYLE_BLUE else IconGenerator.STYLE_RED)
        // options.icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(mLastUpdateTime + requiredArea + city)));
        options.icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(if(isStart) "Start" else "Stop")))
        options.anchor(iconFactory.anchorU, iconFactory.anchorV)
        val currentLatLng = LatLng(location.latitude, location.longitude)
        options.position(currentLatLng)
        val mapMarker: Marker? = googleMap?.addMarker(options)
        /*val atTime: Long = location.getTime()
        mLastUpdateTime = DateFormat.getTimeInstance().format(Date(atTime))
        val title: String = mLastUpdateTime.concat(", $requiredArea").concat(", $city").concat(", $country")
        mapMarker?.setTitle(title)
        val mapTitle: TextView = findViewById(R.id.textViewTitle) as TextView
        mapTitle.setText(title)*/
        Log.d(TAG, "Marker added.............................")
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13f))
        Log.d(TAG, "Zoom done.............................")
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        activity?.let { UtilsHelper().checkLocationServiceEnable(it) }
    }

    override fun onDetach() {
        super.onDetach()

        activityListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface Callbacks {
        fun requestFineLocationPermission()
        fun requestBackgroundLocationPermission()
    }

    companion object {
        val WORKOUT_RECORD_DATA = "WORKOUT_RECORD_DATA"
        fun newInstance() = WorkoutRecordDetails()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap?) {
        isMapReady = true
        p0?.isMyLocationEnabled = true
        p0?.setOnMyLocationButtonClickListener(object: GoogleMap.OnMyLocationButtonClickListener{
            override fun onMyLocationButtonClick(): Boolean {
                Toast.makeText(this@WorkoutRecordDetails.context, "Location button clicked", Toast.LENGTH_LONG).show()
                return true;
            }

        })
        p0?.uiSettings?.isZoomControlsEnabled = true
        p0?.uiSettings?.setAllGesturesEnabled(true)

        googleMap = p0
        reDrawLine()
    }

    @SuppressLint("MissingPermission")
    private fun centerMapOnMyLocation() {
        fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    // Got last known location. In some rare situations this can be null.
                    location?.let {
                        val zoomLevel = 19.0f //This goes up to 21
                        val latLng = LatLng(location.latitude, location.longitude)
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
                    }
                }
    }
}
