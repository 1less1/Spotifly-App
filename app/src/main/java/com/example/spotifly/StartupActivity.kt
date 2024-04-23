package com.example.spotifly

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class StartupActivity: AppCompatActivity() {

    // App Lifecycle Functions -----------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {

        var accessToken = Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN", "")
        var expirationTime = Spotifly.SharedPrefsHelper.getSharedPref("EXPIRATION_TIME", 0L)
        var refreshToken = Spotifly.SharedPrefsHelper.getSharedPref("REFRESH_TOKEN", "")


        var splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition{true}


        super.onCreate(savedInstanceState)

        Handler().postDelayed({
            splashScreen.setKeepOnScreenCondition{false}
        },3500)

        setContentView(R.layout.activity_startup)

        val enterButton = findViewById<Button>(R.id.enter_button)
        enterButton.setOnClickListener {
            checkAccessToken(accessToken, refreshToken, expirationTime)
        }

        val refreshButton = findViewById<Button>(R.id.refresh_button)
        refreshButton.setOnClickListener {
            forceRefresh(accessToken, refreshToken, expirationTime)
        }

    }

    // Methods -----------------------------------------------------------------------------


    fun checkAccessToken(at: String, rt: String, et: Long) {
        // at = accessToken
        // rt = refreshToken
        // et = expirationTime

        if (at == "") {
            // Need to sign in again
            startActivity(Intent(this, AuthorizationActivity::class.java))
            finish()

        } else if (System.currentTimeMillis() >= et) {
            // Refresh the Access Token then navigate to the Main Activity
            RefreshToken(at, rt, et).refreshAccessToken()
            navigateToMainActivity()

        } else {
            // Navigate to the Main Activity if there is an Access Token and it is NOT expired
            navigateToMainActivity()

        }

    }

    fun forceRefresh(at: String, rt: String, et: Long) {
        RefreshToken(at, rt, et).refreshAccessToken()
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












