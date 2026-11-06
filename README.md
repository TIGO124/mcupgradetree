# UpgradeTree 2.1 — Minecraft 26.3 Altıgen Yükseltme Ağacı

Roblox tarzı yükseltme ağacı. Sandık menüsü yerine oyuncunun önünde, sadece ona görünen 3B bir panel açılır:
yıldızlı uzay arka planında petek gibi dizilmiş altıgen düğümler. Bir düğüm alınınca ondan sonraki düğümler
ortaya çıkar (kademeli açılma).

*English summary at the bottom.*

## Kurulum
1. Sunucu: Paper 26.3 (veya Purpur/Leaf gibi bir Paper türevi), Java 25.
2. `UpgradeTree-2.1.0.jar` dosyasını sunucunun `plugins` klasörüne at, sunucuyu yeniden başlat.
3. Ayarlar: `plugins/UpgradeTree/config.yml`, ağaçlar: `trees/*.yml` (her dosya bir sekme), mesajlar: `lang/tr.yml`, `lang/en.yml`.
4. 1.x'ten geçiş: eski `tree.yml` kendiliğinden `trees/ana.yml` olur, oyuncu verileri olduğu gibi kalır.

## Özellikler
- **Altıgen panel:** renkli kenarlı düğümler, alınanların etrafında parlama halkası, alınabilenlerde nabız, ilerleme yazısı ("3/33 Tamamlandı!").
- **Kademeli açılma:** düğüm, gerektirdiği düğümlerden biri alınınca görünür (`menu.progressive-reveal`).
- **Sekmeler:** her `trees/*.yml` dosyası ayrı bir ağaç. Hazır gelenler: Ana Ağaç, Yetenekler, Toplayıcı.
- **Gelişmiş gereksinimler:** `requires`, `requires-any` (biri yeter), `requires-level` (belirli seviye), `exclusive-with` (sınıf seçimi), `permission`, `min-prestige`.
- **Efektler:** özellikler (can, hasar, zırh, hız, erişim, boy...), kalıcı iksirler, XP artışı, çift ganimet (blok listesiyle), kritik vuruş, can çalma, kaçınma, otomatik eritme, damar kazma, ağaç kesme, mob ganimeti, ölünce XP/eşya koruma, açlık tasarrufu, konsol komutu.
- **Aktif yetenekler:** Atılma, Sıçrama, Şifa, Kalkan, Hız Patlaması, Şok Dalgası. Tetikler: Shift + F, çift Shift, Shift + Zıpla.
- **Sıfırlama (respec):** harcananın %80'i geri verilir (ayarlanabilir), bekleme süresi var.
- **Prestij:** ağaç bitince her şey sıfırlanır, kalıcı bonus kazanılır, bedeller artar.
- **Para birimi:** XP seviyesi, Vault parası veya eklentinin kendi puanı. Puan kaynakları: mob/oyuncu öldürme (çiftlik ve alt hesap korumalı), oynama süresi, doğal cevher kazma, başarımlar, günlük ödül.
- **Liderlik tablosu:** `/yukselt top` ve oyunda asılı hologram.
- **Bedrock (Geyser/Floodgate):** bu oyuncular kendiliğinden sandık menüsünü kullanır. Herkes `/yukselt menu sandik|panel` ile seçebilir.
- **Veri:** YAML, SQLite veya MySQL. Değişiklikler toplu kaydedilir. YAML verisi boş veritabanına kendiliğinden aktarılır.
- **Erişim:** komut, isteğe bağlı menü eşyası, etiketli NPC (`/yukselt npc`), "yeni yükseltme alınabilir" bildirimi.
- **Kısıtlar:** `restrictions.disabled-worlds`, savaştan sonra menü bekleme süresi.
- **Serbest hareket:** panel açıldığı yerde kalır, oyuncu yürümeye devam eder. Panele bakarken tıklamalar menüye gider, başka yere bakınca oyun normal çalışır. `menu.max-distance` bloktan uzaklaşınca menü kapanır. Eski kilitli davranış için `menu.lock-player: true`.
- **Admin paneli (sadece OP):** `/yukselt admin [oyuncu]`. Oyuncu listesi, puan/prestij düzenleme, düğüm seviyeleri (sol +1, sağ −1, Shift+sol maks, Shift+sağ 0), hepsini maks/sıfırla, bonus temizleme, tek tıkla ayarlar (kademeli açılma, tıkla-al, oyuncu kilidi, para birimi, menü türü), hologram, yenileme ve ağaç kontrolü. "3B ağaçta düzenle" ile oyuncunun ağacı altıgen panelde açılır: tüm düğümler görünür, tıklayınca seviye değişir.

## Kullanım
- `/yukselt` (veya /upgrade, /agac, /ut) menüyü açar/kapatır.
- Nişangâhla bir altıgene bak, **sol tık** ile satın al. Panel yerinde kalır, etrafında yürüyebilirsin. 10 ve üstü bedellerde ikinci tık onay ister (`menu.confirm-cost`).
- Üstteki sekmelerle ağaç değiştir. **↺ Sıfırla** ve **★ Prestij** butonları iki tıkla çalışır. Panele bakarken **Shift** veya X kapatır.

| Komut | Açıklama |
|---|---|
| `/yukselt bilgi [oyuncu]` | Yükseltmeleri listeler |
| `/yukselt sifirla [onayla]` | Kendi ağacını iadeli sıfırlar |
| `/yukselt prestij [onayla]` | Prestij yapar |
| `/yukselt top [prestij\|seviye]` | Liderlik tablosu |
| `/yukselt menu <panel\|sandik\|otomatik>` | Menü türü |
| `/yukselt gunluk` | Günlük ödül (POINTS) |

OP'ler için `/yukselt admin [oyuncu]` paneli. Yönetici komutları (`upgradetree.admin`): `yenile`, `kontrol` (ağaç hataları: döngü, ulaşılamayan, üst üste düğüm),
`sifirla <oyuncu>`, `ayarla <oyuncu> <düğüm> <seviye>`, `puan <ver|al|ayarla> <oyuncu> <miktar>`,
`prestij ayarla <oyuncu> <seviye>`, `temizle <oyuncu|herkes>`, `npc`, `top koy|kaldir`, `ac <oyuncu>`,
`debug <onizleme|konum|hedef|varlik> <oyuncu> [düğüm]`.

## Ağaç dosyası
Örnek ve tüm ayarların açıklaması `trees/ana.yml` başında. Konumlar altıgen ızgarada:
yan komşu `x±1` (aynı `y`), çapraz komşu `x±0.5` ve `y±1`. Çift satırda `x` tam sayı, tek satırda buçuklu.

```yaml
nodes:
  keskinlik:
    name: "<#FCA5A5>Keskin Kılıç"
    icon: IRON_SWORD
    x: 1
    y: 0
    max-level: 5
    cost: 2
    cost-increase: 1
    requires: [baslangic]
    color: "#EF4444"
    bonus: "+{value} hasar"
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
./gradlew build        # jar: build/libs/UpgradeTree-2.1.0.jar (birim testleri de çalışır)
./gradlew runServer    # Paper 26.3'ü indirip eklentiyle yerel test sunucusu açar
```
GitHub Actions her push'ta derler. `v*` etiketi atılınca jar'ı Releases'a koyar.

## Notlar
- Hayatta kalma modunda oyun sol tıkları saniyede en fazla ~2 kez algılar (Minecraft kuralı).
- 4:3 ekranlarda panel taşarsa `config.yml` → `menu.scale: 0.9`.
- Kalabalık sunucularda `menu.quality: LOW` daha az varlık kullanır.
- Eklentiyi kaldırmadan önce `cleanup-mode: true` yap ya da `/yukselt temizle herkes` çalıştır. Aksi halde verilen can/hasar bonusları oyuncularda kalır.

---

## English summary
UpgradeTree is a Paper 26.3 plugin (Java 25) that shows a Roblox-style **hexagon upgrade tree** as a floating 3D panel
made of display entities, visible only to its viewer. Nodes reveal progressively as their parents are bought. Features:
multiple trees as tabs, advanced requirements (any-of, level, exclusive, permission, prestige), 20+ effect types including
active abilities (dash, leap, heal, shield, speed, shockwave), respec with refunds, prestige, XP / Vault / points currency
with anti-farm point sources, leaderboard and hologram, a chest menu for Bedrock players, YAML / SQLite / MySQL storage,
PlaceholderAPI placeholders, a developer API with events, and Turkish/English messages (`language: en`).
The panel stays where it was opened while the player walks freely (`menu.lock-player: true` restores the old locked mode).
Operators get an admin panel with `/upgrade admin [player]` (chest GUI plus a 3D tree editor).
Install: drop `UpgradeTree-2.1.0.jar` into `plugins/`. Open with `/upgrade`. Build with `./gradlew build`.
