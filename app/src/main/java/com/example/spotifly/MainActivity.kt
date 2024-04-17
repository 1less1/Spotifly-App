package com.example.spotifly
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.spotify.sdk.android.auth.AuthorizationClient

// Make network request and update the UI
class MainActivity : AppCompatActivity() {

    // MainActivity is declared to inherit from AppCompatActivity using the colon (:)
    // This means that MainActivity extends the functionality of AppCompatActivity

    // onCreate() function is called upon everytime the activity is started. This is a 'Lifecycle Function'
    // Bundle? checks for null state
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Sets the layout (UI) for this activity (Screen) to activity_main. Layout file usually in res/layout directory
        setContentView(R.layout.activity_main)

        val signOutButton = findViewById<Button>(R.id.sign_out_button)
        signOutButton.setOnClickListener{
            signOutSpotify()
        }

        val apiCallButton = findViewById<Button>(R.id.api_call_button)
        apiCallButton.setOnClickListener {
            makePlaylist()
        }

    }

    override fun onResume() {
        super.onResume()
        var accessToken = Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN","")
        var refreshToken = Spotifly.SharedPrefsHelper.getSharedPref("REFRESH_TOKEN","")
        var expires_in = Spotifly.SharedPrefsHelper.getSharedPref("EXPIRES_IN",0)

        // Logcat Output - Debugging
        println("---------------------------------")
        println("Main Activity Values: ")
        println("Access Token: $accessToken")
        println("Expires In: $expires_in seconds")
        println("Refresh Token: $refreshToken")

        /* In App Debugging
        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Refresh Token: " + accessToken, Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Access Token: " + refreshToken, Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Expires in $expires_in seconds", Toast.LENGTH_SHORT).show()

         */
    }


    fun signOutSpotify() {
        // The code below cancels any cached Authorization Flow and resets the Access Token to a null value
        AuthorizationClient.stopLoginActivity(this, Spotifly.Global.REQUEST_CODE)

        Spotifly.SharedPrefsHelper.saveSharedPref("ACCESS_TOKEN", "")
        println("Sign Out Success -> Access Token = "+Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN",""))
        val intent = Intent(this, StartupActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun makePlaylist() {
        // TODO: make instance of an api call class and return if it was successful or not
        var mainInstance=callAPI()

        Handler().postDelayed({
            mainInstance.getUserInfo()
            mainInstance.getUserTopItems()
        }, 750)

        var playlistName = "Test Playlist"

        /*
        if (Spotifly.SharedPrefsHelper.doesKeyExist(playlistName.toUpperCase())) {
            // TODO: Prompt user to either choose a new name or to overwrite previous data
        }

         */
        mainInstance.createPlaylist(playlistName)

        Handler().postDelayed({
            mainInstance.addSongToPlaylist(playlistName)
        }, 750)
        // Delay definitely needed since sharedPreferences needs time to read data
        // TODO: I should probably feed arguments into each function as local variables and save things to sharedPrefs as I go
        // Ex: getUserInfo() calls createPlaylist(playListName, userID) which then calls addSongToPlaylist(playlistID)


    }











}