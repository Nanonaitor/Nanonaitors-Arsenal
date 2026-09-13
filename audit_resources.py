from pathlib import Path
import json
root=Path(__file__).resolve().parent/'src/main/resources'
missing=set();count=0
for p in (root/'assets/nanonaitors_arsenal/models').rglob('*.json'):
 d=json.loads(p.read_text());count+=1
 refs=[('models',d.get('parent',''),'.json')]
 refs += [('models',o.get('model',''),'.json') for o in d.get('overrides',[])]
 refs += [('textures',v,'.png') for v in d.get('textures',{}).values()]
 for typ,ref,ext in refs:
  if ref.startswith('nanonaitors_arsenal:'):
   q=root/'assets/nanonaitors_arsenal'/typ/(ref.split(':',1)[1]+ext)
   if not q.exists():missing.add(str(q.relative_to(root)))
print('Audited',count,'models. Missing Arsenal references:',len(missing))
for p in sorted(missing):print(p)
raise SystemExit(bool(missing))
