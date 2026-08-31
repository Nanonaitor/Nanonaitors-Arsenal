import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OLD = ROOT.parent / "Nanonaitors-Arsenal" / "src/main/resources/assets/nanonaitors_arsenal/textures/items"
TEX = ROOT / "src/main/resources/assets/nanonaitors_arsenal/textures/item"
MODELS = ROOT / "src/main/resources/assets/nanonaitors_arsenal/models/item"
ITEMS = ROOT / "src/main/resources/assets/nanonaitors_arsenal/items"
TIERS = ("wood", "stone", "copper", "gold", "iron", "diamond", "netherite")


def luminance(color):
    return color[0] * 0.2126 + color[1] * 0.7152 + color[2] * 0.0722


def palette(path):
    image = Image.open(path).convert("RGBA")
    colors = {p[:3] for p in image.getdata() if p[3] and max(p[:3]) - min(p[:3]) > 3}
    return sorted(colors, key=luminance)


def recolor(source, reference, target):
    image = Image.open(source).convert("RGBA")
    colors = palette(reference)
    if not colors:
        image.save(target)
        return
    values = sorted({luminance(p[:3]) for p in image.getdata() if p[3]})
    out = []
    for r, g, b, a in image.getdata():
        if not a:
            out.append((r, g, b, a))
            continue
        rank = values.index(luminance((r, g, b))) / max(1, len(values) - 1)
        color = colors[round(rank * (len(colors) - 1))]
        out.append((*color, a))
    image.putdata(out)
    image.save(target)


def write_json(path, value):
    path.write_text(json.dumps(value, separators=(",", ":")) + "\n", encoding="utf-8")


for tier in TIERS:
    for suffix in ("", "_trail_near", "_trail_far"):
        source_tier = tier if tier in {"wood", "stone", "gold", "iron", "diamond"} else "iron"
        source = OLD / f"flail_spikeball_{source_tier}{suffix}.png"
        target = TEX / f"flail_spikeball_{tier}{suffix}.png"
        if tier in {"copper", "netherite"}:
            recolor(source, TEX / f"flail_swinging_{tier}.png", target)
        else:
            target.write_bytes(source.read_bytes())

        item_id = f"flail_spikeball_visual_{tier}{suffix}"
        write_json(MODELS / f"{item_id}.json", {
            "parent": "item/generated",
            "textures": {"layer0": f"nanonaitors_arsenal:item/flail_spikeball_{tier}{suffix}"}
        })
        write_json(ITEMS / f"{item_id}.json", {
            "model": {"type": "minecraft:model", "model": f"nanonaitors_arsenal:item/{item_id}"}
        })

# Reflection is rendered explicitly at the center. Hide the ordinary held copy
# in both first- and third-person contexts while the reflection state is active.
for tier in TIERS:
    model_path = MODELS / f"blade_staff_{tier}.json"
    model = json.loads(model_path.read_text(encoding="utf-8"))
    for hand in ("firstperson_righthand", "firstperson_lefthand"):
        model["display"][hand]["scale"] = [1.875, 1.875, 1.875]
    write_json(model_path, model)
    write_json(ITEMS / f"blade_staff_{tier}.json", {
        "model": {
            "type": "minecraft:condition", "property": "minecraft:using_item",
            "on_false": {"type": "minecraft:model", "model": f"nanonaitors_arsenal:item/blade_staff_{tier}"},
            "on_true": {
                "type": "minecraft:select", "property": "minecraft:display_context",
                "cases": [{
                    "when": ["first_person_left_hand", "first_person_right_hand",
                             "third_person_left_hand", "third_person_right_hand"],
                    "model": {"type": "minecraft:empty"}
                }],
                "fallback": {"type": "minecraft:model",
                             "model": f"nanonaitors_arsenal:item/blade_staff_{tier}"}
            }
        }
    })
