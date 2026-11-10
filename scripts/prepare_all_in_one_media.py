from pathlib import Path
import re
R=Path.cwd()
def change(p,old,new,count=1):
 f=R/p;s=f.read_text(encoding='utf-8');n=s.count(old)
 if n!=count: raise RuntimeError(f'{p}: expected {count} instances, found {n}: {old[:80]!r}')
 f.write_text(s.replace(old,new,count),encoding='utf-8')

change('pubspec.yaml','version: 2.3.5+23637','version: 1.0.0+1')
change('pubspec.yaml','  android: true\n  ios: true','  android: true\n  adaptive_icon_background: "#08051B"\n  adaptive_icon_foreground: "assets/icon/app_icon.png"\n  ios: true')
change('lib/core/app_config.dart',"const String kAppName = 'Zangetsu';","const String kAppName = 'All IN ONE Media';")
change('lib/core/app_config.dart',"const String kDiscordInviteUrl = 'https://discord.gg/hey6vz9kg6';","const String kDiscordInviteUrl = '';" )
change('lib/core/app_config.dart',"const String kLogIntakeUrl = 'https://zangetsu-logs.log-intake.workers.dev';","const String kLogIntakeUrl = '';" )
change('lib/core/app_config.dart',"    'https://avatars.zangetsu.online/profile-picker/manifest.json';","    '';" )
change('lib/core/app_config.dart',"    'https://raw.githubusercontent.com/Spyou/Zangetsu/main/announcements.json';","    '';" )

ep=R/'lib/core/environment.dart';e=ep.read_text(encoding='utf-8')
for a,b in [
("static const String appwriteProjectId = '6a1ed44f0029b50bccde';","static const String appwriteProjectId = '';"),
("static const String appwriteProjectName = 'Zangetsu';","static const String appwriteProjectName = 'All IN ONE Media';"),
("    'https://sgp.cloud.appwrite.io/v1';","    '';"),
("defaultValue: 'https://eogwzrlfoercfwcfwlmv.supabase.co',","defaultValue: ''),
("defaultValue: 'https://sync.zangetsu.online',","defaultValue: ''),
("static const String passwordResetUrl = 'https://zangetsu.online/';","static const String passwordResetUrl = '';"),
("static const String siteBaseUrl = 'https://zangetsu.online';","static const String siteBaseUrl = '';"),
("static const String trackerRedirectScheme = 'zangetsu';","static const String trackerRedirectScheme = 'allinonemedia';"),
("static const String simklAppName = 'zangetsu';","static const String simklAppName = 'allinonemedia';"),
("static const String anilistClientId = '43052';","static const String anilistClientId = '';"),
("static const String malClientId = 'ac006943589381143c4c4e54eac93a89';","static const String malClientId = '';"),
("static const String mangabakaClientId = 'EkBIEASsBsfRvPKGevUZIcRLSdubVHPK';","static const String mangabakaClientId = '';"),
]: e=e.replace(a,b,1)
e=re.sub(r"defaultValue:\s*'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9\.[^']+'\s*,", "defaultValue: ''),", e, count=1)
e=re.sub(r"static const String simklClientId =\s*'[^']+';", "static const String simklClientId = '';");", e, count=1)
e=re.sub(r"static const String simklClientSecret =\s*'[^']+';", "static const String simklClientSecret = '';");", e, count=1)
ep.write_text(e,encoding='utf-8')

mp=R/'lib/main.dart';m=mp.read_text(encoding='utf-8')
m=m.replace("final bootOffline = await probeBootOffline(\n        host: Uri.tryParse(Environment.vpsSyncUrl)?.host ?? '',\n      );", "final bootOffline = Environment.vpsSyncUrl.isEmpty ? true : await probeBootOffline(\n        host: Uri.tryParse(Environment.vpsSyncUrl)?.host ?? '',\n      );",1)
m=m.replace("Future&lt;void&gt; _retrySupabaseInit() async {\n  for (var attempt = 0; attempt &lt; 15; attempt++) {", "Future&lt;void&gt; _retrySupabaseInit() async {\n  if (Environment.supabaseUrl.isEmpty || Environment.supabaseAnonKey.isEmpty) return;\n  for (var attempt = 0; attempt &lt; 15; attempt++) {",1)
mp.write_text(m,encoding='utf-8')

gp=R/'android/app/build.gradle.kts';g=gp.read_text(encoding='utf-8')
g=g.replace('applicationId = "com.spyou.watch_app"','applicationId = "com.allinonemedia.entertainment"',1)
cm='    externalNativeBuild {\n        cmake {\n            path = file("src/main/cpp/CMakeLists.txt")\n            version = "3.22.1"\n        }\n    }\n'
if cm not in g: raise RuntimeError('Discord CMake block not found')
g=g.replace(cm,'',1)
dc='    val discordSdk = rootProject.file("discord-sdk/discord_partner_sdk.aar")\n    require(discordSdk.exists()) {\n        "Discord SDK missing: run python3 tool/setup_discord_sdk.py /path/to/DiscordSocialSdk-1.10.19337.zip"\n    }\n    implementation(files(discordSdk))\n'
if dc not in g: raise RuntimeError('Discord AAR dependency block not found')
gp.write_text(g.replace(dc,'',1),encoding='utf-8')
(R/'android/app/src/main/kotlin/com/spyou/watch_app/DiscordPresenceBridge.kt').write_text('''package com.spyou.watch_app
import android.app.Activity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
/** Optional Discord Rich Presence is disabled: its proprietary native SDK is not bundled. */
class DiscordPresenceBridge(activity: Activity, messenger: BinaryMessenger) {
  private val channel = MethodChannel(messenger, "zangetsu/discord_presence")
  init { channel.setMethodCallHandler { call, result -> when (call.method) {
    "close", "clear" -> result.success(null)
    "start", "update" -> result.error("DISCORD_DISABLED", "Discord Rich Presence is not configured in this build.", null)
    else -> result.notImplemented()
  } } }
  fun dispose() { channel.setMethodCallHandler(null) }
}
''',encoding='utf-8')

man=R/'android/app/src/main/AndroidManifest.xml';s=man.read_text(encoding='utf-8')
s=s.replace('android:label="Zangetsu"','android:label="All IN ONE Media"').replace('android:scheme="zangetsu"','android:scheme="allinonemedia"')
s=re.sub(r'@mipmap/ic_launcher_(?:crescent|default|classic)','@mipmap/ic_launcher',s)
man.write_text(s,encoding='utf-8')
sp=R/'android/gradle.properties';s=sp.read_text(encoding='utf-8').replace('-Xmx8G','-Xmx5G').replace('MaxMetaspaceSize=4G','MaxMetaspaceSize=2G');sp.write_text(s,encoding='utf-8')

screen=r'''import 'dart:async';
import 'dart:convert';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:hive_flutter/hive_flutter.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';

class _ShortItem {
  const _ShortItem(this.title,this.url,this.source,{this.poster='',this.description=''});
  final String title,url,source,poster,description;
}
class ShortDramaScreen extends StatefulWidget {
  const ShortDramaScreen({super.key});
  @override State&lt;ShortDramaScreen&gt; createState()=>_ShortDramaScreenState();
}
class _ShortDramaScreenState extends State&lt;ShortDramaScreen&gt; {
  static const _boxName='all_in_one_short_drama_feeds';
  final _pages=PageController();
  final _dio=Dio(BaseOptions(connectTimeout:const Duration(seconds:12),receiveTimeout:const Duration(seconds:20),headers:const {'Accept':'application/json'}));
  late final Player _player=Player();
  late final VideoController _video=VideoController(_player);
  final Set&lt;String&gt; _liked={};
  List&lt;_ShortItem&gt; _items=[];
  int _index=0;
  bool _loading=true;
  String? _message,_playError;
  @override void initState(){super.initState();unawaited(_load());}
  @override void dispose(){_pages.dispose();unawaited(_player.dispose());_dio.close(force:true);super.dispose();}
  String _pick(Map&lt;String,dynamic&gt; m,List&lt;String&gt; keys,[String fallback='']){for(final k in keys){final v=m[k];if(v is String&amp;&amp;v.trim().isNotEmpty)return v.trim();}return fallback;}
  bool _url(String s){final u=Uri.tryParse(s);return u!=null&amp;&amp;u.hasScheme&amp;&amp;(u.scheme=='http'||u.scheme=='https');}
  Future&lt;void&gt; _load() async {
    if(mounted)setState(()=>_loading=true);
    final found=&lt; _ShortItem&gt;[];
    try{
      final box=await Hive.openBox&lt;String&gt;(_boxName);
      for(final feed in box.values.toList()){
        try{
          final res=await _dio.get&lt;dynamic&gt;(feed);
          dynamic body=res.data;if(body is String)body=jsonDecode(body);
          var name=Uri.parse(feed).host;dynamic entries;
          if(body is List){entries=body;}else if(body is Map){final map=Map&lt;String,dynamic&gt;.from(body);name=_pick(map,['name','title'],name);entries=map['videos']??map['items']??map['results']??map['episodes'];}
          if(entries is! List)continue;
          for(final raw in entries){if(raw is! Map)continue;final item=Map&lt;String,dynamic&gt;.from(raw);final url=_pick(item,['videoUrl','streamUrl','url','video']);if(!_url(url))continue;found.add(_ShortItem(_pick(item,['title','name'],'Untitled short drama'),url,name,poster:_pick(item,['posterUrl','poster','cover','thumbnail']),description:_pick(item,['description','overview','summary'])));}
        }catch(_){/* Skip one unavailable provider and continue. */}
      }
      if(!mounted)return;
      setState((){_items=found;_index=0;_loading=false;_message=found.isEmpty?'No playable videos were found in your feeds.':null;});
      if(found.isNotEmpty){if(_pages.hasClients)_pages.jumpToPage(0);unawaited(_play(0));}
    }catch(e){if(mounted)setState((){_loading=false;_message='Could not load feeds: $e';});}
  }
  Future&lt;void&gt; _play(int i) async {if(i&lt;0||i&gt;=_items.length)return;setState(()=>_playError=null);try{await _player.open(Media(_items[i].url));}catch(e){if(mounted)setState(()=>_playError='Playback failed: $e');}}
  Future&lt;void&gt; _add() async {
    final c=TextEditingController();
    final url=await showDialog&lt;String&gt;(context:context,builder:(ctx)=>AlertDialog(title:const Text('Add short-drama feed'),content:TextField(controller:c,autofocus:true,keyboardType:TextInputType.url,decoration:const InputDecoration(labelText:'JSON feed URL',hintText:'https://example.com/dramas.json'),onSubmitted:(v)=>Navigator.pop(ctx,v.trim())),actions:[TextButton(onPressed:()=>Navigator.pop(ctx),child:const Text('Cancel')),FilledButton(onPressed:()=>Navigator.pop(ctx,c.text.trim()),child:const Text('Add'))]));
    c.dispose();if(url==null||url.isEmpty)return;if(!_url(url)){if(mounted)ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content:Text('Enter a valid HTTP or HTTPS URL.')));return;}
    final box=await Hive.openBox&lt;String&gt;(_boxName);await box.put(url,url);await _load();
  }
  Future&lt;void&gt; _manage() async {
    final box=await Hive.openBox&lt;String&gt;(_boxName);if(!mounted)return;final urls=box.values.toList();
    await showModalBottomSheet&lt;void&gt;(context:context,showDragHandle:true,builder:(ctx)=>SafeArea(child:urls.isEmpty?const Padding(padding:EdgeInsets.all(24),child:Text('No feeds added yet.')):ListView(children:[for(final url in urls)ListTile(leading:const Icon(Icons.rss_feed_rounded),title:Text(Uri.tryParse(url)?.host??url),subtitle:Text(url,maxLines:1,overflow:TextOverflow.ellipsis),trailing:IconButton(icon:const Icon(Icons.delete_outline),onPressed:()async{await box.delete(url);if(ctx.mounted)Navigator.pop(ctx);await _load();}))])));
  }
  Widget _poster(_ShortItem x)=>x.poster.isEmpty?const ColoredBox(color:Color(0xFF151323),child:Center(child:Icon(Icons.movie_creation_outlined,color:Colors.white54,size:54))):CachedNetworkImage(imageUrl:x.poster,fit:BoxFit.cover,errorWidget:(_,_,_)=&gt;const ColoredBox(color:Color(0xFF151323)));
  @override Widget build(BuildContext context)=&gt;Scaffold(backgroundColor:Colors.black,body:_loading?const Center(child:CircularProgressIndicator()):_items.isEmpty?_empty():PageView.builder(controller:_pages,scrollDirection:Axis.vertical,itemCount:_items.length,onPageChanged:(i){setState(()=>_index=i);unawaited(_play(i));},itemBuilder:(ctx,i){final x=_items[i];final active=i==_index;final liked=_liked.contains(x.url);return Stack(fit:StackFit.expand,children:[_poster(x),if(active)Video(controller:_video,controls:NoVideoControls,fit:BoxFit.cover),const DecoratedBox(decoration:BoxDecoration(gradient:LinearGradient(begin:Alignment.topCenter,end:Alignment.bottomCenter,colors:[Color(0x99000000),Colors.transparent,Color(0xDD000000)],stops:[0,.42,1]))),SafeArea(child:Stack(children:[Positioned(top:8,left:16,right:12,child:Row(children:[const Icon(Icons.bolt,color:Color(0xFFBFA4FF)),const SizedBox(width:8),const Expanded(child:Text('SHORT DRAMAS',style:TextStyle(color:Colors.white,fontWeight:FontWeight.w800,letterSpacing:1.4))),IconButton(tooltip:'Add feed',onPressed:_add,icon:const Icon(Icons.add_circle_outline,color:Colors.white)),IconButton(tooltip:'Manage feeds',onPressed:_manage,icon:const Icon(Icons.tune,color:Colors.white))])),Positioned(left:18,right:78,bottom:106,child:Column(crossAxisAlignment:CrossAxisAlignment.start,mainAxisSize:MainAxisSize.min,children:[Text(x.source.toUpperCase(),style:const TextStyle(color:Color(0xFFD4C5FF),fontSize:11,fontWeight:FontWeight.w700,letterSpacing:1.1)),const SizedBox(height:8),Text(x.title,maxLines:3,overflow:TextOverflow.ellipsis,style:const TextStyle(color:Colors.white,fontSize:22,fontWeight:FontWeight.w800)),if(x.description.isNotEmpty)Padding(padding:const EdgeInsets.only(top:8),child:Text(x.description,maxLines:3,overflow:TextOverflow.ellipsis,style:const TextStyle(color:Colors.white70,fontSize:13))),if(active&amp;&amp;_playError!=null)Text(_playError!,style:const TextStyle(color:Color(0xFFFFB4AB),fontSize:12))])),Positioned(right:12,bottom:150,child:Column(children:[IconButton.filledTonal(onPressed:()=>setState(()=>liked?_liked.remove(x.url):_liked.add(x.url)),icon:Icon(liked?Icons.favorite:Icons.favorite_border,color:liked?const Color(0xFFFF86A6):Colors.white)),const SizedBox(height:12),IconButton.filledTonal(onPressed:_load,icon:const Icon(Icons.refresh,color:Colors.white))])),Positioned(left:18,bottom:24,child:Text('${i+1} / ${_items.length} · Swipe up for next',style:const TextStyle(color:Colors.white70,fontSize:11)))]))]);}));}
  Widget _empty()=>SafeArea(child:Center(child:Padding(padding:const EdgeInsets.all(28),child:Column(mainAxisSize:MainAxisSize.min,children:[const Icon(Icons.video_library_rounded,color:Color(0xFFBCA6FF),size:64),const SizedBox(height:18),const Text('Your short-drama feed',style:TextStyle(color:Colors.white,fontSize:23,fontWeight:FontWeight.w800)),const SizedBox(height:10),Text(_message??'Add a JSON feed to start watching vertical short dramas.',textAlign:TextAlign.center,style:const TextStyle(color:Colors.white70,height:1.45)),const SizedBox(height:18),FilledButton.icon(onPressed:_add,icon:const Icon(Icons.add),label:const Text('Add a feed')),const SizedBox(height:14),const Text('{ "name": "My Dramas", "videos": [{ "title": "Episode 1", "videoUrl": "https://…/video.m3u8", "posterUrl": "https://…/cover.jpg" }] }',textAlign:TextAlign.center,style:TextStyle(color:Colors.white38,fontSize:11))]))));
}
'''
(R/'lib/features/short_drama').mkdir(parents=True,exist_ok=True)
(R/'lib/features/short_drama/short_drama_screen.dart').write_text(screen,encoding='utf-8')

change('lib/core/ui/nav_prefs.dart',"  home('Home'),\n  myList('My List'),","  home('Home'),\n  shortDrama('Shorts'),\n  myList('My List'),")
change('lib/core/ui/nav_prefs.dart',"  static const List&lt;DockTab&gt; defaultTabs = [\n    DockTab.home,\n    DockTab.myList,\n    DockTab.sources,\n    DockTab.profile,\n  ];","  static const List&lt;DockTab&gt; defaultTabs = [\n    DockTab.home,\n    DockTab.shortDrama,\n    DockTab.myList,\n    DockTab.profile,\n  ];")
change('lib/l10n/ui_strings.dart',"        DockTab.home =&gt; context.l10n.home,\n        DockTab.myList =&gt; context.l10n.myList,","        DockTab.home =&gt; context.l10n.home,\n        DockTab.shortDrama =&gt; 'Shorts',\n        DockTab.myList =&gt; context.l10n.myList,")
change('lib/features/shell/root_shell.dart',"import '../search/browse_sources_screen.dart';","import '../search/browse_sources_screen.dart';\nimport '../short_drama/short_drama_screen.dart';")
change('lib/features/shell/root_shell.dart',"            DockTab.home =&gt; shared[0],\n            DockTab.search =&gt; shared[1],","            DockTab.home =&gt; shared[0],\n            DockTab.shortDrama =&gt; const ShortDramaScreen(),\n            DockTab.search =&gt; shared[1],")
change('lib/features/shell/root_shell.dart',"  DockTab.sources =&gt; (Icons.extension_outlined, Icons.extension_rounded),","  DockTab.sources =&gt; (Icons.extension_outlined, Icons.extension_rounded),\n  DockTab.shortDrama =&gt; (Icons.video_library_outlined, Icons.video_library_rounded),")

hp=R/'lib/features/home/home_screen.dart';h=hp.read_text(encoding='utf-8')
old='''          final actions = [
            HomeDownloadsAction(edgeBanner: edgeBanner),
            _notificationBell(context, edgeBanner: edgeBanner),
            HomeSearchAction(
              edgeBanner: edgeBanner,
              fixedSourceId: _sourceCatalogueSelected
                  ? _sourceCatalogueSourceId
                  : null,
            ),
          ];'''
 ew='''          final actions = [
            HomeDownloadsAction(edgeBanner: edgeBanner),
            _notificationBell(context, edgeBanner: edgeBanner),
            IconButton(
              tooltip: 'Providers &amp; extensions',
              onPressed: () =&gt; Navigator.of(context).push&lt;void&gt;(MaterialPageRoute&lt;void&gt;(builder: (_) =&gt; const ProvidersHubScreen())),
              icon: const Icon(Icons.extension_rounded),
            ),
            HomeSearchAction(
              edgeBanner: edgeBanner,
              fixedSourceId: _sourceCatalogueSelected
                  ? _sourceCatalogueSourceId
                  : null,
            ),
          ];'''
if old not in h:raise RuntimeError('Home header action row not found')
h=h.replace(old,new,1).replace("tooltip: 'Zangetsu',","tooltip: 'All IN ONE Media',").replace("'assets/icon/logo_mark.png'","'assets/icon/app_icon.png'")
hp.write_text(h,encoding='utf-8')

icon=r'''&lt;svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512"&gt;&lt;defs&gt;&lt;linearGradient id="edge" x1="0" y1="0" x2="1" y2="1"&gt;&lt;stop stop-color="#e34bff"/&gt;&lt;stop offset=".52" stop-color="#5b42ff"/&gt;&lt;stop offset="1" stop-color="#14d8ff"/&gt;&lt;/linearGradient&gt;&lt;linearGradient id="page" x1="0" y1="0" x2="1" y2="1"&gt;&lt;stop stop-color="#f17aff"/&gt;&lt;stop offset=".48" stop-color="#7346ff"/&gt;&lt;stop offset="1" stop-color="#42e5ff"/&gt;&lt;/linearGradient&gt;&lt;linearGradient id="play" x1="0" y1="0" x2="1" y2="1"&gt;&lt;stop stop-color="#ff62c7"/&gt;&lt;stop offset=".55" stop-color="#a64cff"/&gt;&lt;stop offset="1" stop-color="#ffe080"/&gt;&lt;/linearGradient&gt;&lt;filter id="glow" x="-50%" y="-50%" width="200%" height="200%"&gt;&lt;feGaussianBlur stdDeviation="9" result="b"/&gt;&lt;feMerge&gt;&lt;feMergeNode in="b"/&gt;&lt;feMergeNode in="SourceGraphic"/&gt;&lt;/feMerge&gt;&lt;/filter&gt;&lt;/defs&gt;&lt;rect width="512" height="512" rx="112" fill="#08051b"/&gt;&lt;rect x="8" y="8" width="496" height="496" rx="104" fill="none" stroke="url(#edge)" stroke-width="10" filter="url(#glow)"/&gt;&lt;path d="M54 115 C156 38 286 42 438 102 C370 99 286 124 256 190 C225 125 144 96 54 115Z" fill="#22115c" stroke="#6948ff" stroke-width="3" opacity=".8"/&gt;&lt;path d="M67 186 Q153 94 255 174 L255 370 Q157 298 67 367Z" fill="url(#page)" stroke="#f5a0ff" stroke-width="5" filter="url(#glow)"/&gt;&lt;path d="M445 186 Q357 94 257 174 L257 370 Q355 298 445 367Z" fill="url(#page)" stroke="#7eeeff" stroke-width="5" filter="url(#glow)"/&gt;&lt;path d="M93 208 Q162 153 229 199 L229 324 Q159 278 93 343Z" fill="#110c35" stroke="#ff9af5" stroke-width="4"/&gt;&lt;path d="M419 208 Q350 153 283 199 L283 324 Q353 278 419 343Z" fill="#0a1944" stroke="#83e9ff" stroke-width="4"/&gt;&lt;g fill="url(#edge)" stroke="#fff" stroke-opacity=".55" stroke-width="2"&gt;&lt;rect x="110" y="224" width="19" height="26" rx="5"/&gt;&lt;rect x="110" y="269" width="19" height="26" rx="5"/&gt;&lt;rect x="110" y="314" width="19" height="26" rx="5"/&gt;&lt;rect x="383" y="224" width="19" height="26" rx="5"/&gt;&lt;rect x="383" y="269" width="19" height="26" rx="5"/&gt;&lt;rect x="383" y="314" width="19" height="26" rx="5"/&gt;&lt;/g&gt;&lt;path d="M214 220 Q214 204 230 213 L326 266 Q341 275 326 284 L230 337 Q214 346 214 330Z" fill="url(#play)" stroke="#fff" stroke-width="5" filter="url(#glow)"/&gt;&lt;path d="M54 393 Q169 465 256 390 Q343 465 458 393" fill="none" stroke="url(#edge)" stroke-width="9" stroke-linecap="round"/&gt;&lt;g fill="#fff"&gt;&lt;path d="M112 91l8 18 18 8-18 8-8 18-8-18-18-8 18-8z"/&gt;&lt;path d="M407 93l6 13 13 6-13 6-6 13-6-13-13-6 13-6z"/&gt;&lt;circle cx="78" cy="430" r="4"/&gt;&lt;circle cx="437" cy="430" r="4"/&gt;&lt;/g&gt;&lt;/svg&gt;'''
(R/'assets/icon/all_in_one_media.svg').write_text(icon,encoding='utf-8')
word=r'''&lt;svg xmlns="http://www.w3.org/2000/svg" width="620" height="90" viewBox="0 0 620 90"&gt;&lt;defs&gt;&lt;linearGradient id="g" x1="0" y1="0" x2="1" y2="0"&gt;&lt;stop stop-color="#e879ff"/&gt;&lt;stop offset=".55" stop-color="#9a75ff"/&gt;&lt;stop offset="1" stop-color="#4be4ff"/&gt;&lt;/linearGradient&gt;&lt;/defs&gt;&lt;text x="4" y="62" font-family="DejaVu Sans,sans-serif" font-size="49" font-weight="800" letter-spacing="2" fill="url(#g)"&gt;ALL IN ONE&lt;/text&gt;&lt;text x="404" y="62" font-family="DejaVu Sans,sans-serif" font-size="23" font-weight="600" letter-spacing="3" fill="#fff"&gt;MEDIA&lt;/text&gt;&lt;/svg&gt;'''
(R/'assets/icon/all_in_one_wordmark.svg').write_text(word,encoding='utf-8')

readme='''# All IN ONE Media

Separate personal-use Android fork; the upstream Zangetsu project and previous All IN ONE APK remain untouched.

Included base functionality: anime/movie/series streaming through installed providers, manga/manhwa via Mihon/AnIyomi-compatible runtime, novels via LNReader, CloudStream .cs3, Aniyomi, Mihon, Mangayomi and Zangetsu JS sources; existing player, reader, library, downloads, history and themes.

New Shorts tab: vertical playback from user-added JSON feeds. Each feed returns videos, items, or results; each item contains title, videoUrl (MP4/HLS), and optional posterUrl/description.

Not yet integrated: Hikari .hiki/Stremio runtime and Vega providers. ANI-KUTA has no finalized licence and AnimeZe did not expose a clear licence during the audit, so their code is not copied.

This fork disables the upstream author's Supabase, Appwrite, VPS sync, announcements and diagnostic upload endpoints by default. It starts local-first; cloud services and tracker sign-ins require credentials registered for this fork.

The base and CloudStream runtime are GPL-3.0. Retain LICENSE, NOTICE.md and third-party notices. Android application ID: com.allinonemedia.entertainment.
'''
(R/'README-ALL-IN-ONE-MEDIA.md').write_text(readme,encoding='utf-8')
for p in (R/'lib').rglob('*.dart'):
 s=p.read_text(encoding='utf-8')
 s=s.replace('Spyou/Zangetsu','lmy246848-cloud/All-IN-ONE-Media')
 p.write_text(s,encoding='utf-8')
print('Prepared All IN ONE Media brand, local-first configuration, ARM64 package identity, Short Dramas tab and custom icon assets.')
