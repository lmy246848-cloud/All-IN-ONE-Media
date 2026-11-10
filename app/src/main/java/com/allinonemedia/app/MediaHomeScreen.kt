package com.allinonemedia.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Night = Color(0xFF090812)
private val Panel = Color(0x6618152B)
private val PanelRaised = Color(0x96271D40)
private val Purple = Color(0xFFB58CFF)
private val Pink = Color(0xFFFF78D2)
private val Aqua = Color(0xFF64E6DB)
private val SoftText = Color(0xFFB8B1C8)
private val categories = listOf("Anime", "Donghua", "Movies", "Series", "Manga", "Manhwa", "Light Novels", "Shorts")

private fun Modifier.liquidGlass(shape: RoundedCornerShape = RoundedCornerShape(18.dp)): Modifier =
    this.clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.045f),
                    Purple.copy(alpha = 0.075f)
                )
            ),
            shape
        )
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.09f),
                    Aqua.copy(alpha = 0.17f)
                )
            ),
            shape
        )

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
    var activeManga by remember { mutableStateOf<MediaCard?>(null) }
    var activeBrowser by remember { mutableStateOf<Triple<String, String, Map<String, String>>?>(null) }

    LaunchedEffect(Unit) { delay(1250); showSplash = false }

    val background = if (darkTheme) Color(0xFF080713) else Color(0xFFF0ECF8)
    val surface = if (darkTheme) Panel else Color(0x99FFFFFF)
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

    fun openInApp(url: String, title: String, headers: Map<String, String> = emptyMap()) {
        if (url.startsWith("https://", true)) activeBrowser = Triple(title, url, headers)
    }

    fun openItem(item: MediaCard) {
        when {
            item.id.startsWith("mangadex-") -> activeManga = item
            item.textUrl.isNotBlank() -> scope.launch {
                activeReader = item.copy(textContent = "Loading book text…")
                try {
                    val text = CatalogService.fetchPlainText(item.textUrl, item.httpHeaders)
                    activeReader = item.copy(textContent = text, description = item.description)
                } catch (e: Exception) {
                    activeReader = item.copy(description = (item.description + "\n\nCould not load the book text: " + (e.message ?: "network error")).trim())
                }
            }
            item.id.startsWith("archive-") -> scope.launch {
                activeReader = item.copy(description = "Finding a playable file in Internet Archive…")
                try {
                    val stream = CatalogService.archiveStream(item.id.removePrefix("archive-"))
                    activeReader = null
                    activeVideo = item.copy(streamUrl = stream, subtitle = "Internet Archive • ready to play")
                } catch (e: Exception) {
                    activeReader = item.copy(description = (item.description + "\n\nCould not prepare playback: " + (e.message ?: "no MP4 stream")).trim())
                }
            }
            else -> {
                if (item.pageUrl.isNotBlank() && item.childItems.isEmpty() && item.pages.isEmpty() &&
                    item.textContent.isBlank() && item.textUrl.isBlank() && item.streamUrl.isBlank()) {
                    openInApp(item.pageUrl, item.title, item.httpHeaders)
                } else {
                    activeReader = item
                }
            }
        }
    }

    fun openChild(item: MediaCard) {
        when {
            item.streamUrl.isNotBlank() -> activeVideo = item
            item.id.startsWith("mangadex-") || item.textUrl.isNotBlank() || item.pages.isNotEmpty() || item.textContent.isNotBlank() -> openItem(item)
            item.pageUrl.startsWith("https://", true) -> openInApp(item.pageUrl, item.title, item.httpHeaders)
            else -> activeReader = item
        }
    }

    fun readMangaChapter(manga: MediaCard, chapter: MediaChapter) {
        activeManga = null
        scope.launch {
            try {
                val pages = CatalogService.mangaPages(chapter.id)
                activeReader = MediaCard(
                    id = "mangadex-chapter-" + chapter.id,
                    title = manga.title + " — " + chapter.title,
                    subtitle = chapter.subtitle,
                    category = manga.category,
                    pages = pages,
                    providerName = "MangaDex",
                    description = "Provided by MangaDex. Scanlation group: " +
                        chapter.scanlationGroups.ifBlank { "not listed in the chapter metadata" } +
                        ". Please credit the creators and scanlation group."
                )
            } catch (e: Exception) {
                activeReader = MediaCard(
                    id = "mangadex-error-" + chapter.id,
                    title = chapter.title,
                    subtitle = "Chapter unavailable",
                    category = manga.category,
                    providerName = "MangaDex",
                    description = "Could not load this chapter: " + (e.message ?: "network error")
                )
            }
        }
    }

    fun syncProvider(provider: MediaProvider) {
        scope.launch {
            syncingId = provider.id
            syncMessage = "Connecting to " + provider.name + "…"
            try {
                val loaded = CatalogService.fetchProvider(provider)
                val updated = sourceItems.filterNot { it.providerId == provider.id || (it.providerId.isBlank() && it.providerName == provider.name) } + loaded
                sourceItems = updated
                store.saveSourceItems(updated)
                syncMessage = if (loaded.isEmpty()) "Connected, but this source returned no titles. Check its feed format and URL." else "Loaded " + loaded.size + " items from " + provider.name
            } catch (e: Exception) {
                syncMessage = "Could not load provider: " + (e.message ?: "network error").take(190)
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
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp)),
                    containerColor = surface.copy(alpha = 0.62f), tonalElevation = 0.dp
                ) {
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
            Column(
                Modifier.fillMaxSize()
                    .background(Brush.linearGradient(listOf(background, if (darkTheme) Color(0xFF1C1230) else Color(0xFFE6DDF4), background)))
                    .drawBehind {
                        val glowRadius = size.minDimension * 0.58f
                        val upperGlowCenter = Offset(size.width * 0.10f, size.height * 0.14f)
                        val lowerGlowCenter = Offset(size.width * 0.92f, size.height * 0.72f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Purple.copy(alpha = if (darkTheme) 0.20f else 0.11f), Color.Transparent),
                                center = upperGlowCenter,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = upperGlowCenter
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Aqua.copy(alpha = if (darkTheme) 0.12f else 0.07f), Color.Transparent),
                                center = lowerGlowCenter,
                                radius = glowRadius * 0.82f
                            ),
                            radius = glowRadius * 0.82f,
                            center = lowerGlowCenter
                        )
                    }
                    .padding(insets)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(13.dp))
                            .background(Brush.linearGradient(listOf(Purple, Pink, Color(0xFF6332D8))))
                            .border(1.dp, Color.White.copy(alpha = 0.42f), RoundedCornerShape(13.dp)),
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
                            foreground, secondary, surface, library, sourceItems,
                            onCategory = { category -> discoverCategory = category; page = "Discover" },
                            onDiscover = { page = "Discover" }, onLibrary = { page = "Library" },
                            onSave = { toggleLibrary(it) }, onOpen = { openItem(it) },
                            onPlay = { activeVideo = it }, onDownload = { downloadMedia(context, it) }
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
                                sourceItems = sourceItems.filterNot { it.providerId == provider.id || (it.providerId.isBlank() && it.providerName == provider.name) }
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
                        else -> HomeScreen(foreground, secondary, surface, library, sourceItems, {}, { page = "Discover" }, { page = "Library" }, { toggleLibrary(it) }, { openItem(it) }, { activeVideo = it }, { downloadMedia(context, it) })
                    }
                }
            }
        }
    }
    activeManga?.let { manga ->
        MangaDetailsDialog(
            item = manga,
            onDismiss = { activeManga = null },
            onReadChapter = { chapter -> readMangaChapter(manga, chapter) }
        )
    }
    activeVideo?.let { item -> VideoPlayerDialog(item = item, onDismiss = { activeVideo = null }) }
    activeReader?.let { item ->
        ReaderDialog(
            item = item,
            onDismiss = { activeReader = null },
            onOpenSource = { url, headers -> openInApp(url, item.title, headers) },
            onOpenChild = { openChild(it) },
            onPlay = { activeVideo = it }
        )
    }
    activeBrowser?.let { browser ->
        InAppBrowserDialog(title = browser.first, url = browser.second, headers = browser.third, onDismiss = { activeBrowser = null })
    }
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
    foreground: Color, secondary: Color, surface: Color, library: List<MediaCard>, providerItems: List<MediaCard>,
    onCategory: (String) -> Unit, onDiscover: () -> Unit, onLibrary: () -> Unit,
    onSave: (MediaCard) -> Unit, onOpen: (MediaCard) -> Unit,
    onPlay: (MediaCard) -> Unit, onDownload: (MediaCard) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(Color(0xCC472080), Color(0xB3201338), Color(0xA6111020))))
                    .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f), Aqua.copy(alpha = 0.15f))), RoundedCornerShape(25.dp))
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
                Text("From your extensions", color = foreground, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDiscover) { Text("Browse", color = Purple) }
            }
            if (providerItems.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("No synced titles yet", color = foreground, fontWeight = FontWeight.SemiBold)
                        Text("Open Extensions, install or add a source, then tap Sync. Its titles will appear here.", color = secondary, fontSize = 12.sp)
                        TextButton(onClick = onDiscover) { Text("Explore catalogues", color = Purple) }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    providerItems.take(8).forEach { item ->
                        MediaCardRow(item, library.any { it.id == item.id }, onSave, onOpen, onPlay, onDownload)
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
                Card(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(18.dp)) {
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
    Card(modifier = modifier.height(83.dp).liquidGlass(RoundedCornerShape(18.dp)).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(18.dp)) {
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
            val shorts = providerItems.filter {
                (it.category.equals("Shorts", true) || it.category.equals("All", true)) &&
                    (term.isBlank() || it.title.contains(term, true) || it.description.contains(term, true))
            }
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
                (it.category.equals(category, true) || it.category.equals("All", true) ||
                    (category == "Donghua" && it.category.equals("Anime", true)) ||
                    (category == "Manhwa" && it.category.equals("Manga", true))) &&
                    (term.isBlank() || it.title.contains(term, true) || it.description.contains(term, true) || it.subtitle.contains(term, true))
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
    Card(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(18.dp)) {
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
                if (CatalogService.isDownloadable(item.streamUrl) && item.httpHeaders.isEmpty()) {
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
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).liquidGlass(RoundedCornerShape(17.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(17.dp)) {
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
    Card(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(17.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(17.dp)) {
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
                    }
                    TextButton(onClick = { onOpen(item) }) {
                        Icon(Icons.Filled.Explore, contentDescription = null, tint = Purple, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            when {
                                item.pages.isNotEmpty() || item.textContent.isNotBlank() || item.textUrl.isNotBlank() -> "Read"
                                item.childItems.isNotEmpty() -> "Episodes / chapters"
                                else -> "Open details"
                            },
                            color = Purple, fontSize = 11.sp
                        )
                    }
                    if (CatalogService.isDownloadable(item.streamUrl) && item.httpHeaders.isEmpty()) {
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
            Card(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(18.dp)) {
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
    Card(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(14.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(14.dp)) {
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
    var headersText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var appliedMessage by remember { mutableStateOf("") }
    val providerTypes = listOf("All", "Anime", "Donghua", "Movies", "Series", "Manga", "Manhwa", "Light Novels", "Shorts")

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Extensions & Providers", color = foreground, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("Add a compatible feed or media link, Sync it, then open a title to read or watch.", color = secondary, fontSize = 12.sp)
        Spacer(Modifier.height(11.dp))
        Card(
            modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add extension / provider", color = foreground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = name, onValueChange = { name = it; error = ""; appliedMessage = "" },
                    label = { Text("Extension name (optional)") },
                    placeholder = { Text("e.g. Anime source") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White.copy(alpha = 0.035f),
                        focusedContainerColor = Color.White.copy(alpha = 0.075f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                        focusedBorderColor = Purple.copy(alpha = 0.85f)
                    )
                )
                Text("Content type", color = secondary, fontSize = 11.sp)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(providerTypes) { type ->
                        FilterChip(selected = category == type, onClick = { category = type }, label = { Text(type, fontSize = 10.sp) })
                    }
                }
                OutlinedTextField(
                    value = url, onValueChange = { url = it; error = ""; appliedMessage = "" },
                    label = { Text("Extension / provider link") },
                    placeholder = { Text("https://example.com/source.json") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(15.dp)
                )
                Text(
                    "Use an HTTPS JSON catalogue, M3U playlist, direct video URL, or website page. Mihon / Aniyomi / CloudStream plugin repository links are not compatible feed URLs. The name is optional.",
                    color = secondary, fontSize = 10.sp
                )
                OutlinedTextField(
                    value = headersText,
                    onValueChange = { headersText = it; error = ""; appliedMessage = "" },
                    label = { Text("Advanced HTTP headers (optional JSON)") },
                    placeholder = { Text("{\n  \"Referer\": \"https://example.org/\"\n}") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(15.dp)
                )
                Text("Only use headers provided by a source you trust. Header values are saved locally with this provider.", color = secondary, fontSize = 10.sp)
                if (error.isNotBlank()) Text(error, color = Pink, fontSize = 11.sp)
                if (appliedMessage.isNotBlank()) Text(appliedMessage, color = Aqua, fontSize = 11.sp)
                Button(
                    onClick = {
                        val cleanUrl = url.trim()
                        when {
                            cleanUrl.isBlank() -> error = "Paste an extension or provider link first."
                            !cleanUrl.startsWith("https://", true) -> error = "For security, use an HTTPS link."
                            providers.any { it.url.equals(cleanUrl, true) } -> error = "This source is already added."
                            else -> {
                                val parsedHeaders = runCatching { CatalogService.parseHeadersJson(headersText) }
                                if (parsedHeaders.isFailure) {
                                    error = parsedHeaders.exceptionOrNull()?.message ?: "Request headers must be valid JSON."
                                } else {
                                    val automaticName = name.trim().ifBlank {
                                        runCatching { android.net.Uri.parse(cleanUrl).host?.removePrefix("www.") }.getOrNull()
                                            ?.takeIf { it.isNotBlank() } ?: "Provider " + (providers.size + 1)
                                    }
                                    onAdd(MediaProvider(
                                        System.currentTimeMillis().toString(), automaticName, category, cleanUrl,
                                        headers = parsedHeaders.getOrDefault(emptyMap())
                                    ))
                                    appliedMessage = "$automaticName added. Tap Sync below to load its titles."
                                    name = ""
                                    url = ""
                                    headersText = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = url.isNotBlank()
                ) {
                    Icon(Icons.Filled.Extension, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply / Add extension", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (message.isNotBlank()) Text(message, color = if (message.startsWith("Could not load", true)) Pink else Aqua, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Your extensions (" + providers.size + ")", color = foreground, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Sync, then tap Open / Read / Watch", color = secondary, fontSize = 10.sp)
        }
        if (providers.isEmpty()) {
            Text("No extensions added yet. Paste a source link above and press Apply / Add extension.", color = secondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        LazyColumn(
            modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            items(providers, key = { it.id }) { provider ->
                Card(
                    modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(13.dp)) {
                        Text(provider.name, color = foreground, fontWeight = FontWeight.Bold)
                        Text(
                            provider.category + (if (provider.headers.isNotEmpty()) " • custom headers" else "") + " • " + provider.url,
                            color = secondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { onSync(provider) }, enabled = syncingId != provider.id,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp)
                            ) {
                                if (syncingId == provider.id) CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(if (syncingId == provider.id) "Syncing…" else "Sync")
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
                item { Text("Loaded titles (" + sourceItems.size + ")", color = foreground, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)) }
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
        Card(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(17.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(17.dp)) {
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
        Card(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(17.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(17.dp)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Movie & series catalogue", color = foreground, fontWeight = FontWeight.Bold)
                Text("TMDB requires a free API key. Create one on themoviedb.org and paste the v3 API key here. The app stores it on this device.", color = secondary, fontSize = 11.sp)
                OutlinedTextField(value = keyValue, onValueChange = onKeyChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("TMDB API key") })
                Button(onClick = onSaveKey, modifier = Modifier.fillMaxWidth()) { Text("Save API key") }
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(17.dp)), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(17.dp)) {
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
        Text("All IN ONE Media • Version 0.4.1", color = secondary, fontSize = 11.sp)
        Text("Your library, providers, and settings are stored locally on this device.", color = secondary, fontSize = 10.sp)
    }
}


@Composable
private fun ReaderDialog(
    item: MediaCard, onDismiss: () -> Unit, onOpenSource: (String, Map<String, String>) -> Unit,
    onOpenChild: (MediaCard) -> Unit, onPlay: (MediaCard) -> Unit
) {
    var pageIndex by remember(item.id) { mutableIntStateOf(0) }
    var fontSize by remember(item.id) { mutableFloatStateOf(17f) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f).padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF100D19)
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                item.pages.isNotEmpty() -> "Chapter pages"
                                item.providerName == "Project Gutenberg" -> "Public-domain book text"
                                item.textContent.isNotBlank() -> "Reading view"
                                else -> "Title details"
                            },
                            color = SoftText, fontSize = 10.sp
                        )
                    }
                    TextButton(onClick = onDismiss) { Text("Close", color = Purple) }
                }
                if (item.pages.isNotEmpty()) {
                    if (item.providerName == "MangaDex") {
                        Text("MangaDex • Scroll vertically to read • Please credit creators and groups.", color = SoftText, fontSize = 10.sp)
                        LazyColumn(
                            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF050407)),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(item.pages) { pageUrl ->
                                AsyncImage(
                                    model = pageUrl,
                                    contentDescription = "Manga page",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        AsyncImage(
                            model = item.pages[pageIndex.coerceIn(item.pages.indices)],
                            contentDescription = "Page " + (pageIndex + 1),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF050407))
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            OutlinedButton(onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) }, enabled = pageIndex > 0) { Text("Previous") }
                            Text((pageIndex + 1).toString() + " / " + item.pages.size, color = SoftText, fontSize = 12.sp)
                            Button(onClick = { pageIndex = (pageIndex + 1).coerceAtMost(item.pages.lastIndex) }, enabled = pageIndex < item.pages.lastIndex) { Text("Next") }
                        }
                        Slider(
                            value = pageIndex.toFloat(), onValueChange = { pageIndex = it.toInt().coerceIn(item.pages.indices) },
                            valueRange = 0f..(item.pages.lastIndex.coerceAtLeast(1)).toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (item.textContent.isNotBlank()) {
                    Column(Modifier.fillMaxWidth().weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Text size", color = SoftText, fontSize = 11.sp)
                            Spacer(Modifier.width(10.dp))
                            Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 13f..28f, modifier = Modifier.weight(1f))
                            Text(fontSize.toInt().toString(), color = SoftText, fontSize = 11.sp)
                        }
                        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 12.dp)) {
                            if (item.description.isNotBlank()) {
                                Text(item.description, color = Purple, fontSize = (fontSize - 1).sp, lineHeight = (fontSize + 7).sp)
                                Spacer(Modifier.height(16.dp))
                            }
                            Text(item.textContent, color = Color(0xFFF2ECFA), fontSize = fontSize.sp, lineHeight = (fontSize * 1.55f).sp)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(item.category + " • " + item.subtitle, color = Purple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        if (item.coverUrl.isNotBlank()) {
                            AsyncImage(model = item.coverUrl, contentDescription = item.title, contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).clip(RoundedCornerShape(12.dp)))
                        }
                        Text(item.description.ifBlank { "This source has provided catalogue information but has not supplied readable chapters or a direct stream for this title." },
                            color = Color(0xFFF2ECFA), fontSize = 14.sp, lineHeight = 22.sp)
                        if (item.childItems.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Episodes / chapters (" + item.childItems.size + ")", color = Purple, fontWeight = FontWeight.Bold)
                            item.childItems.forEach { child ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable { onOpenChild(child) },
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (child.streamUrl.isNotBlank()) Icons.Filled.PlayArrow else Icons.Filled.MenuBook, contentDescription = null, tint = Aqua)
                                        Spacer(Modifier.width(9.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(child.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            if (child.subtitle.isNotBlank()) Text(child.subtitle, color = SoftText, fontSize = 10.sp)
                                        }
                                        Icon(Icons.Filled.Explore, contentDescription = "Open", tint = Purple)
                                    }
                                }
                            }
                        }
                        Text("Direct chapter pages, text and supported video streams open in the app. Website-only sources open in the in-app browser. A catalogue entry by itself does not include a playable episode.", color = SoftText, fontSize = 11.sp)
                        if (item.streamUrl.isNotBlank()) {
                            Button(onClick = { onPlay(item) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(7.dp))
                                Text(if (item.mimeType.startsWith("audio/")) "Play audio" else "Play video")
                            }
                        }
                        if (item.pageUrl.isNotBlank()) {
                            Button(onClick = { onOpenSource(item.pageUrl, item.httpHeaders) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Explore, contentDescription = null)
                                Spacer(Modifier.width(7.dp))
                                Text("Open source inside app")
                            }
                        }
                        if (item.providerName == "MangaDex") {
                            Text("Manga powered by MangaDex • Credit creators and scanlation groups.", color = SoftText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaDetailsDialog(
    item: MediaCard, onDismiss: () -> Unit, onReadChapter: (MediaChapter) -> Unit
) {
    var chapters by remember(item.id) { mutableStateOf<List<MediaChapter>>(emptyList()) }
    var loading by remember(item.id) { mutableStateOf(true) }
    var error by remember(item.id) { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(item.id) {
        loading = true
        error = ""
        try {
            chapters = CatalogService.mangaChapters(item.id.removePrefix("mangadex-"))
            if (chapters.isEmpty()) error = "No English chapters are currently listed for this title."
        } catch (e: Exception) {
            error = e.message ?: "Could not load chapter list."
        } finally {
            loading = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.91f).padding(9.dp),
            shape = RoundedCornerShape(22.dp), color = Color(0xFF100D19)
        ) {
            Column(Modifier.fillMaxSize().padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("MangaDex • English chapters", color = SoftText, fontSize = 11.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("Close", color = Purple) }
                }
                if (item.description.isNotBlank()) {
                    Text(item.description.take(420), color = SoftText, fontSize = 11.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(10.dp))
                when {
                    loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Purple)
                            Text("Loading chapter list…", color = SoftText, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    error.isNotBlank() -> Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error, color = Pink, fontSize = 12.sp)
                        Text("MangaDex only lists chapters supplied by its contributors. Try another title or check the source website.", color = SoftText, fontSize = 11.sp)
                        Button(onClick = {
                            scope.launch {
                                loading = true
                                error = ""
                                try {
                                    chapters = CatalogService.mangaChapters(item.id.removePrefix("mangadex-"))
                                    if (chapters.isEmpty()) error = "No English chapters are currently listed for this title."
                                } catch (e: Exception) { error = e.message ?: "Could not load chapter list." }
                                finally { loading = false }
                            }
                        }) { Text("Retry") }
                    }
                    else -> LazyColumn(
                        Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp),
                        contentPadding = PaddingValues(vertical = 5.dp)
                    ) {
                        items(chapters, key = { it.id }) { chapter ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onReadChapter(chapter) },
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.065f)),
                                shape = RoundedCornerShape(13.dp)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Aqua)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(chapter.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        val chapterMeta = listOf(chapter.subtitle, chapter.scanlationGroups)
                                            .filter { it.isNotBlank() }.joinToString(" • ")
                                        if (chapterMeta.isNotBlank()) Text(chapterMeta, color = SoftText, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Filled.Explore, contentDescription = "Read chapter", tint = Purple)
                                }
                            }
                        }
                    }
                }
                Text("MangaDex is the source. Please credit authors and scanlation groups.", color = SoftText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun InAppBrowserDialog(title: String, url: String, headers: Map<String, String>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val webView = remember(url, headers) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.safeBrowsingEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val target = request?.url ?: return true
                    return !target.scheme.equals("https", true)
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(url, headers)
        }
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.96f).padding(7.dp),
            shape = RoundedCornerShape(18.dp), color = Color(0xFF100D19)
        ) {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(url, color = SoftText, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = {
                        if (webView.canGoBack()) webView.goBack() else onDismiss()
                    }) { Text("Back", color = Aqua) }
                    TextButton(onClick = onDismiss) { Text("Close", color = Purple) }
                }
                AndroidView(factory = { webView }, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoPlayerDialog(item: MediaCard, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var playbackError by remember(item.streamUrl, item.mimeType, item.httpHeaders) { mutableStateOf("") }
    var buffering by remember(item.streamUrl, item.mimeType, item.httpHeaders) { mutableStateOf(true) }
    val player = remember(item.streamUrl, item.mimeType, item.httpHeaders) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("AllInOneMedia/0.4 Android")
            .setDefaultRequestProperties(item.httpHeaders)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        ExoPlayer.Builder(context).setMediaSourceFactory(mediaSourceFactory).build().apply {
            val mediaBuilder = PlayerMediaItem.Builder().setUri(item.streamUrl)
            if (item.mimeType.isNotBlank()) mediaBuilder.setMimeType(item.mimeType)
            setMediaItem(mediaBuilder.build())
            addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    buffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    playbackError = error.errorCodeName + ": " + (error.message ?: "Playback failed")
                    buffering = false
                }
            })
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
                if (buffering) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        CircularProgressIndicator(color = Purple, modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                        Text("Connecting to stream…", color = SoftText, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (playbackError.isNotBlank()) {
                    Text(playbackError, color = Pink, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
                    Text("The link may be offline, require unavailable access headers, use DRM, or point to an unsupported format.", color = SoftText, fontSize = 10.sp)
                    TextButton(onClick = {
                        playbackError = ""
                        buffering = true
                        player.prepare()
                        player.playWhenReady = true
                    }) { Text("Retry playback", color = Aqua) }
                }
                Text("Playback uses the stream URL supplied by your provider. Streams with DRM or restricted access are not supported.", color = SoftText, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

private fun downloadMedia(context: Context, item: MediaCard) {
    val url = item.streamUrl
    if (!CatalogService.isDownloadable(url)) {
        Toast.makeText(context, "This source does not provide a downloadable direct file.", Toast.LENGTH_SHORT).show()
        return
    }
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
        Toast.makeText(context, "Download added to Android Downloads.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: " + (e.message ?: "unknown error"), Toast.LENGTH_LONG).show()
    }
}
