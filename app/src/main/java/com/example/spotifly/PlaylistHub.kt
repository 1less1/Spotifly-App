package com.example.spotifly

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PlaylistHub(context: Context, accessToken: String, userId: String) {
    // Assign Arguments to local variables
    val context = context
    val accessToken = accessToken
    val userId = userId

    fun main(playlistType: String, playlistName: String) {
        val playlistCreator = CreatePlaylistAPI(context, accessToken, userId)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                runBlocking {


                    // Get a list of the user's top 50 songs and a list of the corresponding artists
                    val (userTopSongs, userTopSongsArtists) = async { playlistCreator.getUserTopItems() }.await()
                    println("User Top Songs After Function Call: $userTopSongs")
                    println("Artists for each Top Songs: $userTopSongsArtists")

                    if (playlistType=="My Top Songs") {
                        val playlistId = async { playlistCreator.createPlaylist(playlistName)}.await()
                        println("Playlist ID After Function Call: $playlistId")

                        async { playlistCreator.editPlaylist(playlistId, userTopSongs) }.await()


                    } else {
                        val (customPlaylistParams, targetGenres, seedGenres) = async { getPlaylistParameters(playlistType) }.await()
                        // targetGenres - the genres of songs you would like to sort out of the user's top 50 songs
                        // seedGenres - the 3 genres you would like your recommendation generation to focus on

                        // Get a 2 dimensional list containing the genres associated with each of the user's top 50 songs
                        val userTopSongsGenres = async { playlistCreator.getArtistInfo(userTopSongsArtists)}.await()
                        println("Genres for each Top Song: $userTopSongsGenres")

                        val filteredUserSongs = async { playlistCreator.getSongsFromGenre(targetGenres, userTopSongs, userTopSongsGenres)}.await()
                        println("Filtered Songs: $filteredUserSongs")

                        val seedTracks = async { playlistCreator.finalizeSeedTracks(filteredUserSongs)}.await()
                        println("Seed Tracks: $seedTracks")

                        val url = async { playlistCreator.createRecommendationUrl2(
                            seed_tracks = seedTracks,
                            seed_genres = seedGenres,
                            parameters = customPlaylistParams) }.await()
                        println("URL for the final GET Request: $url")

                        val recommendedTracks = async { playlistCreator.getRecommendations(url)}.await()
                        println("Seed Generation of Recommended Tracks: $recommendedTracks")

                        val playlistId = async { playlistCreator.createPlaylist(playlistName)}.await()
                        println("Playlist ID After Function Call: $playlistId")

                        async { playlistCreator.editPlaylist(playlistId, recommendedTracks) }.await()

                    }

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



    fun getPlaylistParameters(playlistType: String): Triple< Map<String, String?>, List<String>, List<String> > {
        return when (playlistType) {
            "Workout" -> Triple(
                mapOf(
                    "target_danceability" to "0.6",
                    "min_energy" to "0.8",
                    "min_popularity" to "70",
                    "min_tempo" to "120"
                ),
                listOf("edm", "dance pop", "dance", "house", "electro", "electronic", "techno", "work-out", "dubstep"), // targetGenres -> all available genres you are looking to filter out from userTopSongs
                listOf("edm", "pop", "dance pop" ) // seedGenres -> limit of 3, used when generating recommendation tracks
            )
            // Add other playlist types similarly
            else -> throw IllegalArgumentException("Invalid playlistType: $playlistType")
        }
    }

}