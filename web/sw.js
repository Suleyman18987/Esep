const CACHE='esep-web-2.5-v1';
const FILES=["./", "index.html", "styles.css", "app.js", "engine.js", "artwork.js", "assets/app-orbit.svg", "assets/app-prism.svg", "assets/app-wave.svg", "assets/Lucide-LICENSE.txt", "assets/orbit.png", "assets/prism.png", "assets/wave.png", "manifest-wave.webmanifest", "manifest-orbit.webmanifest", "manifest-prism.webmanifest", "assets/app-wave-192.png", "assets/app-wave-512.png", "assets/app-orbit-192.png", "assets/app-orbit-512.png", "assets/app-prism-192.png", "assets/app-prism-512.png"];
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(FILES.map(url=>new Request(url,{cache:'reload'}))))));
self.addEventListener('activate',event=>event.waitUntil(Promise.all([caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('esep-web-')&&key!==CACHE).map(key=>caches.delete(key)))),self.clients.claim()])));
self.addEventListener('message',event=>{if(event.data?.type==='SKIP_WAITING')self.skipWaiting();});
self.addEventListener('fetch',event=>{
 if(event.request.method!=='GET'||new URL(event.request.url).origin!==self.location.origin)return;
 if(event.request.mode==='navigate'){event.respondWith(fetch(event.request).catch(()=>caches.match('./')));return;}
 event.respondWith(caches.match(event.request).then(hit=>hit||fetch(event.request)));
});
