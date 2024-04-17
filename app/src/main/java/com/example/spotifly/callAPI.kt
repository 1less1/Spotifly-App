package com.example.spotifly

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull

import retrofit2.http.*


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
                Log.e("API Error", "Failed to get user profile: ", e)
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

    // HTTP GET Request
    fun getUserTopItems() {


        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/top/tracks?limit=50&offset=0")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                Log.e("API Error", "Failed to get top tracks: ", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    val jsonObject = JSONObject(responseBody)

                    Log.d("API Response", responseBody ?: "Empty response")


                } else {
                    Log.e("API Error", "Failed to get top tracks: ${response.code}")

                    response.body?.close()

                }
            }

        })

    }






    // HTTP POST Request
    fun createPlaylist(name: String) {

        val userID = Spotifly.SharedPrefsHelper.getSharedPref("user_id", "")

        val client = OkHttpClient()

        val playlistName = name
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
                Log.e("API Error", "Failed to create playlist: ", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    val jsonObject = JSONObject(responseBody)
                    val playlistID = jsonObject.getString("id")

                    // Debugging - Logcat
                    println("New Playlist ID: $playlistID")


                    // Storing playlist in Shared Preferences like so:
                    // Key: Playlist Name in ALL CAPS
                    // Value: Playlist ID as a string
                    Spotifly.SharedPrefsHelper.saveSharedPref(playlistName.toUpperCase(), playlistID)

                    Log.d("API Response", responseBody ?: "Empty response")


                } else {
                    Log.e("API Error", "Failed to create playlist: ${response.code}")

                    response.body?.close()

                }
            }

        })

    }

    fun addSongToPlaylist(name: String) {

        val playlistID = Spotifly.SharedPrefsHelper.getSharedPref(name.toUpperCase(), "")
        println("Adding songs to $name: $playlistID")

        var client = OkHttpClient()

        // POST Request REQUIRES JSON Body!!!!
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(), """
        {
            "uris": [ "spotify:track:27NovPIUIRrOZoCHxABJwK" ]
        }
        """.trimIndent()
        )

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/playlists/$playlistID/tracks")
            .post(requestBody)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                Log.e("API Error", "Failed to add song: ", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    val jsonObject = JSONObject(responseBody)
                    val snapshotID = jsonObject.getString("snapshot_id")

                    Log.d("API Response", responseBody ?: "Empty response")


                } else {
                    Log.e("API Error", "Failed to add song: ${response.code}")

                    response.body?.close()

                }
            }

        })

    }

    

}
