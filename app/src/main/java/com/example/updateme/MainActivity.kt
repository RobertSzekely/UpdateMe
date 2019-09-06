package com.example.updateme

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : AppCompatActivity() {

    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForUpdate()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FLEXIBLE_UPDATE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                }//  handle user's approval
                Activity.RESULT_CANCELED -> {
                }//  handle user's rejection
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                } //  handle update failure
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

    private fun showToastAndLogMessage(message: String){
        showToast(message)
        Log.d(MainActivity::class.java.simpleName, message)
    }

    companion object {
        private const val REQUEST_CODE_FLEXIBLE_UPDATE = 5324
    }
}
