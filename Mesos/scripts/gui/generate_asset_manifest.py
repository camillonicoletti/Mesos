#!/usr/bin/env python3
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RESOURCES = ROOT / "src" / "main" / "resources"
OUT_DIR = RESOURCES / "gui" / "assets" / "manifest"


def load_json(name):
    with (RESOURCES / name).open(encoding="utf-8") as handle:
        return json.load(handle)


def card_path(kind, card_id):
    folders = {
        "character": "characters",
        "event": "events",
        "building": "buildings",
    }
    return f"gui/assets/cards/{folders[kind]}/card_{card_id:03d}.png"


def era_key(era):
    return {
        "I": "era_i",
        "II": "era_ii",
        "III": "era_iii",
    }[era]


def character_back_path(era):
    return f"gui/assets/cards/backs/back_character_{era_key(era)}.png"


def building_back_path(era):
    return f"gui/assets/cards/backs/back_building_{era_key(era)}.png"


def event_back_path(card):
    if card.get("closeEvent"):
        return "gui/assets/cards/backs/back_event_final.png"
    return character_back_path(card.get("era"))


def build_cards_manifest():
    cards = {}

    for card in load_json("characters.json"):
        card_id = int(card["id"])
        cards[str(card_id)] = {
            "id": card_id,
            "kind": "character",
            "type": card.get("type"),
            "era": card.get("era"),
            "asset": card_path("character", card_id),
            "backAsset": character_back_path(card.get("era")),
        }

    for card in load_json("events.json"):
        card_id = int(card["id"])
        cards[str(card_id)] = {
            "id": card_id,
            "kind": "event",
            "type": card.get("type"),
            "era": card.get("era"),
            "asset": card_path("event", card_id),
            "backAsset": event_back_path(card),
        }

    for card in load_json("buildings.json"):
        card_id = int(card["id"])
        cards[str(card_id)] = {
            "id": card_id,
            "kind": "building",
            "era": card.get("era"),
            "asset": card_path("building", card_id),
            "backAsset": building_back_path(card.get("era")),
        }

    return {
        "backs": {
            "characterEraI": character_back_path("I"),
            "characterEraII": character_back_path("II"),
            "characterEraIII": character_back_path("III"),
            "buildingEraI": building_back_path("I"),
            "buildingEraII": building_back_path("II"),
            "buildingEraIII": building_back_path("III"),
            "eventFinal": "gui/assets/cards/backs/back_event_final.png",
        },
        "defaultCardBack": character_back_path("I"),
        "cards": dict(sorted(cards.items(), key=lambda item: int(item[0]))),
    }


def build_components_manifest():
    offer_tiles = load_json("offer_tiles.json")
    tiles = {}
    for tile in offer_tiles:
        letter = tile.get("letter")
        if letter:
            tiles[letter] = {
                "letter": letter,
                "asset": None,
            }

    return {
        "totems": {
            "RED": {"asset": None, "fallbackColor": "#c8342f"},
            "BLUE": {"asset": None, "fallbackColor": "#2f67b1"},
            "PURPLE": {"asset": None, "fallbackColor": "#7b3f98"},
            "YELLOW": {"asset": None, "fallbackColor": "#e5b72f"},
            "WHITE": {"asset": None, "fallbackColor": "#f4efe6"},
        },
        "offerTiles": tiles,
        "food": {"asset": None, "fallbackText": "food"},
        "prestige": {"asset": None, "fallbackText": "prestige"},
        "referenceSheets": {
            "fustelleFront": [
                "gui/assets/board/fustelle_front_1.png",
                "gui/assets/board/fustelle_front_2.png",
                "gui/assets/board/fustelle_front_3.png"
            ],
            "fustelleBack": [
                "gui/assets/board/fustelle_back_1.png",
                "gui/assets/board/fustelle_back_2.png",
                "gui/assets/board/fustelle_back_3.png"
            ],
            "diecut": [
                "gui/assets/board/fustelle_diecut_1.png",
                "gui/assets/board/fustelle_diecut_2.png",
                "gui/assets/board/fustelle_diecut_3.png"
            ],
            "pedineSpecs": ["gui/assets/tokens/pedine_specs_1.png"]
        },
    }


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def main():
    write_json(OUT_DIR / "cards.json", build_cards_manifest())
    write_json(OUT_DIR / "components.json", build_components_manifest())


if __name__ == "__main__":
    main()
