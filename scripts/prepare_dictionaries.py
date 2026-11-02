#!/usr/bin/env python3
"""Bundle openly licensed offline wordlists for iKeyboard.
Sources:
English ENABLE (public domain): https://github.com/dolph/dictionary/blob/master/enable1.txt
Filipino-wordlist (MIT): https://github.com/AustinZuniga/Filipino-wordlist
"""
from pathlib import Path
from urllib.request import urlopen, Request
import re

sources = {
    "english": ("https://raw.githubusercontent.com/dolph/dictionary/master/enable1.txt", 45000),
    "filipino": ("https://raw.githubusercontent.com/AustinZuniga/Filipino-wordlist/master/Filipino-wordlist.txt", 30000),
}
assets = Path("app/src/main/assets")
assets.mkdir(parents=True, exist_ok=True)
for language, (url, limit) in sources.items():
    print("Downloading", language, url, flush=True)
    request = Request(url, headers={"User-Agent": "iKeyboard-build/1.0"})
    with urlopen(request, timeout=90) as response:
        content = response.read(12_000_000).decode("utf-8-sig", errors="replace")
    words = []
    seen = set()
    for line in content.splitlines():
        word = line.strip().lower()
        if not re.fullmatch(r"[a-z]{2,24}", word) or word in seen:
            continue
        seen.add(word)
        words.append(word)
        if len(words) >= limit:
            break
    if len(words) < 1000:
        raise RuntimeError(f"{language} wordlist unexpectedly small: {len(words)}")
    (assets / f"{language}_words.txt").write_text("\n".join(words)+"\n", encoding="utf-8")
    print(f"Bundled {len(words)} {language} words", flush=True)
