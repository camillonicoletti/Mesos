#!/usr/bin/env python3
import argparse
import subprocess
import tempfile
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SOURCE = Path("/Users/giorgio/Desktop/Risorse Grafiche Mesos")
ASSETS = ROOT / "src" / "main" / "resources" / "gui" / "assets"

CARD_FRONT_PDF = "Cards_total_front_NO PROMO.pdf"
CARD_BACK_PDF = "Cards_total_back_NO PROMO.pdf"


def run(command):
    subprocess.run(command, check=True)


def render_pdf_page(pdf_path, page, out_prefix, dpi):
    run([
        "pdftoppm",
        "-png",
        "-r",
        str(dpi),
        "-f",
        str(page),
        "-l",
        str(page),
        "-singlefile",
        str(pdf_path),
        str(out_prefix),
    ])
    return out_prefix.with_suffix(".png")


def crop_card(src, dst):
    image = Image.open(src).convert("RGB")
    width, height = image.size
    crop = (
        int(width * 0.10),
        int(height * 0.06),
        int(width * 0.90),
        int(height * 0.94),
    )
    dst.parent.mkdir(parents=True, exist_ok=True)
    image.crop(crop).save(dst, optimize=True)


def optimize_copy(src, dst):
    image = Image.open(src).convert("RGB")
    dst.parent.mkdir(parents=True, exist_ok=True)
    image.save(dst, optimize=True)


def crop_by_ratio(src, dst, ratio_box):
    image = Image.open(src).convert("RGB")
    width, height = image.size
    box = (
        int(width * ratio_box[0]),
        int(height * ratio_box[1]),
        int(width * ratio_box[2]),
        int(height * ratio_box[3]),
    )
    dst.parent.mkdir(parents=True, exist_ok=True)
    image.crop(box).save(dst, optimize=True)


def card_output_path(card_id):
    if 1 <= card_id <= 84:
        return ASSETS / "cards" / "characters" / f"card_{card_id:03d}.png"
    if 85 <= card_id <= 96:
        return ASSETS / "cards" / "events" / f"card_{card_id:03d}.png"
    if 97 <= card_id <= 117:
        return ASSETS / "cards" / "buildings" / f"card_{card_id:03d}.png"
    return ASSETS / "cards" / "summary" / f"card_{card_id:03d}.png"


def extract_cards(source_dir, dpi):
    front_pdf = source_dir / CARD_FRONT_PDF
    back_pdf = source_dir / CARD_BACK_PDF
    if not front_pdf.exists():
        raise FileNotFoundError(front_pdf)
    if not back_pdf.exists():
        raise FileNotFoundError(back_pdf)

    with tempfile.TemporaryDirectory(prefix="mesos_cards_") as tmp_name:
        tmp = Path(tmp_name)

        for page in range(1, 123):
            rendered = render_pdf_page(front_pdf, page, tmp / f"front_{page:03d}", dpi)
            crop_card(rendered, card_output_path(page))

        backs = {
            "back_character_era_i.png": 1,
            "back_character_era_ii.png": 30,
            "back_character_era_iii.png": 58,
            "back_building_era_i.png": 97,
            "back_building_era_ii.png": 103,
            "back_building_era_iii.png": 110,
            "back_event_final.png": 95,
        }
        for filename, page in backs.items():
            rendered = render_pdf_page(back_pdf, page, tmp / f"back_{page:03d}", dpi)
            output = ASSETS / "cards" / "backs" / filename
            crop_card(rendered, output)


def extract_reference_pages(source_dir, dpi):
    sheets = {
        "fustelle_front.pdf": ASSETS / "board" / "fustelle_front",
        "fustelle_back.pdf": ASSETS / "board" / "fustelle_back",
        "fustelle_front_prev.pdf": ASSETS / "board" / "fustelle_front_prev",
        "fustelle_back_prev.pdf": ASSETS / "board" / "fustelle_back_prev",
        "fustelle_DIECUT.pdf": ASSETS / "board" / "fustelle_diecut",
        "pedine_specs.pdf": ASSETS / "tokens" / "pedine_specs",
        "Box_ITA_ART.pdf": ASSETS / "ui" / "box_art",
    }

    with tempfile.TemporaryDirectory(prefix="mesos_refs_") as tmp_name:
        tmp = Path(tmp_name)
        for pdf_name, output_prefix in sheets.items():
            pdf = source_dir / pdf_name
            if not pdf.exists():
                continue
            prefix = tmp / output_prefix.name
            run(["pdftoppm", "-png", "-r", str(dpi), str(pdf), str(prefix)])
            for rendered in sorted(tmp.glob(f"{output_prefix.name}-*.png")):
                suffix = rendered.stem.split("-")[-1]
                optimize_copy(rendered, output_prefix.parent / f"{output_prefix.name}_{suffix}.png")


def extract_offer_tiles(source_dir):
    fustelle_front = source_dir / "fustelle_front.pdf"
    if not fustelle_front.exists():
        return

    with tempfile.TemporaryDirectory(prefix="mesos_offer_tiles_") as tmp_name:
        tmp = Path(tmp_name)
        rendered = render_pdf_page(fustelle_front, 1, tmp / "fustelle_front_1", 300)

        # Tile E is the "1 upper + 1 lower" offer tile.
        crop_by_ratio(
            rendered,
            ASSETS / "board" / "offer_E.png",
            (0.332, 0.386, 0.535, 0.735),
        )


def parse_args():
    parser = argparse.ArgumentParser(description="Extract Mesos GUI assets from professor PDFs.")
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--card-dpi", type=int, default=200)
    parser.add_argument("--reference-dpi", type=int, default=120)
    parser.add_argument("--skip-references", action="store_true")
    return parser.parse_args()


def main():
    args = parse_args()
    extract_cards(args.source, args.card_dpi)
    if not args.skip_references:
        extract_reference_pages(args.source, args.reference_dpi)
        extract_offer_tiles(args.source)
    run(["python3", str(ROOT / "scripts" / "gui" / "generate_asset_manifest.py")])


if __name__ == "__main__":
    main()
