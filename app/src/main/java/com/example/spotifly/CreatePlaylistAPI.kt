package com.example.spotifly

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class CreatePlaylistAPI(context: Context, accessToken: String, userId: String) {

    val accessToken = accessToken
    val userId = userId
    val context = context

    // HTTP GET Request - Returns 2 Lists: User Top 50 Songs and the 50 Artists associated with each song
    fun getUserTopItems(): Pair<List<String>, List<String>> {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/top/tracks?limit=50")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()


        val response = client.newCall(request).execute()

        // Unsuccessful Response
        if (!response.isSuccessful) {
            response.body?.close()
            throw IOException("Failed to get top tracks: HTTP ${response.code}")
        }

        // Successful Response
        val responseBody = response.body?.string()

        val trackIds = mutableListOf<String>()
        val artistIds = mutableListOf<String>()
        val jsonObject = JSONObject(responseBody)

        try {
            val itemsArray = jsonObject.getJSONArray("items")

            for (i in 0 until itemsArray.length()) {
                val trackObject = itemsArray.getJSONObject(i)
                val trackId = trackObject.getString("id")
                trackIds.add(trackId)

                // New Code to get the first Artist Listed for each Song
                val artistsArray = trackObject.getJSONArray("artists")

                if (artistsArray.length()>0) {
                    val firstArtistObject = artistsArray.getJSONObject(0)
                    val artistId = firstArtistObject.getString("id")
                    artistIds.add(artistId)
                    }
                }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.d("API Response", responseBody ?: "Empty response")
        return Pair(trackIds, artistIds)

    }


    // HTTP GET Request - Takes in a list of Artist IDs and then returns each artist's Genres.
    fun getArtistInfo(artistList: List<String>): List<List<String>> {
        val client = OkHttpClient()

        val idsString = artistList.joinToString(",")

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/artists?ids=$idsString")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()


        val response = client.newCall(request).execute()

        // Unsuccessful Response
        if (!response.isSuccessful) {
            response.body?.close()
            throw IOException("Failed to get artist info: HTTP ${response.code}")
        }

        // Successful Response
        val responseBody = response.body?.string()

        val jsonObject = JSONObject(responseBody)
        var masterGenresList = mutableListOf<List<String>>()

        try {
            val artistsArray = jsonObject.getJSONArray("artists")

             for (i in 0 until artistsArray.length()) {
                 val artistObject = artistsArray.getJSONObject(i)

                 if (artistObject.has("genres")) {
                     val artistGenres = artistObject.getJSONArray("genres")
                     val artistGenresList = jsonArrayToList(artistGenres)
                     masterGenresList.add(artistGenresList)
                 } else {
                     masterGenresList.add(arrayListOf("No Genre Specified"))
                 }
             }


        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.d("API Response", responseBody ?: "Empty response")
        return masterGenresList

    }

    // Returns a list of songs that match the Target Genre from the list of Track IDs
    fun getSongsFromGenre(targetGenres: List<String>, trackIds: List<String>, trackGenres: List<List<String>>): List<String> {
        var matchingSongs = mutableListOf<String>() // List for songs that match the targetGenre


        for (i in 0 until trackGenres.size) {
            var found = false

            for (genre in targetGenres) {
                if (genre in trackGenres[i]) {
                    matchingSongs.add(trackIds[i])
                    found = true
                    break
                }
            }

            if (found) {
                continue
            }
        }
        return matchingSongs

    }

    // Chooses 1 random user track from the filtered track list returned from getSongsFromGenre() to be used as a seedTrack
    fun finalizeSeedTracks(tracksList: List<String>, presetSeedTracks: List<String>): List<String> {
        val seedTracks = mutableListOf<String>()
        val shuffledTracksList = tracksList.shuffled()
        val shuffledPresetSeedTracks = presetSeedTracks.shuffled()
        var count = 3 // Number of preset tracks to be added
        var index = 0

        // If the user has tracks for the playlist...
        if (tracksList.size!=0) {
            // Add random user track
            seedTracks.add(shuffledTracksList[0])
            count = 2 // Change preset track count to 2 since there was a user track added
        }

        while (count > 0 && index < shuffledPresetSeedTracks.size) {
            val track = shuffledPresetSeedTracks[index]
            if (track !in seedTracks) {
                seedTracks.add(track)
                count--
            }
            index++
        }

        return seedTracks
    }

    fun createRecommendationURL(
        seed_tracks: List<String>,
        seed_genres: List<String>? = null,
        seed_artists: List<String>? = null,
        parameters: Map<String, String?> ): String {

        val urlParams = mutableListOf<String>()

        // Add Required Parameters
        urlParams.add("limit=50")
        urlParams.add("market=US")
        urlParams.add("seed_tracks=${seed_tracks.joinToString(",")}")
        seed_genres?.let { urlParams.add("seed_genres=${it.joinToString(",") { genre -> genre.replace(" ", "+") }}") }
        seed_artists?.let { urlParams.add("seed_artists=${it.joinToString(",")}") }


        // Add Optional Parameters
        for ((key, value) in parameters) {
            if (value != null) {
                urlParams.add("$key=$value")
            }
        }

        // Construct the URL
        val baseUrl = "https://api.spotify.com/v1/recommendations"
        val url = if (urlParams.isNotEmpty()) {
            "$baseUrl?${urlParams.joinToString("&")}"
        } else {
            baseUrl
        }
        return url
    }


    // HTTP Get Request - Generates and returns a list of tracks based on all the info gathered above
    fun getRecommendations(url: String): List<String> {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()


        val response = client.newCall(request).execute()

        // Unsuccessful Response
        if (!response.isSuccessful) {
            response.body?.close()
            throw IOException("Failed to get recommendations: HTTP ${response.code}")
        }

        // Successful Response
        val responseBody = response.body?.string()

        val jsonObject = JSONObject(responseBody)

        val recommendedTracks = mutableListOf<String>()

        try {
            val tracksArray = jsonObject.getJSONArray("tracks")

            for (i in 0 until tracksArray.length()) {
                val trackObject = tracksArray.getJSONObject(i)
                val trackId = trackObject.getString("id")
                recommendedTracks.add(trackId)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Response is a little too long so we will rely on error checking!
        //Log.d("API Response", responseBody ?: "Empty response")
        return recommendedTracks

    }



    // HTTP POST Request - Creates a Spotify Playlist on the User's Account with the designated name
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
            .url("https://api.spotify.com/v1/users/$userId/playlists")
            .post(requestBody)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .build()


        val response = client.newCall(request).execute()

        // Unsuccessful Response
        if (!response.isSuccessful) {
            response.body?.close()
            throw IOException("Failed to create playlist: HTTP ${response.code}")
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



    // HTTP POST Request - Adds Songs from a List to an established Playlist identified by its Playlist ID
    fun editPlaylist(playlistId: String, trackIds: List<String>) {

        var client = OkHttpClient()

        val trackURIs = trackIds.joinToString(",") { "spotify:track:$it" }
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

    // Converts JSON Array to a Mutable List full of Strings
    fun jsonArrayToList(jsonArray: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            val element = jsonArray.getString(i)
            list.add(element)
        }
        return list
    }


}