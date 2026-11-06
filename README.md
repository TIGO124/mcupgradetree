# UpgradeTree — Minecraft 26.3 Yükseltme Ağacı

Roblox tarzı yükseltme ağacı. Sandık menüsü yerine oyuncunun önünde, sadece ona görünen 3B bir panel açılır.

## Kurulum
1. Sunucu: Paper 26.3 (veya Purpur/Leaf gibi bir Paper türevi), Java 25.
2. `UpgradeTree-1.0.0.jar` dosyasını sunucunun `plugins` klasörüne at.
3. Sunucuyu yeniden başlat. Ayarlar `plugins/UpgradeTree/config.yml` ve `tree.yml` içinde.

## Kullanım
- `/yukselt` (veya /upgrade, /agac, /ut) menüyü açar/kapatır.
- Nişangâhla bir yeteneğe bak, **sol tık** ile satın al. **Shift** veya X düğmesi kapatır.
- Yönetici: `/yukselt yenile`, `sifirla <oyuncu>`, `ayarla <oyuncu> <düğüm> <seviye>`, `puan ver|al|ayarla <oyuncu> <miktar>`, `bilgi`.
- Para birimi (`currency.type`): XP_LEVELS (varsayılan), POINTS veya VAULT.

## Notlar
- Hayatta kalma modunda oyun sol tıkları saniyede en fazla ~2 kez algılar (Minecraft kuralı).
- 4:3 ekranlarda panel taşarsa `config.yml` → `menu.scale: 0.9`.

## Klasörler
- `UpgradeTree-1.0.0.jar` — sunucuya atılacak hazır eklenti.
- `src/` — kaynak kod (Java) ve varsayılan ayar dosyaları.
- `build.gradle.kts` — kaynak koddan kendin derlemek istersen (`gradle build`).
