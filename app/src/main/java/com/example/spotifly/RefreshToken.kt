package com.example.spotifly

import android.util.Log
import okhttp3.*
import okhttp3.Request
import okio.IOException
import org.json.JSONObject
import java.util.Base64

class RefreshToken(at:String, rt: String, et: Long) {

    var accessToken = at
    val refreshToken = rt
    var expirationTime = et

    // I will be grabbing constant variables from the Spotifly Class' "Global" singleton object since
    // those are retrieved from RAM vs the hard disk with sharedPreferences

    // HTTP POST request needed to refresh existing tokens
    fun refreshAccessToken() {
        Log.d("RefreshToken", "Refreshing Access Token...")

        val oldAccessToken = accessToken
        val oldExpirationTime = expirationTime

        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
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
                Log.e("Exchange Failure", "Failed to exchange Refresh Token: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonObject = JSONObject(responseBody)

                    val newAccessToken = jsonObject.getString("access_token")
                    val newExpiresIn = jsonObject.getInt("expires_in")

                    Spotifly.SharedPrefsHelper.saveSharedPref("ACCESS_TOKEN", newAccessToken)
                    Spotifly.SharedPrefsHelper.saveSharedPref("EXPIRES_IN", newExpiresIn)


                    // Calculate Access Token Expiration Time
                    val newExpirationTime = System.currentTimeMillis() + (newExpiresIn*1000)
                    Spotifly.SharedPrefsHelper.saveSharedPref("EXPIRATION_TIME", newExpirationTime)

                    // Debugging
                    Log.d("API Response", responseBody ?: "Empty response")

                    // Logcat - Debugging
                    println("---------------------------------")
                    println("Tokens Before Refresh: ")
                    println("Access Token: $oldAccessToken")
                    println("Refresh Token: $refreshToken")
                    println("Expiration Time: $oldExpirationTime")
                    println()
                    println("Tokens After Refresh: ")
                    println("Access Token: " + Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN",""))
                    println("Refresh Token: " + Spotifly.SharedPrefsHelper.getSharedPref("REFRESH_TOKEN", ""))
                    println("Expiration Time: " + Spotifly.SharedPrefsHelper.getSharedPref("EXPIRATION_TIME",0L))

                } else {
                    Log.e("API Error","Unsuccessful response: ${response.code}" )
                }

            }
        })

    }


}