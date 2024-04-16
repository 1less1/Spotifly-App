package com.example.spotifly

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull


class callAPI {

    private val accessToken = Spotifly.SharedPrefsHelper.getSharedPref("ACCESS_TOKEN", "")


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
                Log.e("API Error", "Failed to get user profile", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonObject = JSONObject(responseBody)

                    val userID = jsonObject.getString("id")

                    Spotifly.SharedPrefsHelper.saveSharedPref("user_id", userID)

                    // Debugging - Logcat
                    println("User Id: $userID")



                    Log.d("API Response", responseBody ?: "Empty response")
                } else {
                    // Handle error
                    Log.e("API Error", "Failed to get user profile: ${response.code}")
                }
            }
        })

    }



    // HTTP POST Request
    fun createPlaylist() {

        val userID = Spotifly.SharedPrefsHelper.getSharedPref("user_id", "")
        //println("User Id: $userID")

        val client = OkHttpClient()

        val playlistName = "Spotify OAuth2 can suck my @#$%!"
        val playlistDescription = "This playlist was created for testing!"
        val isPublic = false
        val isCollaborative = false


        // POST Request REQUIRES JSON Body!!!!
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(), """
        {
            "name": "$playlistName",
            "description": "$playlistDescription",
            "public": "$isPublic",
            "collaborative": "$isCollaborative"
        }
        """.trimIndent()
        )


        val request = Request.Builder()
            .url("https://api.spotify.com/v1/users/$userID/playlists")
            .post(requestBody)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                Log.e("API Error", "Failed to create playlist", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    val jsonObject = JSONObject(responseBody)
                    val playlistID = jsonObject.getString("id")

                    // Debugging - Logcat
                    println("New Playlist ID: $playlistID")

                    Log.d("API Response", responseBody ?: "Empty response")


                } else {
                    Log.e("API Error", "Failed to create playlist: ${response.code}")

                    response.body?.close()

                }
            }

        })

    }

    

}
