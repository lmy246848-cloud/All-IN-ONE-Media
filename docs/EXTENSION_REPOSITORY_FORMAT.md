# All IN ONE Media extension repository format

All IN ONE Media 0.3.0 supports **data-source extensions** from HTTPS JSON repositories. These are installable source definitions, not executable APK, DEX, JavaScript, CloudStream `.cs3`, Mihon, Aniyomi, LNReader, or Mangayomi binaries.

## Repository JSON

The repository URL entered in Extensions must return either a JSON array or an object containing an `extensions`, `sources`, or `items` array.

```json
{
  "name": "Community sources",
  "extensions": [
    {
      "id": "sample-anime",
      "name": "Sample Anime",
      "version": "1.0.0",
      "category": "Anime",
      "description": "Example catalogue source",
      "catalogUrl": "https://example.org/all-in-one/anime.json"
    },
    {
      "id": "sample-manga",
      "name": "Sample Manga",
      "version": "1.0.0",
      "category": "Manga",
      "description": "Example manga source",
      "catalogUrl": "https://example.org/all-in-one/manga.json"
    }
  ]
}
```

Each entry needs a name and a secure HTTPS catalogue URL. The user taps **Install** to add that source to the local provider list, then taps **Sync** to load its catalogue. Installed sources can be removed from the app.

## Catalogue response

A catalogue URL can return a JSON array or an object with an `items` or `results` array. Every item should have a stable `id` and a `title`.

```json
{
  "items": [
    {
      "id": "anime-001",
      "title": "Example Anime",
      "category": "Anime",
      "subtitle": "Season 1",
      "description": "A short synopsis.",
      "coverUrl": "https://cdn.example.org/cover.jpg",
      "pageUrl": "https://example.org/anime/example",
      "streamUrl": "https://cdn.example.org/video/episode-1.m3u8"
    },
    {
      "id": "manga-001",
      "title": "Example Manga Chapter 1",
      "category": "Manga",
      "subtitle": "Chapter 1",
      "coverUrl": "https://cdn.example.org/manga-cover.jpg",
      "pages": [
        "https://cdn.example.org/pages/001.jpg",
        "https://cdn.example.org/pages/002.jpg"
      ]
    },
    {
      "id": "novel-001",
      "title": "Example Novel Chapter 1",
      "category": "Light Novels",
      "subtitle": "Chapter 1",
      "content": "The chapter text goes here."
    },
    {
      "id": "short-001",
      "title": "Example Short Drama",
      "category": "Shorts",
      "streamUrl": "https://cdn.example.org/shorts/episode-1.mp4"
    }
  ]
}
```

Supported categories: `Anime`, `Donghua`, `Movies`, `Series`, `Manga`, `Manhwa`, `Light Novels`, `Shorts`, and `All`.

Supported item fields:
- `id`, `title`, `category`, `subtitle`, `description`
- `coverUrl`, `pageUrl` (or `link`)
- `streamUrl` (or `url`) for direct supported video sources
- `pages`: array of image URLs, opened in the manga/webtoon reader
- `content`: plain chapter text, opened in the novel reader

Direct media playback supports MP4, M3U8/HLS, MPD/DASH, WebM, and M4V. Direct MP4/WebM/M4V can be passed to Android Download Manager. HLS/DASH playback is not an offline-file download.

## Important compatibility and security notes

- The app validates HTTPS for repository and provider endpoints.
- This version installs source definitions and reads their JSON data. It does not execute arbitrary code from an extension.
- A repository made for Mihon/Komikku, Aniyomi, CloudStream, LNReader, Mangayomi, or another application's binary/plugin runtime is not automatically compatible with this format. Those ecosystems require dedicated adapters or their own runtime and are not claimed as supported by this build.
- Sources should only expose content the user is authorised to access. The app does not host media or bypass paywalls, DRM, logins, or access controls.
