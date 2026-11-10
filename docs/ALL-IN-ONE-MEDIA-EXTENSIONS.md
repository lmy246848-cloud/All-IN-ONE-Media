# All IN ONE Media — Extension and playback guide

## What the app does

All IN ONE Media is a player and reader that uses compatible third-party sources. It does not host the shows, films, manga, or novels itself. A source supplies the catalogue and the actual episode stream or chapter content.

The full Android build includes these source runtimes:

- **Zangetsu JavaScript providers** — repository manifests that install JavaScript catalogue providers.
- **CloudStream** — Android `.cs3` extensions.
- **Aniyomi** — compatible anime-source extension packages.
- **Mihon** — compatible manga-source extension packages.
- **Mangayomi** — compatible JavaScript and Dart video, manga, and novel sources through the source bridge.
- **LNReader** — compatible JavaScript novel sources.

These are separate ecosystems. A repository URL for one ecosystem is not automatically valid in another. Stremio add-ons, browser extensions, arbitrary APKs, or arbitrary JavaScript packages are not universal formats and are not automatically supported unless a dedicated adapter exists.

## The normal watch/read flow

1. Open the **Sources** tab in the bottom navigation.
2. Open **Providers** and choose the ecosystem that matches the repository you have.
3. Open its **Repositories** tab, add the repository URL, and refresh/sync it.
4. Install one or more extensions from that repository. Wait for the install result; a repository being added is not the same as an extension being installed.
5. Return to **Sources**. Choose **Streaming**, **Manga**, or **Novel** and tap an installed source.
6. Browse or search its catalogue. Open a title. For video, choose an episode and press **Play**. For manga, open a chapter and read its pages. For a novel, open a chapter and read the text/HTML inside the app.

The Providers screen also includes **Browse installed sources** once at least one source is installed.

## If an installed source shows no titles

- Confirm the source is installed under the correct ecosystem and is enabled where that ecosystem supports enabling.
- Check the source's language filter. A source can be installed but hidden from the browse list by language preferences.
- Use pull-to-refresh or the repository refresh action.
- Try the source's search field. Some sources expose search but do not implement a home/popular catalogue.
- A source may have been abandoned, changed its website, added anti-bot protection, or stopped returning playable links. No app can make a broken remote source work without updating its adapter/source code.

## Safety and limits

Extensions are third-party code and can make network requests to their own websites. Install repositories you trust, and review their permissions/maintainers where available. All IN ONE Media does not bypass DRM, subscriptions, logins, or access restrictions. Content availability and playback depend on the source and the rights it has to distribute the material.

The app's upstream cloud account/sync backend is not used by this fork. Your library and reading/watch progress remain local unless a separately configured, fork-owned backend is added.
