
package com.tamhuynh.trackme.ui.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.tamhuynh.trackme.BuildConfig
import com.tamhuynh.trackme.R
import com.tamhuynh.trackme.databinding.FragmentPermissionRequestBinding
import com.tamhuynh.trackme.hasPermission
import com.tamhuynh.trackme.requestPermissionWithRationale

private const val TAG = "PermissionRequestFrag"

/**
 * Displays information about why a user should enable either the fine location permission or the
 * background location permission (depending on what is needed).
 *
 * Allows users to grant the permissions as well.
 */
class PermissionRequestFragment : Fragment() {

    // Type of permission to request (fine or background). Set by calling Activity.
    private var permissionRequestType: PermissionRequestType? = null

    private lateinit var binding: FragmentPermissionRequestBinding

    private var activityListener: Callbacks? = null

    // If the user denied a previous permission request, but didn't check "Don't ask again", these
    // Dialog provided an explanation for why user should approve, i.e., the additional
    // rationale.
    private val fineLocationRationalDialog by lazy {
        activity?.let{
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Permission Required")
            builder.setMessage(R.string.fine_location_permission_rationale)
            builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE
                )
            }
            builder.create()
        }

    }

    private val backgroundRationalDialog by lazy {
        activity?.let{
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Permission Required")
            builder.setMessage(R.string.background_location_permission_rationale)
            //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))
            builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE
                )
            }
            builder.create()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Callbacks) {
            activityListener = context
        } else {
            throw RuntimeException("$context must implement PermissionRequestFragment.Callbacks")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionRequestType =
            arguments?.getSerializable(ARG_PERMISSION_REQUEST_TYPE) as PermissionRequestType
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentPermissionRequestBinding.inflate(inflater, container, false)

        when (permissionRequestType) {
            PermissionRequestType.FINE_LOCATION -> {

                binding.apply {
                    iconImageView.setImageResource(R.drawable.ic_location_on_24px)

                    titleTextView.text =
                        getString(R.string.fine_location_access_rationale_title_text)

                    detailsTextView.text =
                        getString(R.string.fine_location_access_rationale_details_text)

                    permissionRequestButton.text =
                        getString(R.string.enable_fine_location_button_text)
                }
            }

            PermissionRequestType.BACKGROUND_LOCATION -> {

                binding.apply {
                    iconImageView.setImageResource(R.drawable.ic_my_location_24px)

                    titleTextView.text =
                        getString(R.string.background_location_access_rationale_title_text)

                    detailsTextView.text =
                        getString(R.string.background_location_access_rationale_details_text)

                    permissionRequestButton.text =
                        getString(R.string.enable_background_location_button_text)
                }
            }
        }

        binding.permissionRequestButton.setOnClickListener {
            when (permissionRequestType) {
                PermissionRequestType.FINE_LOCATION ->
                    requestFineLocationPermission()

                PermissionRequestType.BACKGROUND_LOCATION ->
                    requestBackgroundLocationPermission()
            }
        }

        return binding.root
    }

    override fun onDetach() {
        super.onDetach()

        activityListener = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE,
            REQUEST_BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive an empty array.
                    Log.d(TAG, "User interaction was cancelled.")

                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    val permissionApproved =
                            context?.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ?: return
                    if(permissionApproved) {
                        activityListener?.displayHomeUI()
                    }
                    else {
                        requestBackgroundLocationPermission()
                    }
                }

                else -> {

                    val permissionDeniedExplanation =
                        if (requestCode == REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE) {
                            R.string.fine_permission_denied_explanation
                        } else {
                            R.string.background_permission_denied_explanation
                        }
                    activity?.let{
                        val builder = AlertDialog.Builder(it)
                        builder.setTitle("Permission Required")
                        builder.setMessage(permissionDeniedExplanation)
                        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))
                        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        builder.show()
                    }
                }
            }
        }
    }

    private fun requestFineLocationPermission() {
        val permissionApproved =
            context?.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ?: return

        if (permissionApproved) {
            activityListener?.displayHomeUI()
        } else {
            requestPermissionWithRationale(
                Manifest.permission.ACCESS_FINE_LOCATION,
                    REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE,
                fineLocationRationalDialog)
        }
    }

    private fun requestBackgroundLocationPermission() {
        val permissionApproved =
            context?.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ?: return

        if (permissionApproved) {
            activityListener?.displayHomeUI()
        } else {
            requestPermissionWithRationale(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    REQUEST_BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE,
                backgroundRationalDialog)
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    interface Callbacks {
        fun displayHomeUI()
    }

    companion object {
        private const val ARG_PERMISSION_REQUEST_TYPE =
            "com.tamhuynh.trackme.PERMISSION_REQUEST_TYPE"

        private const val REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE = 56

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param permissionRequestType Type of permission you would like to request.
         * @return A new instance of fragment PermissionRequestFragment.
         */
        @JvmStatic
        fun newInstance(permissionRequestType: PermissionRequestType) =
            PermissionRequestFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PERMISSION_REQUEST_TYPE, permissionRequestType)
                }
            }
    }
}

enum class PermissionRequestType {
    FINE_LOCATION, BACKGROUND_LOCATION
}
