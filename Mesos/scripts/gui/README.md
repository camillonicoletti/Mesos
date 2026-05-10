GUI asset preparation
=====================

1. Keep original professor PDFs outside runtime resources.
2. Extract card fronts as PNG files named by game id:
   - characters: card_001.png ... card_084.png
   - events: card_085.png ... card_096.png
   - buildings: card_097.png ... card_117.png
   - summary cards: card_118.png ... card_122.png
3. Extract only the unique card backs:
   - src/main/resources/gui/assets/cards/backs/back_character_era_i.png
   - src/main/resources/gui/assets/cards/backs/back_character_era_ii.png
   - src/main/resources/gui/assets/cards/backs/back_character_era_iii.png
   - src/main/resources/gui/assets/cards/backs/back_building_era_i.png
   - src/main/resources/gui/assets/cards/backs/back_building_era_ii.png
   - src/main/resources/gui/assets/cards/backs/back_building_era_iii.png
   - src/main/resources/gui/assets/cards/backs/back_event_final.png
4. Extract board tiles, totems, food, and prestige tokens into the folders
   declared by src/main/resources/gui/assets/manifest/components.json.
5. Run:
   python3 scripts/gui/generate_asset_manifest.py

The GUI should load assets through the manifest files rather than hardcoding
paths in views.
