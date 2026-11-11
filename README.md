# All IN ONE Media

All IN ONE Media is an Android media hub for searching titles, adding compatible sources, opening episodes and chapters, reading text and image pages, and playing supported video streams.

## Version 0.4.3

- Anime and donghua title discovery through AniList metadata.
- Manga and manhwa search through MangaDex, including English chapter lists, scanlation-group attribution, and a continuous vertical image reader.
- Public-domain book discovery through Project Gutenberg / Gutendex, with an in-app text reader.
- Public-domain movie search through Internet Archive licence metadata, with MP4 resolution and in-app playback when an MP4 derivative exists.
- TV-series metadata search through TMDB after you supply a TMDB API key. TMDB is metadata only; it does not supply video.
- Manual HTTPS providers that return JSON catalogues, nested episodes/chapters, RSS/Atom feeds, M3U playlists with per-stream headers, basic server-rendered HTML catalogues, or direct video/audio streams. Relative URLs are resolved against the feed URL.
- Stremio HTTP add-ons: `/manifest.json` detection, catalog sync, catalogue search (when the add-on declares the `search` extra), metadata/episode loading, stream requests, and playback of direct HTTPS streams.
- Nested episode and chapter selection in title details.
- In-app HTTPS browser for source pages that don't expose direct media URLs.
- Media3 playback for MP4, WebM, M4V, HLS/M3U8, DASH/MPD and common audio formats, with MIME-type and custom request-header support.
- Local library, downloads for supported direct files, liquid-glass-inspired interface, and dark/light themes.

## Quick start

1. Open **Discover** and choose a category.
2. Search for a manga or manhwa, then open its title and choose an available English chapter.
3. Search **Light Novels** for public-domain books; open a result to load its text.
4. Search **Movies** for Internet Archive items; open a result to resolve and play an MP4 when one is available.
5. For anime, donghua, series, and other sources, open **Extensions**. You can paste a compatible HTTPS JSON catalogue, RSS/Atom feed, M3U playlist, direct media URL, website URL, or a Stremio add-on's full `manifest.json` URL. Give it an optional name, select a type, and press **Apply / Add extension**. When a source requires headers, enter them in the advanced JSON field. Then press **Sync** and open a loaded result.

A title listing is not itself a video or chapter. For an anime or series to play, its provider must supply a usable episode stream URL (or a web page that can play it). A JSON feed can provide nested `episodes` or `chapters`; see [the provider format](docs/PROVIDER_FORMAT.md).

## Extension limitations and safety

This release reads data and media links. It includes a Stremio HTTP add-on client, but does not install or execute Mihon/Komikku or Aniyomi APK extensions, CloudStream Kotlin-script plugins, LNReader/Mangayomi plugins, or arbitrary APK/DEX/JavaScript runtimes. Those ecosystems use different extension APIs and are not interchangeable with Stremio's HTTP protocol.

The app does not bundle films, episodes, manga scans, novels, or third-party APKs. Only use sources and content you are authorised to access. It does not bypass logins, paywalls, DRM, or other access controls. MangaDex API usage requires attribution to MangaDex and scanlation groups and prohibits advertisements or paid services in apps using its API.

## Build

- Android application ID: `com.allinonemedia.app.v043`
- Version: `0.4.3` (versionCode 11)
- Minimum Android version: Android 8.0 (API 26)
- Build branch: [scratch-rebuild](https://github.com/lmy246848-cloud/All-IN-ONE-Media/tree/scratch-rebuild)
- APK workflow: [Android APK and Release](https://github.com/lmy246848-cloud/All-IN-ONE-Media/actions/workflows/android.yml)

The GitHub Actions workflow produces an APK and verifies its ZIP structure, package metadata, and signature before publishing it. This 0.4.3 build uses a new application ID (`com.allinonemedia.app.v043`) so it can install alongside earlier builds despite the unrecoverable temporary CI signing key. Android treats it as a separate app, so local library/provider data from 0.4.0 is not copied automatically. Without a persistent keystore configured through Actions secrets, later CI builds will also need separate install identities or a reinstall after a signing-key change.

## API references

- [AniList GraphQL](https://docs.anilist.co/)
- [MangaDex API documentation and acceptable use policy](https://api.mangadex.org/docs/)
- [Gutendex API](https://gutendex.com/)
- [Internet Archive advanced search](https://archive.org/advancedsearch.php)
- [TMDB API](https://developer.themoviedb.org/)
- [Stremio add-on protocol](https://stremio.github.io/stremio-addon-sdk/protocol.html)
- [Android Media3 HLS](https://developer.android.com/media/media3/exoplayer/hls)
- [Android Media3 DASH](https://developer.android.com/media/media3/exoplayer/dash)
