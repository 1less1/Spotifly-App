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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class CreatePlaylistAPI(c: Context, token:String, id: String) {

    var accessToken = token
    var userID = id
    val context = c

    fun main(playlistType: String, playlistName: String) {
        CoroutineScope(Dispatchers.IO).launch {

            try {
                runBlocking {

                    val (userTopSongs, userTopSongsArtists) = async { getUserTopItems() }.await()
                    println("User Top Songs After Function Call: $userTopSongs")
                    println("Artists for each Top Songs: $userTopSongsArtists")

                    val userTopSongsGenres = async { getArtistInfo(userTopSongsArtists) }.await()
                    println("Genres for each Top Song: $userTopSongsGenres")

                    val targetGenres = arrayListOf("edm", "pop")
                    val targetSongList = async { getSongsFromGenre(targetGenres, userTopSongs, userTopSongsGenres) }.await()
                    println("Target Songs that fall under 'electronic' and 'dance pop' $targetSongList")

                    val seedTracks = async { finalizeSeedTracks(targetSongList)}.await()
                    println("Seed Tracks from the target list: $seedTracks")

                    val url = async { createRecommendationUrl(
                        limit = "50",
                        seed_tracks = seedTracks,
                        seed_genres = targetGenres,
                        target_danceability = "0.6",
                        min_energy = "0.8",
                        min_popularity = "70",
                        min_tempo = "120",) }.await()
                    println("URL for the final GET Request: $url")

                    val recommendedTracks = async { getRecommendations(url) }.await()
                    println("Seed Generation of Recommended Tracks: $recommendedTracks")

                    val playlistId = async { createPlaylist(playlistName) }.await()
                    println("Playlist ID After Function Call: $playlistId")

                    async { editPlaylist(playlistId, recommendedTracks) }.await()
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


    // HTTP GET Request - Takes in a list of Artist IDs. Then returns each artist's Genres.
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

    fun finalizeSeedTracks(tracksList: List<String>): List<String> {
        val seedTracks = mutableListOf<String>()
        val shuffledTracksList = tracksList.shuffled()

        if (tracksList.size>3) {
            // If the tracksList has more than 3 songs, add 3 "random" songs
            val randomTracks = shuffledTracksList.take(3)
            seedTracks.addAll(randomTracks)

        } else {
            // If the tracksList has less than 3 songs, add all of the songs
            val randomTracks = shuffledTracksList.take(tracksList.size)
            seedTracks.addAll(randomTracks)
        }
        return seedTracks
    }

    fun createRecommendationUrl(
        limit: String,
        seed_tracks: List<String>,
        seed_genres: List<String>? = null,
        seed_artists: List<String>? = null,
        min_acousticness: String? = null,
        max_acousticness: String? = null,
        target_acousticness: String? = null,
        min_danceability: String? = null,
        max_danceability: String? = null,
        target_danceability: String? = null,
        target_duration_ms: String? = null,
        min_energy: String? = null,
        max_energy: String? = null,
        target_energy: String? = null,
        min_instrumentalness: String? = null,
        max_instrumentalness: String? = null,
        target_instrumentalness: String? = null,
        min_key: String? = null,
        max_key: String? = null,
        target_key: String? = null,
        min_liveness: String? = null,
        max_liveness: String? = null,
        target_liveness: String? = null,
        min_loudness: String? = null,
        max_loudness: String? = null,
        target_loudness: String? = null,
        min_popularity: String? = null,
        max_popularity: String? = null,
        target_popularity: String? = null,
        min_tempo: String? = null,
        max_tempo: String? = null,
        target_tempo: String? = null,
        target_valence: String? = null
    ): String {
        val urlParams = mutableListOf<String>()

        // Add Required Parameters
        urlParams.add("limit=$limit")
        urlParams.add("market=US")
        urlParams.add("seed_tracks=${seed_tracks.joinToString(",")}")
        seed_genres?.let { urlParams.add("seed_genres=${it.joinToString(",") { genre -> genre.replace(" ", "+") }}") }
        seed_artists?.let { urlParams.add("seed_artists=${it.joinToString(",")}") }

        // Add Optional Parameters
        min_acousticness?.let { urlParams.add("min_acousticness=$it") }
        max_acousticness?.let { urlParams.add("max_acousticness=$it") }
        target_acousticness?.let { urlParams.add("target_acousticness=$it") }
        min_danceability?.let { urlParams.add("min_danceability=$it") }
        max_danceability?.let { urlParams.add("max_danceability=$it") }
        target_danceability?.let { urlParams.add("target_danceability=$it") }
        target_duration_ms?.let { urlParams.add("target_duration_ms=$it") }
        min_energy?.let { urlParams.add("min_energy=$it") }
        max_energy?.let { urlParams.add("max_energy=$it") }
        target_energy?.let { urlParams.add("target_energy=$it") }
        min_instrumentalness?.let { urlParams.add("min_instrumentalness=$it") }
        max_instrumentalness?.let { urlParams.add("max_instrumentalness=$it") }
        target_instrumentalness?.let { urlParams.add("target_instrumentalness=$it") }
        min_key?.let { urlParams.add("min_key=$it") }
        max_key?.let { urlParams.add("max_key=$it") }
        target_key?.let { urlParams.add("target_key=$it") }
        min_liveness?.let { urlParams.add("min_liveness=$it") }
        max_liveness?.let { urlParams.add("max_liveness=$it") }
        target_liveness?.let { urlParams.add("target_liveness=$it") }
        min_loudness?.let { urlParams.add("min_loudness=$it") }
        max_loudness?.let { urlParams.add("max_loudness=$it") }
        target_loudness?.let { urlParams.add("target_loudness=$it") }
        min_popularity?.let { urlParams.add("min_popularity=$it") }
        max_popularity?.let { urlParams.add("max_popularity=$it") }
        target_popularity?.let { urlParams.add("target_popularity=$it") }
        min_tempo?.let { urlParams.add("min_tempo=$it") }
        max_tempo?.let { urlParams.add("max_tempo=$it") }
        target_tempo?.let { urlParams.add("target_tempo=$it") }
        target_valence?.let { urlParams.add("target_valence=$it") }

        // Construct the URL
        val baseUrl = "https://api.spotify.com/v1/recommendations"
        val url = if (urlParams.isNotEmpty()) {
            "$baseUrl?${urlParams.joinToString("&")}"
        } else {
            baseUrl
        }
        return url

    }
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
            .url("https://api.spotify.com/v1/users/$userID/playlists")
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