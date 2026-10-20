#!/usr/bin/env python3
"""Regenerate docs/media from the real app UI.

Runs the Robolectric renderers (no device or Wi-Fi hardware), then assembles:
  docs/media/wifi-spectrum-demo.gif  silent preview, 1 frame per second
  docs/media/wifi-spectrum-demo.mp4  silent H.264 video
  docs/media/spectrum-24ghz.png, spectrum-6ghz.png, mlo-links.png
and refreshes assets/wifi-spectrum.png and the Google Play images in store/.

Needs JDK 17+, the Android SDK and ffmpeg on PATH.
"""
from __future__ import annotations

import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MEDIA = ROOT / 'docs' / 'media'
FRAMES = ROOT / 'app' / 'build' / 'demo'
STORE = ROOT / 'app' / 'build' / 'store'


def run(*cmd: str) -> None:
    print('+', ' '.join(cmd))
    subprocess.run(cmd, cwd=ROOT, check=True)


def main() -> int:
    if shutil.which('ffmpeg') is None:
        print('ffmpeg is required', file=sys.stderr)
        return 1
    gradlew = str(ROOT / ('gradlew.bat' if sys.platform == 'win32' else 'gradlew'))
    run(gradlew, 'testDebugUnitTest', '--tests', '*DemoMediaTest*', '--tests', '*StoreScreenshotTest*',
        '--tests', '*StoreAssetsTest*')
    MEDIA.mkdir(parents=True, exist_ok=True)
    pattern = str(FRAMES / 'frame_%03d.png')
    palette = str(FRAMES / 'palette.png')
    run('ffmpeg', '-y', '-loglevel', 'error', '-framerate', '1', '-i', pattern,
        '-vf', 'scale=360:-1:flags=lanczos,palettegen=max_colors=128', palette)
    run('ffmpeg', '-y', '-loglevel', 'error', '-framerate', '1', '-i', pattern, '-i', palette,
        '-lavfi', 'scale=360:-1:flags=lanczos[x];[x][1:v]paletteuse=dither=bayer:bayer_scale=4',
        '-loop', '0', str(MEDIA / 'wifi-spectrum-demo.gif'))
    run('ffmpeg', '-y', '-loglevel', 'error', '-framerate', '1', '-i', pattern,
        '-vf', 'scale=540:-2,format=yuv420p', '-r', '30', '-c:v', 'libx264', '-crf', '20',
        '-movflags', '+faststart', '-an', str(MEDIA / 'wifi-spectrum-demo.mp4'))
    for src, dst in [('phone_2_spectrum_24.png', 'spectrum-24ghz.png'),
                     ('phone_3_spectrum_6.png', 'spectrum-6ghz.png'),
                     ('phone_4_mlo.png', 'mlo-links.png')]:
        run('ffmpeg', '-y', '-loglevel', 'error', '-i', str(STORE / src), '-vf', 'scale=540:-1', str(MEDIA / dst))
    store = ROOT / 'store'
    store.mkdir(exist_ok=True)
    for png in STORE.glob('*.png'):
        shutil.copy2(png, store / png.name)
    shutil.copy2(STORE / 'icon_512.png', ROOT / 'assets' / 'wifi-spectrum.png')
    print('demo media updated in', MEDIA)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
