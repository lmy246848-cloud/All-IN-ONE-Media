package com.allinonemedia.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Night = Color(0xFF090812)
private val Panel = Color(0xFF171326)
private val PanelRaised = Color(0xFF211A35)
private val Purple = Color(0xFFB58CFF)
private val Pink = Color(0xFFFF78D2)
private val Aqua = Color(0xFF64E6DB)
private val SoftText = Color(0xFFB8B1C8)
private val categories = listOf("Anime", "Donghua", "Movies", "Series", "Manga", "Manhwa", "Light Novels", "Shorts")

@Composable
fun AllInOneApp() {
    val context = LocalContext.current
    val store = remember { AppStore(context) }
    val scope = rememberCoroutineScope()
    var showSplash by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf("Home") }
    var discoverCategory by remember { mutableStateOf("Anime") }
    var darkTheme by remember { mutableStateOf(store.darkTheme()) }
    var tmdbKey by remember { mutableStateOf(store.tmdbKey()) }
    var providers by remember { mutableStateOf(store.providers()) }
    var library by remember { mutableStateOf(store.library()) }
    var sourceItems by remember { mutableStateOf(store.sourceItems()) }
    var syncingId by remember { mutableStateOf("") }
    var syncMessage by remember { mutableStateOf("") }
    var activeVideo by remember { mutableStateOf<MediaCard?>(null) }
    var activeReader by remember { mutableStateOf<MediaCard?>(null) }

    LaunchedEffect(Unit) { delay(1250); showSplash = false }

    val background = if (darkTheme) Night else Color(0xFFF5F1FB)
    val surface = if (darkTheme) Panel else Color.White
    val foreground = if (darkTheme) Color.White else Color(0xFF211A30)
    val secondary = if (darkTheme) SoftText else Color(0xFF665D73)

    fun toggleLibrary(item: MediaCard) {
        val updated = if (library.any { it.id == item.id }) library.filterNot { it.id == item.id } else listOf(item) + library
        library = updated
        store.saveLibrary(updated)
    }

    fun openExternal(url: String) {
        if (url.isBlank()) return
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) { }
    }

    fun openItem(item: MediaCard) {
        if (item.pages.isNotEmpty() || item.textContent.isNotBlank()) activeReader = item
        else openExternal(item.pageUrl)
    }

    fun syncProvider(provider: MediaProvider) {
        scope.launch {
            syncingId = provider.id
            syncMessage = "Connecting to " + provider.name + "…"
            try {
                val loaded = CatalogService.fetchProvider(provider)
                val updated = sourceItems.filterNot { it.providerName == provider.name } + loaded
                sourceItems = updated
                store.saveSourceItems(updated)
                syncMessage = "Loaded " + loaded.size + " items from " + provider.name
            } catch (e: Exception) {
                syncMessage = "Could not load provider: " + (e.message ?: "network error")
            } finally { syncingId = "" }
        }
    }

    if (showSplash) {
        SplashScreen()
        return
    }

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme(
            primary = Purple, secondary = Aqua, background = Night, surface = Panel,
            onPrimary = Night, onBackground = Color.White, onSurface = Color.White
        ) else lightColorScheme(primary = Color(0xFF7143B8), secondary = Color(0xFF168F87))
    ) {
        Scaffold(
            containerColor = background,
            bottomBar = {
                NavigationBar(containerColor = surface, tonalElevation = 0.dp) {
                    listOf("Home", "Discover", "Library", "Extensions", "Settings").forEach { tab ->
                        val icon = when (tab) {
                            "Home" -> Icons.Filled.Home
                            "Discover" -> Icons.Filled.Explore
                            "Library" -> Icons.Filled.Bookmark
                            "Extensions" -> Icons.Filled.Extension
                            else -> Icons.Filled.Settings
                        }
                        NavigationBarItem(
                            selected = page == tab, onClick = { page = tab },
                            icon = { Icon(icon, contentDescription = tab) },
                            label = { Text(tab, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Purple, selectedTextColor = Purple,
                                indicatorColor = if (darkTheme) PanelRaised else Color(0xFFEFE5FF),
                                unselectedIconColor = secondary, unselectedTextColor = secondary
                            )
                        )
                    }
                }
            }
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets)) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
                            .background(Brush.linearGradient(listOf(Purple, Pink, Color(0xFF6332D8)))),
                        contentAlignment = Alignment.Center
                    ) { Text("A", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ALL IN ONE", color = foreground, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
                        Text("Media", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (page == "Discover") Icon(Icons.Filled.Search, contentDescription = null, tint = secondary)
                }
                AnimatedContent(targetState = page, label = "main-pages") { destination ->
                    when (destination) {
                        "Home" -> HomeScreen(
                            foreground, secondary, surface, library,
                            onCategory = { category -> discoverCategory = category; page = "Discover" },
                            onDiscover = { page = "Discover" }, onLibrary = { page = "Library" }
                        )
                        "Discover" -> DiscoverScreen(
                            initialCategory = discoverCategory, providerItems = sourceItems, tmdbKey = tmdbKey,
                            libraryIds = library.map { it.id }.toSet(),
                            onSave = { toggleLibrary(it) }, onOpen = { openItem(it) },
                            onPlay = { activeVideo = it }, onDownload = { downloadMedia(context, it) }
                        )
                        "Library" -> LibraryScreen(
                            savedItems = library, foreground = foreground, secondary = secondary,
                            surface = surface, onOpen = { openItem(it) },
                            onPlay = { activeVideo = it },
                            onRemove = { item ->
                                library = library.filterNot { it.id == item.id }
                                store.saveLibrary(library)
                            }
                        )
                        "Extensions" -> ProviderManagerScreen(
                            providers = providers, sourceItems = sourceItems, syncingId = syncingId,
                            message = syncMessage, foreground = foreground, secondary = secondary, surface = surface,
                            onAdd = { provider -> providers = listOf(provider) + providers; store.saveProviders(providers) },
                            onRemove = { provider ->
                                providers = providers.filterNot { it.id == provider.id }
                                store.saveProviders(providers)
                                sourceItems = sourceItems.filterNot { it.providerName == provider.name }
                                store.saveSourceItems(sourceItems)
                            },
                            onSync = { syncProvider(it) }, onOpen = { openItem(it) },
                            onPlay = { activeVideo = it }, onDownload = { downloadMedia(context, it) },
                            libraryIds = library.map { it.id }.toSet(), onSave = { toggleLibrary(it) }
                        )
                        "Settings" -> SettingsScreen(
                            keyValue = tmdbKey, darkTheme = darkTheme, foreground = foreground,
                            secondary = secondary, surface = surface,
                            onKeyChange = { tmdbKey = it }, onSaveKey = { store.setTmdbKey(tmdbKey) },
                            onThemeChange = { darkTheme = it; store.setDarkTheme(it) },
                            onDownloads = {
                                try { context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) } catch (_: Exception) { }
                            }
                        )
                        else -> HomeScreen(foreground, secondary, surface, library, {}, { page = "Discover" }, { page = "Library" })
                    }
                }
            }
        }
    }
    activeVideo?.let { item -> VideoPlayerDialog(item = item, onDismiss = { activeVideo = null }) }
    activeReader?.let { item -> ReaderDialog(item = item, onDismiss = { activeReader = null }) }
}

@Composable
private fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash-rotation")
    val rotation = transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "rotation"
    )
    Box(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF090511), Color(0xFF21103A), Color(0xFF090812)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(116.dp).graphicsLayer { rotationZ = rotation.value }
                    .border(3.dp, Brush.sweepGradient(listOf(Purple, Pink, Aqua, Purple)), CircleShape)
                    .padding(9.dp).border(1.dp, Purple.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("A", color = Color.White, fontSize = 62.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.height(24.dp))
            Text("ALL IN ONE", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp)
            Text("MEDIA", color = Purple, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
            Spacer(Modifier.height(12.dp))
            Text("Watch  •  Read  •  Explore", color = SoftText, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(color = Pink, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            Text("Preparing your world…", color = SoftText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HomeScreen(
    foreground: Color, secondary: Color, surface: Color, library: List<MediaCard>,
    onCategory: (String) -> Unit, onDiscover: () -> Unit, onLibrary: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF472080), Color(0xFF201338), Color(0xFF111020))))
                    .padding(20.dp)
            ) {
                Column {
                    Text("YOUR ENTERTAINMENT HUB", color = Aqua, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Everything you love.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text("All in one place.", color = Color(0xFFE7CFFF), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Anime • Donghua • Movies • Manga • Novels • Shorts", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onDiscover, colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Night)) {
                        Icon(Icons.Filled.Explore, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Explore library", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Explore categories", color = foreground, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDiscover) { Text("See all", color = Purple) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                categories.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        pair.forEach { name ->
                            CategoryTile(name, surface, foreground, secondary, Modifier.weight(1f)) { onCategory(name) }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your library", color = foreground, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onLibrary) { Text("View all", color = Purple) }
            }
        }
        if (library.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Purple, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Your saved titles appear here", color = foreground, fontWeight = FontWeight.SemiBold)
                        Text("Save anime, books, films, and streams as you explore.", color = secondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(library.take(4), key = { it.id }) { item -> CompactLibraryRow(item, foreground, secondary, surface) }
        }
    }
}

@Composable
private fun CategoryTile(
    name: String, surface: Color, foreground: Color, secondary: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val icon = when (name) {
        "Anime", "Donghua" -> Icons.Filled.PlayArrow
        "Movies" -> Icons.Filled.Movie
        "Series" -> Icons.Filled.Tv
        "Manga", "Manhwa", "Light Novels" -> Icons.Filled.MenuBook
        else -> Icons.Filled.PlayArrow
    }
    Card(modifier = modifier.height(83.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(Purple.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = foreground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(when (name) {
                    "Light Novels" -> "Books & stories"
                    "Shorts" -> "Short-form video"
                    "Donghua" -> "Chinese animation"
                    else -> "Discover titles"
                }, color = secondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DiscoverScreen(
    initialCategory: String, providerItems: List<MediaCard>, tmdbKey: String, libraryIds: Set<String>,
    onSave: (MediaCard) -> Unit, onOpen: (MediaCard) -> Unit,
    onPlay: (MediaCard) -> Unit, onDownload: (MediaCard) -> Unit
) {
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var term by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<MediaCard>()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Search the catalogues or add a source in Extensions.") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Discover", color = MaterialTheme.colorScheme.onBackground, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text("Find something worth watching or reading.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = term, onValueChange = { term = it }, modifier = Modifier.weight(1f),
                singleLine = true, placeholder = { Text("Search titles…") }, shape = RoundedCornerShape(15.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (term.isNotBlank()) {
                        loading = true
                        message = "Searching " + category + "…"
                        scope.launch {
                            try {
                                results = CatalogService.search(category, term.trim(), tmdbKey)
                                message = "Found " + results.size + " results."
                            } catch (e: Exception) {
                                results = emptyList()
                                message = e.message ?: "Search failed. Check your connection."
                            } finally { loading = false }
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Icon(Icons.Filled.Search, contentDescription = "Search") }
        }
        Spacer(Modifier.height(9.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(categories) { name -> FilterChip(selected = category == name, onClick = { category = name; results = emptyList() }, label = { Text(name) }) }
        }
        Text(message, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
        if (loading) {
            Column { repeat(3) { LoadingCard() } }
        } else if (category == "Shorts") {
            val shorts = providerItems.filter { it.category.equals("Shorts", true) || it.category.equals("All", true) }
            if (shorts.isEmpty()) {
                Text("Your short-drama feed is ready for a source. Add a Shorts JSON feed, M3U playlist, or direct MP4/HLS link in Extensions, then tap Sync.", color = SoftText, fontSize = 12.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(shorts, key = { it.id }) { item ->
                        ShortVideoCard(item, libraryIds.contains(item.id), onSave, onOpen, onPlay, onDownload)
                    }
                }
            }
        } else {
            val custom = providerItems.filter {
                it.category.equals(category, true) || it.category.equals("All", true) ||
                    (category == "Donghua" && it.category.equals("Anime", true)) ||
                    (category == "Manhwa" && it.category.equals("Manga", true))
            }
            val displayed = (results + custom).distinctBy { it.id }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(displayed, key = { it.id }) { item ->
                    MediaCardRow(item, libraryIds.contains(item.id), onSave, onOpen, onPlay, onDownload)
                }
            }
        }
    }
}


@Composable
private fun ShortVideoCard(
    item: MediaCard, saved: Boolean, onSave: (MediaCard) -> Unit,
    onOpen: (MediaCard) -> Unit, onPlay: (MediaCard) -> Unit, onDownload: (MediaCard) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            if (item.coverUrl.isNotBlank()) {
                AsyncImage(model = item.coverUrl, contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(13.dp)))
            } else {
                Box(Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF4A2076), Color(0xFF191326)))),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(item.subtitle.ifBlank { item.providerName.ifBlank { "Short drama" } }, color = SoftText, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { if (item.streamUrl.isNotBlank()) onPlay(item) else onOpen(item) }, enabled = item.streamUrl.isNotBlank() || item.pageUrl.isNotBlank()) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (item.streamUrl.isNotBlank()) "Watch" else "Open source")
                }
                IconButton(onClick = { onSave(item) }) {
                    Icon(Icons.Filled.Bookmark, contentDescription = "Save", tint = if (saved) Aqua else SoftText)
                }
                if (CatalogService.isDownloadable(item.streamUrl)) {
                    IconButton(onClick = { onDownload(item) }) {
                        Icon(Icons.Filled.Download, contentDescription = "Download", tint = Aqua)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    val transition = rememberInfiniteTransition(label = "loading")
    val alpha = transition.animateFloat(0.25f, 0.8f, infiniteRepeatable(tween(850), repeatMode = RepeatMode.Reverse), label = "pulse")
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(70.dp, 88.dp).clip(RoundedCornerShape(10.dp)).background(Purple.copy(alpha = alpha.value)))
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(0.68f).height(14.dp).clip(CircleShape).background(Purple.copy(alpha = alpha.value)))
                Box(Modifier.fillMaxWidth(0.45f).height(10.dp).clip(CircleShape).background(SoftText.copy(alpha = alpha.value)))
                Box(Modifier.fillMaxWidth(0.82f).height(10.dp).clip(CircleShape).background(SoftText.copy(alpha = alpha.value)))
            }
        }
    }
}

@Composable
private fun MediaCardRow(
    item: MediaCard, saved: Boolean, onSave: (MediaCard) -> Unit,
    onOpen: (MediaCard) -> Unit, onPlay: (MediaCard) -> Unit, onDownload: (MediaCard) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.coverUrl.isNotBlank()) {
                AsyncImage(model = item.coverUrl, contentDescription = item.title, modifier = Modifier.size(76.dp, 98.dp).clip(RoundedCornerShape(11.dp)))
            } else {
                Box(Modifier.size(76.dp, 98.dp).clip(RoundedCornerShape(11.dp)).background(PanelRaised), contentAlignment = Alignment.Center) {
                    Text(item.title.take(1).uppercase(), color = Purple, fontSize = 29.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.subtitle.ifBlank { item.category }, color = SoftText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.description.isNotBlank()) {
                    Text(item.description, color = SoftText.copy(alpha = 0.82f), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSave(item) }) {
                        Icon(Icons.Filled.Bookmark, contentDescription = "Save to library", tint = if (saved) Aqua else SoftText)
                    }
                    if (item.streamUrl.isNotBlank()) {
                        IconButton(onClick = { onPlay(item) }) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Purple) }
                    } else {
                        IconButton(onClick = { onOpen(item) }, enabled = item.pageUrl.isNotBlank() || item.pages.isNotEmpty() || item.textContent.isNotBlank()) {
                            Icon(Icons.Filled.Explore, contentDescription = "Open details", tint = Purple)
                        }
                    }
                    if (CatalogService.isDownloadable(item.streamUrl)) {
                        IconButton(onClick = { onDownload(item) }) { Icon(Icons.Filled.Download, contentDescription = "Download", tint = Aqua) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    savedItems: List<MediaCard>, foreground: Color, secondary: Color, surface: Color,
    onOpen: (MediaCard) -> Unit, onPlay: (MediaCard) -> Unit, onRemove: (MediaCard) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("My Library", color = foreground, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text(savedItems.size.toString() + " saved titles and sources", color = secondary, fontSize = 12.sp)
        Spacer(Modifier.height(11.dp))
        if (savedItems.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Bookmark, contentDescription = null, tint = Purple, modifier = Modifier.size(30.dp))
                    Text("Your library is empty", color = foreground, fontWeight = FontWeight.Bold)
                    Text("Use the bookmark icon on any title to save it here.", color = secondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(savedItems, key = { it.id }) { item ->
                    MediaCardRow(item, true, { onRemove(item) }, onOpen, onPlay, {})
                }
            }
        }
    }
}

@Composable
private fun CompactLibraryRow(item: MediaCard, foreground: Color, secondary: Color, surface: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Purple.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Text(item.title.take(1).uppercase(), color = Purple, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = foreground, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.category, color = secondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ProviderManagerScreen(
    providers: List<MediaProvider>, sourceItems: List<MediaCard>, syncingId: String, message: String,
    foreground: Color, secondary: Color, surface: Color,
    onAdd: (MediaProvider) -> Unit, onRemove: (MediaProvider) -> Unit, onSync: (MediaProvider) -> Unit,
    onOpen: (MediaCard) -> Unit, onPlay: (MediaCard) -> Unit, onDownload: (MediaCard) -> Unit,
    libraryIds: Set<String>, onSave: (MediaCard) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var error by remember { mutableStateOf("") }
    val providerTypes = listOf("All", "Anime", "Donghua", "Movies", "Series", "Manga", "Manhwa", "Light Novels", "Shorts")

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Extensions & Providers", color = foreground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("Connect catalogues and media sources you trust.", color = secondary, fontSize = 12.sp)
        Spacer(Modifier.height(11.dp))
        Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Add a provider", color = foreground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Provider name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Content type", color = secondary, fontSize = 11.sp)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(providerTypes) { type -> FilterChip(selected = category == type, onClick = { category = type }, label = { Text(type, fontSize = 10.sp) }) }
                }
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("Provider URL (JSON, M3U, MP4 or HLS)") },
                    placeholder = { Text("https://example.com/provider.json") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Text("Supported feeds: a JSON object with an items array, an M3U playlist, or a direct media URL. Secure HTTPS links only; provider code is never executed.", color = secondary, fontSize = 10.sp)
                if (error.isNotBlank()) Text(error, color = Pink, fontSize = 11.sp)
                Button(
                    onClick = {
                        when {
                            name.isBlank() -> error = "Enter a provider name."
                            !url.startsWith("https://") -> error = "For security, provider URLs must use HTTPS."
                            else -> {
                                onAdd(MediaProvider(System.currentTimeMillis().toString(), name.trim(), category, url.trim()))
                                name = ""; url = ""; error = "Provider added. Tap Sync to load its catalogue."
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)
                ) { Text("Add provider") }
            }
        }
        if (message.isNotBlank()) Text(message, color = secondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 7.dp))
        Text("Your providers (" + providers.size + ")", color = foreground, fontWeight = FontWeight.Bold)
        if (providers.isEmpty()) Text("No custom providers yet. Add a JSON feed, playlist, or direct media URL above.", color = secondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(top = 7.dp, bottom = 15.dp)
        ) {
            items(providers, key = { it.id }) { provider ->
                Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(provider.name, color = foreground, fontWeight = FontWeight.Bold)
                        Text(provider.category + " • " + provider.url, color = secondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { onSync(provider) }, enabled = syncingId != provider.id, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)) {
                                if (syncingId == provider.id) CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (syncingId == provider.id) "Loading" else "Sync")
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { onRemove(provider) }) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = Pink, modifier = Modifier.size(16.dp))
                                Text("Remove", color = Pink)
                            }
                        }
                    }
                }
            }
            if (sourceItems.isNotEmpty()) {
                item { Text("Loaded source items (" + sourceItems.size + ")", color = foreground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)) }
                items(sourceItems, key = { "source-" + it.id }) { item ->
                    MediaCardRow(item, libraryIds.contains(item.id), onSave, onOpen, onPlay, onDownload)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    keyValue: String, darkTheme: Boolean, foreground: Color, secondary: Color, surface: Color,
    onKeyChange: (String) -> Unit, onSaveKey: () -> Unit, onThemeChange: (Boolean) -> Unit, onDownloads: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Settings", color = foreground, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text("Personalise your media experience.", color = secondary, fontSize = 12.sp)
        Spacer(Modifier.height(13.dp))
        Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(17.dp)) {
            Column(Modifier.padding(15.dp)) {
                Text("Appearance", color = foreground, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Dark theme", color = foreground)
                        Text("Use the night-purple interface.", color = secondary, fontSize = 10.sp)
                    }
                    Switch(checked = darkTheme, onCheckedChange = onThemeChange)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(17.dp)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Movie & series catalogue", color = foreground, fontWeight = FontWeight.Bold)
                Text("TMDB requires a free API key. Create one on themoviedb.org and paste the v3 API key here. The app stores it on this device.", color = secondary, fontSize = 11.sp)
                OutlinedTextField(value = keyValue, onValueChange = onKeyChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("TMDB API key") })
                Button(onClick = onSaveKey, modifier = Modifier.fillMaxWidth()) { Text("Save API key") }
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(17.dp)) {
            Column(Modifier.padding(15.dp)) {
                Text("Downloads", color = foreground, fontWeight = FontWeight.Bold)
                Text("Open Android's download manager to view completed and active downloads.", color = secondary, fontSize = 11.sp)
                TextButton(onClick = onDownloads) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Open downloads")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("All IN ONE Media • Version 0.2.0", color = secondary, fontSize = 11.sp)
        Text("Your library, providers, and settings are stored locally on this device.", color = secondary, fontSize = 10.sp)
    }
}


@Composable
private fun ReaderDialog(item: MediaCard, onDismiss: () -> Unit) {
    var pageIndex by remember(item.id) { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF100D19)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (item.pages.isNotEmpty()) "Chapter pages" else "Reading view", color = SoftText, fontSize = 10.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("Close", color = Purple) }
                }
                if (item.pages.isNotEmpty()) {
                    AsyncImage(
                        model = item.pages[pageIndex.coerceIn(item.pages.indices)],
                        contentDescription = "Page " + (pageIndex + 1),
                        modifier = Modifier.fillMaxWidth().height(470.dp).clip(RoundedCornerShape(10.dp))
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        OutlinedButton(onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) }, enabled = pageIndex > 0) { Text("Previous") }
                        Text((pageIndex + 1).toString() + " / " + item.pages.size, color = SoftText, fontSize = 12.sp)
                        Button(onClick = { pageIndex = (pageIndex + 1).coerceAtMost(item.pages.lastIndex) }, enabled = pageIndex < item.pages.lastIndex) { Text("Next") }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().heightIn(min = 250.dp, max = 620.dp).verticalScroll(rememberScrollState()).padding(8.dp)) {
                        Text(item.textContent, color = Color(0xFFF2ECFA), fontSize = 15.sp, lineHeight = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerDialog(item: MediaCard, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val player = remember(item.streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(item.streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(14.dp), shape = RoundedCornerShape(20.dp), color = Color(0xFF100D19)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = onDismiss) { Text("Close", color = Purple) }
                }
                AndroidView(
                    factory = { viewContext -> PlayerView(viewContext).apply { this.player = player; useController = true } },
                    modifier = Modifier.fillMaxWidth().height(245.dp)
                )
                Text("Playback uses the stream URL supplied by your provider.", color = SoftText, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

private fun downloadMedia(context: Context, item: MediaCard) {
    val url = item.streamUrl
    if (!CatalogService.isDownloadable(url)) return
    try {
        val safeName = item.title.replace(Regex("[^A-Za-z0-9._-]"), "_").take(70).ifBlank { "media" }
        val extension = url.substringBefore("?").substringAfterLast(".", "mp4")
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(item.title).setDescription("All IN ONE Media download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true).setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AllINONEMedia_" + safeName + "." + extension)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    } catch (_: Exception) { }
}
