
package com.tamhuynh.trackme.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.tamhuynh.trackme.R
import com.tamhuynh.trackme.WorkoutRecordData
import com.tamhuynh.trackme.databinding.ActivityMainBinding
import com.tamhuynh.trackme.hasPermission
import com.tamhuynh.trackme.ui.fragment.*
import com.tamhuynh.trackme.ui.fragment.WorkoutRecordDetails.Companion.WORKOUT_RECORD_DATA
import kotlinx.android.synthetic.main.activity_main.*


/**
 * This app allows a user to receive location updates in the background.
 *
 * Users have four options in Android 11+ regarding location:
 *
 *  * One time only
 *  * Allow while app is in use, i.e., while app is in foreground
 *  * Allow all the time
 *  * Not allow location at all
 *
 * IMPORTANT NOTE: You should generally prefer 'while-in-use' for location updates, i.e., receiving
 * location updates while the app is in use and create a foreground service (tied to a Notification)
 * when the user navigates away from the app. To learn how to do that instead, review the
 * @see <a href="https://codelabs.developers.google.com/codelabs/while-in-use-location/index.html?index=..%2F..index#0">
 * Receive location updates in Android 10 with Kotlin</a> codelab.
 *
 * If you do have an approved use case for receiving location updates in the background, it will
 * require an additional permission (android.permission.ACCESS_BACKGROUND_LOCATION).
 *
 *
 * Best practices require you to spread out your first fine/course request and your background
 * request.
 */
class MainActivity : AppCompatActivity(), PermissionRequestFragment.Callbacks,
    LocationUpdateFragment.Callbacks, ListWorkoutFragment.ItemClickCallbacks, FragmentManager.OnBackStackChangedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        // toolbar
        val toolbar: Toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // add back arrow to toolbar
        if (supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }
        supportFragmentManager.addOnBackStackChangedListener(this)

        if(hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)){
            // do nothing
        } else {
            requestFineLocationPermission()
        }

        btnGoToListRecoredWorkout.setOnClickListener {
            if(hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if(UtilsHelper().checkLocationServiceEnable(this@MainActivity)) {
                    openWorkoutRecordedList()
                } else {
                    //wait until the user enable Location Service
                }
            } else{
                requestFineLocationPermission()
            }
        }

        btnGoToRecordScreen.setOnClickListener {
            if(hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if(UtilsHelper().checkLocationServiceEnable(this@MainActivity)) {
                    openWorkoutRecordScreen()
                } else {
                    //wait until the user enable Location Service
                }
            } else {
                requestFineLocationPermission()
            }
        }
    }

    // Triggered from the permission Fragment that it's the app has permissions to display the
    // location fragment.
    override fun displayHomeUI() {
        onBackPressed()
    }

    private fun openWorkoutRecordScreen() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {

            val fragment = LocationUpdateFragment.newInstance()

            addFragment(fragment)
        }
    }

    private fun openWorkoutRecordedList(){
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {

            val fragment = ListWorkoutFragment.newInstance()

            addFragment(fragment)
        }
    }

    // Triggers a splash screen (fragment) to help users decide if they want to approve the missing
    // fine location permission.
    override fun requestFineLocationPermission() {
        val fragment = PermissionRequestFragment.newInstance(PermissionRequestType.FINE_LOCATION)

        addFragment(fragment)
    }

    // Triggers a splash screen (fragment) to help users decide if they want to approve the missing
    // background location permission.
    override fun requestBackgroundLocationPermission() {
        val fragment = PermissionRequestFragment.newInstance(PermissionRequestType.BACKGROUND_LOCATION)

        addFragment(fragment)
    }

    private fun addFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, fragment,fragment::class.java.simpleName)
            .addToBackStack(fragment::class.java.simpleName)
            .commitAllowingStateLoss()
        //after transaction you must call the executePendingTransaction
        supportFragmentManager.executePendingTransactions()
    }

    @SuppressLint("ResourceType")
    override fun onBackPressed() {
        val fm : FragmentManager = supportFragmentManager
        val count = fm.backStackEntryCount
        if (count == 0) {
            super.onBackPressed()
        } else {
            val indexLastFragment = fm.backStackEntryCount?.minus(1)
            val fragmentTag : String = fm?.getBackStackEntryAt(indexLastFragment)?.name ?:""
            val fragment = fm.findFragmentByTag(fragmentTag)
            if(fragment is LocationUpdateFragment){
                fragment.stopLocationUpdates()
                fragment.stopAndSaveCurrentWorkout()
            }
            supportFragmentManager.popBackStack()
        }
    }

    override fun itemWorkoutSelected(workoutRecordData: WorkoutRecordData) {
        val fragment = WorkoutRecordDetails.newInstance()

        var bundle : Bundle = Bundle()
        bundle.putSerializable(WORKOUT_RECORD_DATA, workoutRecordData)
        fragment.arguments = bundle

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(WorkoutRecordDetails::class.java.simpleName)
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle arrow click here
        if (item.itemId === android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackStackChanged() {

        val indexLastFragment = supportFragmentManager.backStackEntryCount?.minus(1)
        if (indexLastFragment<0){
            toolbar.title = "Track Me"
            return
        }
        val fragmentTag : String = supportFragmentManager?.getBackStackEntryAt(indexLastFragment)?.name ?:""
        when {
            WorkoutRecordDetails::class.java.simpleName == fragmentTag -> {
                toolbar.title = "Workout Record Details"
            }
            LocationUpdateFragment::class.java.simpleName == fragmentTag -> {
                toolbar.title = "Workout Record"
            }
            PermissionRequestFragment::class.java.simpleName == fragmentTag -> {
                toolbar.title = "Permission Request"
            }
            ListWorkoutFragment::class.java.simpleName == fragmentTag -> {
                toolbar.title = "List Workout"
            }

        }

        // add more logic here if needed
    }
}
