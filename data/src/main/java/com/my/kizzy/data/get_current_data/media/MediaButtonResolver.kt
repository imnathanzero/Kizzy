package com.my.kizzy.data.get_current_data.media

import android.media.MediaMetadata
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object MediaButtonResolver {
    private val urlCache = ConcurrentHashMap<String, Pair<String, String>>()

    private val YOUTUBE_VIDEO_ID_REGEX = Regex("\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\"")
    private val YOUTUBE_IMG_REGEX = Regex("(?:vi|vi_webp)/([a-zA-Z0-9_-]{11})")
    private val ITUNES_TRACK_URL_REGEX = Regex("\"trackViewUrl\"\\s*:\\s*\"([^\"]+)\"")
    private val DEEZER_TRACK_URL_REGEX = Regex("\"link\"\\s*:\\s*\"([^\"]+)\"")
    private val SOUNDCLOUD_HREF_REGEX = Regex("href=\"/([a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+)\"")
    private val SOUNDCLOUD_EXCLUDED_PATHS = setOf(
        "search", "pages", "terms", "jobs", "imprint", "settings",
        "messages", "notifications", "discover", "upload", "stream", "you", "popular"
    )

    fun resolve(
        packageName: String,
        title: String,
        artist: String?,
        metadata: MediaMetadata?
    ): Pair<String, String>? {
        if (title.isBlank()) return null

        val query = if (!artist.isNullOrBlank()) "$title $artist" else title
        val pkg = packageName.lowercase()
        val cacheKey = "$pkg:$query"

        urlCache[cacheKey]?.let { return it }

        // 1. Instant local extraction from metadata (0ms, no network)
        val localResolved = resolveFromMetadata(pkg, metadata, query)
        if (localResolved != null) {
            cacheResult(cacheKey, localResolved)
            return localResolved
        }

        // 2. Direct network resolution (fetch direct watch / track URL)
        val networkResolved = resolveFromNetwork(pkg, query)
        if (networkResolved != null) {
            cacheResult(cacheKey, networkResolved)
            return networkResolved
        }

        // 3. Graceful fallback to platform search query URL
        val fallback = resolveFallbackSearch(pkg, query)
        if (fallback != null) {
            cacheResult(cacheKey, fallback)
        }
        return fallback
    }

    private fun cacheResult(key: String, result: Pair<String, String>) {
        if (urlCache.size > 200) {
            urlCache.clear()
        }
        urlCache[key] = result
    }

    private fun resolveFromMetadata(
        pkg: String,
        metadata: MediaMetadata?,
        query: String
    ): Pair<String, String>? {
        if (metadata == null) return null

        val mediaId = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
            ?: metadata.description?.mediaId
        val mediaUri = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
            ?: metadata.description?.mediaUri?.toString()
        val artUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
            ?: metadata.description?.iconUri?.toString()

        when {
            // YouTube Music
            pkg.contains("youtube.music") -> {
                val videoId = extractYouTubeVideoId(mediaId, mediaUri, artUri)
                if (videoId != null) {
                    return "Listen on YouTube Music" to "https://music.youtube.com/watch?v=$videoId"
                }
            }

            // YouTube
            pkg.contains("youtube") -> {
                val videoId = extractYouTubeVideoId(mediaId, mediaUri, artUri)
                if (videoId != null) {
                    return "Watch on YouTube" to "https://www.youtube.com/watch?v=$videoId"
                }
            }

            // Spotify
            pkg.contains("spotify") -> {
                val spotifyUri = mediaUri?.takeIf { it.startsWith("spotify:track:") }
                    ?: mediaId?.takeIf { it.startsWith("spotify:track:") }
                if (spotifyUri != null) {
                    val trackId = spotifyUri.removePrefix("spotify:track:")
                    return "Listen on Spotify" to "https://open.spotify.com/track/$trackId"
                }
                if (mediaId != null && mediaId.length == 22 && mediaId.matches(Regex("^[a-zA-Z0-9]{22}$"))) {
                    return "Listen on Spotify" to "https://open.spotify.com/track/$mediaId"
                }
            }

            // SoundCloud
            pkg.contains("soundcloud") -> {
                if (mediaUri != null && mediaUri.startsWith("https://soundcloud.com/") && !mediaUri.contains("/search")) {
                    return "Listen on SoundCloud" to mediaUri
                }
            }
        }

        return null
    }

    private fun extractYouTubeVideoId(
        mediaId: String?,
        mediaUri: String?,
        artUri: String?
    ): String? {
        if (mediaId != null && mediaId.length == 11 && mediaId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return mediaId
        }
        if (mediaUri != null) {
            val match = Regex("(?:v=|youtu\\.be/)([a-zA-Z0-9_-]{11})").find(mediaUri)
            if (match != null) return match.groupValues[1]
        }
        if (artUri != null) {
            val match = YOUTUBE_IMG_REGEX.find(artUri)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun resolveFromNetwork(pkg: String, query: String): Pair<String, String>? {
        val encodedQuery = encodeQuery(query)

        return when {
            // YouTube Music: Resolve video ID from YouTube search HTML
            pkg.contains("youtube.music") -> {
                val videoId = fetchYouTubeVideoId(encodedQuery)
                if (videoId != null) {
                    "Listen on YouTube Music" to "https://music.youtube.com/watch?v=$videoId"
                } else null
            }

            // YouTube (main video): Resolve video ID from YouTube search HTML
            pkg.contains("youtube") -> {
                val videoId = fetchYouTubeVideoId(encodedQuery)
                if (videoId != null) {
                    "Watch on YouTube" to "https://www.youtube.com/watch?v=$videoId"
                } else null
            }

            // Apple Music: Resolve via free iTunes Search API
            pkg.contains("apple.android.music") || pkg.contains("music.apple") -> {
                val trackUrl = fetchAppleMusicTrackUrl(encodedQuery)
                if (trackUrl != null) {
                    "Listen on Apple Music" to trackUrl
                } else null
            }

            // Deezer: Resolve via free Deezer Search API
            pkg.contains("deezer") -> {
                val trackUrl = fetchDeezerTrackUrl(encodedQuery)
                if (trackUrl != null) {
                    "Listen on Deezer" to trackUrl
                } else null
            }

            // SoundCloud: Resolve top track slug from search HTML
            pkg.contains("soundcloud") -> {
                val trackUrl = fetchSoundCloudTrackUrl(encodedQuery)
                if (trackUrl != null) {
                    "Listen on SoundCloud" to trackUrl
                } else null
            }

            // Other / local players: Resolve exact watch link on YouTube
            else -> {
                val videoId = fetchYouTubeVideoId(encodedQuery)
                if (videoId != null) {
                    "Listen on YouTube" to "https://www.youtube.com/watch?v=$videoId"
                } else null
            }
        }
    }

    private fun resolveFallbackSearch(pkg: String, query: String): Pair<String, String>? {
        val encodedQuery = encodeQuery(query)

        return when {
            pkg.contains("youtube.music") -> "Listen on YouTube Music" to "https://music.youtube.com/search?q=$encodedQuery"
            pkg.contains("spotify") -> "Listen on Spotify" to "https://open.spotify.com/search/$encodedQuery"
            pkg.contains("soundcloud") -> "Listen on SoundCloud" to "https://soundcloud.com/search?q=$encodedQuery"
            pkg.contains("apple.android.music") || pkg.contains("music.apple") -> "Listen on Apple Music" to "https://music.apple.com/search?term=$encodedQuery"
            pkg.contains("tidal") -> "Listen on TIDAL" to "https://listen.tidal.com/search?q=$encodedQuery"
            pkg.contains("deezer") -> "Listen on Deezer" to "https://www.deezer.com/search/$encodedQuery"
            pkg.contains("amazon.mp3") || pkg.contains("amazon.music") -> "Listen on Amazon Music" to "https://music.amazon.com/search/$encodedQuery"
            pkg.contains("youtube") -> "Watch on YouTube" to "https://www.youtube.com/results?search_query=$encodedQuery"
            else -> "Listen on YouTube" to "https://www.youtube.com/results?search_query=$encodedQuery"
        }
    }

    private fun fetchYouTubeVideoId(encodedQuery: String): String? {
        val html = fetchText("https://www.youtube.com/results?search_query=$encodedQuery", timeoutMs = 2500)
            ?: return null
        return YOUTUBE_VIDEO_ID_REGEX.find(html)?.groupValues?.get(1)
    }

    private fun fetchAppleMusicTrackUrl(encodedQuery: String): String? {
        val json = fetchText("https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=1", timeoutMs = 2000)
            ?: return null
        val rawUrl = ITUNES_TRACK_URL_REGEX.find(json)?.groupValues?.get(1) ?: return null
        return rawUrl.replace("\\/", "/")
    }

    private fun fetchDeezerTrackUrl(encodedQuery: String): String? {
        val json = fetchText("https://api.deezer.com/search?q=$encodedQuery&limit=1", timeoutMs = 2000)
            ?: return null
        val rawUrl = DEEZER_TRACK_URL_REGEX.find(json)?.groupValues?.get(1) ?: return null
        return rawUrl.replace("\\/", "/")
    }

    private fun fetchSoundCloudTrackUrl(encodedQuery: String): String? {
        val html = fetchText("https://soundcloud.com/search?q=$encodedQuery", timeoutMs = 2500)
            ?: return null
        val matches = SOUNDCLOUD_HREF_REGEX.findAll(html)
        for (match in matches) {
            val path = match.groupValues[1]
            val firstSegment = path.substringBefore("/")
            if (firstSegment !in SOUNDCLOUD_EXCLUDED_PATHS) {
                return "https://soundcloud.com/$path"
            }
        }
        return null
    }

    private fun fetchText(urlStr: String, timeoutMs: Int): String? {
        return try {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "*/*")
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeQuery(query: String): String {
        return try {
            URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query.replace(" ", "+")
        }
    }
}
