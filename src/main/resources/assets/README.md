GUI assets
==========

This folder contains the images used by the Java GUI.

The GUI does not read the original PDFs directly. Images must be kept as PNG
or JPEG with stable names, so that the code can find them through the manifest
files.

Expected structure:

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

The images are resources provided by the course instructors and must not be
committed to Git. The manifest files remain tracked to document the names
expected at runtime.

The original PDFs are kept outside the project, for example in:

/Users/giorgio/Desktop/Risorse Grafiche Mesos

Those files should be used only as local source material.
