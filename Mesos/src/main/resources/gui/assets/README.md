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
- cards/backs/card_back.png
- board/
- tokens/
- ui/
- manifest/cards.json
- manifest/components.json

Original source PDFs currently live outside the project at:

/Users/giorgio/Desktop/Risorse Grafiche Mesos

Keep those as source material, but commit only the extracted assets needed by
the GUI.
