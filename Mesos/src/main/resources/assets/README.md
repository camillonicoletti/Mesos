GUI asset layout
================

This folder contains runtime assets for the Java GUI.

Do not load the original PDF files directly from the GUI. Convert them to
optimized PNG files and keep the file names stable, so the view can resolve
assets through the generated manifests.

Expected layout:

- cards/characters/card_001.png ... card_084.png
- cards/events/card_085.png ... card_096.png
- cards/buildings/card_097.png ... card_117.png
- cards/summary/card_118.png ... card_122.png
- cards/backs/back_character_era_i.png
- cards/backs/back_character_era_ii.png
- cards/backs/back_character_era_iii.png
- cards/backs/back_building_era_i.png
- cards/backs/back_building_era_ii.png
- cards/backs/back_building_era_iii.png
- cards/backs/back_event_final.png
- board/
- tokens/
- ui/
- manifest/cards.json
- manifest/components.json

Image files in this folder are professor-provided local assets and are ignored
by git. The tracked manifest files document the expected runtime names; keep
the local PNG/JPEG files next to them when running the GUI.

Original source PDFs currently live outside the project at:

/Users/giorgio/Desktop/Risorse Grafiche Mesos

Keep those as source material. Do not commit the extracted PNG/JPEG assets.
