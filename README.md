# All IN ONE Media — independent rebuild

This branch is a clean Android-native Kotlin/Jetpack Compose implementation. It is not the old Zangetsu rebrand and does not modify the main branch or previous app.

## First milestone
- Android app shell with Home, Anime, Movies, Manga, Novels, Shorts, Library and Settings sections.
- Working anime and manga catalogue search using the public AniList GraphQL API.
- Search results show title and episode/chapter metadata.
- GitHub Actions builds a debug APK for ARM64-compatible Android devices (universal debug APK).

## Not implemented yet
Movies/series metadata and playback, manga page reader/source adapters, light-novel EPUB reader, persistent library/progress, downloads, vertical short-video player/feed, and third-party provider integrations. Navigation sections do not imply those features are already functional.

## Build
GitHub Actions workflow: Android APK. It runs on pushes to scratch-rebuild and uploads app-debug.apk as an artifact.

This is an independent implementation. It does not copy code or media from the referenced projects. External catalogues and video sources must be used in accordance with their terms and licences.
