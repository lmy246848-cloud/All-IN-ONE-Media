# Provider format — All IN ONE Media 0.4.1

A provider is a **data feed or media endpoint**, not an executable extension package. Add its HTTPS URL in **Extensions**, choose the category, press **Apply / Add extension**, then press **Sync**. Open a title, choose an episode or chapter when provided, and use the in-app reader, player, or website view.

## Supported source types

- JSON catalogue feeds: arrays or wrapper objects containing items such as `items`, `results`, `data`, `titles`, `entries`, `chapters`, or `episodes`. Relative stream, page, image and text URLs are resolved against the feed URL.
- RSS 2.0 and Atom feeds: entries with HTTPS links and/or media enclosures are imported as cards.
- Server-rendered HTML websites: the app extracts a limited set of HTTPS title links and nearby thumbnails/descriptions. JavaScript-only catalogues may still need to be browsed in the in-app browser.
- M3U playlists: `#EXTM3U` / `#EXTINF` with absolute or relative HTTPS stream URLs, optional `tvg-logo`/`group-title` metadata, and per-entry headers expressed with supported `#EXTVLCOPT` options or `#EXTHTTP` JSON.
- Direct HTTPS video/audio URLs: MP4, M4V, WebM, HLS/M3U8, DASH/MPD, MP3, M4A, AAC, OGG/OGA, Opus, and WAV. A direct endpoint without a file extension can be detected using its HTTP content type.
- Website pages: HTTPS detail, reading, or streaming pages open in the app's embedded browser. Some sites may block embedded browsers or require a sign-in/captcha.

## Nested episode and chapter feeds

A title can contain its own `episodes`, `chapters`, `children`, or `parts` array. Each child can provide a direct `streamUrl`, `pages`, `content`, `textUrl`, or `pageUrl`.

```json
{
  "items": [
    {
      "id": "anime-001",
      "title": "Example Series",
      "category": "Anime",
      "coverUrl": "https://cdn.example.org/series.jpg",
      "description": "Series synopsis",
      "episodes": [
        {
          "id": "episode-1",
          "title": "Episode 1",
          "streamUrl": "https://cdn.example.org/video/episode-1.m3u8",
          "mimeType": "application/x-mpegURL",
          "headers": {
            "Referer": "https://example.org/",
            "User-Agent": "ExamplePlayer/1.0"
          }
        },
        {
          "id": "episode-2",
          "title": "Episode 2",
          "streamUrl": "https://cdn.example.org/play?id=episode-2",
          "mimeType": "application/x-mpegURL"
        }
      ]
    },
    {
      "id": "manga-001",
      "title": "Example Manga",
      "category": "Manga",
      "coverUrl": "https://cdn.example.org/manga.jpg",
      "chapters": [
        {
          "id": "chapter-1",
          "title": "Chapter 1",
          "pages": [
            "https://cdn.example.org/pages/001.jpg",
            "https://cdn.example.org/pages/002.jpg"
          ]
        },
        {
          "id": "chapter-2",
          "title": "Chapter 2",
          "textUrl": "https://cdn.example.org/chapters/2.txt"
        }
      ]
    }
  ]
}
```

For streams that require request headers, include a `headers` object on the video item or its parent title. Supported headers are applied to the video requests; they are stored locally with the item. Do not publish private access tokens or cookies in a public feed. For streams whose URL doesn't end in a known file extension, set `mimeType` to one of `application/x-mpegURL`, `application/vnd.apple.mpegurl`, `application/dash+xml`, `video/mp4`, or `video/webm`. A source can also put `mime_type` or `streamMimeType` on the entry. HLS and DASH require compatible manifests and codecs; unsupported formats, DRM, and access-control challenges will not play.

## Single item examples

A manga chapter can be a single record:

```json
{
  "id": "chapter-001",
  "title": "Example Chapter 1",
  "category": "Manga",
  "pages": [
    "https://cdn.example.org/pages/001.jpg",
    "https://cdn.example.org/pages/002.jpg"
  ]
}
```

A novel chapter can provide inline text or a plain-text URL:

```json
{
  "id": "novel-001",
  "title": "Example Novel Chapter 1",
  "category": "Light Novels",
  "content": "Chapter text goes here."
}
```

`textUrl` is intended for an HTTPS plain-text document. `pageUrl` or `link` opens a website in the in-app browser.

## Recognised fields

| Field | Meaning |
|---|---|
| `id`, `slug`, `key` | Stable item identifier |
| `title`, `name`, `label` | Display name |
| `category`, `type`, `mediaType` | Anime, Donghua, Movies, Series, Manga, Manhwa, Light Novels, Shorts, or All |
| `subtitle`, `description`, `summary` | Metadata |
| `coverUrl`, `poster`, `thumbnail`, `image` | Artwork |
| `streamUrl`, `playUrl`, `videoUrl`, `file`, `src`, `hls`, `mp4` | Direct video URL |
| `mimeType`, `mime_type`, `streamMimeType`, `format` | Optional stream MIME type |
| `headers`, `requestHeaders` | Optional HTTP headers for the stream, such as Referer, Origin, User-Agent, or Authorization |
| `pageUrl`, `webUrl`, `detailUrl`, `link` | Website page |
| `pages`, `pageUrls`, `images` | Manga/comic image pages |
| `content`, `text`, `chapterText`, `body` | Inline readable text |
| `textUrl`, `readerUrl`, `textFile` | HTTPS text document |
| `episodes`, `chapters`, `children`, `parts` | Nested episode/chapter list |

## Built-in sources

- Manga and manhwa search uses MangaDex, can list available English chapters, displays scanlation-group attribution, and opens image pages in a vertical reader. MangaDex requires attribution to MangaDex and scanlation groups, requires honouring group removal requests, and prohibits ads or paid services in apps using its API.
- Light-novel search currently uses Project Gutenberg through Gutendex. Its catalogue contains public-domain books in general; it is not a specialist modern light-novel catalogue.
- Movies search currently filters Internet Archive records to items marked with a public-domain licence and attempts to resolve a direct MP4 file for in-app playback. Not every record has an MP4 file.
- Anime and donghua search uses AniList for metadata. AniList entries do not include episode video files; add a compatible provider that supplies episode streams or website links to watch.
- TV series search uses TMDB metadata and requires your own TMDB API key. TMDB does not provide episode streams.

## Unsupported extension runtimes

This version does **not** execute or install Mihon/Komikku, Aniyomi, CloudStream, LNReader, Mangayomi, APK, DEX, JavaScript, or arbitrary extension packages. Their extensions use different runtime APIs. A repository index from one of those apps is not a catalogue feed. Use an All IN ONE Media JSON feed, M3U playlist, direct stream URL, or website URL instead.

All source URLs must use HTTPS. Only use sources and content you are authorised to access. The app does not bypass logins, paywalls, DRM, geographic restrictions, or other access controls.
