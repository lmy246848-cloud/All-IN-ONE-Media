package com.allinonemedia.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
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
            connection.setRequestProperty("User-Agent", "AllInOneMedia/0.3 Android")
            connection.setRequestProperty("Accept", "application/json, application/x-mpegURL, audio/x-mpegurl, */*")
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

    private fun parseJsonValue(source: String, depth: Int = 0): Any {
        if (depth > 5) throw IllegalStateException("Provider JSON is nested too deeply.")
        val text = source.removePrefix("\uFEFF").trim()
        if (text.isBlank()) throw IllegalStateException("The provider returned an empty response.")
        val value = try {
            JSONTokener(text).nextValue()
        } catch (e: Exception) {
            val preview = text.replace(Regex("\\s+"), " ").take(90)
            throw IllegalStateException(
                "The provider did not return valid JSON. It may be a website page or an unsupported extension link. Response starts: $preview",
                e
            )
        }
        if (value is String) {
            val nested = value.removePrefix("\uFEFF").trim()
            if (nested.startsWith("[") || nested.startsWith("{")) {
                return parseJsonValue(nested, depth + 1)
            }
        }
        return value
    }

    private fun arrayFromPayload(value: Any, depth: Int = 0): JSONArray {
        if (depth > 5) throw IllegalStateException("Provider JSON is nested too deeply.")
        return when (value) {
            is JSONArray -> value
            is JSONObject -> {
                val wrapperKeys = listOf(
                    "items", "results", "data", "titles", "entries", "list",
                    "catalog", "catalogue", "manga", "anime", "books", "streams",
                    "response", "payload", "chapters"
                )
                for (key in wrapperKeys) {
                    if (!value.has(key) || value.isNull(key)) continue
                    val nested = value.opt(key)
                    when (nested) {
                        is JSONArray -> return nested
                        is JSONObject -> {
                            val result = runCatching { arrayFromPayload(nested, depth + 1) }.getOrNull()
                            if (result != null) return result
                        }
                        is String -> {
                            val raw = nested.trim()
                            if (raw.startsWith("[") || raw.startsWith("{")) {
                                val result = runCatching {
                                    arrayFromPayload(parseJsonValue(raw, depth + 1), depth + 1)
                                }.getOrNull()
                                if (result != null) return result
                            }
                        }
                    }
                }
                val recordKeys = listOf(
                    "title", "name", "label", "mangaTitle", "animeTitle",
                    "streamUrl", "stream_url", "url", "pageUrl", "link"
                )
                if (recordKeys.any { value.has(it) }) JSONArray().put(value)
                else throw IllegalStateException(
                    "The JSON response has no title list. Expected an array or an object containing items, results, data, titles, entries, or list."
                )
            }
            is String -> {
                val raw = value.trim()
                if (raw.startsWith("[") || raw.startsWith("{")) {
                    arrayFromPayload(parseJsonValue(raw, depth + 1), depth + 1)
                } else {
                    JSONArray().put(raw)
                }
            }
            else -> throw IllegalStateException("Unsupported provider response. Expected a JSON array, JSON object, or M3U playlist.")
        }
    }

    private fun firstValue(source: JSONObject?, vararg keys: String): Any? {
        if (source == null) return null
        for (key in keys) {
            if (source.has(key) && !source.isNull(key)) return source.opt(key)
        }
        return null
    }

    private fun firstText(source: JSONObject?, vararg keys: String): String {
        if (source == null) return ""
        for (key in keys) {
            val value = firstValue(source, key) ?: continue
            when (value) {
                is String -> if (value.isNotBlank()) return value.trim()
                is Number, is Boolean -> return value.toString()
                is JSONObject -> {
                    val nested = firstText(value, "url", "src", "href", "name", "title", "text")
                    if (nested.isNotBlank()) return nested
                }
                is JSONArray -> if (value.length() > 0) {
                    val first = value.opt(0)
                    if (first is String && first.isNotBlank()) return first
                }
            }
        }
        return ""
    }

    private fun stringList(value: Any?): List<String> {
        return when (value) {
            is JSONArray -> (0 until value.length()).mapNotNull { index ->
                when (val item = value.opt(index)) {
                    is String -> item.trim().takeIf { it.isNotBlank() }
                    is JSONObject -> firstText(item, "url", "src", "href", "image", "page", "path")
                        .takeIf { it.isNotBlank() }
                    else -> null
                }
            }
            is String -> {
                val raw = value.trim()
                if (raw.isBlank()) emptyList()
                else if (raw.startsWith("[")) {
                    runCatching {
                        val parsed = parseJsonValue(raw)
                        if (parsed is JSONArray) stringList(parsed) else listOf(raw)
                    }.getOrDefault(listOf(raw))
                } else listOf(raw)
            }
            is JSONObject -> listOf(firstText(value, "url", "src", "href", "image", "page", "path")).filter { it.isNotBlank() }
            else -> emptyList()
        }
    }

    private fun cardFromProviderEntry(entry: Any?, index: Int, provider: MediaProvider): MediaCard? {
        if (entry is String) {
            val raw = entry.trim()
            if (raw.isBlank()) return null
            val playable = raw.takeIf { isPlayable(it) } ?: ""
            val isHttpsPage = raw.startsWith("https://", true)
            val pathTitle = runCatching {
                URL(raw).path.substringAfterLast('/').substringBefore('?').ifBlank { URL(raw).host }
            }.getOrDefault("")
            return MediaCard(
                id = provider.id + "-" + index,
                title = if (isHttpsPage) pathTitle.ifBlank { "Title " + (index + 1) } else raw,
                subtitle = if (playable.isNotBlank()) "Direct media source" else provider.name,
                category = provider.category,
                pageUrl = if (isHttpsPage && playable.isBlank()) raw else "",
                streamUrl = playable,
                providerName = provider.name
            )
        }
        val item = entry as? JSONObject ?: return null
        val metadata = item.optJSONObject("attributes")
            ?: item.optJSONObject("metadata")
            ?: item.optJSONObject("meta")
        fun field(vararg keys: String): String {
            return firstText(item, *keys).ifBlank { firstText(metadata, *keys) }
        }

        val rawUrl = field(
            "streamUrl", "stream_url", "playUrl", "play_url", "videoUrl",
            "video_url", "file", "src", "hls", "mp4"
        )
        val genericUrl = field("url", "link", "sourceUrl", "source_url")
        val stream = (rawUrl.ifBlank { genericUrl }).takeIf { isPlayable(it) } ?: ""
        val explicitPage = field("pageUrl", "page_url", "webUrl", "web_url", "link", "detailUrl")
        val genericPage = if (genericUrl.isNotBlank() && stream.isBlank()) genericUrl else ""
        val cover = field(
            "coverUrl", "cover_url", "cover", "poster", "posterUrl",
            "poster_url", "thumbnail", "thumb", "image", "imageUrl"
        )
        val pagesValue = firstValue(item, "pages", "pageUrls", "page_urls", "images", "chapterPages", "chapter_pages")
            ?: firstValue(metadata, "pages", "pageUrls", "page_urls", "images")
        val content = field("content", "text", "chapterText", "chapter_text", "body")
        val title = field("title", "name", "label", "mangaTitle", "animeTitle", "bookTitle")
        val itemId = field("id", "slug", "key").ifBlank { provider.id + "-" + index }

        return MediaCard(
            id = itemId,
            title = title.ifBlank { "Untitled " + (index + 1) },
            subtitle = field("subtitle", "description", "chapter", "episode", "author"),
            category = field("category", "type", "contentType", "content_type", "mediaType")
                .ifBlank { provider.category },
            pageUrl = explicitPage.ifBlank { genericPage },
            streamUrl = stream,
            coverUrl = cover,
            description = field("summary", "description", "overview"),
            providerName = provider.name,
            pages = stringList(pagesValue).filter { it.startsWith("https://", true) },
            textContent = content
        )
    }

    suspend fun fetchProvider(provider: MediaProvider): List<MediaCard> = withContext(Dispatchers.IO) {
        val url = provider.url.trim()
        val parsedUrl = runCatching { URL(url) }.getOrNull()
        require(
            url.startsWith("https://", true) &&
                parsedUrl?.host?.isNotBlank() == true
        ) { "Enter a complete HTTPS provider URL." }

        if (isPlayable(url)) {
            return@withContext listOf(
                MediaCard(
                    id = "source-" + provider.id,
                    title = provider.name,
                    subtitle = "Direct media source",
                    category = provider.category,
                    streamUrl = url,
                    providerName = provider.name
                )
            )
        }

        val text = request(url).removePrefix("\uFEFF").trim()
        if (text.startsWith("#EXTM3U", true)) {
            return@withContext parseM3u(text, provider)
        }

        val payload = parseJsonValue(text)
        val array = arrayFromPayload(payload)
        val cards = (0 until array.length()).mapNotNull { index ->
            cardFromProviderEntry(array.opt(index), index, provider)
        }
        if (cards.isEmpty()) {
            throw IllegalStateException(
                "The source returned valid data, but no title entries could be read. Check that the feed contains title, name, URL, or stream fields."
            )
        }
        cards
    }

    private fun parseM3u(text: String, provider: MediaProvider): List<MediaCard> {
        val result = mutableListOf<MediaCard>()
        var title = provider.name
        var index = 0
        text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
            if (line.startsWith("#EXTINF", true)) title = line.substringAfter(",", "Stream").trim().ifBlank { "Stream" }
            else if (line.startsWith("https://", true) && isPlayable(line)) {
                result.add(MediaCard(provider.id + "-" + index++, title, "Playlist • " + provider.name,
                    provider.category, streamUrl = line, providerName = provider.name))
                title = provider.name
            }
        }
        return result
    }

    fun isPlayable(url: String): Boolean {
        val u = url.lowercase().substringBefore("?").substringBefore("#")
        return u.endsWith(".mp4") || u.endsWith(".m3u8") || u.endsWith(".mpd") || u.endsWith(".webm") || u.endsWith(".m4v")
    }
    fun isDownloadable(url: String): Boolean {
        val u = url.lowercase().substringBefore("?")
        return u.endsWith(".mp4") || u.endsWith(".webm") || u.endsWith(".m4v")
    }
}
