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

private fun isValidMediaHeader(name: String, value: String): Boolean {
    val key = name.trim()
    val blocked = setOf("host", "connection", "content-length", "transfer-encoding", "proxy-authorization", "proxy-connection", "upgrade")
    return key.matches(Regex("[A-Za-z0-9-]{1,64}")) &&
        key.lowercase() !in blocked &&
        value.length <= 4096 &&
        !value.contains("\r") && !value.contains("\n")
}

data class MediaCard(
    val id: String, val title: String, val subtitle: String, val category: String,
    val pageUrl: String = "", val streamUrl: String = "", val coverUrl: String = "",
    val description: String = "", val providerName: String = "",
    val pages: List<String> = emptyList(), val textContent: String = "",
    val textUrl: String = "", val childItems: List<MediaCard> = emptyList(),
    val mimeType: String = "", val httpHeaders: Map<String, String> = emptyMap()
) {
    fun toJson() = JSONObject().put("id", id).put("title", title).put("subtitle", subtitle)
        .put("category", category).put("pageUrl", pageUrl).put("streamUrl", streamUrl)
        .put("coverUrl", coverUrl).put("description", description).put("providerName", providerName)
        .put("pages", JSONArray(pages)).put("content", textContent).put("textUrl", textUrl)
        .put("children", JSONArray().apply { childItems.forEach { put(it.toJson()) } })
        .put("mimeType", mimeType).put("headers", JSONObject(httpHeaders))

    companion object {
        fun fromJson(o: JSONObject) = MediaCard(
            o.optString("id", o.optString("title")), o.optString("title", "Untitled"),
            o.optString("subtitle"), o.optString("category", "Anime"), o.optString("pageUrl"),
            o.optString("streamUrl"), o.optString("coverUrl"), o.optString("description"),
            o.optString("providerName"),
            o.optJSONArray("pages")?.let { a -> (0 until a.length()).map { i -> a.optString(i) } } ?: emptyList(),
            o.optString("content"), o.optString("textUrl"),
            o.optJSONArray("children")?.let { a ->
                (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(::fromJson) }
            } ?: emptyList(),
            o.optString("mimeType"),
            o.optJSONObject("headers")?.let { headerObject ->
                headerObject.keys().asSequence().mapNotNull { key ->
                    val value = headerObject.optString(key)
                    if (isValidMediaHeader(key, value)) key to value else null
                }.toMap()
            } ?: emptyMap()
        )
    }
}

data class MediaProvider(
    val id: String, val name: String, val category: String, val url: String,
    val enabled: Boolean = true, val headers: Map<String, String> = emptyMap()
) {
    fun toJson() = JSONObject().put("id", id).put("name", name).put("category", category)
        .put("url", url).put("enabled", enabled).put("headers", JSONObject(headers))
    companion object {
        fun fromJson(o: JSONObject) = MediaProvider(
            o.optString("id"), o.optString("name"), o.optString("category", "All"),
            o.optString("url"), o.optBoolean("enabled", true),
            o.optJSONObject("headers")?.let { objectValue ->
                objectValue.keys().asSequence().mapNotNull { key ->
                    val value = objectValue.optString(key)
                    if (isValidMediaHeader(key, value)) key to value else null
                }.toMap()
            } ?: emptyMap()
        )
    }
}

data class MediaChapter(
    val id: String, val title: String, val subtitle: String = "",
    val scanlationGroups: String = ""
)

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
            "Anime", "Donghua" -> aniList(term, false)
            "Manga", "Manhwa" -> mangaDexSearch(term, category)
            "Movies" -> archiveMovies(term)
            "Series" -> {
                if (tmdbKey.isBlank()) throw IllegalStateException("Add your free TMDB API key in Settings to search TV series. TV catalogue results do not include episode streams; add a compatible provider or open the official source page.")
                tmdb(term, true, tmdbKey)
            }
            "Light Novels" -> gutendex(term)
            else -> emptyList()
        }
    }

    private data class HttpTextResponse(val body: String, val contentType: String)

    private fun requestDetailed(
        url: String, method: String = "GET", body: String? = null,
        headers: Map<String, String> = emptyMap()
    ): HttpTextResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("User-Agent", "AllInOneMedia/0.4 Android")
            connection.setRequestProperty("Accept", "application/json, application/x-mpegURL, audio/x-mpegurl, text/plain, text/html, */*")
            headers.filter { (key, value) -> isValidMediaHeader(key, value) }.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("Service returned HTTP " + code)
            return HttpTextResponse(text, connection.contentType?.substringBefore(";")?.trim()?.lowercase().orEmpty())
        } finally { connection.disconnect() }
    }

    private fun request(url: String, method: String = "GET", body: String? = null): String =
        requestDetailed(url, method, body).body

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

    private fun mangaDexSearch(term: String, category: String): List<MediaCard> {
        val q = URLEncoder.encode(term, "UTF-8")
        val endpoint = "https://api.mangadex.org/manga?title=$q&limit=24" +
            "&includes%5B%5D=cover_art&contentRating%5B%5D=safe&contentRating%5B%5D=suggestive" +
            "&availableTranslatedLanguage%5B%5D=en&order%5Brelevance%5D=desc"
        val root = JSONObject(request(endpoint))
        val list = root.optJSONArray("data") ?: JSONArray()
        return (0 until list.length()).mapNotNull { i ->
            val item = list.optJSONObject(i) ?: return@mapNotNull null
            val id = item.optString("id")
            val attributes = item.optJSONObject("attributes") ?: JSONObject()
            val titles = attributes.optJSONObject("title") ?: JSONObject()
            val title = sequenceOf("en", "ja-ro", "ja").map { titles.optString(it) }
                .firstOrNull { it.isNotBlank() } ?: "Untitled manga"
            val descriptions = attributes.optJSONObject("description") ?: JSONObject()
            val description = sequenceOf("en", "ja-ro", "ja").map { descriptions.optString(it) }
                .firstOrNull { it.isNotBlank() }.orEmpty()
            val relationships = item.optJSONArray("relationships") ?: JSONArray()
            var coverFile = ""
            for (r in 0 until relationships.length()) {
                val rel = relationships.optJSONObject(r) ?: continue
                if (rel.optString("type") == "cover_art") {
                    coverFile = rel.optJSONObject("attributes")?.optString("fileName").orEmpty()
                    if (coverFile.isNotBlank()) break
                }
            }
            val status = attributes.optString("status").replaceFirstChar { it.uppercase() }
            val year = attributes.optInt("year")
            MediaCard(
                id = "mangadex-$id", title = title,
                subtitle = listOf(category, status.takeIf { it.isNotBlank() }, year.takeIf { it > 0 }?.toString())
                    .filterNotNull().joinToString(" • "),
                category = category, pageUrl = "https://mangadex.org/title/$id",
                coverUrl = if (coverFile.isNotBlank()) "https://uploads.mangadex.org/covers/$id/$coverFile.256.jpg" else "",
                description = description, providerName = "MangaDex"
            )
        }
    }

    private fun gutendex(term: String): List<MediaCard> {
        val q = URLEncoder.encode(term, "UTF-8")
        val root = JSONObject(request("https://gutendex.com/books/?search=$q&languages=en&mime_type=text%2F"))
        val list = root.optJSONArray("results") ?: JSONArray()
        return (0 until list.length()).mapNotNull { i ->
            val book = list.optJSONObject(i) ?: return@mapNotNull null
            val formats = book.optJSONObject("formats") ?: JSONObject()
            val textUrl = formats.keys().asSequence()
                .filter { it.startsWith("text/plain", true) }
                .map { formats.optString(it) }
                .firstOrNull { it.startsWith("https://", true) }.orEmpty()
            val authors = book.optJSONArray("authors") ?: JSONArray()
            val author = (0 until authors.length()).mapNotNull { authors.optJSONObject(it)?.optString("name") }
                .filter { it.isNotBlank() }.joinToString(", ")
            val subjects = book.optJSONArray("subjects") ?: JSONArray()
            val subject = (0 until subjects.length()).map { subjects.optString(it) }.take(2).joinToString(" • ")
            val id = book.optInt("id")
            val image = formats.optString("image/jpeg")
            MediaCard(
                id = "gutenberg-$id", title = book.optString("title", "Untitled"),
                subtitle = author.ifBlank { "Project Gutenberg" }, category = "Light Novels",
                pageUrl = "https://www.gutenberg.org/ebooks/$id",
                coverUrl = image, description = subject, providerName = "Project Gutenberg",
                textUrl = textUrl
            )
        }
    }

    private fun archiveMovies(term: String): List<MediaCard> {
        val query = URLEncoder.encode(
            "mediatype:movies AND title:($term) AND licenseurl:http*publicdomain*",
            "UTF-8"
        )
        val endpoint = "https://archive.org/advancedsearch.php?q=$query&fl%5B%5D=identifier" +
            "&fl%5B%5D=title&fl%5B%5D=description&fl%5B%5D=year&fl%5B%5D=licenseurl&rows=24&page=1&output=json"
        val root = JSONObject(request(endpoint))
        val docs = root.optJSONObject("response")?.optJSONArray("docs") ?: JSONArray()
        return (0 until docs.length()).mapNotNull { i ->
            val item = docs.optJSONObject(i) ?: return@mapNotNull null
            val id = item.optString("identifier")
            if (id.isBlank()) return@mapNotNull null
            val year = item.optString("year")
            MediaCard(
                id = "archive-$id", title = item.optString("title", id),
                subtitle = "Internet Archive" + if (year.isNotBlank()) " • $year" else "",
                category = "Movies", pageUrl = "https://archive.org/details/$id",
                coverUrl = "https://archive.org/services/img/$id",
                description = (
                    item.optString("description").replace(Regex("<[^>]*>"), " ").take(320) +
                        if (item.optString("licenseurl").isNotBlank()) "\n\nLicence: " + item.optString("licenseurl") else
                        "\n\nSearch filtered to Internet Archive records marked public domain."
                    ),
                providerName = "Internet Archive"
            )
        }
    }

    suspend fun mangaChapters(mangaId: String): List<MediaChapter> = withContext(Dispatchers.IO) {
        val id = URLEncoder.encode(mangaId, "UTF-8")
        val endpoint = "https://api.mangadex.org/manga/$id/feed?translatedLanguage%5B%5D=en" +
            "&includes%5B%5D=scanlation_group&order%5Bchapter%5D=asc&limit=100"
        val root = JSONObject(request(endpoint))
        val list = root.optJSONArray("data") ?: JSONArray()
        (0 until list.length()).mapNotNull { index ->
            val entry = list.optJSONObject(index) ?: return@mapNotNull null
            val chapterId = entry.optString("id")
            val attr = entry.optJSONObject("attributes") ?: JSONObject()
            if (chapterId.isBlank()) return@mapNotNull null
            val number = attr.optString("chapter").ifBlank { "?" }
            val volume = attr.optString("volume")
            val name = attr.optString("title")
            val label = "Chapter $number" + if (name.isNotBlank()) " — $name" else ""
            val relationships = entry.optJSONArray("relationships") ?: JSONArray()
            val groupNames = (0 until relationships.length()).mapNotNull { relationshipIndex ->
                val relation = relationships.optJSONObject(relationshipIndex) ?: return@mapNotNull null
                if (relation.optString("type") != "scanlation_group") return@mapNotNull null
                relation.optJSONObject("attributes")?.optString("name")?.takeIf { it.isNotBlank() }
            }.distinct().joinToString(" / ")
            MediaChapter(chapterId, label, if (volume.isNotBlank()) "Volume $volume" else "", groupNames)
        }
    }

    suspend fun mangaPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val id = URLEncoder.encode(chapterId, "UTF-8")
        val root = JSONObject(request("https://api.mangadex.org/at-home/server/$id"))
        val chapter = root.optJSONObject("chapter") ?: throw IllegalStateException("MangaDex returned no chapter data.")
        val base = root.optString("baseUrl").trimEnd('/')
        val hash = chapter.optString("hash")
        val files = chapter.optJSONArray("data") ?: chapter.optJSONArray("dataSaver") ?: JSONArray()
        if (base.isBlank() || hash.isBlank() || files.length() == 0) {
            throw IllegalStateException("No readable image pages were provided for this chapter.")
        }
        val folder = if (chapter.has("data") && !chapter.isNull("data")) "data" else "data-saver"
        (0 until files.length()).mapNotNull { index ->
            val file = files.optString(index)
            file.takeIf { it.isNotBlank() }?.let { "$base/$folder/$hash/$it" }
        }
    }

    suspend fun fetchPlainText(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        require(url.startsWith("https://", true)) { "The book text link must use HTTPS." }
        requestDetailed(url, headers = headers).body
    }

    fun parseHeadersJson(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val parsed = JSONTokener(raw).nextValue()
        val obj = parsed as? JSONObject ?: throw IllegalArgumentException("Request headers must be a JSON object.")
        val result = obj.keys().asSequence().mapNotNull { key ->
            val value = obj.optString(key)
            if (isValidMediaHeader(key, value)) key to value else null
        }.toMap()
        if (result.size != obj.length()) throw IllegalArgumentException("One or more header names or values are invalid.")
        return result
    }

    suspend fun archiveStream(identifier: String): String = withContext(Dispatchers.IO) {
        val safeId = URLEncoder.encode(identifier, "UTF-8")
        val root = JSONObject(request("https://archive.org/metadata/$safeId"))
        val files = root.optJSONArray("files") ?: JSONArray()
        val candidates = (0 until files.length()).mapNotNull { index ->
            files.optJSONObject(index)?.takeIf { file ->
                val name = file.optString("name").lowercase()
                name.endsWith(".mp4") && !name.contains("thumb") && !name.contains("sample")
            }
        }
        if (candidates.isEmpty()) throw IllegalStateException("This Internet Archive item has no MP4 stream file available.")
        val selected = candidates.sortedWith(
            compareByDescending<JSONObject> { it.optString("format").contains("mpeg4", true) }
                .thenBy { it.optString("size").toLongOrNull() ?: Long.MAX_VALUE }
        ).first()
        val fileName = selected.optString("name")
        if (fileName.isBlank()) throw IllegalStateException("The archive item has no usable video filename.")
        "https://archive.org/download/$safeId/" + URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
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
                if (value.has("extensions") || value.has("sources")) {
                    throw IllegalStateException(
                        "This URL is an extension repository, not a playable All IN ONE Media catalogue. Mihon, Aniyomi, CloudStream, and LNReader plugins need their own runtime and cannot be installed by pasting the repository URL here. Add a compatible JSON feed, M3U playlist, or direct stream URL instead."
                    )
                }
                val wrapperKeys = listOf(
                    "items", "results", "data", "titles", "entries", "list",
                    "catalog", "catalogue", "manga", "anime", "books", "streams",
                    "response", "payload", "chapters", "episodes", "videos", "movies",
                    "series", "channels"
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

    private fun cardFromProviderEntry(entry: Any?, index: Int, provider: MediaProvider, depth: Int = 0): MediaCard? {
        if (depth > 3) return null
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
            "streamUrl", "stream_url", "playUrl", "play_url", "playbackUrl",
            "playback_url", "videoUrl", "video_url", "stream", "file", "src", "hls", "mp4"
        )
        val genericUrl = field("url", "link", "sourceUrl", "source_url")
        val stream = when {
            rawUrl.isNotBlank() && isHttpsUrl(rawUrl) -> rawUrl
            genericUrl.isNotBlank() && isHttpsUrl(genericUrl) && (isPlayable(genericUrl) || mimeType.isNotBlank()) -> genericUrl
            else -> ""
        }
        val explicitPage = field("pageUrl", "page_url", "webUrl", "web_url", "link", "detailUrl")
        val genericPage = if (genericUrl.isNotBlank() && stream.isBlank()) genericUrl else ""
        val cover = field(
            "coverUrl", "cover_url", "cover", "poster", "posterUrl",
            "poster_url", "thumbnail", "thumb", "image", "imageUrl"
        )
        val pagesValue = firstValue(item, "pages", "pageUrls", "page_urls", "images", "chapterPages", "chapter_pages")
            ?: firstValue(metadata, "pages", "pageUrls", "page_urls", "images")
        val content = field("content", "text", "chapterText", "chapter_text", "body")
        val textUrl = field("textUrl", "text_url", "readerUrl", "reader_url", "textFile", "downloadUrl")
        val mimeType = mimeTypeFromHint(field("mimeType", "mime_type", "streamMimeType", "stream_mime_type", "format"))
        val title = field("title", "name", "label", "mangaTitle", "animeTitle", "bookTitle")
        val itemId = field("id", "slug", "key").ifBlank { provider.id + "-" + index }
        val headersValue = firstValue(item, "headers", "requestHeaders", "request_headers")
            ?: firstValue(metadata, "headers", "requestHeaders", "request_headers")
        val headerObject = headersValue as? JSONObject
        val headers = headerObject?.keys()?.asSequence()?.mapNotNull { key ->
            val value = headerObject.optString(key)
            if (validHeader(key, value)) key to value else null
        }?.toMap() ?: emptyMap()
        val nestedValue = firstValue(item, "episodes", "chapters", "children", "parts")
        val nestedArray = when (nestedValue) {
            is JSONArray -> nestedValue
            is JSONObject -> JSONArray().put(nestedValue)
            else -> JSONArray()
        }
        val children = (0 until nestedArray.length()).mapNotNull { childIndex ->
            cardFromProviderEntry(nestedArray.opt(childIndex), childIndex, provider, depth + 1)
        }

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
            textContent = content,
            textUrl = textUrl,
            childItems = children.map { child ->
                if (child.httpHeaders.isEmpty()) child.copy(httpHeaders = headers) else child
            },
            mimeType = mimeType.ifBlank { guessMimeType(stream) },
            httpHeaders = headers
        )
    }

    suspend fun fetchProvider(provider: MediaProvider): List<MediaCard> = withContext(Dispatchers.IO) {
        val url = provider.url.trim()
        val parsedUrl = runCatching { URL(url) }.getOrNull()
        require(
            url.startsWith("https://", true) &&
                parsedUrl?.host?.isNotBlank() == true
        ) { "Enter a complete HTTPS provider URL." }

        val detectedMime = if (isPlayable(url)) guessMimeType(url) else probeStreamMimeType(url, provider.headers)
        if (isPlayable(url) || detectedMime.isNotBlank()) {
            return@withContext listOf(
                MediaCard(
                    id = "source-" + provider.id,
                    title = provider.name,
                    subtitle = "Direct media source",
                    category = provider.category,
                    streamUrl = url,
                    providerName = provider.name,
                    mimeType = detectedMime.ifBlank { guessMimeType(url) }
                )
            )
        }

        val response = requestDetailed(url, headers = provider.headers)
        if (response.contentType.contains("text/html") || response.body.trimStart().startsWith("<!doctype html", true) ||
            response.body.trimStart().startsWith("<html", true)) {
            val pageTitle = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(response.body)?.groupValues?.getOrNull(1)?.replace(Regex("\\s+"), " ")?.trim()
            return@withContext listOf(
                MediaCard(
                    id = "web-" + provider.id,
                    title = pageTitle?.takeIf { it.isNotBlank() } ?: provider.name,
                    subtitle = "Website source • " + provider.category,
                    category = provider.category,
                    pageUrl = url,
                    description = "This source is a website rather than a JSON or M3U feed. Open it inside the app to browse its available content.",
                    providerName = provider.name
                )
            )
        }
        val text = response.body.removePrefix("\uFEFF").trim()
        if (text.startsWith("#EXTM3U", true)) {
            return@withContext parseM3u(text, provider).map { attachDetectedMimeTypes(it) }
        }

        val payload = parseJsonValue(text)
        val array = arrayFromPayload(payload)
        val parsedCards = (0 until array.length()).mapNotNull { index ->
            cardFromProviderEntry(array.opt(index), index, provider)
        }
        val cards = parsedCards.map { attachDetectedMimeTypes(it) }
        if (cards.isEmpty()) {
            throw IllegalStateException(
                "The source returned valid data, but no title entries could be read. Check that the feed contains title, name, URL, or stream fields."
            )
        }
        cards
    }

    private fun attachDetectedMimeTypes(card: MediaCard): MediaCard {
        val children = card.childItems.map { attachDetectedMimeTypes(it) }
        val detected = if (card.streamUrl.isNotBlank() && card.mimeType.isBlank()) {
            probeStreamMimeType(card.streamUrl, card.httpHeaders)
        } else card.mimeType
        return card.copy(mimeType = detected.ifBlank { guessMimeType(card.streamUrl) }, childItems = children)
    }

    private fun parseM3u(text: String, provider: MediaProvider): List<MediaCard> {
        val result = mutableListOf<MediaCard>()
        var title = provider.name
        var index = 0
        text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.forEach { line ->
            if (line.startsWith("#EXTINF", true)) title = line.substringAfter(",", "Stream").trim().ifBlank { "Stream" }
            else if (line.startsWith("https://", true)) {
                result.add(MediaCard(provider.id + "-" + index++, title, "Playlist • " + provider.name,
                    provider.category, streamUrl = line, providerName = provider.name))
                title = provider.name
            }
        }
        return result
    }

    private fun validHeader(name: String, value: String): Boolean {
        val key = name.trim()
        val blocked = setOf("host", "connection", "content-length", "transfer-encoding", "proxy-authorization", "proxy-connection", "upgrade")
        return key.matches(Regex("[A-Za-z0-9-]{1,64}")) &&
            key.lowercase() !in blocked &&
            value.length <= 4096 &&
            !value.contains("\r") && !value.contains("\n")
    }

    private fun isHttpsUrl(value: String): Boolean =
        value.startsWith("https://", true) && runCatching { URL(value).host.isNotBlank() }.getOrDefault(false)

    private fun guessMimeType(url: String): String {
        val path = url.lowercase().substringBefore("?").substringBefore("#")
        return when {
            path.endsWith(".m3u8") -> "application/x-mpegURL"
            path.endsWith(".mpd") -> "application/dash+xml"
            path.endsWith(".mp4") || path.endsWith(".m4v") -> "video/mp4"
            path.endsWith(".webm") -> "video/webm"
            else -> ""
        }
    }

    private fun probeStreamMimeType(url: String, headers: Map<String, String> = emptyMap()): String {
        if (!isHttpsUrl(url)) return ""
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "HEAD"
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("User-Agent", "AllInOneMedia/0.4 Android")
                headers.filter { (key, value) -> isValidMediaHeader(key, value) }.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }
                val code = connection.responseCode
                val type = connection.contentType?.substringBefore(";")?.trim()?.lowercase().orEmpty()
                if (code in 200..299 && (
                    type.startsWith("video/") ||
                    type.contains("mpegurl") ||
                    type.contains("dash+xml")
                )) type else ""
            } finally { connection.disconnect() }
        } catch (_: Exception) { "" }
    }

    fun isPlayable(url: String): Boolean = guessMimeType(url).isNotBlank()

    fun mimeTypeFromHint(hint: String): String = when (hint.lowercase().substringBefore(";").trim()) {
        "hls", "m3u8", "application/vnd.apple.mpegurl", "application/x-mpegurl" -> "application/x-mpegURL"
        "dash", "mpd", "application/dash+xml" -> "application/dash+xml"
        "mp4", "video/mp4" -> "video/mp4"
        "webm", "video/webm" -> "video/webm"
        else -> hint.takeIf { it.startsWith("video/") } ?: ""
    }
    fun isDownloadable(url: String): Boolean {
        val u = url.lowercase().substringBefore("?")
        return u.endsWith(".mp4") || u.endsWith(".webm") || u.endsWith(".m4v")
    }
}
