import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
models = root / "src/main/resources/assets/nanonaitors_arsenal/models/item"

for path in sorted(models.glob("flail_*.json")):
    if "swinging" in path.stem:
        continue
    data = json.loads(path.read_text(encoding="utf-8"))
    overrides = data.get("overrides", [])
    swinging = [entry for entry in overrides if "nanonaitors_arsenal:swinging" in entry.get("predicate", {})]
    parts = [entry for entry in overrides if "nanonaitors_arsenal:animation_part" in entry.get("predicate", {})]
    other = [entry for entry in overrides if entry not in swinging and entry not in parts]
    parts.sort(key=lambda entry: entry["predicate"]["nanonaitors_arsenal:animation_part"])
    data["overrides"] = swinging + parts + other
    path.write_text(json.dumps(data, separators=(",", ":")) + "\n", encoding="utf-8")
