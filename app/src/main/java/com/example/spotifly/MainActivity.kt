package com.example.spotifly
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.webkit.CookieManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.spotify.sdk.android.auth.AuthorizationClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Make network request and update the UI
class MainActivity : AppCompatActivity() {
    // MainActivity is declared to inherit from AppCompatActivity using the colon (:)
    // This means that MainActivity extends the functionality of AppCompatActivity

    lateinit var accessToken: String
    lateinit var user_id: String
    lateinit var apiCaller: CreatePlaylistAPI

    // App Lifecycle Functions -----------------------------------------------------------------------------
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accessToken = Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN", "")
        user_id = Spotifly.SharedPrefsHelper.getSharedPref("user_id", "")

        // Sets the layout (UI) for this activity
        Handler().postDelayed({
            apiCaller = CreatePlaylistAPI(accessToken)
            setUI()
        },450)
        // Inflates the Progress Bar Loading Screen
        //Spotifly.HorizontalProgressBar.animateProgress(this)

    }

    override fun onStart() {
        super.onStart()

        // Every Time the activity is started I want to fetch user data from shared preferences so it is up to date and ready
        Log.d("Main Activity", "Access Token: $accessToken")
        Log.d("Main Activity", "User ID: $user_id")

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies(null)

    }

    // Methods -----------------------------------------------------------------------------

    fun setUI() {
        // Sets the layout (UI) for this activity (Screen) to Layout file usually in res/layout directory
        setContentView(R.layout.activity_main)


        val signOutButton = findViewById<Button>(R.id.sign_out_button)
        signOutButton.setOnClickListener{
            signOutSpotify()
        }

        val apiCallButton = findViewById<Button>(R.id.api_call_button)
        apiCallButton.setOnClickListener {
            makePlaylist()
        }

        var welcomeMessage = findViewById<TextView>(R.id.main_message)
        welcomeMessage.text = ("Hello $user_id!")


    }


    fun signOutSpotify() {
        // The code below cancels any cached Authorization Flow and resets the Access Token to a null value
        AuthorizationClient.stopLoginActivity(this, Spotifly.Global.REQUEST_CODE)
        Spotifly.SharedPrefsHelper.saveSharedPref("ACCESS_TOKEN", "")
        Spotifly.SharedPrefsHelper.saveSharedPref("user_id", "")

        println("Sign Out Success -> Access Token = "+Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN",""))


        val intent = Intent(this, StartupActivity::class.java)
        startActivity(intent)
        finish()
    }


    fun makePlaylist() {
        apiCaller.getUserTopItems()


        // TODO: make instance of an api call class and return if it was successful or not

        /*
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

         */


    }
}










