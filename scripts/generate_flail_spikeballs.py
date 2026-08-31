"""Generate tier-matched flail spike heads and their two baked-alpha echoes.

The commissioned/recolored tier palettes already live in each swinging flail
texture. This keeps the tested iron spike-ball silhouette exactly intact and
maps its shades onto those palettes rather than inventing a second shape.
"""
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/nanonaitors_arsenal/textures/items"
MODELS = ROOT / "src/main/resources/assets/nanonaitors_arsenal/models/item"
TIERS = (
    "wood", "stone", "gold", "diamond", "silver", "bronze", "steel",
    "umbrium", "dragonbone", "flamed_dragonbone", "iced_dragonbone",
    "electric_dragonbone", "desert_myrmex", "jungle_myrmex",
    "desert_venom", "jungle_venom", "living", "sentient",
)
HANDLE_COLORS = {(44, 36, 20), (101, 77, 30)}


def luminance(color: tuple[int, int, int]) -> float:
    return color[0] * 0.2126 + color[1] * 0.7152 + color[2] * 0.0722


def tier_palette(tier: str) -> list[tuple[int, int, int]]:
    image = Image.open(TEXTURES / f"flail_swinging_{tier}.png").convert("RGBA")
    counts = Counter(
        (red, green, blue)
        for red, green, blue, alpha in image.getdata()
        if alpha >= 128 and (red, green, blue) not in HANDLE_COLORS
    )
    colors = [color for color, _ in counts.most_common(6)]
    if len(colors) < 2:
        raise RuntimeError(f"Not enough palette colors for {tier}: {colors}")
    return sorted(colors, key=luminance)


def recolor(source: Path, destination: Path, palette: list[tuple[int, int, int]]) -> None:
    image = Image.open(source).convert("RGBA")
    opaque_luma = sorted({
        round(luminance((red, green, blue)))
        for red, green, blue, alpha in image.getdata() if alpha
    })
    if not opaque_luma:
        raise RuntimeError(f"Source sprite is empty: {source}")
    pixels = []
    for red, green, blue, alpha in image.getdata():
        if alpha == 0:
            pixels.append((0, 0, 0, 0))
            continue
        value = round(luminance((red, green, blue)))
        source_rank = min(range(len(opaque_luma)), key=lambda index: abs(opaque_luma[index] - value))
        target_rank = round(source_rank * (len(palette) - 1) / max(1, len(opaque_luma) - 1))
        target = palette[target_rank]
        pixels.append((*target, alpha))
    output = Image.new("RGBA", image.size)
    output.putdata(pixels)
    output.save(destination, optimize=False)


def write_models(tier: str) -> None:
    base = json.loads((MODELS / "animation_flail_ball_iron.json").read_text(encoding="utf-8"))
    base["textures"] = {
        "particle": f"nanonaitors_arsenal:items/flail_spikeball_{tier}",
        "spikeball": f"nanonaitors_arsenal:items/flail_spikeball_{tier}",
    }
    (MODELS / f"animation_flail_ball_{tier}.json").write_text(
        json.dumps(base, indent=2) + "\n", encoding="utf-8"
    )
    for trail in ("near", "far"):
        model = {
            "parent": f"nanonaitors_arsenal:item/animation_flail_ball_{tier}",
            "textures": {
                "particle": f"nanonaitors_arsenal:items/flail_spikeball_{tier}_trail_{trail}",
                "spikeball": f"nanonaitors_arsenal:items/flail_spikeball_{tier}_trail_{trail}",
            },
        }
        (MODELS / f"animation_flail_ball_{tier}_trail_{trail}.json").write_text(
            json.dumps(model, indent=2) + "\n", encoding="utf-8"
        )


def update_flail_model(tier: str) -> None:
    path = MODELS / f"flail_{tier}.json"
    model = json.loads(path.read_text(encoding="utf-8"))
    retained = [
        override for override in model.get("overrides", [])
        if override.get("predicate", {}).get("nanonaitors_arsenal:animation_part") not in (2, 4, 5)
    ]
    retained.extend((
        {"predicate": {"nanonaitors_arsenal:animation_part": 2},
         "model": f"nanonaitors_arsenal:item/animation_flail_ball_{tier}"},
        {"predicate": {"nanonaitors_arsenal:animation_part": 4},
         "model": f"nanonaitors_arsenal:item/animation_flail_ball_{tier}_trail_near"},
        {"predicate": {"nanonaitors_arsenal:animation_part": 5},
         "model": f"nanonaitors_arsenal:item/animation_flail_ball_{tier}_trail_far"},
    ))
    model["overrides"] = retained
    path.write_text(json.dumps(model, separators=(",", ":")) + "\n", encoding="utf-8")


def main() -> None:
    source_names = {
        "": "flail_spikeball_iron.png",
        "_trail_near": "flail_spikeball_iron_trail_near.png",
        "_trail_far": "flail_spikeball_iron_trail_far.png",
    }
    for tier in TIERS:
        palette = tier_palette(tier)
        for suffix, source_name in source_names.items():
            recolor(TEXTURES / source_name,
                    TEXTURES / f"flail_spikeball_{tier}{suffix}.png", palette)
        write_models(tier)
        update_flail_model(tier)


if __name__ == "__main__":
    main()
