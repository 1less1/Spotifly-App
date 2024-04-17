package com.example.spotifly

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class StartupActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.sleep(3000) // 3 seconds
        installSplashScreen()

        setContentView(R.layout.activity_startup)

        var expirationTime = Spotifly.SharedPrefsHelper.getSharedPref("EXPIRATION_TIME",0L)
        var accessToken = Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN", "")

        val enterButton = findViewById<Button>(R.id.enter_button)
        enterButton.setOnClickListener {
          checkAccessToken(accessToken, expirationTime)
        }

        val refreshButton = findViewById<Button>(R.id.refresh_button)
        refreshButton.setOnClickListener {
            forceRefresh()
        }

    }

    fun checkAccessToken(token: String, expirationTime: Long) {

        if (token == "") {
            // Navigate to Main Activity
            startActivity(Intent(this, AuthorizationActivity::class.java))
            finish()

        } else if (System.currentTimeMillis() >= expirationTime) {
            // Call RefreshToken Class and then navigate to Main Activity if Successful
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
        Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } ,750)

    }










}


