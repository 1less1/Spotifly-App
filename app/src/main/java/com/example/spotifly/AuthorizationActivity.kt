package com.example.spotifly

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.sdk.android.auth.AuthorizationRequest
import android.util.Base64 as AndroidBase64

import android.webkit.CookieManager
import okhttp3.*
import okhttp3.Request
import okio.IOException
import org.json.JSONObject
import java.util.Base64 as JavaBase64
import java.security.MessageDigest
import java.security.SecureRandom



// I am changing the functionality of this activity to handle an auth code for this branch
class AuthorizationActivity: AppCompatActivity() {

    // App Lifecycle Functions -----------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUI()

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
        val codeVerifier = generateCodeVerifier()
        Spotifly.SharedPrefsHelper.saveSharedPref("CODE_VERIFIER", codeVerifier)

        val codeChallenge = generateCodeChallenge(codeVerifier)

        val builder = AuthorizationRequest.Builder(
            Spotifly.Global.CLIENT_ID,
            AuthorizationResponse.Type.CODE,
            Spotifly.Global.REDIRECT_URI
        )

        builder.setScopes(arrayOf(
            "user-read-private", "user-read-email", "playlist-read-private", "playlist-read-collaborative",
            "playlist-modify-private", "playlist-modify-public", "user-top-read", "user-read-recently-played",
            "user-follow-read"
        ))

        builder.setShowDialog(true)
        builder.setCustomParam("code_challenge", codeChallenge)
        builder.setCustomParam("code_challenge_method", "S256")

        val request = builder.build()

        // Add a small delay to ensure SharedPreferences has saved the codeVerifier
        Handler(Looper.getMainLooper()).postDelayed({
            AuthorizationClient.openLoginActivity(this, Spotifly.Global.REQUEST_CODE, request)
        }, 200)
    }


    // Function to generate a code verifier
    fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val codeVerifier = ByteArray(32)
        random.nextBytes(codeVerifier)
        return AndroidBase64.encodeToString(codeVerifier, AndroidBase64.URL_SAFE or AndroidBase64.NO_PADDING or AndroidBase64.NO_WRAP)
    }

    // Function to generate a code challenge
    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return AndroidBase64.encodeToString(digest, AndroidBase64.URL_SAFE or AndroidBase64.NO_PADDING or AndroidBase64.NO_WRAP)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == Spotifly.Global.REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            when (response.type) {
                AuthorizationResponse.Type.CODE -> {
                    val authCode = response.code
                    val codeVerifier = Spotifly.SharedPrefsHelper.getSharedPref("CODE_VERIFIER", "")

                    if (codeVerifier.isNotEmpty()) {
                        exchangeCode(authCode, codeVerifier)
                    } else {
                        Log.e("PKCE Error", "Code Verifier is missing!")
                        Toast.makeText(this, "Login Error: Missing Code Verifier", Toast.LENGTH_SHORT).show()
                    }

                    Spotifly.HorizontalProgressBar.animateProgress(this)

                    val intent = Intent(this, MainActivity::class.java)
                    Handler().postDelayed({
                        startActivity(intent)
                        finish()
                    }, 1850)
                }
                AuthorizationResponse.Type.ERROR -> {
                    Toast.makeText(this, "Login Error: ${response.error}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Login Canceled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    // Exchanges the Auth Code received from the AuthorizationClient for Refresh and Access Tokens
    fun exchangeCode(authCode: String, codeVerifier: String) {
        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", authCode)
            .add("redirect_uri", Spotifly.Global.REDIRECT_URI)
            .add("client_id", Spotifly.Global.CLIENT_ID) // Include client_id
            .add("code_verifier", codeVerifier) // Add code_verifier for PKCE
            .build()

        val request = Request.Builder()
            .url("https://accounts.spotify.com/api/token")
            .post(requestBody)
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
                        val expiresIn = jsonObject.getInt("expires_in")
                        val refreshToken = jsonObject.optString("refresh_token", "")

                        val apiCaller = UserDataAPI(accessToken)
                        apiCaller.getUserInfo()

                        Spotifly.SharedPrefsHelper.saveSharedPref("ACCESS_TOKEN", accessToken)
                        Spotifly.SharedPrefsHelper.saveSharedPref("REFRESH_TOKEN", refreshToken)
                        Spotifly.SharedPrefsHelper.saveSharedPref("EXPIRES_IN", expiresIn)

                        val accessTokenExpirationTime = System.currentTimeMillis() + (expiresIn * 1000)
                        Spotifly.SharedPrefsHelper.saveSharedPref("EXPIRATION_TIME", accessTokenExpirationTime)

                        Log.d("API Response", responseBody ?: "Empty response")
                    } else {
                        Log.e("API Error", "Unsuccessful response: ${response.code}")
                    }
                }
            }
        })
    }


}