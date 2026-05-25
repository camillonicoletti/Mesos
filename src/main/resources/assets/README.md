Asset GUI
=========

Questa cartella contiene le immagini usate dalla GUI Java.

La GUI non legge direttamente i PDF originali. Le immagini vanno tenute in PNG
o JPEG con nomi stabili, in modo che il codice possa trovarle tramite i file
manifest.

Struttura attesa:

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

Le immagini sono risorse fornite dai docenti e non vanno caricate su Git.
I manifest rimangono tracciati per documentare i nomi attesi a runtime.

I PDF originali sono conservati fuori dal progetto, ad esempio in:

/Users/giorgio/Desktop/Risorse Grafiche Mesos

Quei file vanno usati solo come materiale locale di partenza.
