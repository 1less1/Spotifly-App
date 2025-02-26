package com.example.spotifly

import android.animation.ObjectAnimator
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity



// This class is automatically called on every App Startup
class Spotifly : Application() {

    // Singleton objects only have one instance so this allows me to create unchanging "Global Variables" and one instance of SharedPreferences

    object Global {
        // Base Authentication Information (never changes)
        const val CLIENT_ID = "7be462e85af34b18bef22cd0455002cf"
        const val REDIRECT_URI = "spotifly://callback"
        const val REQUEST_CODE = 4343

    }

    object SharedPrefsHelper {
        private lateinit var sharedPreferences: SharedPreferences
        private lateinit var editor: Editor

        fun init(context: Context) {
            sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            editor = sharedPreferences.edit()
        }

        fun saveSharedPref(key: String, value: Any?) {
            editor.apply {
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    else -> throw IllegalArgumentException("Invalid data type")
                }
            }.commit()
        }

        internal inline fun <reified T : Any> getSharedPref(key: String, defaultValue: T): T {
            return when (T::class) {
                String::class -> sharedPreferences.getString(key, defaultValue as String) as T
                Int::class -> sharedPreferences.getInt(key, defaultValue as Int) as T
                Long::class -> sharedPreferences.getLong(key, defaultValue as Long) as T
                Boolean::class -> sharedPreferences.getBoolean(key, defaultValue as Boolean) as T
                Float::class -> sharedPreferences.getFloat(key, defaultValue as Float) as T
                else -> throw IllegalArgumentException("Invalid data type")
            }
        }

        fun doesKeyExist(key: String): Boolean {
            return sharedPreferences.contains(key)
        }

        fun clearSharedPrefs() {
            editor.clear().apply()
        }
    }


    object HorizontalProgressBar {
        fun animateProgress(activity: AppCompatActivity) {

            Log.d("Progress Bar", "Progress Bar Started")

            // Adjust system UI flags
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    // Add any additional flags you need
                    )

            val inflater = activity.layoutInflater
            val progressBarLayout = inflater.inflate(R.layout.progress_bar, null)
            progressBarLayout.visibility = ProgressBar.VISIBLE

            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            activity.addContentView(progressBarLayout, layoutParams)

            val progressBar = progressBarLayout.findViewById<ProgressBar>(R.id.progressBar)

            val animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 200)
            animation.duration = 3000
            animation.repeatCount = ObjectAnimator.INFINITE
            animation.repeatMode = ObjectAnimator.RESTART
            animation.start()

            // Delay hiding the ProgressBar after 2 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                progressBarLayout.visibility = ProgressBar.INVISIBLE
                Log.d("Progress Bar", "Progress Bar Done")
            }, 3000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        SharedPrefsHelper.init(applicationContext)

        Log.d("Spotifly", "Loading app launch data")


        var isFirstLaunch = SharedPrefsHelper.getSharedPref("is_first_launch", true)

        // Save the following data to sharedPreferences on ONLY the FIRST ever launch of this application
        if (isFirstLaunch) {
            loadFirstLaunch()
        }


    }


    fun loadFirstLaunch() {
        // Base Data
        Spotifly.SharedPrefsHelper.saveSharedPref("CLIENT_ID", Spotifly.Global.CLIENT_ID)
        //Spotifly.SharedPrefsHelper.saveSharedPref("CLIENT_SECRET", Spotifly.Global.CLIENT_SECRET)
        Spotifly.SharedPrefsHelper.saveSharedPref("REDIRECT_URI", Spotifly.Global.REDIRECT_URI)
        Spotifly.SharedPrefsHelper.saveSharedPref("REQUEST_CODE", Spotifly.Global.REQUEST_CODE)
        Spotifly.SharedPrefsHelper.saveSharedPref("is_first_launch", false)
    }

}