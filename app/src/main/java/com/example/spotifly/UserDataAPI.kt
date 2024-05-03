package com.example.spotifly

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.util.Log


class UserDataAPI(token: String) {

    var accessToken = token

    // HTTP GET Request
    fun getUserInfo() {
        // Need to get user client id and then save it to separate shared prefs or keep the same for consistency?

        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API Error", "Failed to get user profile: ", e)
            }

            override fun onResponse(call: Call, response: Response) {
                // Successful response
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonObject = JSONObject(responseBody)

                    val userID = jsonObject.getString("id")
                    val userEmail = jsonObject.getString("email")
                    val displayName = jsonObject.getString("display_name")
                    val imagesArray = jsonObject.getJSONArray("images")

                    if (imagesArray.length()>0) {
                        val firstImage = imagesArray.getJSONObject(1)
                        val pfp_url = firstImage.getString("url")
                        Spotifly.SharedPrefsHelper.saveSharedPref("pfp_url", pfp_url)
                        println("PFP URL: $pfp_url")
                    }

                    Spotifly.SharedPrefsHelper.saveSharedPref("user_id", userID)
                    Spotifly.SharedPrefsHelper.saveSharedPref("email", userEmail)
                    Spotifly.SharedPrefsHelper.saveSharedPref("display_name", displayName)

                    // Debugging - Logcat
                    println("User Id: $userID")
                    println("Email: $userEmail")
                    println("Display Name: $displayName")

                    Log.d("API Response", responseBody ?: "Empty response")

                } else {
                    // Handle error
                    Log.e("API Error", "Failed to get user profile: ${response.code}")
                }
            }
        })


    }





    

}
