package com.example.spotifly

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class CreatePlaylistAPI(token:String) {

    var accessToken = token

    // HTTP GET Request
    fun getUserTopItems() {


        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/top/tracks?")
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


                    val trackIds = mutableListOf<String>()
                    val jsonObject = JSONObject(responseBody)

                    try {
                        val itemsArray = jsonObject.getJSONArray("items")

                        for (i in 0 until itemsArray.length()) {
                            val itemObject = itemsArray.getJSONObject(i)
                            val trackId = itemObject.getString("id")
                            trackIds.add(trackId)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    Log.d("API Response", responseBody?: "Empty response")

                    println("User Top Songs: ")
                    println(trackIds)


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