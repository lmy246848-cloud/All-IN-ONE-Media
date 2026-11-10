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
