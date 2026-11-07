package com.my.kizzy.data.get_current_data.media

import android.media.MediaMetadata
import java.net.URLEncoder

object MediaButtonResolver {
    fun resolve(
        packageName: String,
        title: String,
        artist: String?,
        metadata: MediaMetadata?
    ): Pair<String, String>? {
        if (title.isBlank()) return null

        val query = if (!artist.isNullOrBlank()) "$title $artist" else title
        val encodedQuery = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query.replace(" ", "+")
        }
        val pkg = packageName.lowercase()

        val mediaUri = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
            ?: metadata?.description?.mediaUri?.toString()

        return when {
            // YouTube Music
            pkg.contains("youtube.music") -> {
                val mediaId = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
                val url = if (mediaId != null && mediaId.length == 11 && mediaId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
                    "https://music.youtube.com/watch?v=$mediaId"
                } else {
                    "https://music.youtube.com/search?q=$encodedQuery"
                }
                "Listen on YouTube Music" to url
            }

            // Spotify
            pkg.contains("spotify") -> {
                val spotifyUri = mediaUri?.takeIf { it.startsWith("spotify:track:") }
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)?.takeIf { it.startsWith("spotify:track:") }
                val url = if (spotifyUri != null) {
                    val trackId = spotifyUri.removePrefix("spotify:track:")
                    "https://open.spotify.com/track/$trackId"
                } else {
                    "https://open.spotify.com/search/$encodedQuery"
                }
                "Listen on Spotify" to url
            }

            // SoundCloud
            pkg.contains("soundcloud") -> {
                val url = if (mediaUri != null && mediaUri.startsWith("https://soundcloud.com/")) {
                    mediaUri
                } else {
                    "https://soundcloud.com/search?q=$encodedQuery"
                }
                "Listen on SoundCloud" to url
            }

            // Apple Music
            pkg.contains("apple.android.music") || pkg.contains("music.apple") -> {
                "Listen on Apple Music" to "https://music.apple.com/search?term=$encodedQuery"
            }

            // TIDAL
            pkg.contains("tidal") -> {
                "Listen on TIDAL" to "https://listen.tidal.com/search?q=$encodedQuery"
            }

            // Deezer
            pkg.contains("deezer") -> {
                "Listen on Deezer" to "https://www.deezer.com/search/$encodedQuery"
            }

            // Amazon Music
            pkg.contains("amazon.mp3") || pkg.contains("amazon.music") -> {
                "Listen on Amazon Music" to "https://music.amazon.com/search/$encodedQuery"
            }

            // YouTube (main video app)
            pkg.contains("youtube") -> {
                val mediaId = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
                val url = if (mediaId != null && mediaId.length == 11 && mediaId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
                    "https://www.youtube.com/watch?v=$mediaId"
                } else {
                    "https://www.youtube.com/results?search_query=$encodedQuery"
                }
                "Watch on YouTube" to url
            }

            // Generic / local music players (e.g. VLC, Poweramp, Musicolet, BlackPlayer, Samsung Music, etc.)
            else -> {
                "Listen on YouTube" to "https://www.youtube.com/results?search_query=$encodedQuery"
            }
        }
    }
}
