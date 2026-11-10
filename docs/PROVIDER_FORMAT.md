# Provider format

All provider URLs must use HTTPS. Add a provider from the app's **Extensions** page and then tap **Sync**.

## JSON catalogue

The endpoint may return either a JSON array or an object containing an `items` array (a `results` array is also accepted).

```json
{
  "name": "My Manga and Shorts Provider",
  "items": [
    {
      "id": "chapter-001",
      "title": "Example Chapter 1",
      "category": "Manga",
      "subtitle": "Chapter 1",
      "coverUrl": "https://cdn.example.org/covers/example.jpg",
      "pageUrl": "https://example.org/title/example",
      "pages": [
        "https://cdn.example.org/pages/001.jpg",
        "https://cdn.example.org/pages/002.jpg"
      ]
    },
    {
      "id": "short-001",
      "title": "Example Short Drama",
      "category": "Shorts",
      "subtitle": "Episode 1",
      "coverUrl": "https://cdn.example.org/posters/short.jpg",
      "streamUrl": "https://cdn.example.org/video/short-001.mp4"
    },
    {
      "id": "novel-001",
      "title": "Example Light Novel",
      "category": "Light Novels",
      "content": "Chapter text goes here. It is displayed in the in-app text reader."
    }
  ]
}
```

### Item fields

| Field | Required | Meaning |
|---|---|---|
| `id` | Recommended | Stable unique ID |
| `title` | Yes | Display title |
| `category` | No | Anime, Donghua, Movies, Series, Manga, Manhwa, Light Novels, Shorts, or All |
| `subtitle` | No | Short metadata |
| `coverUrl` | No | HTTPS artwork URL |
| `pageUrl` / `link` | No | External title/source page |
| `streamUrl` / `url` | No | Direct playable media URL |
| `pages` | No | Array of HTTPS page-image URLs for manga/manhwa reading |
| `content` | No | Plain text content for the in-app novel reader |
| `description` | No | Summary text |

## M3U playlist

The provider URL can return an M3U playlist using `#EXTM3U` and `#EXTINF` lines followed by HTTPS media URLs. The app uses each `#EXTINF` title and URL as an item.

## Direct media

A direct URL ending in `.mp4`, `.m3u8`, `.mpd`, `.webm`, or `.m4v` becomes a playable item. Direct MP4, WebM, and M4V files can be sent to Android Download Manager. HLS/DASH playlists are playback sources; they are not packaged into offline downloads.

## Security and compatibility

The app does not execute downloaded Kotlin, JavaScript, or other code. Providers are data feeds and media links only. Use sources you own or are authorised to access. DRM-protected content and providers requiring custom authentication/signing are not supported by this version.
