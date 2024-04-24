package com.example.spotifly

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class CreatePlaylistAPI(c: Context, token:String, id: String) {

    var accessToken = token
    var userID = id
    val context = c

    fun main() {
        CoroutineScope(Dispatchers.IO).launch {

            try {
                runBlocking {
                    val userTopSongs = async { getUserTopItems() }.await()
                    println("User Top Songs After Function Call: $userTopSongs")

                    val playlistId = async { createPlaylist("Your Top Songs") }.await()
                    println("Playlist ID After Function Call: $playlistId")

                    async { editPlaylist(playlistId, userTopSongs) }.await()
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Playlist Made Successfully!", Toast.LENGTH_SHORT)
                        .show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error -> ${e.message}", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    // HTTP GET Request
    fun getUserTopItems(): MutableList<String> {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/top/tracks?limit=35")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()


        val response = client.newCall(request).execute()

        // Unsuccessful Response
        if (!response.isSuccessful) {
            response.body?.close()
            throw IOException("Failed to get top tracks: ${response.code}")
        }

        // Successful Response
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

        Log.d("API Response", responseBody ?: "Empty response")
        return trackIds






    }

    // HTTP POST REQUEST
    fun createPlaylist(name: String): String {

        val client = OkHttpClient()

        val playlistName = name
        val playlistDescription = "This playlist was created using Spotifly!"
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


        val response = client.newCall(request).execute()

        // Unsuccessful Response
        if (!response.isSuccessful) {
            response.body?.close()
            throw IOException("Failed to create playlist: ${response.code}")
        }

        // Successful Response
        val responseBody = response.body?.string()

        val jsonObject = JSONObject(responseBody)
        val playlistID = jsonObject.getString("id")


        // Storing playlist in Shared Preferences like so:
        // Key: Playlist Name in ALL CAPS
        // Value: Playlist ID as a string
        //Spotifly.SharedPrefsHelper.saveSharedPref(playlistName.toUpperCase(), playlistID)

        Log.d("API Response", responseBody ?: "Empty response")
        return playlistID




    }



    // HTTP POST Request
    fun editPlaylist(playlistId: String, songList: MutableList<String>) {

        var client = OkHttpClient()

        val trackURIs = songList.joinToString(",") { "spotify:track:$it" }
        val url = "https://api.spotify.com/v1/playlists/$playlistId/tracks?uris=$trackURIs"

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ""))
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .build()



        val response = client.newCall(request).execute()

        // Unsuccessful Response
        if (!response.isSuccessful) {
            response.body?.close()
            throw IOException("Failed to edit playlist: HTTP ${response.code}")
        }

        // Successful Response
        val responseBody = response.body?.string()

        val jsonObject = JSONObject(responseBody)

        Log.d("API Response", responseBody ?: "Empty response")

    }


}