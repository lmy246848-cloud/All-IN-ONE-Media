package com.allinonemedia.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MediaCard(
    val id: String, val title: String, val subtitle: String, val category: String,
    val pageUrl: String = "", val streamUrl: String = "", val coverUrl: String = "",
    val description: String = "", val providerName: String = "",
    val pages: List<String> = emptyList(), val textContent: String = ""
) {
    fun toJson() = JSONObject().put("id", id).put("title", title).put("subtitle", subtitle)
        .put("category", category).put("pageUrl", pageUrl).put("streamUrl", streamUrl)
        .put("coverUrl", coverUrl).put("description", description).put("providerName", providerName)
        .put("pages", JSONArray(pages)).put("content", textContent)

    companion object {
        fun fromJson(o: JSONObject) = MediaCard(
            o.optString("id", o.optString("title")), o.optString("title", "Untitled"),
            o.optString("subtitle"), o.optString("category", "Anime"), o.optString("pageUrl"),
            o.optString("streamUrl"), o.optString("coverUrl"), o.optString("description"),
            o.optString("providerName"),
            o.optJSONArray("pages")?.let { a -> (0 until a.length()).map { i -> a.optString(i) } } ?: emptyList(),
            o.optString("content")
        )
    }
}

data class MediaProvider(val id: String, val name: String, val category: String, val url: String, val enabled: Boolean = true) {
    fun toJson() = JSONObject().put("id", id).put("name", name).put("category", category).put("url", url).put("enabled", enabled)
    companion object {
        fun fromJson(o: JSONObject) = MediaProvider(o.optString("id"), o.optString("name"),
            o.optString("category", "All"), o.optString("url"), o.optBoolean("enabled", true))
    }
}

class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("all_in_one_media", Context.MODE_PRIVATE)
    fun library() = readCards("library")
    fun saveLibrary(items: List<MediaCard>) = saveCards("library", items)
    fun sourceItems() = readCards("source_items")
    fun saveSourceItems(items: List<MediaCard>) = saveCards("source_items", items)
    fun providers(): List<MediaProvider> {
        val a = parseArray("providers")
        return (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(MediaProvider::fromJson) }
    }
    fun saveProviders(items: List<MediaProvider>) {
        prefs.edit().putString("providers", JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()).apply()
    }
    fun tmdbKey() = prefs.getString("tmdb_key", "") ?: ""
    fun setTmdbKey(value: String) { prefs.edit().putString("tmdb_key", value.trim()).apply() }
    fun darkTheme() = prefs.getBoolean("dark_theme", true)
    fun setDarkTheme(value: Boolean) { prefs.edit().putBoolean("dark_theme", value).apply() }
    private fun parseArray(key: String) = try { JSONArray(prefs.getString(key, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
    private fun readCards(key: String): List<MediaCard> {
        val a = parseArray(key)
        return (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(MediaCard::fromJson) }
    }
    private fun saveCards(key: String, items: List<MediaCard>) {
        prefs.edit().putString(key, JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()).apply()
    }
}

object CatalogService {
    suspend fun search(category: String, term: String, tmdbKey: String): List<MediaCard> = withContext(Dispatchers.IO) {
        when (category) {
            "Anime", "Donghua", "Manga", "Manhwa" -> aniList(term, category == "Manga" || category == "Manhwa")
            "Movies", "Series" -> {
                if (tmdbKey.isBlank()) throw IllegalStateException("Add your free TMDB API key in Settings to search movies and series.")
                tmdb(term, category == "Series", tmdbKey)
            }
            "Light Novels" -> openLibrary(term)
            else -> emptyList()
        }
    }

    private fun request(url: String, method: String = "GET", body: String? = null): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("User-Agent", "AllInOneMedia/0.2 Android")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("Service returned HTTP " + code)
            return text
        } finally { connection.disconnect() }
    }

    private fun aniList(term: String, manga: Boolean): List<MediaCard> {
        val type = if (manga) "MANGA" else "ANIME"
        val query = "query SearchMedia(\$search: String!, \$type: MediaType!) { Page(perPage: 20) { media(search: \$search, type: \$type, sort: POPULARITY_DESC) { id title { english romaji } description(asHtml: false) siteUrl episodes chapters averageScore format coverImage { large } } } }"
        val payload = JSONObject().put("query", query).put("variables", JSONObject().put("search", term).put("type", type))
        val root = JSONObject(request("https://graphql.anilist.co", "POST", payload.toString()))
        val media = root.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media") ?: JSONArray()
        return (0 until media.length()).mapNotNull { i ->
            val o = media.optJSONObject(i) ?: return@mapNotNull null
            val t = o.optJSONObject("title")
            val name = t?.optString("english")?.takeIf { it.isNotBlank() } ?: t?.optString("romaji")?.takeIf { it.isNotBlank() } ?: "Untitled"
            val count = if (manga) o.optInt("chapters") else o.optInt("episodes")
            val countLabel = if (manga) "chapters" else "episodes"
            val info = listOfNotNull(
                o.optString("format").takeIf { it.isNotBlank() },
                if (count > 0) "$count $countLabel" else null,
                if (o.optInt("averageScore") > 0) "Score " + (o.optInt("averageScore") / 10.0) else null
            ).joinToString(" • ")
            MediaCard(
                id = "anilist-" + o.optInt("id"), title = name,
                subtitle = info.ifBlank { if (manga) "Manga catalogue" else "Anime catalogue" },
                category = if (manga) "Manga" else "Anime", pageUrl = o.optString("siteUrl"),
                coverUrl = o.optJSONObject("coverImage")?.optString("large") ?: "",
                description = o.optString("description").replace(Regex("<[^>]*>"), " ").trim()
            )
        }
    }

    private fun openLibrary(term: String): List<MediaCard> {
        val q = URLEncoder.encode(term, "UTF-8")
        val root = JSONObject(request("https://openlibrary.org/search.json?q=$q&limit=20"))
        val docs = root.optJSONArray("docs") ?: JSONArray()
        return (0 until docs.length()).mapNotNull { i ->
            val o = docs.optJSONObject(i) ?: return@mapNotNull null
            val key = o.optString("key")
            val authors = o.optJSONArray("author_name")
            val author = if (authors != null && authors.length() > 0) authors.optString(0) else "Unknown author"
            val year = o.optInt("first_publish_year")
            val coverId = o.optInt("cover_i")
            MediaCard(
                id = "openlibrary-" + key, title = o.optString("title").ifBlank { "Untitled" },
                subtitle = author + if (year > 0) " • $year" else "", category = "Light Novels",
                pageUrl = if (key.isNotBlank()) "https://openlibrary.org$key" else "",
                coverUrl = if (coverId > 0) "https://covers.openlibrary.org/b/id/$coverId-M.jpg" else ""
            )
        }
    }

    private fun tmdb(term: String, tv: Boolean, key: String): List<MediaCard> {
        val q = URLEncoder.encode(term, "UTF-8")
        val route = if (tv) "tv" else "movie"
        val auth = URLEncoder.encode(key, "UTF-8")
        val root = JSONObject(request("https://api.themoviedb.org/3/search/$route?api_key=$auth&query=$q"))
        val list = root.optJSONArray("results") ?: JSONArray()
        return (0 until list.length()).mapNotNull { i ->
            val o = list.optJSONObject(i) ?: return@mapNotNull null
            val title = o.optString(if (tv) "name" else "title").ifBlank { "Untitled" }
            val date = o.optString(if (tv) "first_air_date" else "release_date").take(4)
            val poster = o.optString("poster_path")
            MediaCard(
                id = "tmdb-" + o.optInt("id"), title = title,
                subtitle = (if (tv) "TV series" else "Movie") + if (date.isNotBlank()) " • $date" else "",
                category = if (tv) "Series" else "Movies",
                pageUrl = "https://www.themoviedb.org/" + route + "/" + o.optInt("id"),
                coverUrl = if (poster.isNotBlank()) "https://image.tmdb.org/t/p/w500$poster" else "",
                description = o.optString("overview")
            )
        }
    }

    suspend fun fetchProvider(provider: MediaProvider): List<MediaCard> = withContext(Dispatchers.IO) {
        val url = provider.url.trim()
        require(url.startsWith("https://")) { "For safety, provider URLs must use HTTPS." }
        if (isPlayable(url)) return@withContext listOf(MediaCard(
            id = "source-" + provider.id, title = provider.name, subtitle = "Direct media source",
            category = provider.category, streamUrl = url, providerName = provider.name
        ))
        val text = request(url)
        if (text.trimStart().startsWith("#EXTM3U")) return@withContext parseM3u(text, provider)
        val array = if (text.trimStart().startsWith("[")) JSONArray(text) else {
            val json = JSONObject(text)
            json.optJSONArray("items") ?: json.optJSONArray("results") ?: JSONArray()
        }
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val stream = o.optString("streamUrl", o.optString("url"))
            MediaCard(
                id = o.optString("id", provider.id + "-" + i),
                title = o.optString("title", o.optString("name")).ifBlank { "Untitled" },
                subtitle = o.optString("subtitle", o.optString("description")),
                category = o.optString("category", provider.category),
                pageUrl = o.optString("pageUrl", o.optString("link")),
                streamUrl = stream.takeIf { isPlayable(it) } ?: "",
                coverUrl = o.optString("coverUrl", o.optString("cover", o.optString("poster"))),
                description = o.optString("description"), providerName = provider.name,
                pages = o.optJSONArray("pages")?.let { a -> (0 until a.length()).map { i -> a.optString(i) } } ?: emptyList(),
                textContent = o.optString("content")
            )
        }
    }

    private fun parseM3u(text: String, provider: MediaProvider): List<MediaCard> {
        val result = mutableListOf<MediaCard>()
        var title = provider.name
        var index = 0
        text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
            if (line.startsWith("#EXTINF", true)) title = line.substringAfter(",", "Stream").trim().ifBlank { "Stream" }
            else if (line.startsWith("https://")) {
                result.add(MediaCard(provider.id + "-" + index++, title, "Playlist • " + provider.name,
                    provider.category, streamUrl = line, providerName = provider.name))
                title = provider.name
            }
        }
        return result
    }

    fun isPlayable(url: String): Boolean {
        val u = url.lowercase().substringBefore("?")
        return u.endsWith(".mp4") || u.endsWith(".m3u8") || u.endsWith(".mpd") || u.endsWith(".webm") || u.endsWith(".m4v")
    }
    fun isDownloadable(url: String): Boolean {
        val u = url.lowercase().substringBefore("?")
        return u.endsWith(".mp4") || u.endsWith(".webm") || u.endsWith(".m4v")
    }
}
