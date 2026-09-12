"""Add idempotent Forge conditions to Arsenal's static weapon recipes."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/nanonaitors_arsenal/recipes'
families=('double_bladed_scimitar','morning_star','scimitar','claws','flail','battering_ram','ball_and_chain','tartsy_shield','sun_war_bulwark')
for path in root.glob('*.json'):
    if path.name.startswith('_'): continue
    data=json.loads(path.read_text(encoding='utf-8-sig'))
    result=data.get('result',{})
    item=result.get('item','') if isinstance(result,dict) else result
    if not item.startswith('nanonaitors_arsenal:'): continue
    name=item.split(':',1)[1]
    family=next((f for f in families if name.startswith(f)),None)
    if family is None: continue
    condition={'type':'nanonaitors_arsenal:weapon_enabled','family':family}
    conditions=data.setdefault('conditions',[])
    if condition not in conditions: conditions.append(condition)
    path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
