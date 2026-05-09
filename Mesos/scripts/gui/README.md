GUI asset preparation
=====================

1. Keep original professor PDFs outside runtime resources.
2. Extract card fronts as PNG files named by game id:
   - characters: card_001.png ... card_084.png
   - events: card_085.png ... card_096.png
   - buildings: card_097.png ... card_117.png
3. Put one card back image at:
   - src/main/resources/gui/assets/cards/backs/card_back.png
4. Extract board tiles, totems, food, and prestige tokens into the folders
   declared by src/main/resources/gui/assets/manifest/components.json.
5. Run:
   python3 scripts/gui/generate_asset_manifest.py

The GUI should load assets through the manifest files rather than hardcoding
paths in views.
