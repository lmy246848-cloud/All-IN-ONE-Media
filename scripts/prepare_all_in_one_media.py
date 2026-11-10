from pathlib import Path
R = Path.cwd()

def replace_once(path, old, new):
    p = R / path
    s = p.read_text(encoding="utf-8")
    if old not in s:
        raise RuntimeError("Expected source fragment missing in " + path + ": " + old[:80])
    p.write_text(s.replace(old, new, 1), encoding="utf-8")

replace_once("pubspec.yaml", "version: 2.3.5+23637", "version: 1.0.0+1")
replace_once("lib/core/app_config.dart", "const String kAppName = 'Zangetsu';", "const String kAppName = 'All IN ONE Media';")
replace_once("android/app/build.gradle.kts", 'applicationId = "com.spyou.watch_app"', 'applicationId = "com.allinonemedia.entertainment"')

# Disable the optional proprietary Discord native SDK, which is not in the repository.
p = R / "android/app/build.gradle.kts"
g = p.read_text(encoding="utf-8")
cmake = '''    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
'''
discord = '''    val discordSdk = rootProject.file("discord-sdk/discord_partner_sdk.aar")
    require(discordSdk.exists()) {
        "Discord SDK missing: run python3 tool/setup_discord_sdk.py /path/to/DiscordSocialSdk-1.10.19337.zip"
    }
    implementation(files(discordSdk))
'''
if cmake not in g or discord not in g:
    raise RuntimeError("Expected optional Discord build configuration not found")
g = g.replace(cmake, "", 1).replace(discord, "", 1)
p.write_text(g, encoding="utf-8")

bridge = R / "android/app/src/main/kotlin/com/spyou/watch_app/DiscordPresenceBridge.kt"
bridge.write_text("""package com.spyou.watch_app

import android.app.Activity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel

/** Discord Rich Presence is optional and disabled in this build. */
class DiscordPresenceBridge(activity: Activity, messenger: BinaryMessenger) {
    private val channel = MethodChannel(messenger, "zangetsu/discord_presence")
    init {
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "clear", "close" -> result.success(null)
                "start", "update" -> result.error("DISCORD_DISABLED",
                    "Discord Rich Presence is not configured in this build.", null)
                else -> result.notImplemented()
            }
        }
    }
    fun dispose() { channel.setMethodCallHandler(null) }
}
""", encoding="utf-8")

p = R / "android/app/src/main/AndroidManifest.xml"
m = p.read_text(encoding="utf-8")
m = m.replace('android:label="Zangetsu"', 'android:label="All IN ONE Media"')
m = m.replace('android:scheme="zangetsu"', 'android:scheme="allinonemedia"')
p.write_text(m, encoding="utf-8")

# The saved language preference remains editable; a fresh install starts in English.
p = R / "lib/core/locale/locale_controller.dart"
s = p.read_text(encoding="utf-8")
s = s.replace("if (v == null || v.isEmpty) return systemTag;", "if (v == null || v.isEmpty) return 'en';")
p.write_text(s, encoding="utf-8")

icon = """<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#e34bff"/><stop offset=".5" stop-color="#6548ff"/><stop offset="1" stop-color="#14d8ff"/></linearGradient></defs><rect width="512" height="512" rx="112" fill="#08051b"/><rect x="8" y="8" width="496" height="496" rx="104" fill="none" stroke="url(#g)" stroke-width="10"/><path d="M67 186Q153 94 255 174V370Q157 298 67 367Z" fill="url(#g)" stroke="#f5a0ff" stroke-width="5"/><path d="M445 186Q357 94 257 174V370Q355 298 445 367Z" fill="url(#g)" stroke="#7eeeff" stroke-width="5"/><path d="M214 220Q214 204 230 213L326 266Q341 275 326 284L230 337Q214 346 214 330Z" fill="#ff62c7" stroke="#fff" stroke-width="5"/></svg>"""
(R / "assets/icon/all_in_one_media.svg").write_text(icon, encoding="utf-8")
p = R / "pubspec.yaml"
s = p.read_text(encoding="utf-8")
s = s.replace("version: 2.3.5+23637", "version: 1.0.0+1")
s = s.replace('  image_path: "assets/icon/app_icon.png"', '  image_path: "assets/icon/app_icon.png"\n  adaptive_icon_background: "#08051B"\n  adaptive_icon_foreground: "assets/icon/app_icon.png"')
p.write_text(s, encoding="utf-8")

readme = """# All IN ONE Media

A separate Android entertainment app fork. The previous All IN ONE app and upstream projects are not modified.

The inherited base includes anime/movie/series streaming through installed providers, manga/manhwa reading, light novels, CloudStream .cs3, Aniyomi/Mihon-style sources, libraries, downloads, history and playback.

The new application identity is All IN ONE Media, with English selected on fresh installs. Android package: com.allinonemedia.entertainment.

The broader integration of Hikari Stremio/.hiki and Vega providers remains future work. ANI-KUTA and AnimeZe were not copied because their licence status was not verified. HGhub code is not copied because its PolyForm Noncommercial licence restricts redistribution.

Retain upstream LICENSE, NOTICE and third-party notices. Base source is GPL-3.0.
"""
(R / "README-ALL-IN-ONE-MEDIA.md").write_text(readme, encoding="utf-8")
print("Prepared All IN ONE Media branding, English-first language, custom icon and Android package identity.")


# Add a simple vertical Shorts reader backed by user-supplied JSON feeds.
replace_once("lib/core/ui/nav_prefs.dart",
             "enum DockTab {\n  home('Home'),",
             "enum DockTab {\n  home('Home'),\n  shortDrama('Shorts'),")
replace_once("lib/core/ui/nav_prefs.dart",
             "  static const List<DockTab> defaultTabs = [\n    DockTab.home,\n    DockTab.myList,\n    DockTab.sources,\n    DockTab.profile,\n  ];",
             "  static const List<DockTab> defaultTabs = [\n    DockTab.home,\n    DockTab.shortDrama,\n    DockTab.myList,\n    DockTab.profile,\n  ];")
replace_once("lib/l10n/ui_strings.dart",
             "        DockTab.home => context.l10n.home,",
             "        DockTab.home => context.l10n.home,\n        DockTab.shortDrama => 'Shorts',")
replace_once("lib/features/shell/root_shell.dart",
             "import '../search/browse_sources_screen.dart';",
             "import '../search/browse_sources_screen.dart';\nimport '../short_drama/short_drama_screen.dart';")
replace_once("lib/features/shell/root_shell.dart",
             "            DockTab.home => shared[0],",
             "            DockTab.home => shared[0],\n            DockTab.shortDrama => const ShortDramaScreen(),")
replace_once("lib/features/shell/root_shell.dart",
             "  DockTab.sources => (Icons.extension_outlined, Icons.extension_rounded),",
             "  DockTab.sources => (Icons.extension_outlined, Icons.extension_rounded),\n  DockTab.shortDrama => (Icons.video_library_outlined, Icons.video_library_rounded),")

shorts = r'''import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';

class ShortDramaScreen extends StatefulWidget {
  const ShortDramaScreen({super.key});
  @override
  State<ShortDramaScreen> createState() => _ShortDramaScreenState();
}

class _ShortDramaScreenState extends State<ShortDramaScreen> {
  final _pages = PageController();
  final _dio = Dio();
  late final Player _player = Player();
  late final VideoController _video = VideoController(_player);
  List<Map<String, dynamic>> _videos = [];
  int _index = 0;
  bool _loading = true;
  String _message = 'Add a JSON feed to watch short dramas.';

  @override
  void initState() {
    super.initState();
    unawaited(_load());
  }

  @override
  void dispose() {
    _pages.dispose();
    unawaited(_player.dispose());
    _dio.close(force: true);
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final result = <Map<String, dynamic>>[];
    try {
      final box = await Hive.openBox<String>('all_in_one_short_drama_feeds');
      for (final url in box.values.toList()) {
        try {
          final response = await _dio.get<dynamic>(url);
          dynamic data = response.data;
          if (data is String) data = jsonDecode(data);
          dynamic entries = data;
          if (data is Map) {
            entries = data['videos'] ?? data['items'] ?? data['results'] ?? data['episodes'];
          }
          if (entries is List) {
            for (final item in entries) {
              if (item is Map) {
                final clip = Map<String, dynamic>.from(item);
                final videoUrl = clip['videoUrl'] ?? clip['url'] ?? clip['streamUrl'];
                final uri = videoUrl is String ? Uri.tryParse(videoUrl) : null;
                if (uri != null && (uri.scheme == 'https' || uri.scheme == 'http')) {
                  result.add(clip);
                }
              }
            }
          }
        } catch (_) {}
      }
      if (!mounted) return;
      setState(() {
        _videos = result;
        _loading = false;
        _message = result.isEmpty ? 'No playable videos found. Add a JSON feed.' : '';
      });
      if (result.isNotEmpty) unawaited(_play(0));
    } catch (error) {
      if (mounted) setState(() { _loading = false; _message = error.toString(); });
    }
  }

  Future<void> _play(int index) async {
    if (index < 0 || index >= _videos.length) return;
    final item = _videos[index];
    final url = item['videoUrl'] ?? item['url'] ?? item['streamUrl'];
    if (url is String) {
      try { await _player.open(Media(url)); } catch (_) {}
    }
  }

  Future<void> _addFeed() async {
    final controller = TextEditingController();
    final value = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Add short-drama JSON feed'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.url,
          decoration: const InputDecoration(hintText: 'https://example.com/dramas.json'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(dialogContext, controller.text.trim()), child: const Text('Add')),
        ],
      ),
    );
    controller.dispose();
    if (value == null || value.isEmpty) return;
    final uri = Uri.tryParse(value);
    if (uri == null || (uri.scheme != 'https' && uri.scheme != 'http')) return;
    final box = await Hive.openBox<String>('all_in_one_short_drama_feeds');
    await box.put(value, value);
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(backgroundColor: Colors.black, body: Center(child: CircularProgressIndicator()));
    }
    if (_videos.isEmpty) {
      return Scaffold(
        backgroundColor: Colors.black,
        body: SafeArea(
          child: Center(
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              const Icon(Icons.video_library_rounded, color: Colors.deepPurpleAccent, size: 60),
              const SizedBox(height: 16),
              const Text('SHORT DRAMAS', style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              Padding(padding: const EdgeInsets.all(24), child: Text(_message, textAlign: TextAlign.center, style: const TextStyle(color: Colors.white70))),
              FilledButton.icon(onPressed: _addFeed, icon: const Icon(Icons.add), label: const Text('Add feed')),
            ]),
          ),
        ),
      );
    }
    return Scaffold(
      backgroundColor: Colors.black,
      body: PageView.builder(
        controller: _pages,
        scrollDirection: Axis.vertical,
        itemCount: _videos.length,
        onPageChanged: (index) { setState(() => _index = index); unawaited(_play(index)); },
        itemBuilder: (context, index) {
          final item = _videos[index];
          final title = item['title'] is String ? item['title'] as String : 'Short drama';
          final description = item['description'] is String ? item['description'] as String : '';
          return Stack(fit: StackFit.expand, children: [
            if (index == _index) Video(controller: _video, controls: NoVideoControls),
            const DecoratedBox(decoration: BoxDecoration(gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Color(0x99000000), Colors.transparent, Color(0xDD000000)]))),
            SafeArea(child: Stack(children: [
              Positioned(top: 12, left: 16, right: 16, child: Row(children: [
                const Expanded(child: Text('SHORT DRAMAS', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, letterSpacing: 1.3))),
                IconButton(onPressed: _addFeed, icon: const Icon(Icons.add_circle_outline, color: Colors.white)),
                IconButton(onPressed: _load, icon: const Icon(Icons.refresh, color: Colors.white)),
              ])),
              Positioned(left: 18, right: 24, bottom: 42, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(title, style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
                if (description.isNotEmpty) Padding(padding: const EdgeInsets.only(top: 8), child: Text(description, style: const TextStyle(color: Colors.white70))),
              ])),
            ])),
          ]);
        },
      ),
    );
  }
}
'''
(R / "lib/features/short_drama").mkdir(parents=True, exist_ok=True)
(R / "lib/features/short_drama/short_drama_screen.dart").write_text(shorts, encoding="utf-8")

wordmark = r'''<svg xmlns="http://www.w3.org/2000/svg" width="620" height="90" viewBox="0 0 620 90"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="0"><stop stop-color="#e879ff"/><stop offset=".55" stop-color="#9a75ff"/><stop offset="1" stop-color="#4be4ff"/></linearGradient></defs><text x="4" y="62" font-family="DejaVu Sans,sans-serif" font-size="49" font-weight="800" letter-spacing="2" fill="url(#g)">ALL IN ONE</text><text x="404" y="62" font-family="DejaVu Sans,sans-serif" font-size="23" font-weight="600" letter-spacing="3" fill="#fff">MEDIA</text></svg>'''
(R / "assets/icon/all_in_one_wordmark.svg").write_text(wordmark, encoding="utf-8")
