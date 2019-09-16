package com.robertszekely.updateme

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : AppCompatActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var flexibleInstallStateUpdatedListener: InstallStateUpdatedListener
    private var lastSelectedUpdateType = AppUpdateType.FLEXIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            lastSelectedUpdateType = savedInstanceState.getInt(LAST_SELECTED_UPDATE_TYPE)
        }
        setContentView(R.layout.activity_main)
        appUpdateManager = AppUpdateManagerFactory.create(this)

        flexibleInstallStateUpdatedListener = InstallStateUpdatedListener { state ->
            handleFlexibleInstallState(state)
        }

        findViewById<Button>(R.id.flexible_update_button).setOnClickListener {
            appUpdateManager.registerListener(flexibleInstallStateUpdatedListener)
            checkForUpdate(AppUpdateType.FLEXIBLE)
        }

        findViewById<Button>(R.id.immediate_update_button).setOnClickListener {
            //listener is not required for immediate update, the restart of the app is handled automatically
            appUpdateManager.unregisterListener(flexibleInstallStateUpdatedListener)
            checkForUpdate(AppUpdateType.IMMEDIATE)
            lastSelectedUpdateType = AppUpdateType.IMMEDIATE
        }

        findViewById<TextView>(R.id.version_name).text = getString(R.string.version_name, BuildConfig.VERSION_NAME)
        findViewById<TextView>(R.id.version_code).text = getString(R.string.version_code, BuildConfig.VERSION_CODE)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UPDATE) {
            // onActivityResult triggered after calling startUpdateFlowForResult()
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // handle user's approval
                    showToastAndLogMessage("User accepted the update")
                }
                Activity.RESULT_CANCELED -> {
                    // handle user's rejection
                    showToastAndLogMessage("User cancelled the update")
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    // handle update failure
                    showToastAndLogMessage("Update failed")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(flexibleInstallStateUpdatedListener)
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                //check if the update downloaded while the app was in the background
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    showToastAndLogMessage("Update was downloaded in the background")
                    appUpdateManager.completeUpdate()
                }
                //check if the user left the app and returned before the immediate update got the finish in the background
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        REQUEST_CODE_UPDATE
                    )
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(LAST_SELECTED_UPDATE_TYPE, lastSelectedUpdateType)
        super.onSaveInstanceState(outState)
    }

    private fun requestUpdate(appUpdateInfo: AppUpdateInfo?, @AppUpdateType updateType: Int) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            updateType,
            this,
            REQUEST_CODE_UPDATE
        )
    }

    private fun checkForUpdate(@AppUpdateType updateType: Int) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener {
            //  check for the type of update flow you want
            if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && it.isUpdateTypeAllowed(updateType)) {
                showToastAndLogMessage("Version code available ${it.availableVersionCode()}")
                //request the update
                requestUpdate(it, updateType)
            } else {
                showToastAndLogMessage("Update not available.")
            }
        }
    }

    private fun handleFlexibleInstallState(state: InstallState) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // After the update is downloaded, show a notification
            // and request user confirmation to restart the app.
            Log.d(MainActivity::class.java.simpleName, "Updated downloaded.")
            popupSnackbarForCompleteUpdate()
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            findViewById(R.id.activity_main_layout), "An update has just been downloaded.", Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            setActionTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
            show()
        }
    }

    private fun showToastAndLogMessage(message: String) {
        showToast(message)
        Log.d(MainActivity::class.java.simpleName, message)
    }

    companion object {
        private const val REQUEST_CODE_UPDATE = 5324
        private const val LAST_SELECTED_UPDATE_TYPE = "lastSelectedUpdateType"
    }
}
