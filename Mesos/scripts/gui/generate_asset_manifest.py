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
        }

    for card in load_json("events.json"):
        card_id = int(card["id"])
        cards[str(card_id)] = {
            "id": card_id,
            "kind": "event",
            "type": card.get("type"),
            "era": card.get("era"),
            "asset": card_path("event", card_id),
        }

    for card in load_json("buildings.json"):
        card_id = int(card["id"])
        cards[str(card_id)] = {
            "id": card_id,
            "kind": "building",
            "era": card.get("era"),
            "asset": card_path("building", card_id),
        }

    return {
        "cardBack": "gui/assets/cards/backs/card_back.png",
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
                "asset": f"gui/assets/board/offer_tile_{letter.lower()}.png",
            }

    return {
        "totems": {
            "RED": "gui/assets/tokens/totem_red.png",
            "BLUE": "gui/assets/tokens/totem_blue.png",
            "PURPLE": "gui/assets/tokens/totem_purple.png",
            "YELLOW": "gui/assets/tokens/totem_yellow.png",
            "WHITE": "gui/assets/tokens/totem_white.png",
        },
        "offerTiles": tiles,
        "food": "gui/assets/tokens/food.png",
        "prestige": "gui/assets/tokens/prestige.png",
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
