package com.example.spotifly
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.webkit.CookieManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.spotify.sdk.android.auth.AuthorizationClient

// Holds the Main UI for the APP and is where most API calls will happen
class MainActivity : AppCompatActivity() {

    lateinit var accessToken: String
    lateinit var user_id: String
    lateinit var playlistHub: PlaylistHub
    lateinit var context: Context
    lateinit var display_name: String

    var playlistType=""
    var playlistName=""

    // App Lifecycle Functions -----------------------------------------------------------------------------
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accessToken = Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN", "")
        user_id = Spotifly.SharedPrefsHelper.getSharedPref("user_id", "")
        display_name = Spotifly.SharedPrefsHelper.getSharedPref("display_name", "")
        context = applicationContext

        // Sets the layout (UI) for this activity
        Handler().postDelayed({
            playlistHub = PlaylistHub(context,accessToken,user_id)
            setUI()
        },585)



    }

    override fun onStart() {
        super.onStart()

        // Every Time the activity is started I want to fetch user data from shared preferences so it is up to date and ready
        Log.d("Main Activity", "Access Token: $accessToken")
        Log.d("Main Activity", "User ID: $user_id")
        Log.d("Main Activity", "Display Name: $display_name")

    }

    override fun onResume() {
        super.onResume()
        Log.d("Main Activity", "Activity Resumed")

    }

    override fun onDestroy() {
        super.onDestroy()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies(null)


    }

    // Methods -----------------------------------------------------------------------------

    fun setUI() {
        // Sets the layout (UI) for this activity (Screen) to Layout file usually in res/layout directory
        setContentView(R.layout.activity_main_drawer_layout)

        var welcomeMessage = findViewById<TextView>(R.id.main_message)
        if (welcomeMessage!=null) {
            welcomeMessage.text = ("Hello $display_name!")
        }

        // Set Dropdown Menu -----------------------------------------------------------------------------
        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)
        // Playlist Options that correspond with Playlist Types in PlaylistHub
        // TODO: Put playlistOptions into a map (dictionary) under this format key = Playlist Type/Name and value = Fun two sentence maximum description of how the playlist is designed
        val playlistOptions = arrayOf("My Top Songs", "Electric Dance Anthems", "Pumped Up Pop", "Riding the Waves")
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, playlistOptions)
        autoCompleteTextView.setAdapter(adapter)

        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            playlistType = parent.getItemAtPosition(position).toString()
            //Toast.makeText(this, "Selected: $selectedPlaylistType", Toast.LENGTH_SHORT).show()
        }

        // Set Edit Text -----------------------------------------------------------------------------
        val editText = findViewById<EditText>(R.id.customEditText)

        editText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                playlistName = s.toString()
                //Toast.makeText(applicationContext, "Playlist Name: $playlistName", Toast.LENGTH_SHORT).show()
            }

        })

        // Drawer Menu -----------------------------------------------------------------------------
        val drawerLayout = findViewById<DrawerLayout>(R.id.activity_main_drawer_layout)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        val drawerNavigationView = findViewById<NavigationView>(R.id.drawer_navigation_view)

        val drawerHeaderView = drawerNavigationView.getHeaderView(0)

        val drawerUsername = drawerHeaderView.findViewById<TextView>(R.id.drawer_username)
        drawerUsername.text = "$display_name" // TODO: Do the same for profile pic

        val drawerMenuButton = findViewById<ImageButton>(R.id.drawer_button)
        drawerMenuButton.setOnClickListener {
            drawerLayout.open()

        }

        drawerNavigationView.setNavigationItemSelectedListener {menuItem ->
            when (menuItem.itemId) {
                R.id.drawer_settings -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.drawer_sign_out -> {
                    signOutSpotify()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> {
                    false
                }
            }

        }


        // Buttons -----------------------------------------------------------------------------
        val createPlaylistButton = findViewById<Button>(R.id.create_playlist_button)
        createPlaylistButton.setOnClickListener {
            // Check if selectedPlaylistType and playlistName are not empty or null

            if (playlistType.isNullOrEmpty() or (playlistType == "Select Playlist Option")) {
                Toast.makeText(this, "Please select a playlist type to continue!", Toast.LENGTH_SHORT).show()
            } else if (playlistName.isNullOrEmpty()) {
                Toast.makeText(this, "Please input a playlist name to continue!", Toast.LENGTH_SHORT).show()
            } else {
                createPlaylistButton.isEnabled = false
                makePlaylist(playlistType, playlistName)

                // Prevent Button Spamming - Quick and Easy -> Maybe implement returning a boolean when makePlaylist is fully done processing
                Handler().postDelayed( {
                    createPlaylistButton.isEnabled = true
                }, 2000)


            }

        }

        // Below code clears focus from UI elements when clicked off
        val rootLayout = findViewById<View>(R.id.activity_main_layout)
        rootLayout.setOnTouchListener { _, event ->
            // Check if the touch event is outside of the interactive elements
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Clear focus from the interactive elements
                editText.clearFocus()
                autoCompleteTextView.clearFocus()
                drawerLayout.close()
                rootLayout.performClick()
            }
            false // Return false to allow other touch events to be processed
        }

    }


    fun signOutSpotify() {
        // The code below cancels any cached Authorization Flow and resets all user data
        AuthorizationClient.stopLoginActivity(this, Spotifly.Global.REQUEST_CODE)
        Spotifly.SharedPrefsHelper.clearSharedPrefs()

        println("Sign Out Success -> Access Token = "+Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN",""))

        val intent = Intent(this, StartupActivity::class.java)
        startActivity(intent)
        finish()
    }


    fun makePlaylist(playlistType: String, playlistName: String) {
        playlistHub.main(playlistType, playlistName)

    }

}










