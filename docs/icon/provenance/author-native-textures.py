#!/usr/bin/env python3
"""Native pixel art: 64x14 atlases, 24x8 body, 24x4 end; never resize revamp input.
Pillow resizes are confined to the labelled comparison sheet, never asset creation.
"""
from pathlib import Path
import hashlib
import json
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
TIERS = {
    'basic': {'rank': 1, 'name': 'Ruby wayfinder', 'body': (100,19,37), 'border': (161,103,30), 'line': (232,179,68), 'light': (255,229,144), 'gem': (228,81,83), 'shadow': (48,12,26)},
    'advanced': {'rank': 2, 'name': 'Copper astral compass', 'body': (23,40,56), 'border': (153,74,29), 'line': (241,145,63), 'light': (158,236,247), 'gem': (53,163,194), 'shadow': (12,20,38)},
    'legendary': {'rank': 3, 'name': 'Amethyst star crown', 'body': (65,23,112), 'border': (81,126,30), 'line': (182,226,89), 'light': (250,225,255), 'gem': (178,112,244), 'shadow': (32,10,64)},
}
MOTIFS = {
    'basic': ['...L...', '..LBL..', '.LBGBL.', 'LBGHGBL', '.LBGBL.', '..LBL..', '...L...'],
    'advanced': ['...L...', '...G...', '..LHL..', 'LGHGHGL', '..LHL..', '...G...', '...L...'],
    'legendary': ['..L.L.L..', '...LHL...', '..LHGHL..', 'LHHGBGHHL', '..LHGHL..', '...LHL...', '..L.L.L..'],
}
RUNES = [('101','111','010'), ('010','110','011'), ('101','111','100')]
PIP = (255,244,214,255)
BADGE = (24,16,32,255)
ENCODING = 'Variant B has a dark rank badge on BOTH end borders: 1 ivory 2x2 square pip = basic / tier 1; 2 = advanced / tier 2; 3 = legendary / tier 3. Each pip is separated by four dark pixels. No color key, floating particles or label is needed. A and B have identical middle/body panels.'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tinted(source, color):
    out = Image.new('RGBA', source.size)
    values = [sum(p[:3])/3 for p in source.get_flattened_data()]
    mean = sum(values)/len(values)
    for y in range(source.height):
        for x in range(source.width):
            p = source.getpixel((x,y))
            delta = (sum(p[:3])/3-mean)*0.28 + (2 if (x+y)%2 else -2)
            out.putpixel((x,y),tuple(max(0,min(255,round(c+delta))) for c in color)+(p[3],))
    return out


def body_panel(source, tier):
    c = TIERS[tier]
    im = tinted(source,c['body'])
    d = ImageDraw.Draw(im)
    for x in [0,23]: d.line((x,0,x,7),fill=c['shadow'])
    for x in [1,22]:
        d.line((x,0,x,7),fill=c['line'])
        for y in [1,5]: d.point((x,y),fill=c['light'])
    d.line((11,0,11,7),fill=c['border'])
    palette = dict(L=c['line'], B=c['border'], G=c['gem'], H=c['light'])
    rows = MOTIFS[tier]
    start = 11-len(rows[0])//2
    assert len({len(row) for row in rows}) == 1
    for y,row in enumerate(rows):
        for x,token in enumerate(row):
            if token != '.': d.point((start+x,y),fill=palette[token])
    rank = c['rank']
    for x,rows in [(4,RUNES[rank-1]),(18,RUNES[rank%3])]:
        for y,row in enumerate(rows):
            for xx,token in enumerate(row):
                if token == '1': d.point((x+xx,2+y),fill=c['line'] if rank==1 else c['light'])
    for x,y in [(6,0),(18,6)]: d.point((x,y),fill=c['light'])
    if rank==3:
        for x,y in [(4,6),(20,0)]: d.point((x,y),fill=c['gem'])
    return im


def end_panel(source, tier, variant):
    c = TIERS[tier]
    im = tinted(source,c['border'])
    d = ImageDraw.Draw(im)
    for y in [0,3]: d.line((0,y,23,y),fill=c['line'])
    d.rectangle((0,1,23,2),fill=c['shadow'])
    if variant == 'variant-a':
        # Native two-row beading, not a filtered copy of the 2x diamonds.
        for x in range(3,24,4):
            d.point((x,1),fill=c['light'])
            d.line((x-1,2,x+1,2),fill=c['border'])
    else:
        d.rectangle((2,1,21,2),fill=BADGE)
        rank = c['rank']
        left = 11-3*(rank-1)
        for n in range(rank):
            x = left+6*n
            d.rectangle((x,1,x+1,2),fill=PIP)
        for x in [0,23]: d.line((x,1,x,2),fill=c['border'])
    return im


def atlas(tier, variant):
    original = Image.open(ROOT/'original/entity'/f'{tier}_magic_carpet.png').convert('RGBA')
    assert original.size == (64,14)
    alpha = original.getchannel('A')
    out = Image.new('RGBA',(64,14))
    out.paste(tinted(original.crop((0,0,64,5)),TIERS[tier]['border']),(0,0))
    out.paste(tinted(original.crop((0,5,64,14)),TIERS[tier]['body']),(0,5))
    mid = body_panel(original.crop((8,5,32,13)),tier)
    end = end_panel(original.crop((4,0,28,4)),tier,variant)
    for x in [8,32]: out.paste(mid,(x,5))
    for x in [4,28]: out.paste(end,(x,0))
    d = ImageDraw.Draw(out)
    for y in [4,13]:
        d.line((0,y,63,y),fill=TIERS[tier]['line'])
        for x in range(1,64,4): d.point((x,y),fill=TIERS[tier]['light'])
    out.putalpha(alpha)
    return out


def assembled(im):
    scale = im.width//64
    end = im.crop((4*scale,0,28*scale,4*scale))
    mid = im.crop((8*scale,5*scale,32*scale,13*scale))
    out = Image.new('RGBA',(24*scale,32*scale))
    out.paste(end,(0,0))
    for y in [4,12,20]: out.paste(mid,(0,y*scale))
    out.paste(end,(0,28*scale))
    return out


def comparison():
    sheet = Image.new('RGB',(1696,1620),'#12101c')
    d = ImageDraw.Draw(sheet)
    title = ImageFont.load_default(size=32)
    text = ImageFont.load_default(size=22)
    small = ImageFont.load_default(size=17)
    d.text((32,22),'MagicCarpet | Native-resolution texture variants',font=title,fill='#f4e8ff')
    d.text((32,70),'Original atlas: 64x14. A/B: redrawn at 64x14. Approved 2x: 128x28. Same model / UVs.',font=text,fill='#c6b9d8')
    d.text((32,104),'Nearest-neighbor enlargement below is for inspection only. Entity and item bytes match for each tier.',font=small,fill='#aa9abb')
    stages = [('original','Original / 64x14'),('revamp-2x','Approved 2x / 128x28'),('variant-a','Variant A / 64x14'),('variant-b','Variant B / 64x14')]
    for i,(tier,c) in enumerate(TIERS.items()):
        y = 148+i*448
        d.text((32,y),f'TIER {c["rank"]} / {tier.upper()} / {c["name"]}',font=text,fill=c['line'])
        for j,(stage,label) in enumerate(stages):
            x = 32+j*416
            source = ROOT/stage if stage in ['original','revamp-2x'] else ROOT/'textures'/stage
            im = Image.open(source/'entity'/f'{tier}_magic_carpet.png').convert('RGBA')
            d.text((x,y+33),label,font=text,fill='#eee3fa')
            zoom = im.resize((384,84),Image.Resampling.NEAREST)
            sheet.paste(zoom,(x,y+68),zoom)
            face = assembled(im).resize((168,224),Image.Resampling.NEAREST)
            sheet.paste(face,(x+4,y+168),face)
            d.text((x+192,y+171),'Assembled top',font=small,fill='#bfb0ce')
            d.text((x+192,y+197),'3 body repeats',font=small,fill='#bfb0ce')
            d.text((x+192,y+229),'Actual atlas 1:1',font=small,fill='#bfb0ce')
            sheet.paste(im,(x+192,y+255),im)
            if stage=='variant-b':
                badge = im.crop((4,0,28,4))
                sheet.paste(badge.resize((168,28),Image.Resampling.NEAREST),(x+192,y+298))
                d.text((x+192,y+339),f'{c["rank"]} square pip'+('s' if c['rank']>1 else ''),font=text,fill=PIP[:3])
                d.text((x+192,y+371),'Both end borders',font=small,fill='#bfb0ce')
        d.line((32,y+425,1664,y+425),fill='#392b49')
    d.text((32,1510),'Variant B rank: 1 / 2 / 3 ivory 2x2 square pips on dark end badges. Body motifs stay exactly A.',font=text,fill='#fff4d6')
    d.text((32,1551),'No glow, labels or floating particles are baked into assets. All comparisons use exact PNG pixels.',font=small,fill='#bfb0ce')
    sheet.save(ROOT/'texture-comparison.png')


def main():
    records = []
    for variant in ['variant-a','variant-b']:
        for tier,c in TIERS.items():
            im = atlas(tier,variant)
            for kind in ['entity','item']:
                rel = f'{variant}/{kind}/{tier}_magic_carpet.png'
                path = ROOT/'textures'/rel
                path.parent.mkdir(parents=True,exist_ok=True)
                im.save(path,optimize=True)
                records.append({'variant':variant,'tier':c['rank'],'tier_name':tier,'kind':kind,'name':path.name,'path':rel,'size':list(im.size),'sha256':sha(path),'install_path':f'common/src/main/resources/assets/magic_carpet/textures/{kind}/{path.name}'})
    comparison()
    report = {'original_size':[64,14],'approved_2x_size':[128,28],'variant_size':[64,14],'native_top_face_size':[24,32],'authoring':'New integer-grid pixel art at 64x14 using only original 64x14 wool luminance and the approved palette; no resampling of 128x28 art. Comparison display scaling is separate.','tier_encoding':ENCODING,'selection':'Choose one variant and copy its six entity/item PNGs to the listed install_path values, replacing the existing six textures. These are texture alternatives, not standalone resource packs. Active approved f08018a assets are deliberately unchanged.','textures':records}
    (ROOT/'texture-inventory.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'native_textures':len(records),'original_size':[64,14],'tier_encoding':ENCODING}))


if __name__ == '__main__':
    main()
