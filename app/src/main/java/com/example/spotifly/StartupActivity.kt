package com.example.spotifly

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartupActivity: AppCompatActivity() {

    // App Lifecycle Functions -----------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {

        var accessToken = Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN", "")
        var expirationTime = Spotifly.SharedPrefsHelper.getSharedPref("EXPIRATION_TIME", 0L)


        var splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition{true}


        super.onCreate(savedInstanceState)

        Handler().postDelayed({
            splashScreen.setKeepOnScreenCondition{false}
        },3500)

        setContentView(R.layout.activity_startup)

        val enterButton = findViewById<Button>(R.id.enter_button)
        enterButton.setOnClickListener {
            checkAccessToken(accessToken, expirationTime)
        }

        val refreshButton = findViewById<Button>(R.id.refresh_button)
        refreshButton.setOnClickListener {
            forceRefresh()
        }

    }

    // Methods -----------------------------------------------------------------------------


    fun checkAccessToken(token: String, expirationTime: Long) {

        if (token == "") {
            // Need to sign in again
            startActivity(Intent(this, AuthorizationActivity::class.java))
            finish()

        } else if (System.currentTimeMillis() >= expirationTime) {
            // Refresh the Access Token then navigate to the Main Activity
            RefreshToken().refreshAccessToken()
            navigateToMainActivity()

        } else {
            // Navigate to the Main Activity if there is an Access Token and it is NOT expired
            navigateToMainActivity()


        }

    }

    fun forceRefresh() {
        RefreshToken().refreshAccessToken()
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












