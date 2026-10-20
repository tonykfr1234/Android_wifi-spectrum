<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.zh-TW.md">繁體中文</a> · <a href="README.ja.md">日本語</a> · <a href="README.ko.md">한국어</a><br>
  <a href="README.de.md">Deutsch</a> · <a href="README.es.md">Español</a> · <strong>Français</strong> · <a href="README.pt-BR.md">Português (Brasil)</a> · <a href="README.vi.md">Tiếng Việt</a> · <a href="README.id.md">Bahasa Indonesia</a> · <a href="README.it.md">Italiano</a>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Icône de Wi-Fi Spectrum"></p>
<h1 align="center">Analyseur Wi-Fi professionnel pour Android</h1>
<p align="center">Wi-Fi Spectrum (Wi-Fi 頻譜分析儀) · 2,4 / 5 / 6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="Licence MIT"></a>
</p>
<p align="center">
  <a href="#quick-start">Démarrage rapide</a> · <a href="#workflow">Fonctionnement</a> · <a href="#features">Fonctionnalités</a> · <a href="#demo">Démonstration</a> · <a href="#faq">FAQ</a> · <a href="#documentation">Documentation</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

Version 1.0 : les trois bandes, toutes les largeurs de canal jusqu'à 320 MHz et le multilien Wi-Fi 7 (MLO) regroupé par adresse MLD.

Wi-Fi Spectrum analyse les points d'accès proches en 2,4, 5 et 6 GHz et montre où chacun émet réellement : son canal primaire, le bloc complet de 20/40/80/160/80+80/320 MHz et tous les canaux couverts. La vue spectre, l'évolution du signal et la notation des canaux aident à choisir un canal calme ; la vue MLO regroupe les liens de chaque routeur Wi-Fi 7.

Apple n'autorise pas les apps iOS à analyser les réseaux Wi-Fi proches ; ces fonctions sont donc réservées à Android. La version iOS, [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum), couvre ce qu'iOS permet.

Les 12 langues du README traduisent la documentation. L'interface de l'app est actuellement en chinois traditionnel.

<a id="quick-start"></a>
## Démarrage rapide

1. Téléchargez `wifi-spectrum-debug-apk` depuis la dernière [exécution GitHub Actions réussie](https://github.com/tonykfr1234/android_wifi-spectrum/actions) ou compilez avec `./gradlew assembleDebug` (JDK 17+ et SDK Android).
2. Installez `app-debug.apk` sur un téléphone Android 8+. Le 6 GHz exige le Wi-Fi 6E ; 320 MHz et MLO exigent Android 13+ avec Wi-Fi 7.
3. Ouvrez l'app et autorisez la position **précise** et, sous Android 13+, « Appareils Wi-Fi à proximité ». Sans cela, Android ne fournit aucun résultat.
4. Laissez la localisation activée. L'app analyse toutes les 30 secondes tant qu'elle est visible ; touchez Actualiser pour analyser tout de suite.
5. Pas de téléphone adapté ? Le mode démonstration présente un environnement Wi-Fi 7 synthétique.

**Publier sur Google Play ?** Suivez [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md) ([résumé en anglais](docs/PUBLISHING.md)) : changez l'ID d'application, créez une clé d'importation hors du projet, générez l'App Bundle signé et terminez le test fermé de la Play Console.

<a id="workflow"></a>
## Fonctionnement

```text
App visible, toutes les 30 secondes (Android autorise 4 analyses au premier plan par 2 minutes)
  → WifiManager.startScan() ; les demandes limitées réutilisent les résultats en cache
  → nettoyer chaque ScanResult : SSID, BSSID, bande, canal primaire
  → bloc occupé selon la largeur et la fréquence centrale (20–320 MHz, 80+80)
  → regrouper les liens Wi-Fi 7 par adresse MLD ; fabricant consulté hors ligne
  → mettre à jour liste, spectre, évolution sur 60 analyses, notation et MLO
```

L'analyse s'arrête dès que l'app quitte l'écran ; rien ne tourne en arrière-plan. Si le pilote omet la fréquence centrale ou place le centre 160 MHz dans `centerFreq1`, le bloc est déduit du plan de canaux standard. Les champs MLO exigent Android 13 ; le signal par lien du routeur connecté, Android 14.

<a id="features"></a>
## Fonctionnalités

| Fonction | Comportement |
| --- | --- |
| Bandes | 2,4, 5 (UNII-4 inclus) et 6 GHz ; alertes de limitation, Wi-Fi coupé et localisation coupée |
| Largeur de canal | 20, 40, 80, 160, 80+80 et 320 MHz ; plage occupée, canal central et canaux couverts |
| Wi-Fi 7 MLO | Adresse MLD, ID de lien et liens affiliés ; liens actifs et RSSI par lien de la connexion |
| Spectre | Une parabole par AP ; vue d'ensemble de la bande avec zoom ; liens du routeur connecté en gras |
| Évolution | RSSI des 10 AP les plus forts sur 60 analyses ; trous quand un AP manquait |
| Notation des canaux | Pondérée par signal et chevauchement ; seulement 1/6/11 en 2,4 GHz ; non-DFS privilégiés ; PSC en 6 GHz |
| Détails | Fabricant du registre IEEE, détection des MAC aléatoires, sécurité dont WPA3 SAE-EXT et OWE, estimation de distance |
| Confidentialité | Aucun accès réseau, publicité ni statistiques ; les résultats restent en mémoire |

La distance est une estimation en espace libre ; les murs la réduisent. Le contenu des analyses dépend de la puce Wi-Fi et du pilote ; vérifiez les valeurs 6 GHz, 320 MHz et MLO avec votre propre matériel avant de vous y fier.

<a id="demo"></a>
## Démonstration

Cette démonstration muette de 40 secondes utilise **la véritable interface de l'app pilotée par l'environnement synthétique intégré** : un appartement à Taipei avec un routeur Wi-Fi 7 (trois liens MLO) et des voisins encombrés, avec des sous-titres bilingues. Ce n'est pas une vraie analyse et cela ne dit rien de la radio d'un téléphone précis.

![Présentation de l'app avec des données synthétiques](docs/media/wifi-spectrum-demo.gif)

[Ouvrir ou télécharger la démonstration MP4](docs/media/wifi-spectrum-demo.mp4)

| Spectre 2,4 GHz | 6 GHz avec 320 MHz | Liens MLO Wi-Fi 7 |
| --- | --- | --- |
| ![Spectre 2,4 GHz](docs/media/spectrum-24ghz.png) | ![6 GHz avec 320 MHz](docs/media/spectrum-6ghz.png) | ![Liens MLO Wi-Fi 7](docs/media/mlo-links.png) |

## Dernières modifications

Première version : analyse sur trois bandes, largeurs de 20 à 320 MHz, Wi-Fi 7 MLO, spectre, évolution, notation des canaux, ressources Google Play et guide de publication, vérifiée par 214 tests hors ligne sur Android 8–16. Voir le [journal des modifications](CHANGELOG.md).

<a id="controls"></a>
## Commandes

| Commande | Action |
| --- | --- |
| Actualiser | Analyser maintenant |
| Pause / Lecture | Arrêter ou reprendre l'analyse automatique |
| ⋮ → intervalle | 3, 5, 10, 30 ou 60 secondes ; sous 30 s Android peut limiter |
| ⋮ → mode démonstration | Basculer entre analyse réelle et données synthétiques |
| Puces bande / largeur / Wi-Fi 7 | Filtrer la liste ; la puce de largeur alterne ≥40/80/160/320 MHz |
| Zoom (5/6 GHz) | Basculer entre la vue d'ensemble et un gros plan défilant |
| Toucher un AP | Afficher plage occupée, canaux couverts, sécurité et liens MLO |

<a id="faq"></a>
## FAQ

**Pourquoi la localisation est-elle nécessaire ?** Android considère les résultats d'analyse Wi-Fi comme des données de localisation. Sans position précise (et « Appareils Wi-Fi à proximité » sous Android 13+), le système ne renvoie rien. L'app ne lit pas les coordonnées GPS.

**Pourquoi le 6 GHz est-il vide ?** Le téléphone doit prendre en charge le Wi-Fi 6E et un routeur 6 GHz doit être à portée.

**Pourquoi pas de MLO ni de 320 MHz ?** Il faut un téléphone Wi-Fi 7 sous Android 13 ou plus et un routeur Wi-Fi 7 avec MLO activé.

**Pourquoi l'analyse est-elle signalée comme limitée ?** Android autorise quatre analyses au premier plan toutes les deux minutes. Gardez l'intervalle de 30 secondes, ou désactivez la limitation de recherche Wi-Fi dans les Options pour les développeurs pendant les tests.

**Existe-t-il une version iOS ?** Les API d'Apple n'exposent pas les réseaux proches ; iOS ne peut donc pas faire ces analyses. Voir [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum) pour ce qu'iOS permet.

<a id="docs"></a>
<a id="documentation"></a>
## Documentation

| Sujet | Document |
| --- | --- |
| Utilisation quotidienne | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| Fonctionnement interne | [Architecture](docs/ARCHITECTURE.md) |
| Publication | [Guide Google Play (繁中)](docs/PUBLISHING.zh-TW.md) · [Résumé en anglais](docs/PUBLISHING.md) · [Politique de confidentialité](docs/PRIVACY.md) |
| Versions et sécurité | [Journal des modifications](CHANGELOG.md) · [Sécurité](SECURITY.md) · [Contribuer](CONTRIBUTING.md) |

Le code se trouve dans `app/src/main/java`, les tests dans `app/src/test`, les outils dans `scripts/` et les images Google Play dans `store/`. Les 214 tests s'exécutent sur la JVM sans téléphone ; vérifiez les vraies valeurs 6 GHz, 320 MHz et MLO avec votre matériel.

## Sécurité

Ne commitez jamais de keystores, `keystore.properties`, `local.properties`, clés d'API ou analyses exportées de réseaux qui ne vous appartiennent pas. Avant de pousser, lancez le même scanner que GitHub Actions :

```bash
python scripts/security_scan.py .
```

## Licence

[MIT](LICENSE).

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[Guide d'utilisation public (chinois traditionnel, texte brut)](docs/使用說明.txt)
