"""Verify every static weapon recipe is guarded by its family switch."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/nanonaitors_arsenal/recipes'
families=('double_bladed_scimitar','morning_star','scimitar','claws','flail','battering_ram','ball_and_chain','tartsy_shield','sun_war_bulwark')
checked=0
for path in root.glob('*.json'):
    data=json.loads(path.read_text(encoding='utf-8-sig'))
    result=data.get('result',{})
    item=result.get('item','') if isinstance(result,dict) else result
    if not item.startswith('nanonaitors_arsenal:'): continue
    name=item.split(':',1)[1]
    family=next((f for f in families if name.startswith(f)),None)
    if family:
        assert {'type':'nanonaitors_arsenal:weapon_enabled','family':family} in data.get('conditions',[]),path
        checked+=1
assert checked>=36,checked
print(f'Validated {checked} static weapon recipe conditions')
