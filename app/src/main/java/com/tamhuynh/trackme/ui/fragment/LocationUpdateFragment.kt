package com.tamhuynh.trackme.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.ui.IconGenerator
import com.tamhuynh.trackme.ListWorkoutRecordEntity
import com.tamhuynh.trackme.R
import com.tamhuynh.trackme.databinding.FragmentLocationUpdateBinding
import com.tamhuynh.trackme.hasPermission
import com.tamhuynh.trackme.ui.MillisecondChronometer
import com.tamhuynh.trackme.ui.UtilsHelper
import com.tamhuynh.trackme.viewmodels.LocationUpdateViewModel
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.roundToInt


private const val TAG = "LocationUpdateFragment"

/**
 * Displays location information via PendingIntent after permissions are approved.
 *
 */
class LocationUpdateFragment : Fragment(), OnMapReadyCallback {

    private var receivingLocation: Boolean = false
    private var speed: Float = 0.0f
    private lateinit var listWorkoutRecordEntity: ListWorkoutRecordEntity
    private var workoutID: Int = 0
    private lateinit var mLastUpdateTime: DateFormat
    private var isMapReady: Boolean = false
    private var activityListener: Callbacks? = null

    private lateinit var binding: FragmentLocationUpdateBinding
    private val points: ArrayList<LatLng> = ArrayList()
    var line: Polyline? = null
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var firstLocation: Location? = null
    private lateinit var endLocation: LatLng
    private var isStopReceiveLocationUpdate = true
    private var previousLocation: Location? = null
    private var newLocation: Location? = null
    private var totalDistanceCalculate: Float = 0.0f
    private lateinit var timer: CountDownTimer
    private var totalTimeWorkOut: Long = 0

    private val locationUpdateViewModel by lazy {
        ViewModelProvider(this).get(LocationUpdateViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Callbacks) {
            activityListener = context

            // If fine location permission isn't approved, instructs the parent Activity to replace
            // this fragment with the permission request fragment.
            if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                activityListener?.requestFineLocationPermission()
            }
        } else {
            throw RuntimeException("$context must implement LocationUpdateFragment.Callbacks")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentLocationUpdateBinding.inflate(inflater, container, false)

        binding.enableBackgroundLocationButton.setOnClickListener {
            activityListener?.requestBackgroundLocationPermission()
        }
        activity?.let {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(it)
        }
        locationUpdateViewModel.getListWorkoutRecord(2, 0).observe(
            viewLifecycleOwner, Observer { it ->
                it?.let {
                    it.size
                }
            }
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fm: SupportMapFragment = childFragmentManager
            .findFragmentById(R.id.googleMap) as SupportMapFragment
        fm.getMapAsync(this)

        locationUpdateViewModel.receivingLocationUpdates.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { receivingLocation ->
                updateStartOrStopButtonState(receivingLocation)
            }
        )

        binding.StopLocationUpdatesButton.setOnClickListener {
            stopAndSaveCurrentWorkout()
        }

        locationUpdateViewModel.locationChangedFrequently.observe(
            viewLifecycleOwner,
            Observer { it ->
                it?.let { it ->
                    if (!isStopReceiveLocationUpdate) {
                        val latln = LatLng(it.latitude, it.longitude)
                        points.add(latln)
                        checkGoogleMapReady()
                        Log.d(TAG, "Got MyLocationEntity ${it.date}")
                        if (firstLocation == null) {
                            //Used for add marked as the start Location if needed
                            firstLocation = Location("")
                            firstLocation?.latitude = latln.latitude
                            firstLocation?.longitude = latln.longitude
                        }
                        previousLocation = if (newLocation == null) {
                            //second location retrieved
                            firstLocation
                        } else {
                            // third location and so on ... location retrieved
                            newLocation

                        }
                        newLocation = Location("")
                        newLocation?.latitude = latln.latitude
                        newLocation?.longitude = latln.longitude
                        val distanceInMeters: Float =
                            previousLocation?.distanceTo(newLocation) ?: 0.0f
                        Log.d(TAG, "distanceInMeters: $distanceInMeters")
                        if(distanceInMeters > 1) { // only update if distance changed greater than 1 meter
                            totalDistanceCalculate += distanceInMeters
                            Log.d(TAG, "totalDistanceCalculate: $totalDistanceCalculate")

                            speed = (totalDistanceCalculate / totalTimeWorkOut) * 3.6f // m/s convert to km/hour by multiply 3.6
                            binding.distance.text =
                                totalDistanceCalculate.toInt().toString() + " m"
                            val roundSpeed = roundSpeed(speed)
                            binding.speed.text = "$roundSpeed ${getString(R.string.km_per_hour)}"
                        }
                    }
                }
            }
        )
    }

    private fun roundSpeed(speed: Float): Float {
        return (speed * 100.0).roundToInt() / 100.0.toFloat()
    }

    private fun checkGoogleMapReady() {
        if (isMapReady) {
            reDrawLine()
        }
    }

    private fun reDrawLine() {
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

    private fun addMarker(location: LatLng, isStart: Boolean) {
        val options = MarkerOptions()

        // following four lines requires 'Google Maps Android API Utility Library'
        // https://developers.google.com/maps/documentation/android/utility/
        // I have used this to display the time as title for location markers
        // you can safely comment the following four lines but for this info
        val iconFactory = IconGenerator(LocationUpdateFragment@ this.activity?.applicationContext)
        iconFactory.setStyle(if (isStart) IconGenerator.STYLE_BLUE else IconGenerator.STYLE_RED)
        // options.icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(mLastUpdateTime + requiredArea + city)));
        options.icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(if (isStart) "Start" else "Stop")))
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
        updateBackgroundButtonState()
    }

    override fun onPause() {
        super.onPause()
        activity?.let { UtilsHelper().checkLocationServiceEnable(it) }

        // Stops location updates if background permissions aren't approved. The FusedLocationClient
        // won't trigger any PendingIntents with location updates anyway if you don't have the
        // background permission approved, but it's best practice to unsubscribing anyway.
        if ((locationUpdateViewModel.receivingLocationUpdates.value == true) &&
            (!requireContext().hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        ) {
            locationUpdateViewModel.stopLocationUpdates()
        }
    }

    fun stopLocationUpdates(){
        if(receivingLocation || workoutID != 0) {
            locationUpdateViewModel.stopLocationUpdates()
        } else{
            // do nothing
        }
    }

    fun stopAndSaveCurrentWorkout() {
        if(receivingLocation || workoutID != 0) {
            firstLocation = null
            if (points.size > 0) {
                var lastLocation = points[points.size - 1]
                var lastLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
//                        addMarker(lastLatLng, false)
                points.clear()
            }
            //stop MillisecondChronometer
            binding.timer.stop()
            //update workout Record into database
            listWorkoutRecordEntity.workoutID = workoutID
            listWorkoutRecordEntity.distance = totalDistanceCalculate.toInt()
            val roundSpeed = roundSpeed(speed)
            listWorkoutRecordEntity.v = roundSpeed
            listWorkoutRecordEntity.totalTime = binding?.timer?.text.toString()
            locationUpdateViewModel.updateWorkoutRecord(listWorkoutRecordEntity)

            totalDistanceCalculate = 0.0f
            totalTimeWorkOut = 0
            workoutID = 0
            speed = 0.0f
            showHideStopButton(false)
            binding.startOrStopLocationUpdatesButton.visibility = View.GONE
        } else {
            //do nothing
        }
    }

    override fun onDetach() {
        super.onDetach()

        activityListener = null
    }

    private fun showBackgroundButton(): Boolean {
        return !requireContext().hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun updateBackgroundButtonState() {
        if (showBackgroundButton()) {
            binding.enableBackgroundLocationButton.visibility = View.VISIBLE
        } else {
            binding.enableBackgroundLocationButton.visibility = View.GONE
        }
    }

    private fun updateStartOrStopButtonState(receivingLocation: Boolean) {
        this.receivingLocation = receivingLocation
        if (receivingLocation) {
            binding.startOrStopLocationUpdatesButton.apply {
                text = getString(R.string.pause_receiving_location)
                setOnClickListener {
                    it?.let {
                        locationUpdateViewModel.stopLocationUpdates()
                        isStopReceiveLocationUpdate = true

                        binding.timer.stop()
                        if (timer != null) {
                            timer.cancel()
                        } else {
                            //do nothing
                        }
                    }
                }
            }
        } else {
            binding.startOrStopLocationUpdatesButton.apply {
                val textString = binding.startOrStopLocationUpdatesButton.text
                if (textString == getString(R.string.pause_receiving_location)) {
                    showHideStopButton(true) //show stop button when PAUSE
                    text = getString(R.string.resume_receiving_location)
                    setOnClickListener {
                        text =
                            getString(R.string.pause_receiving_location) //show stop button when RESUME
                        showHideStopButton(false)
                        binding.timer.start()
                        timer.start()
                        isStopReceiveLocationUpdate = false
                        locationUpdateViewModel.startLocationUpdates(workoutID)
                    }
                } else {
                    showHideStopButton(false) //show stop button when START
                    text = getString(R.string.start_receiving_location)

                    setOnClickListener {
                        binding.timer.base = SystemClock.elapsedRealtime()
                        binding.timer.start()
                        timer = object : CountDownTimer(18000000, 1000) {
                            // set max 5 hours
                            override fun onTick(millisUntilFinished: Long) {
                                totalTimeWorkOut += 1 // calculate the total workout time
                            }

                            override fun onFinish() {
                                Log.d(TAG, "timer onFinish() called")
                            }
                        }
                        timer.start()
                        isStopReceiveLocationUpdate = false

                        locationUpdateViewModel.getTotalWorkoutRecord.observe(
                            viewLifecycleOwner, Observer {
                                it?.let {
                                    // increase the workout ID to update distance/ speed/ timer later
                                    workoutID = it.toInt().plus(1)

                                    listWorkoutRecordEntity = ListWorkoutRecordEntity()
                                    listWorkoutRecordEntity.date = Date()
                                    //add new workout Record which associate with Locations changed for this session
                                    locationUpdateViewModel.addNewWorkoutRecord(
                                        listWorkoutRecordEntity
                                    )
                                    //start receiving the Location changes
                                    locationUpdateViewModel.startLocationUpdates(workoutID)
                                }
                                //remove observe get current total workout Record
                                locationUpdateViewModel.getTotalWorkoutRecord.removeObservers(
                                    viewLifecycleOwner
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showHideStopButton(b: Boolean) {
        if (b) {
            binding.StopLocationUpdatesButton.visibility = View.VISIBLE
        } else {
            binding.StopLocationUpdatesButton.visibility = View.GONE
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    interface Callbacks {
        fun requestFineLocationPermission()
        fun requestBackgroundLocationPermission()
    }

    companion object {
        fun newInstance() = LocationUpdateFragment()
    }

    override fun onStop() {
        super.onStop()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap?) {
        isMapReady = true
        p0?.isMyLocationEnabled = true
        p0?.setOnMyLocationButtonClickListener(object : GoogleMap.OnMyLocationButtonClickListener {
            override fun onMyLocationButtonClick(): Boolean {
                Toast.makeText(
                    this@LocationUpdateFragment.context,
                    "Location button clicked",
                    Toast.LENGTH_LONG
                ).show()
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
            .addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                location?.let {
                    val zoomLevel = 19.0f //This goes up to 21
                    val latLng = LatLng(location.latitude, location.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
                }
            }
    }
}
