"""Swap only texture image bindings in the approved tier stack; keep its styling."""
from pathlib import Path
import hashlib
import json
import os
import sys
import time
import bpy
import numpy as np
from bpy_extras.object_utils import world_to_camera_view

ROOT = Path(__file__).resolve().parent
NAMES = {'variant-a':'magic-carpet-01-native-a-stack','variant-b':'magic-carpet-02-native-b-ranked-stack'}


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def digest(value):
    return hashlib.sha256(json.dumps(value,sort_keys=True,separators=(',',':')).encode()).hexdigest()


def objects():
    # Same invariant inventory as blender-r: transforms, geometry, UVs, effects.
    out = {}
    for o in bpy.context.scene.objects:
        r = {'type':o.type,'matrix':[list(row) for row in o.matrix_world],'hide_render':o.hide_render,'properties':{k:str(o[k]) for k in o.keys()}}
        if o.type == 'MESH':
            r.update(vertices=[list(v.co) for v in o.data.vertices],faces=[list(p.vertices) for p in o.data.polygons],uvs=[list(v.uv) for v in o.data.uv_layers.active.data] if o.data.uv_layers.active else [])
        elif o.type == 'CURVE':
            r.update(bevel_depth=o.data.bevel_depth,splines=[[list(p.co) for p in s.points] for s in o.data.splines])
        elif o.type == 'CAMERA':
            r.update(ortho_scale=o.data.ortho_scale,lens=o.data.lens)
        elif o.type == 'LIGHT':
            r.update(energy=o.data.energy,color=list(o.data.color))
        out[o.name] = r
    return out


def shader_state():
    trees = {m.name:m.node_tree for m in bpy.data.materials if m.use_nodes}
    trees.update(world=bpy.context.scene.world.node_tree,compositor=bpy.context.scene.compositing_node_group)
    out = {}
    for name,tree in trees.items():
        nodes = []
        for n in tree.nodes:
            values = {}
            for s in n.inputs:
                if hasattr(s,'default_value'):
                    v = s.default_value
                    values[s.identifier] = v if isinstance(v,(int,float,str,bool)) else list(v) if hasattr(v,'__iter__') else str(v)
            nodes.append({'name':n.name,'type':n.bl_idname,'values':values})
        out[name] = {'nodes':nodes,'links':sorted([f'{l.from_node.name}:{l.from_socket.identifier}>{l.to_node.name}:{l.to_socket.identifier}' for l in tree.links])}
    return out


def render(variant):
    assert bpy.app.background and '-noaudio' in sys.argv
    assert not os.environ.get('DISPLAY') and not os.environ.get('WAYLAND_DISPLAY')
    assert os.environ.get('CUDA_VISIBLE_DEVICES') == ''
    source = ROOT/'sources/approved-stack.blend'
    bpy.ops.wm.open_mainfile(filepath=str(source))
    s = bpy.context.scene
    before_objects, before_shaders = objects(), shader_state()
    bindings = []
    for tier in ['basic','advanced','legendary']:
        material = next(m for m in bpy.data.materials if m.name.startswith(tier+' — original texture'))
        tex = next(n for n in material.node_tree.nodes if n.type=='TEX_IMAGE')
        old = tex.image
        assert tuple(old.size) == (128,28)
        assert hashlib.sha256(old.packed_file.data).hexdigest() == sha(ROOT/'revamp-2x/entity'/f'{tier}_magic_carpet.png')
        path = ROOT/'textures'/variant/'entity'/f'{tier}_magic_carpet.png'
        new = bpy.data.images.load(str(path),check_existing=False)
        new.name = tier+' '+variant+' native 64x14'
        new.pack()
        assert tuple(new.size) == (64,14)
        tex.image = new
        assert tex.interpolation == 'Closest'
        bindings.append({'tier':tier,'variant':variant,'size':[64,14],'sha256':sha(path),'material':material.name,'filter':tex.interpolation})
    assert objects() == before_objects
    assert shader_state() == before_shaders
    particles = [o for o in s.objects if '-enchantment-' in o.name]
    assert len(particles) == 60 and all(o.get('particle_size_factor')==2 for o in particles)
    subjects = [o for o in s.objects if o.type=='MESH' and (o.get('tier') or '-enchantment-' in o.name)]
    points = [world_to_camera_view(s,s.camera,o.matrix_world@v.co) for o in subjects for v in o.data.vertices]
    bounds = [min(p.x for p in points),min(p.y for p in points),max(p.x for p in points),max(p.y for p in points)]
    assert min(bounds) > .01 and max(bounds) < .99
    name = NAMES[variant]
    s.render.engine = 'CYCLES'
    s.cycles.device = 'CPU'
    s.cycles.samples = 96
    s.render.compositor_device = 'CPU'
    s.render.threads_mode = 'FIXED'
    s.render.threads = 2
    s.render.resolution_x = s.render.resolution_y = 1024
    s.render.resolution_percentage = 100
    s.render.image_settings.file_format = 'PNG'
    s.render.image_settings.color_mode = 'RGBA'
    s.render.filepath = str(ROOT/(name+'.png'))
    bpy.context.preferences.filepaths.save_version = 0
    s['reproducible_script'] = name+'.py + render-native.py'
    s['presentation_note'] = 'Native 64x14 texture art. Floating vanilla glyphs, bloom and luminous perimeter are the unchanged approved preview styling, not new in-game behavior.'
    s['texture_variant'] = variant
    for text in list(bpy.data.texts): bpy.data.texts.remove(text)
    for file in [name+'.py','render-native.py','author-native-textures.py']:
        text = bpy.data.texts.load(str(ROOT/file))
        text.use_fake_user = True
    for im in list(bpy.data.images):
        if im.source == 'FILE' and not im.users: bpy.data.images.remove(im)
    bpy.ops.file.pack_all()
    packed = [{'name':im.name,'sha256':hashlib.sha256(im.packed_file.data).hexdigest(),'bytes':len(im.packed_file.data),'size':list(im.size)} for im in bpy.data.images if im.source=='FILE' and im.users]
    assert all(r['bytes'] for r in packed)
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/(name+'.blend')))
    started = time.perf_counter()
    bpy.ops.render.render(write_still=True)
    seconds = time.perf_counter()-started
    decoded = bpy.data.images.load(str(ROOT/(name+'.png')),check_existing=False)
    assert tuple(decoded.size) == (1024,1024)
    pixels = np.asarray(decoded.pixels[:],dtype=np.float32).reshape((1024,1024,4))
    assert np.isfinite(pixels).all() and pixels[:,:,:3].std() > .08 and pixels[:,:,3].min() > .99
    report = {'name':name,'variant':variant,'engine':'CYCLES','device':'CPU','threads':2,'samples':96,'blender':bpy.app.version_string,'resolution':[1024,1024],'seconds':seconds,'approved_scene_sha256':sha(source),'texture_bindings':bindings,'all_geometry_uvs_camera_lights_effects_unchanged':True,'all_material_parameters_world_compositor_unchanged':True,'source_objects_sha256':digest(before_objects),'result_objects_sha256':digest(objects()),'source_shaders_sha256':digest(before_shaders),'result_shaders_sha256':digest(shader_state()),'bounds':bounds,'particle_size_factor':2,'visible_particles':len(particles),'packed_images':packed,'png_sha256':sha(ROOT/(name+'.png')),'blend_sha256':sha(ROOT/(name+'.blend'))}
    (ROOT/(name+'-metadata.json')).write_text(json.dumps(report,indent=2)+'\n')
    print('RENDER_COMPLETE '+json.dumps({'name':name,'seconds':seconds}),flush=True)


if __name__ == '__main__':
    for variant in (sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else NAMES):
        render(variant)
