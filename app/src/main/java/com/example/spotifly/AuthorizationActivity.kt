package com.example.spotifly

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.AuthorizationRequest

import android.webkit.CookieManager
import okhttp3.*
import okhttp3.Request
import okio.IOException
import org.json.JSONObject
import java.util.Base64

// I am changing the functionality of this activity to handle an auth code for this branch
class AuthorizationActivity: AppCompatActivity() {

    // App Lifecycle Functions -----------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUI()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        // Check if result comes from the correct auth request
        if (requestCode == Spotifly.Global.REQUEST_CODE) {
            // Get the authorization result from the client and save it
            val response = AuthorizationClient.getResponse(resultCode, intent)
            when (response.type) {
                AuthorizationResponse.Type.CODE -> {
                    // Authentication was successful (A user auth token was returned from the request)
                    val authCode = response.code

                    exchangeCode(authCode)

                    Spotifly.HorizontalProgressBar.animateProgress(this)

                    val intent = Intent(this, MainActivity::class.java)
                     Handler().postDelayed({
                         startActivity(intent)
                         finish()
                     }, 1850)

                }
                AuthorizationResponse.Type.ERROR -> {
                    // Authentication error occurred
                    Toast.makeText(this, "Login Error: ${response.error}", Toast.LENGTH_SHORT).show()

                }
                else -> {
                    // Auth flow was cancelled ie: Closing out of the login prompt!
                    Toast.makeText(this, "Login Canceled", Toast.LENGTH_SHORT).show()

                }
                // All of the above "Toast" methods display a status message every time you are done with the Authorization Request
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies(null)

    }

    // Methods -----------------------------------------------------------------------------

    fun setUI() {
        setContentView(R.layout.activity_authorization)

        val loginButton = findViewById<Button>(R.id.sign_in_button)
        loginButton.setOnClickListener {// Upon clicking the button, start the spotify authentication
            authenticateSpotify()

        }
    }

    fun authenticateSpotify() {
        // Create an Authorization Request with the following arguments
        val builder = AuthorizationRequest.Builder(Spotifly.Global.CLIENT_ID, AuthorizationResponse.Type.CODE, Spotifly.Global.REDIRECT_URI)

        // Request Scopes (Permissions) to access from the User's Spotify Account
        val scopeArray = arrayOf("user-read-private", "user-read-email", "playlist-read-private", "playlist-read-collaborative", "playlist-modify-private", "playlist-modify-public",
            "user-top-read", "user-read-recently-played", "user-follow-read")

        builder.setScopes(scopeArray)
        builder.setShowDialog(true)
        // Actually build the full Authorization Request
        val request = builder.build()

        // Use the Spotify client to open a log in screen with the created request from above
        AuthorizationClient.openLoginActivity(this, Spotifly.Global.REQUEST_CODE, request)
    }


    // Exchanges the Auth Code received from the AuthorizationClient for Refresh and Access Tokens
    fun exchangeCode(authCode: String) {
        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", authCode)
            .add("redirect_uri", Spotifly.Global.REDIRECT_URI)
            .build()

        val credentials = Spotifly.Global.CLIENT_ID+":"+Spotifly.Global.CLIENT_SECRET
        val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())

        val request = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(requestBody)
            .header("Authorization", "Basic $encodedCredentials")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Exchange Failure", "Failed to exchange Auth Code: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {

                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val jsonObject = JSONObject(responseBody)

                        val accessToken = jsonObject.getString("access_token")
                        //val tokenType = jsonObject.getString("token_type")
                        val expiresIn = jsonObject.getInt("expires_in")
                        val refreshToken = jsonObject.getString("refresh_token")


                        // Using the API caller to get the USER ID, User Email, and Display Name for future activities.
                        val apiCaller = UserDataAPI(accessToken)
                        apiCaller.getUserInfo()

                        Spotifly.SharedPrefsHelper.saveSharedPref("ACCESS_TOKEN", accessToken)
                        Spotifly.SharedPrefsHelper.saveSharedPref("REFRESH_TOKEN", refreshToken)
                        Spotifly.SharedPrefsHelper.saveSharedPref("EXPIRES_IN", expiresIn)

                        // Calculate Access Token Expiration Time
                        val accessTokenExpirationTime = System.currentTimeMillis() + (expiresIn*1000)
                        Spotifly.SharedPrefsHelper.saveSharedPref("EXPIRATION_TIME", accessTokenExpirationTime)

                        // Debugging
                        Log.d("API Response", responseBody ?: "Empty response")


                    } else {
                        Log.e("API Error","Unsuccessful response: ${response.code}" )
                    }

                }
            }
        })



    }






    }