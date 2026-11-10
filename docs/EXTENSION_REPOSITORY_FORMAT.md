# Extension compatibility

All IN ONE Media 0.4.0 does not currently have an executable third-party plugin runtime or an installable plugin-repository browser.

## What can be added

On the **Extensions** screen, add a direct HTTPS URL that returns one of these:
- an All IN ONE Media-compatible JSON catalogue feed,
- an M3U playlist,
- a direct MP4, WebM, M4V, HLS/M3U8, or DASH/MPD stream,
- an HTTPS website page that can be opened in the in-app browser.

JSON feeds can include nested `episodes` or `chapters` lists. See [PROVIDER_FORMAT.md](PROVIDER_FORMAT.md) for examples.

## What cannot be installed as a URL

A repository URL or plugin package made for Mihon/Komikku, Aniyomi, CloudStream, LNReader, or Mangayomi is not interchangeable with a JSON feed. Those projects have their own extension contracts and runtimes. All IN ONE Media does not download or execute their plugin code.

To add support for another ecosystem properly, the project needs a dedicated adapter for its source API or a separately licensed and reviewed runtime. Arbitrarily executing downloaded scripts or APKs would create a serious device-security risk.

The app does not provide sources or host copyrighted media. Use sources you are authorised to access; DRM, paywall, login, and access-control bypasses are not supported.
