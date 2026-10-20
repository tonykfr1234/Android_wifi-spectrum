<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">简体中文</a> · <a href="README.zh-TW.md">繁體中文</a> · <a href="README.ja.md">日本語</a> · <a href="README.ko.md">한국어</a><br>
  <a href="README.de.md">Deutsch</a> · <a href="README.es.md">Español</a> · <a href="README.fr.md">Français</a> · <strong>Português (Brasil)</strong> · <a href="README.vi.md">Tiếng Việt</a> · <a href="README.id.md">Bahasa Indonesia</a> · <a href="README.it.md">Italiano</a>
</p>

<p align="center"><img src="assets/wifi-spectrum.png" width="112" alt="Ícone do Wi-Fi Spectrum"></p>
<h1 align="center">Analisador de Wi-Fi profissional para Android</h1>
<p align="center">Wi-Fi Spectrum (Wi-Fi 頻譜分析儀) · 2,4 / 5 / 6 GHz · 20–320 MHz · Wi-Fi 7 MLO</p>
<p align="center">
  <a href="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml"><img src="https://github.com/tonykfr1234/android_wifi-spectrum/actions/workflows/security.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF" alt="Kotlin 2.2">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4" alt="Jetpack Compose">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="Licença MIT"></a>
</p>
<p align="center">
  <a href="#quick-start">Início rápido</a> · <a href="#workflow">Fluxo</a> · <a href="#features">Recursos</a> · <a href="#demo">Demonstração</a> · <a href="#faq">Perguntas frequentes</a> · <a href="#documentation">Documentação</a>
</p>

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

Versão 1.0: as três bandas, todas as larguras de canal até 320 MHz e multi-link do Wi-Fi 7 (MLO) agrupado por endereço MLD.

O Wi-Fi Spectrum escaneia os pontos de acesso próximos em 2,4, 5 e 6 GHz e mostra onde cada um realmente transmite: o canal primário, o bloco completo de 20/40/80/160/80+80/320 MHz e todos os canais cobertos. A visão de espectro, a tendência de sinal e a avaliação de canais ajudam a escolher um canal tranquilo; a visão MLO agrupa os links de cada roteador Wi-Fi 7.

A Apple não permite que apps de iOS escaneiem redes Wi-Fi próximas, então esses recursos são exclusivos do Android. A versão para iOS, [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum), cobre o que o iOS permite.

Os 12 idiomas do README traduzem a documentação. A interface do app está atualmente em chinês tradicional.

<a id="quick-start"></a>
## Início rápido

1. Baixe `wifi-spectrum-debug-apk` da última [execução bem-sucedida do GitHub Actions](https://github.com/tonykfr1234/android_wifi-spectrum/actions) ou compile com `./gradlew assembleDebug` (JDK 17+ e Android SDK).
2. Instale `app-debug.apk` em um celular com Android 8+. 6 GHz exige Wi-Fi 6E; 320 MHz e MLO exigem Android 13+ com Wi-Fi 7.
3. Abra o app e permita a localização **precisa** e, no Android 13+, “Dispositivos Wi-Fi por perto”. Sem isso, o Android não entrega resultados.
4. Mantenha a localização ativada. O app escaneia a cada 30 segundos enquanto está visível; toque em atualizar para escanear agora.
5. Sem um celular adequado? Use o modo demonstração para explorar um ambiente Wi-Fi 7 sintético.

**Publicar no Google Play?** Siga [docs/PUBLISHING.zh-TW.md](docs/PUBLISHING.zh-TW.md) ([resumo em inglês](docs/PUBLISHING.md)): altere o ID do aplicativo, crie uma chave de upload fora do projeto, gere o App Bundle assinado e conclua o teste fechado do Play Console.

<a id="workflow"></a>
## Fluxo

```text
App visível, a cada 30 segundos (o Android permite 4 varreduras em primeiro plano a cada 2 minutos)
  → WifiManager.startScan(); pedidos limitados reutilizam os resultados em cache
  → limpar cada ScanResult: SSID, BSSID, banda, canal primário
  → bloco ocupado pela largura e frequência central (20–320 MHz, 80+80)
  → agrupar links Wi-Fi 7 por endereço MLD; consultar o fabricante offline
  → atualizar lista, espectro, tendência de 60 varreduras, avaliação e MLO
```

A varredura para quando o app sai da tela; nada roda em segundo plano. Quando o driver omite a frequência central ou informa o centro de 160 MHz em `centerFreq1`, o bloco é derivado do plano de canais padrão. Os campos MLO exigem Android 13; o sinal por link do roteador conectado, Android 14.

<a id="features"></a>
## Recursos

| Recurso | Comportamento |
| --- | --- |
| Bandas | 2,4, 5 (incl. UNII-4) e 6 GHz; avisos de limitação, Wi-Fi desligado e localização desligada |
| Largura de canal | 20, 40, 80, 160, 80+80 e 320 MHz; faixa ocupada, canal central e canais cobertos |
| Wi-Fi 7 MLO | Endereço MLD, IDs de link e links afiliados; links ativos e RSSI por link da conexão |
| Espectro | Uma parábola por AP; visão geral da banda com zoom; links do roteador conectado em negrito |
| Tendência | RSSI dos 10 APs mais fortes em 60 varreduras; falhas quando um AP não apareceu |
| Avaliação de canais | Ponderada por sinal e sobreposição; só 1/6/11 em 2,4 GHz; preferência por não DFS; PSC em 6 GHz |
| Detalhes | Fabricante do registro IEEE, detecção de MAC aleatório, segurança incl. WPA3 SAE-EXT e OWE, estimativa de distância |
| Privacidade | Sem acesso à rede, anúncios ou análises; os resultados ficam na memória |

A distância é uma estimativa em espaço livre; paredes a encurtam. O conteúdo da varredura depende do chip Wi-Fi e do driver; confira os valores de 6 GHz, 320 MHz e MLO no seu próprio equipamento antes de confiar neles.

<a id="demo"></a>
## Demonstração

Esta demonstração silenciosa de 40 segundos usa a **interface real do app com o ambiente sintético embutido**: um apartamento em Taipé com roteador Wi-Fi 7 (três links MLO) e vizinhos congestionados, com legendas bilíngues em cada tela. Não é uma varredura real e não diz nada sobre o rádio de nenhum celular.

![Demonstração do app com dados sintéticos](docs/media/wifi-spectrum-demo.gif)

[Abrir ou baixar a demonstração em MP4](docs/media/wifi-spectrum-demo.mp4)

| Espectro de 2,4 GHz | 6 GHz com 320 MHz | Links MLO do Wi-Fi 7 |
| --- | --- | --- |
| ![Espectro de 2,4 GHz](docs/media/spectrum-24ghz.png) | ![6 GHz com 320 MHz](docs/media/spectrum-6ghz.png) | ![Links MLO do Wi-Fi 7](docs/media/mlo-links.png) |

## Mudanças recentes

Primeira versão: varredura em três bandas, larguras de 20–320 MHz, Wi-Fi 7 MLO, espectro, tendência, avaliação de canais, recursos para o Google Play e guia de publicação, verificada por 214 testes offline no Android 8–16. Veja o [registro de mudanças](CHANGELOG.md).

<a id="controls"></a>
## Controles

| Controle | Ação |
| --- | --- |
| Atualizar | Escanear agora |
| Pausar / Reproduzir | Parar ou retomar a varredura automática |
| ⋮ → intervalo | 3, 5, 10, 30 ou 60 segundos; abaixo de 30 s o Android pode limitar |
| ⋮ → modo demonstração | Alternar entre varredura real e dados sintéticos |
| Chips de banda / largura / Wi-Fi 7 | Filtrar a lista; o chip de largura alterna ≥40/80/160/320 MHz |
| Zoom (5/6 GHz) | Alternar visão geral da banda e um close rolável |
| Tocar em um AP | Ver faixa ocupada, canais cobertos, segurança e links MLO |

<a id="faq"></a>
## Perguntas frequentes

**Por que precisa da localização?** O Android trata os resultados de varredura Wi-Fi como dados de localização. Sem localização precisa (e “Dispositivos Wi-Fi por perto” no Android 13+) o sistema não retorna nada. O app não lê coordenadas GPS.

**Por que 6 GHz está vazio?** O celular precisa suportar Wi-Fi 6E e um roteador de 6 GHz deve estar ao alcance.

**Por que não aparece MLO nem 320 MHz?** É preciso um celular Wi-Fi 7 com Android 13 ou posterior e um roteador Wi-Fi 7 com MLO ativado.

**Por que diz que a varredura está limitada?** O Android permite quatro varreduras em primeiro plano a cada dois minutos. Mantenha o intervalo de 30 segundos ou desative a limitação de busca de Wi-Fi nas Opções do desenvolvedor durante os testes.

**Existe versão para iOS?** As APIs da Apple não expõem redes próximas, então o iOS não faz essas varreduras. Veja o que o iOS suporta em [IOS_wifi-spectrum](https://github.com/tonykfr1234/ios_wifi-spectrum).

<a id="docs"></a>
<a id="documentation"></a>
## Documentação

| Tópico | Documento |
| --- | --- |
| Uso diário | [繁中使用指南](docs/USER_GUIDE.zh-TW.md) · [English User Guide](docs/USER_GUIDE.md) |
| Funcionamento interno | [Arquitetura](docs/ARCHITECTURE.md) |
| Publicação | [Guia do Google Play (繁中)](docs/PUBLISHING.zh-TW.md) · [Resumo em inglês](docs/PUBLISHING.md) · [Política de privacidade](docs/PRIVACY.md) |
| Versões e segurança | [Registro de mudanças](CHANGELOG.md) · [Segurança](SECURITY.md) · [Como contribuir](CONTRIBUTING.md) |

O código fica em `app/src/main/java`, os testes em `app/src/test`, as ferramentas em `scripts/` e as imagens do Google Play em `store/`. Os 214 testes rodam na JVM sem celular; confira os valores reais de 6 GHz, 320 MHz e MLO no seu equipamento.

## Segurança

Nunca faça commit de keystores, `keystore.properties`, `local.properties`, chaves de API ou varreduras exportadas de redes que não são suas. Antes de fazer push, rode o mesmo scanner do GitHub Actions:

```bash
python scripts/security_scan.py .
```

## Licença

[MIT](LICENSE).

### 2026-10-20 · 1.0.0

首次發布：2.4／5／6 GHz 掃描、20–320 MHz 頻寬、Wi-Fi 7 MLO、頻譜圖、趨勢與頻道評分；214 個離線測試涵蓋 Android 8–16。

First release: 2.4/5/6 GHz scanning, 20–320 MHz widths, Wi-Fi 7 MLO, spectrum, trend and channel rating; 214 offline tests cover Android 8–16.

[Guia de uso público (chinês tradicional, texto simples)](docs/使用說明.txt)
