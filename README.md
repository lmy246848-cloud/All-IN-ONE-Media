# All IN ONE Media

A unified Android entertainment library for discovering media, managing sources, saving titles, and playing content from providers you configure.

## What is included in v0.2.0

- **Polished interface:** custom purple launcher icon, animated startup screen, animated page transitions, loading placeholders, and dark/light theme.
- **Anime and donghua discovery:** live title search through the public AniList catalogue.
- **Manga and manhwa discovery:** live title search through AniList's manga catalogue.
- **Movies and series discovery:** TMDB search after you add your own TMDB v3 API key in Settings.
- **Books and light-novel catalogue:** Open Library title search. This is a general book catalogue, not a specialist light-novel-only database.
- **Extensions and providers:** add HTTPS JSON catalogues, M3U playlists, and direct media links.
- **Short-drama feed:** show short-form titles and videos supplied by a configured Shorts provider.
- **Playback:** built-in Media3 player for direct MP4, HLS/M3U8, DASH/MPD, WebM, and M4V URLs supplied by providers.
- **Reading:** in-app page viewer for image chapters and text view for provider-supplied novel content.
- **Library:** saved titles and configured providers persist locally on the device.
- **Downloads:** direct MP4, WebM, and M4V downloads are handed to Android Download Manager.

## Provider setup

Open **Extensions** in the app, enter a provider name, choose its content type, paste an HTTPS URL, and tap **Add provider**. Tap **Sync** to load it.

Supported provider formats and examples are documented in [PROVIDER_FORMAT.md](docs/PROVIDER_FORMAT.md).

A JSON feed can contain:
- `streamUrl` for a playable video URL.
- `pageUrl` for an external source page.
- `pages` for a list of image URLs used by the in-app chapter reader.
- `content` for text displayed by the in-app reader.
- `coverUrl` for artwork.
- `category` to assign each item to a category.

For safety, provider URLs must use HTTPS. The app reads data feeds and media links; it does not download or execute arbitrary extension code.

## API keys and services

- AniList search uses its public GraphQL endpoint.
- Open Library search uses its public Search API.
- Movie and TV search requires a TMDB v3 API key. You provide and store that key locally in Settings. TMDB is a metadata catalogue; it does not provide streaming video through this app.

## Important capability notes

The app does not bundle films, episodes, manga scans, novels, short dramas, or third-party APKs. Playback and reading availability depends on the sources you add and the rights those sources grant. DRM-protected streams are not supported. The download button is shown only for supported direct-file URLs; HLS playlists are streamed, not downloaded as a single file.

Catalogue results from AniList, Open Library, and TMDB are metadata records with links to their respective pages. They are not streaming or reading links. To play videos or read chapters inside the app, add a compatible provider that supplies the corresponding media URL or page data.

## Project build

- Android application ID: `com.allinonemedia.app`
- Version: `0.2.0`
- Minimum Android version: Android 8.0 (API 26)
- Build: GitHub Actions workflow **Android APK**
- Direct APK: [Latest All IN ONE Media APK](https://github.com/lmy246848-cloud/All-IN-ONE-Media/releases/latest/download/All-IN-ONE-Media.apk)
- Build source: [scratch-rebuild branch](https://github.com/lmy246848-cloud/All-IN-ONE-Media/tree/scratch-rebuild)

## Feature references

These are the projects supplied as product/feature references:

1. [Komikku](https://github.com/komikku-app/komikku) — manga library and reader patterns.
2. [Zangetsu](https://github.com/Spyou/Zangetsu) — multi-category media experience.
3. [Vega](https://github.com/vega-org/vega-app) — media playback and casting concepts.
4. [AnimeZe](https://github.com/DeadZone-0/AnimeZe) — anime, manga, and manhwa discovery.
5. [Anidroid](https://github.com/momcilovicluka/anidroid) — anime search and information.
6. [Hikari](https://github.com/codegeasse1/hikari) — provider/extension ecosystem concepts.
7. [ANI-KUTA](https://github.com/Confused-Creature-180/ANI-KUTA) — extension-driven content sources.
8. [HGhub](https://github.com/Minireel/HGhub) — short-drama and reels product reference.

No source code or APK binaries from those projects are bundled in this build. Repository metadata on 10 November 2026 shows different licensing states: Komikku declares Apache-2.0 and Zangetsu declares GPL-3.0; Vega, Hikari, and HGhub report non-standard/NOASSERTION metadata; AnimeZe, Anidroid, and ANI-KUTA do not declare a repository licence. Those projects with missing or unclear licences are treated as reference material only, not reusable code.

## Development

The Android workflow builds a debug APK and publishes an APK asset to the GitHub Releases page. Release assets are the direct installation files; the Actions artifact is retained as a secondary build record.
