#!/usr/bin/env python3
"""Exercise authored PNGs and native UV regions, not source-code pattern assertions."""
from pathlib import Path
import hashlib
import json
import runpy
import subprocess
import sys
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parent
module = runpy.run_path(str(ROOT/'author-native-textures.py'),run_name='native_author')
TIERS = module['TIERS']
PIP = module['PIP']
sha = module['sha']
paths = sorted((ROOT/'textures').rglob('*.png'))
before = {str(p.relative_to(ROOT)):sha(p) for p in paths}
subprocess.run([sys.executable,str(ROOT/'author-native-textures.py')],check=True)
assert before == {str(p.relative_to(ROOT)):sha(p) for p in paths}, 'Authoring must reproduce every PNG byte'
reports = []
source_inventory = []
for stage in ['original','revamp-2x']:
    for path in sorted((ROOT/stage).rglob('*.png')):
        im = Image.open(path)
        assert im.size == ((64,14) if stage=='original' else (128,28))
        source_inventory.append({'stage':stage,'name':path.name,'path':str(path.relative_to(ROOT)),'size':list(im.size),'sha256':sha(path)})
for tier,c in TIERS.items():
    name = f'{tier}_magic_carpet.png'
    original = Image.open(ROOT/'original/entity'/name).convert('RGBA')
    revamped = Image.open(ROOT/'revamp-2x/entity'/name).convert('RGBA')
    a = Image.open(ROOT/'textures/variant-a/entity'/name).convert('RGBA')
    b = Image.open(ROOT/'textures/variant-b/entity'/name).convert('RGBA')
    for variant,im in [('variant-a',a),('variant-b',b)]:
        assert im.size == original.size == (64,14)
        assert im.getchannel('A').tobytes() == original.getchannel('A').tobytes()
        assert im.crop((4,0,28,4)).tobytes() == im.crop((28,0,52,4)).tobytes()
        assert im.crop((8,5,32,13)).tobytes() == im.crop((32,5,56,13)).tobytes()
        assert (ROOT/'textures'/variant/'entity'/name).read_bytes() == (ROOT/'textures'/variant/'item'/name).read_bytes()
        assert all(im.tobytes()!=revamped.resize((64,14),method).tobytes() for method in [Image.Resampling.NEAREST,Image.Resampling.BOX,Image.Resampling.BILINEAR,Image.Resampling.BICUBIC,Image.Resampling.LANCZOS])
    assert a.crop((0,4,64,14)).tobytes() == b.crop((0,4,64,14)).tobytes(), 'B must only change end ornament'
    end = b.crop((4,0,28,4))
    unseen = {(x,y) for y in range(4) for x in range(24) if end.getpixel((x,y)) == PIP}
    components = []
    while unseen:
        todo = [unseen.pop()]
        part = set(todo)
        while todo:
            x,y = todo.pop()
            for nxt in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)]:
                if nxt in unseen:
                    unseen.remove(nxt)
                    part.add(nxt)
                    todo.append(nxt)
        xs,ys = zip(*part)
        bbox = [min(xs),min(ys),max(xs)+1,max(ys)+1]
        assert len(part) == 4 and bbox[2]-bbox[0] == bbox[3]-bbox[1] == 2
        components.append(bbox)
    components.sort()
    assert len(components) == c['rank']
    assert all(right[0]-left[2] == 4 for left,right in zip(components,components[1:]))
    face = module['assembled'](b)
    assert face.size == (24,32)
    assert face.crop((0,0,24,4)).tobytes() == face.crop((0,28,24,32)).tobytes() == end.tobytes()
    for y in [4,12,20]:
        assert face.crop((0,y,24,y+8)).tobytes() == b.crop((8,5,32,13)).tobytes()
    diff = ImageChops.difference(a.convert('RGB'),b.convert('RGB'))
    reports.append({'tier':tier,'rank':c['rank'],'native_atlas_size':[64,14],'native_top_face_size':[24,32],'item_entity_identical':True,'original_alpha_preserved':True,'top_bottom_uv_identical':True,'three_body_repeats_identical':True,'body_A_B_identical':True,'A_B_difference_bounds':list(diff.getbbox()),'pip_components_per_end':components,'pip_pixels_per_end':4*len(components),'not_resampled_revamp':True})
report = {'result':'PASS','reproducible_PNG_bytes':True,'source_inventory':source_inventory,'tiers':reports,'texture_hashes':before,'comparison_sha256':sha(ROOT/'texture-comparison.png'),'commands':['python3 author-native-textures.py','python3 verify-assets.py'],'pillow_pythonpath':'/nix/store/4v9j9wbzyhrlx9980ygbr812313mazy0-python3.13-pillow-12.3.0/lib/python3.13/site-packages'}
(ROOT/'asset-verification.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'result':'PASS','textures':len(paths),'tiers':len(reports),'dimensions':[64,14],'rank_counts':[len(r['pip_components_per_end']) for r in reports],'deterministic':True}))
