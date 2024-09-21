package com.example.spotifly

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlin.properties.Delegates

class StartupActivity: AppCompatActivity() {

    lateinit var accessToken: String
    var expirationTime by Delegates.notNull<Long>()
    lateinit var refreshToken: String

    // App Lifecycle Functions -----------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        accessToken = Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN", "")
        expirationTime = Spotifly.SharedPrefsHelper.getSharedPref("EXPIRATION_TIME", 0L)
        refreshToken = Spotifly.SharedPrefsHelper.getSharedPref("REFRESH_TOKEN", "")

        /*
        val myENV = BuildConfig.MY_ENV
        val clientID = BuildConfig.CLIENT_ID
        val clientSecret = BuildConfig.CLIENT_SECRET
        val requestCode = BuildConfig. REQUEST_CODE
        val redirectURI = BuildConfig.REDIRECT_URI

        Log.d("Startup Activity", "Client ID: $clientID")
        Log.d("Startup Activity", "Client Secret: $clientSecret")
        Log.d("Startup Activity", "Redirect URI: $redirectURI")
        Log.d("Startup Activity", "Request Code: $requestCode")

         */


        var splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition{true}


        super.onCreate(savedInstanceState)


        Handler().postDelayed({
            splashScreen.setKeepOnScreenCondition{false}
        },3500)

        setUI()

    }

    // Methods -----------------------------------------------------------------------------

    fun setUI() {
        setContentView(R.layout.activity_startup)

        val enterButton = findViewById<Button>(R.id.enter_button)
        enterButton.setOnClickListener {
            checkAccessToken()
        }

    }

    fun checkAccessToken() {

        if (accessToken == "") {
            // Need to sign in again
            startActivity(Intent(this, AuthorizationActivity::class.java))
            finish()

        } else if (System.currentTimeMillis() >= expirationTime) {
            // Refresh the Access Token then navigate to the Main Activity
            RefreshToken(accessToken, refreshToken, expirationTime).refreshAccessToken()
            navigateToMainActivity()

        } else {
            // Navigate to the Main Activity if there is an Access Token and it is NOT expired
            navigateToMainActivity()

        }

    }

    // This is only here for debugging!!!!
    fun forceRefresh() {
        RefreshToken(accessToken, refreshToken, expirationTime).refreshAccessToken()
        navigateToMainActivity()

    }

    fun navigateToMainActivity() {
        Spotifly.HorizontalProgressBar.animateProgress(this)

        val intent = Intent(this, MainActivity::class.java)
        Handler().postDelayed({
            startActivity(intent)
            finish()
        }, 1800)

    }

}












