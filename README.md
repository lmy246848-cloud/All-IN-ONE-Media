# All IN ONE Media

All IN ONE Media is an Android media hub for searching titles, adding compatible sources, opening episodes and chapters, reading text and image pages, and playing supported video streams.

## Version 0.4.1

- Anime and donghua title discovery through AniList metadata.
- Manga and manhwa search through MangaDex, including English chapter lists, scanlation-group attribution, and a continuous vertical image reader.
- Public-domain book discovery through Project Gutenberg / Gutendex, with an in-app text reader.
- Public-domain movie search through Internet Archive licence metadata, with MP4 resolution and in-app playback when an MP4 derivative exists.
- TV-series metadata search through TMDB after you supply a TMDB API key. TMDB is metadata only; it does not supply video.
- Manual HTTPS providers that return JSON catalogues, nested episodes/chapters, RSS/Atom feeds, M3U playlists with per-stream headers, basic server-rendered HTML catalogues, or direct video/audio streams. Relative URLs are resolved against the feed URL.
- Nested episode and chapter selection in title details.
- In-app HTTPS browser for source pages that don't expose direct media URLs.
- Media3 playback for MP4, WebM, M4V, HLS/M3U8, DASH/MPD and common audio formats, with MIME-type and custom request-header support.
- Local library, downloads for supported direct files, liquid-glass-inspired interface, and dark/light themes.

## Quick start

1. Open **Discover** and choose a category.
2. Search for a manga or manhwa, then open its title and choose an available English chapter.
3. Search **Light Novels** for public-domain books; open a result to load its text.
4. Search **Movies** for Internet Archive items; open a result to resolve and play an MP4 when one is available.
5. For anime, donghua, series, and other sources, open **Extensions**. Paste a compatible HTTPS JSON catalogue, M3U playlist, direct media URL, or website URL. Give it an optional name, select a type, and press **Apply / Add extension**. When a source requires headers, enter them in the advanced JSON field. Then press **Sync** and open a loaded result.

A title listing is not itself a video or chapter. For an anime or series to play, its provider must supply a usable episode stream URL (or a web page that can play it). A JSON feed can provide nested `episodes` or `chapters`; see [the provider format](docs/PROVIDER_FORMAT.md).

## Extension limitations and safety

This release reads data and media links. It does not install or execute Mihon/Komikku, Aniyomi, CloudStream, LNReader, or Mangayomi plugin packages or scripts. Those apps use separate extension runtimes. Their repository URLs are not directly compatible.

The app does not bundle films, episodes, manga scans, novels, or third-party APKs. Only use sources and content you are authorised to access. It does not bypass logins, paywalls, DRM, or other access controls. MangaDex API usage requires attribution to MangaDex and scanlation groups and prohibits advertisements or paid services in apps using its API.

## Build

- Android application ID: `com.allinonemedia.app.v041`
- Version: `0.4.1` (versionCode 8)
- Minimum Android version: Android 8.0 (API 26)
- Build branch: [scratch-rebuild](https://github.com/lmy246848-cloud/All-IN-ONE-Media/tree/scratch-rebuild)
- APK workflow: [Android APK and Release](https://github.com/lmy246848-cloud/All-IN-ONE-Media/actions/workflows/android.yml)

The GitHub Actions workflow produces an APK release. Without a persistent release keystore configured through the repository Actions secrets, the workflow uses a temporary CI debug key. Such APKs are not guaranteed to be update-compatible with later builds. Keep a copy of important local app data before changing installations.

## API references

- [AniList GraphQL](https://docs.anilist.co/)
- [MangaDex API documentation and acceptable use policy](https://api.mangadex.org/docs/)
- [Gutendex API](https://gutendex.com/)
- [Internet Archive advanced search](https://archive.org/advancedsearch.php)
- [TMDB API](https://developer.themoviedb.org/)
- [Android Media3 HLS](https://developer.android.com/media/media3/exoplayer/hls)
- [Android Media3 DASH](https://developer.android.com/media/media3/exoplayer/dash)
