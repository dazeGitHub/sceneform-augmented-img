package com.google.ar.sceneform.samples.augmentedimage.app.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

/**
 * <pre>
 *     author : ZYZ
 *     e-mail : zyz163mail@163.com
 *     time   : 2020/12/22
 *     desc   :
 *     version: 1.0
 * </pre>
 */
object ArCheckUtils {

    private var TAG = ArCheckUtils::class.java.name
    private var mUserRequestedInstall = true

    private val MIN_OPENGL_VERSION = 3.0

    fun checkDeviceSupportAr(activity: Activity): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(activity)
        if (availability.isTransient) {
            // Re-query at 5Hz while compatibility is checked in the background.
            Handler().postDelayed(Runnable { checkDeviceSupportAr(activity) }, 200)
        }
        if (availability.isSupported) {
            ToastUtil.showShortToast("设备支持 Ar")
            val isDeviceSupport = checkIsSupportedDeviceOrFinish(activity)
            if (isDeviceSupport) {
                return checkArCoreInstalled(activity)
            }
            // indicator on the button.
        } else { // Unsupported or unknown.
            ToastUtil.showShortToast("设备不支持 Ar")
        }
        return false
    }

    private fun checkArCoreInstalled(activity: Activity): Boolean {
        // Make sure ARCore is installed and up to date.
        try {
            return when (ArCoreApk.getInstance().requestInstall(activity, mUserRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALLED -> {
                    // Success, create the AR session.
                    ToastUtil.showShortToast("ArCore 已经安装")
//                    Session(activity)
                    true
                }
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    // Ensures next invocation of requestInstall() will either return
                    // INSTALLED or throw an exception.
                    ToastUtil.showShortToast("ArCore 未安装")
                    mUserRequestedInstall = false
                    false
                }
                else -> {
                    false
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            // Display an appropriate message to the user and return gracefully.
            ToastUtil.showShortToast("ArCore 安装失败" + e.message)
            return false
        }
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     *
     * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     *
     * Finishes the activity if Sceneform can not run
     */
    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "app requires Android N or later")
            Toast.makeText(activity, "app requires Android N or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        val openGlVersionString =
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Log.e(TAG, "app requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "app requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }
}