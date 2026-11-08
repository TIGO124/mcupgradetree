# UpgradeTree 2.2 — Minecraft 26.3 Altıgen Yükseltme Ağacı

Roblox tarzı yükseltme ağacı. Sandık menüsü yerine oyuncunun önünde, sadece ona görünen 3B bir panel açılır:
yıldızlı uzay arka planında petek gibi dizilmiş altıgen düğümler. Bir düğüm alınınca ondan sonraki düğümler
ortaya çıkar (kademeli açılma).

*English summary at the bottom.*

## Kurulum
1. Sunucu: Paper 26.3 (veya Purpur/Leaf gibi bir Paper türevi), Java 25.
2. `UpgradeTree-2.2.0.jar` dosyasını sunucunun `plugins` klasörüne at, sunucuyu yeniden başlat.
3. Ayarlar: `plugins/UpgradeTree/config.yml`, ağaçlar: `trees/*.yml` (her dosya bir sekme), sandıklar: `chests.yml`, mesajlar: `lang/tr.yml`, `lang/en.yml`.
4. 1.x'ten geçiş: eski `tree.yml` kendiliğinden `trees/ana.yml` olur, oyuncu verileri olduğu gibi kalır.

## Özellikler
- **Altıgen panel:** renkli kenarlı düğümler, alınanların etrafında parlama halkası, alınabilenlerde nabız, ilerleme yazısı ("3/33 Tamamlandı!").
- **Kademeli açılma:** düğüm, gerektirdiği düğümlerden biri alınınca görünür (`menu.progressive-reveal`).
- **Sekmeler:** her `trees/*.yml` dosyası ayrı bir ağaç. Hazır gelenler: Ana Ağaç, Yetenekler, Toplayıcı. Sonda **Chests** sekmesi.
- **Chests (CS2 tarzı sandıklar):** menüdeki Chests bölümünde sandık satın alınır. Sandık oyuncunun önünde belirir; sağ tıklayınca sallanır, kapağı açılır, üstünde eşya şeridi döner ve yavaşlayıp kazanılan ödülde durur. Ödüller Minecraft eşyaları ve XP seviyesi; nadirlik renkleri ve şanslar CS2 kasaları gibi.
- **Gelişmiş gereksinimler:** `requires`, `requires-any` (biri yeter), `requires-level` (belirli seviye), `exclusive-with` (sınıf seçimi), `permission`, `min-prestige`.
- **Efektler:** özellikler (can, hasar, zırh, hız, erişim, boy...), kalıcı iksirler, XP artışı, çift ganimet (blok listesiyle), kritik vuruş, can çalma, kaçınma, otomatik eritme, damar kazma, ağaç kesme, mob ganimeti, ölünce XP/eşya koruma, açlık tasarrufu, konsol komutu.
- **Aktif yetenekler:** Atılma, Sıçrama, Şifa, Kalkan, Hız Patlaması, Şok Dalgası. Tetikler: Shift + F, çift Shift, Shift + Zıpla.
- **Sıfırlama (respec):** harcananın %75'i geri verilir (ayarlanabilir), bekleme süresi var.
- **Prestij:** ağaç bitince her şey sıfırlanır, kalıcı bonus kazanılır, bedeller artar.
- **Para birimi:** XP seviyesi, Vault parası veya eklentinin kendi puanı. Puan kaynakları: mob/oyuncu öldürme (çiftlik ve alt hesap korumalı), oynama süresi, doğal cevher kazma, başarımlar, günlük ödül.
- **Liderlik tablosu:** `/tigo top` ve oyunda asılı hologram.
- **Bedrock (Geyser/Floodgate):** bu oyuncular kendiliğinden sandık menüsünü kullanır. Herkes `/tigo menu chest|panel` ile seçebilir.
- **Veri:** YAML, SQLite veya MySQL. Değişiklikler toplu kaydedilir. YAML verisi boş veritabanına kendiliğinden aktarılır.
- **Erişim:** komut, isteğe bağlı menü eşyası, etiketli NPC (`/tigo npc`), "yeni yükseltme alınabilir" bildirimi.
- **Kısıtlar:** `restrictions.disabled-worlds`, savaştan sonra menü bekleme süresi.
- **Serbest hareket:** panel açıldığı yerde kalır, oyuncu yürümeye devam eder. Panele bakarken tıklamalar menüye gider, başka yere bakınca oyun normal çalışır. `menu.max-distance` bloktan uzaklaşınca menü kapanır. Eski kilitli davranış için `menu.lock-player: true`.
- **Admin paneli (sadece OP):** `/tigo admin [oyuncu]`. Oyuncu listesi, puan/prestij düzenleme, düğüm seviyeleri (sol +1, sağ −1, Shift+sol maks, Shift+sağ 0), hepsini maks/sıfırla, bonus temizleme, tek tıkla ayarlar (kademeli açılma, tıkla-al, oyuncu kilidi, para birimi, menü türü), hologram, yenileme ve ağaç kontrolü. "3B ağaçta düzenle" ile oyuncunun ağacı altıgen panelde açılır: tüm düğümler görünür, tıklayınca seviye değişir.

## Kullanım
- `/tigo` menüyü açar/kapatır. Bütün komutlar `/tigo <komut>` biçimindedir (eski `/upgrade`, `/yukselt`, `/ut` kaldırıldı). Alt komutların eski Türkçe yazımları (`/tigo bilgi`, `/tigo sifirla onayla`, `/tigo puan ver`...) gizli takma ad olarak çalışmaya devam eder.
- **Tuş ataması:** Seçenekler › Kontroller › **Quick Actions** tuşu küçük bir pencere açar, **Ağacı Aç** butonu ağacı açar. Tuşu her oyuncu kendisi değiştirebilir. Kapatmak için `access.quick-action: false` (yeniden başlatma gerekir).
- Nişangâhla bir altıgene bak, **sol tık** ile satın al. Panel yerinde kalır, etrafında yürüyebilirsin. 10 ve üstü bedellerde ikinci tık onay ister (`menu.confirm-cost`).
- Üstteki sekmelerle ağaç değiştir. **↺ Sıfırla** ve **★ Prestij** butonları iki tıkla çalışır. Panele bakarken **Shift** veya X kapatır.

| Komut | Açıklama |
|---|---|
| `/tigo info [oyuncu]` | Yükseltmeleri ve komut listesini gösterir |
| `/tigo chests` | Chests bölümünü açar (sandık al / aç) |
| `/tigo respec [confirm]` | Kendi ağacını iadeli sıfırlar |
| `/tigo prestige [confirm]` | Prestij yapar |
| `/tigo top [prestige\|levels]` | Liderlik tablosu |
| `/tigo menu <panel\|chest\|auto>` | Menü türü |
| `/tigo daily` | Günlük ödül (POINTS) |
| `/tigo language [dil\|default]` | Kendi mesaj dilini seçer (`default` = sunucu dili) |

OP'ler için `/tigo admin [oyuncu]` paneli. Yönetici komutları (`upgradetree.admin`): `reload`, `check` (ağaç hataları: döngü, ulaşılamayan, üst üste düğüm),
`reset <oyuncu>`, `set <oyuncu> <düğüm> <seviye>`, `points <give|take|set> <oyuncu> <miktar>`,
`prestige set <oyuncu> <seviye>`, `cleanup <oyuncu|all>`, `npc`, `top place|remove`, `open <oyuncu>`,
`chests give <oyuncu> <sandık> [adet]` (açılmamış sandık verir, çevrimdışı oyuncuya da),
`serverlanguage <dil>` (sunucu dilini `config.yml`'e yazar ve yeniden yükler), `debug <preview|look|pos|entities|target> <oyuncu> [düğüm]`.

## Diller
- `lang/*.yml` içindeki her dosya bir dil olur (hazır: `tr`, `en`). Oyuncu kendi dilini `/tigo language <dil>` ile seçer, sunucu dili `/tigo serverlanguage <dil>`. Eski sürümden kalan `lang/tr.yml`/`lang/en.yml` ilk açılışta `*.yml.v<sürüm>.bak` olarak yedeklenip yenisi yazılır.
- **İçerik de dile göre değişir:** ağaç adları, sekmeler, düğüm adı/açıklaması/bonus satırı (`trees/*.yml`), sandık, nadirlik ve ödül adları (`chests.yml`) her dil için yazılabilir: `name: { tr: "Keskin Kılıç", en: "Sharp Sword" }`, `description: { tr: [...], en: [...] }`. Oyuncunun dilinde yoksa sunucu dili, o da yoksa ilk yazılan gösterilir. Tek yazı (`"Titan"`) her dilde aynıdır. Hazır ağaçlar ve sandıklar Türkçe + İngilizce gelir.
- Eski sürümün **hiç değiştirilmemiş** Türkçe ağaç/sandık dosyaları ilk açılışta kendiliğinden iki dilli sürüme yükseltilir (eskisi `*.yml.tr.bak`). Değiştirdiğin dosyalara dokunulmaz; onlara `en:` satırlarını kendin eklersin.
- Para birimi adı (`currency.name`) ve menü eşyasının adı (`access.item.name`) boş bırakılınca oyuncunun dilinden gelir (Seviye / Levels). Liderlik hologramı her oyuncuya kendi dilinde görünür. `/tigo check` hataları ve konsol başlangıç mesajları da dile uyar.
- Quick Actions penceresi herkese aynı gönderildiği için (Minecraft kuralı) tüm dillerin yazısını birlikte gösterir.
- Minecraft'ın kendi eşya adları (Elmas / Diamond) oyuncunun **oyun** dil ayarına göre görünür.

## Chests (sandıklar)
- **Satın alma:** `/tigo chests` (veya menüdeki **Chests** sekmesi). Bir sandık kartına bak, **SATIN AL**. Bedel `config.yml` para biriminden düşer (XP seviyesi, Vault veya puan). Alttaki ızgara sandığın içindekileri nadirlik renkleriyle gösterir; bir ödüle bakınca şansı görünür.
- **Açma:** menü kapanır, sandık önünde belirir (sadece sana görünür). **Sağ tıkla**: sandık sallanır, kapağı açılır, içi parlar, üstünde CS2 gibi bir eşya şeridi döner, altın çizginin altında duran ödül verilir (envanter doluysa yere düşer). Efsanevi ve Mitik ödüller sohbette herkese duyurulur.
- **Açılmayan sandık** bir süre sonra (veya uzaklaşınca) kaybolur ama sandıklarında kalır; Chests bölümünde **AÇ ×N** butonuyla tekrar çağrılır. Sunucu açılış sırasında kapanırsa sandık kaybolmaz.
- **Bedrock:** display entity'ler görünmediği için şerit sandık menüsünde (envanter) döner.
- **Ayar dosyası:** `chests.yml` — nadirlikler (renk, ağırlık, duyuru), sandıklar (ad, simge, bedel, açıklama, görünüm blokları), ödüller (`item` + `amount` + isteğe bağlı `name`, `lore`, `enchantments` ya da `levels`). Şans: önce nadirlik ağırlığı, sonra o nadirlikteki ödüller (varsayılan ağırlıklar CS2 oranları: %79.92 / %15.98 / %3.2 / %0.64 / %0.26).

```yaml
chests:
  elmas:
    name: { tr: "<#67E8F9>Elmas Sandık", en: "<#67E8F9>Diamond Chest" }
    icon: CHEST
    price: 15
    look: { body: DARK_OAK_PLANKS, trim: DIAMOND_BLOCK, latch: GOLD_BLOCK }
    rewards:
      - { item: DIAMOND, amount: 4, rarity: nadir }
      - { levels: 20, rarity: nadir }
      - item: DIAMOND_SWORD
        name: { tr: "<#F472B6>Keskin Elmas Kılıç", en: "<#F472B6>Sharp Diamond Sword" }
        enchantments: { sharpness: 5, unbreaking: 3 }
        rarity: destansi
```

## Ağaç dosyası
Örnek ve tüm ayarların açıklaması `trees/ana.yml` başında. Konumlar altıgen ızgarada:
yan komşu `x±1` (aynı `y`), çapraz komşu `x±0.5` ve `y±1`. Çift satırda `x` tam sayı, tek satırda buçuklu.

```yaml
nodes:
  keskinlik:
    name: { tr: "<#FCA5A5>Keskin Kılıç", en: "<#FCA5A5>Sharp Sword" }
    icon: IRON_SWORD
    x: 1
    y: 0
    max-level: 5
    cost: 2
    cost-increase: 1
    requires: [baslangic]
    color: "#EF4444"
    description:
      tr: ["<gray>Tüm saldırıların daha", "<gray>fazla hasar verir."]
      en: ["<gray>All your attacks", "<gray>deal more damage."]
    bonus: { tr: "+{value} hasar", en: "+{value} damage" }
    effects:
      - type: ATTRIBUTE
        attribute: attack_damage
        amount: 0.5
```

## PlaceholderAPI
`%upgradetree_points%`, `%upgradetree_balance%`, `%upgradetree_prestige%`, `%upgradetree_owned%`, `%upgradetree_total%`,
`%upgradetree_levels%`, `%upgradetree_progress%`, `%upgradetree_affordable%`, `%upgradetree_level_<düğüm>%`,
`%upgradetree_maxlevel_<düğüm>%`, `%upgradetree_owned_<ağaç>%`, `%upgradetree_completed_<ağaç>%`,
`%upgradetree_top_<sıra>_name|prestige|levels%`.

Not: PlaceholderAPI 2.12.2 Paper 26.3'te kendisi açılmıyor (sürüm ayrıştırma hatası). 26.x'i destekleyen yeni bir PlaceholderAPI sürümü gerekir.

## Geliştiriciler için
```java
int lvl = UpgradeTreeAPI.level(player, "keskinlik");
UpgradeTreeAPI.addPoints(player, 5);
```
Eventler (`com.upgradetree.api.event`): `UpgradePurchaseEvent` (iptal edilebilir, bedel değiştirilebilir),
`UpgradeLevelChangeEvent`, `UpgradeResetEvent`, `PrestigeEvent`, `AbilityUseEvent`.

## Derleme ve test
```bash
./gradlew build        # jar: build/libs/UpgradeTree-2.2.0.jar (birim testleri de çalışır)
./gradlew runServer    # Paper 26.3'ü indirip eklentiyle yerel test sunucusu açar
```
GitHub Actions her push'ta derler. `v*` etiketi atılınca jar'ı Releases'a koyar.

## Notlar
- Hayatta kalma modunda oyun sol tıkları saniyede en fazla ~2 kez algılar (Minecraft kuralı).
- 4:3 ekranlarda panel taşarsa `config.yml` → `menu.scale: 0.9`.
- Kalabalık sunucularda `menu.quality: LOW` daha az varlık kullanır.
- Eklentiyi kaldırmadan önce `cleanup-mode: true` yap ya da `/tigo cleanup all` çalıştır. Aksi halde verilen can/hasar bonusları oyuncularda kalır.

---

## English summary
UpgradeTree is a Paper 26.3 plugin (Java 25) that shows a Roblox-style **hexagon upgrade tree** as a floating 3D panel
made of display entities, visible only to its viewer. Nodes reveal progressively as their parents are bought. Features:
multiple trees as tabs, advanced requirements (any-of, level, exclusive, permission, prestige), 20+ effect types including
active abilities (dash, leap, heal, shield, speed, shockwave), respec with refunds, prestige, XP / Vault / points currency
with anti-farm point sources, leaderboard and hologram, a chest menu for Bedrock players, YAML / SQLite / MySQL storage,
PlaceholderAPI placeholders, a developer API with events, and Turkish/English messages (`language: en`).
The panel stays where it was opened while the player walks freely (`menu.lock-player: true` restores the old locked mode).
Operators get an admin panel with `/tigo admin [player]` (chest GUI plus a 3D tree editor).
**Chests:** a CS2-style case section (`/tigo chests`, `chests.yml`). A bought chest appears in front of the player;
right click opens its lid and a roulette strip of items slows down and stops on the prize (Minecraft items or XP levels,
CS2-like rarity colours and odds; rare drops are announced). Bedrock players get the roulette in a chest menu.
Every command is a sub command of `/tigo` (`/tigo help`); the old Turkish sub command spellings still work as hidden aliases.
Content texts (trees, chests) can be written per language (`name: { tr: "...", en: "..." }`); the bundled ones ship in
Turkish and English, and untouched Turkish-only defaults of older versions are upgraded automatically. Players pick their own
message language with `/tigo language <code>`, admins set the server language with `/tigo serverlanguage <code>`.
The client's Options › Controls › Quick Actions key opens a small window whose button opens the tree.
It is a Paper plugin (`paper-plugin.yml` with a bootstrapper, needed for the Quick Actions dialog).
Install: drop `UpgradeTree-2.2.0.jar` into `plugins/`. Open with `/tigo`. Build with `./gradlew build`.
