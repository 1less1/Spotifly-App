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
                        val (customPlaylistParams, targetGenres, seedGenres, presetSeedTrack) = async { getPlaylistParameters(playlistType) }.await()
                        // targetGenres - the genres of songs you would like to sort out of the user's top 50 songs
                        // seedGenres - the 2 genres you would like your recommendation generation to focus on

                        // Get a 2 dimensional list containing the genres associated with each of the user's top 50 songs
                        val userTopSongsGenres = async { playlistCreator.getArtistInfo(userTopSongsArtists)}.await()
                        println("Genres for each Top Song: $userTopSongsGenres")

                        // Filter out the songs corresponding to your target genres
                        val filteredUserSongs = async { playlistCreator.getSongsFromGenre(targetGenres, userTopSongs, userTopSongsGenres)}.await()
                        println("Filtered Songs: $filteredUserSongs")

                        // Pick 2 random tracks from the filtered songs to be used as seedTracks
                        val seedTracks = async { playlistCreator.finalizeSeedTracks(filteredUserSongs, presetSeedTrack)}.await()
                        println("Seed Tracks: $seedTracks")

                        // Create getRecommendation url
                        val url = async { playlistCreator.createRecommendationURL(
                            seed_tracks = seedTracks,
                            seed_genres = seedGenres,
                            parameters = customPlaylistParams) }.await()
                        println("URL for the final GET Request: $url")

                        // Get recommended tracks using the previously created url
                        val recommendedTracks = async { playlistCreator.getRecommendations(url)}.await()
                        println("Seed Generation of Recommended Tracks: $recommendedTracks")

                        // Create and edit a new playlist
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


    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun getPlaylistParameters(playlistType: String): Quadruple<Map<String, String?>, List<String>, List<String>, List<String>> {
        // Every Playlist type needs to have 4 parameters: customization settings, targetGenres, seedGenres, presetSeedTrack
        // Available Genre seeds found in genre_seeds.md
        
        return when (playlistType) {
            "Electric Dance Anthems" -> Quadruple(
                mapOf( // Customizations for the recommendation url
                    "target_danceability" to "0.65",
                    "min_energy" to "0.65",
                    "min_popularity" to "40",
                    "min_tempo" to "110"
                ),
                listOf("edm", "dance", "house", "electronic"), // targetGenres -> all available genres you are looking to filter out from userTopSongs
                listOf("edm", "dance"), // seedGenres -> limit of 2, genres to focus on when generating recommended tracks
                listOf("6Pgkp4qUoTmJIPn7ReaGxL", "6YUTL4dYpB9xZO5qExPf05", "70YPzqSEwJvAIQ6nMs1cjY", "2YNVpj4AfBb3HN1QTqEcVB", "5K6Ssv4Z3zRvxt0P6EKUAP", "6ICrDmbUzJaVXF34ag0aaS") // presetSeedTracks (keeps recommendation generation focused)
                // Without You -Avicii, Summer -Calvin Harris, In Your Arms -Illenium, Something Strange -Vicetone, Heartbreak Anthem -Galantis, Red Lights -Tiesto
            )
            "Pumped Up Pop" -> Quadruple(
                mapOf(
                    "target_danceability" to "0.7",
                    "min_energy" to "0.7",
                    "min_popularity" to "50",
                    "min_tempo" to "105"
                ),
                listOf("pop", "dance pop", "edm", "dance"),
                listOf("dance pop", "pop"),
                listOf("0rSLgV8p5FzfnqlEk4GzxE", "09IStsImFySgyp0pIQdqAc") // presetSeedTrack -> Closer -Chainsmokers and The Middle -Zedd
            )
            // Add other playlist types similarly
            else -> throw IllegalArgumentException("Invalid playlistType: $playlistType")
        }
    }

}