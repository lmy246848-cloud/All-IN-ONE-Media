package com.allinonemedia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val bg = Color(0xFF101018)
private val panel = Color(0xFF20202D)
private val accent = Color(0xFFB69CFF)
private val muted = Color(0xFFAAA8B8)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { MediaApp() }
 }
}

@Composable
fun MediaApp() {
 var page by remember { mutableStateOf("Home") }
 var search by remember { mutableStateOf("") }
 var status by remember { mutableStateOf("Search the public AniList catalogue.") }
 var loading by remember { mutableStateOf(false) }
 var results by remember { mutableStateOf(listOf<TitleItem>()) }
 val scope = rememberCoroutineScope()
 MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = bg, surface = panel)) {
  Scaffold(containerColor = bg, bottomBar = {
   NavigationBar(containerColor = panel) {
    listOf("Home", "Anime", "Manga", "Shorts", "Library").forEach { tab ->
     NavigationBarItem(selected = page == tab, onClick = { page = tab }, icon = {
      Icon(when(tab) { "Home" -> Icons.Default.Home; "Anime" -> Icons.Default.PlayCircle; "Manga" -> Icons.Default.MenuBook; "Shorts" -> Icons.Default.VideoLibrary; else -> Icons.Default.Bookmark }, contentDescription = tab)
     }, label = { Text(tab) })
    }
   }
  }) { inset ->
   Column(Modifier.fillMaxSize().background(bg).padding(inset).padding(18.dp)) {
    Text("ALL IN ONE", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
    Text("Media", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
     listOf("Movies", "Novels", "Settings").forEach { AssistChip(onClick = { page = it }, label = { Text(it) }) }
    }
    Spacer(Modifier.height(16.dp))
    when(page) {
     "Anime", "Manga" -> {
      Text("Discover " + page, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
      Row(Modifier.fillMaxWidth().background(panel, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
       BasicTextField(value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White), decorationBox = { inner -> if(search.isEmpty()) Text("Search titles...", color = muted); inner() })
       IconButton(onClick = {
        if(search.isNotBlank()) {
         loading = true
         scope.launch {
          try { results = aniListSearch(search, page == "Manga"); status = "Found " + results.size + " results." }
          catch(e: Exception) { status = "Search failed: " + (e.message ?: "network error") }
          loading = false
         }
        }
       }) { Icon(Icons.Default.Search, contentDescription = "Search", tint = accent) }
      }
      Text(status, color = muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
      if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
       items(results) { item ->
        Card(colors = CardDefaults.cardColors(containerColor = panel), modifier = Modifier.fillMaxWidth()) {
         Column(Modifier.padding(14.dp)) {
          Text(item.name, color = Color.White, fontWeight = FontWeight.Bold)
          Text(item.info, color = muted, fontSize = 12.sp)
          Text("AniList ID: " + item.id, color = accent, fontSize = 12.sp)
         }
        }
       }
      }
     }
     "Home" -> {
      Text("Your entertainment, in one place.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
      Text("This is an independent rebuild, not the previous Zangetsu rebrand.", color = muted, modifier = Modifier.padding(top = 8.dp))
      Spacer(Modifier.height(14.dp))
      listOf("Anime discovery" to "Live AniList search", "Manga discovery" to "Live AniList search", "Movies & series" to "Not implemented yet", "Short dramas & reels" to "Not implemented yet", "Light novels" to "Not implemented yet").forEach {
       Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = panel)) {
        Column(Modifier.padding(14.dp)) { Text(it.first, color = Color.White, fontWeight = FontWeight.Bold); Text(it.second, color = muted, fontSize = 12.sp) }
       }
      }
     }
     "Shorts" -> InfoPage("Short dramas & reels", "A legal vertical-video feed is planned. No unlicensed streams are bundled.")
     "Movies" -> InfoPage("Movies & series", "Metadata and playback providers are not implemented in this first milestone.")
     "Novels" -> InfoPage("Light novels", "EPUB reading support is planned, not active yet.")
     "Library" -> InfoPage("Library", "Saved titles and progress tracking are planned, not active yet.")
     else -> InfoPage("Settings", "All IN ONE Media • Version 0.1.0")
    }
   }
  }
 }
}

@Composable
fun InfoPage(title: String, body: String) {
 Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
 Text(body, color = muted, modifier = Modifier.padding(top = 8.dp))
}

data class TitleItem(val id: Int, val name: String, val info: String)

suspend fun aniListSearch(term: String, manga: Boolean): List<TitleItem> = withContext(Dispatchers.IO) {
 val mediaType = if(manga) "MANGA" else "ANIME"
 val safe = term.replace("\\", "").replace("\"", "")
 val query = "query { Page(perPage: 20) { media(search: \"" + safe + "\", type: " + mediaType + ", sort: POPULARITY_DESC) { id title { english romaji } status episodes chapters averageScore } } }"
 val conn = (URL("https://graphql.anilist.co").openConnection() as HttpURLConnection)
 try {
  conn.requestMethod = "POST"
  conn.connectTimeout = 15000
  conn.readTimeout = 15000
  conn.doOutput = true
  conn.setRequestProperty("Content-Type", "application/json")
  conn.outputStream.use { it.write(JSONObject().put("query", query).toString().toByteArray()) }
  val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
  val arr = json.getJSONObject("data").getJSONObject("Page").getJSONArray("media")
  (0 until arr.length()).map { i ->
   val obj = arr.getJSONObject(i)
   val title = obj.getJSONObject("title")
   val name = title.optString("english").ifBlank { title.optString("romaji", "Untitled") }
   val count = if(manga) obj.optString("chapters", "—") + " chapters" else obj.optString("episodes", "—") + " episodes"
   TitleItem(obj.getInt("id"), name, count + " • " + obj.optString("status") + " • Score " + obj.optString("averageScore", "—"))
  }
 } finally { conn.disconnect() }
}
