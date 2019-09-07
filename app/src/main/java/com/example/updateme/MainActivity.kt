package com.example.updateme

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private lateinit var installStateUpdatedListener: InstallStateUpdatedListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appUpdateManager = AppUpdateManagerFactory.create(this)

        installStateUpdatedListener = InstallStateUpdatedListener { state ->
            handleInstallState(state)
        }

        appUpdateManager.registerListener(installStateUpdatedListener)

        checkForUpdate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FLEXIBLE_UPDATE) {
            // onActivityResult triggered after calling startUpdateFlowForResult()
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // handle user's approval
                    showToastAndLogMessage("User accepted the update")
                }
                Activity.RESULT_CANCELED -> {
                    // handle user's rejection
                    showToastAndLogMessage("User cancelled the update")
                    checkForUpdate()
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    // handle update failure
                    showToastAndLogMessage("Update failed")
                    checkForUpdate()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    override fun onResume() {
        super.onResume()
        //check if the update downloaded while the app was in the background
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    showToastAndLogMessage("Update was downloaded in the background")
                    appUpdateManager.completeUpdate()
                }
            }
    }

    private fun requestUpdate(appUpdateInfo: AppUpdateInfo?) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.FLEXIBLE,
            this,
            REQUEST_CODE_FLEXIBLE_UPDATE
        )
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener {
            //  check for the type of update flow you want
            if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && it.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                showToastAndLogMessage("Version code available ${it.availableVersionCode()}")
                //request the update
                requestUpdate(it)
            } else {
                showToastAndLogMessage("Update not available.")
            }
        }
    }

    private fun handleInstallState(state: InstallState) {
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
        private const val REQUEST_CODE_FLEXIBLE_UPDATE = 5324
    }
}
