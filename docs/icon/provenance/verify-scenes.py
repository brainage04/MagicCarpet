"""Reopen delivered .blends, verify native image bytes and unchanged approved style."""
from pathlib import Path
import hashlib
import json
import runpy
import bpy
import numpy as np

ROOT = Path(__file__).resolve().parent
module = runpy.run_path(str(ROOT/'render-native.py'),run_name='native_render')
objects, shaders, sha = module['objects'], module['shader_state'], module['sha']
bpy.ops.wm.open_mainfile(filepath=str(ROOT/'sources/approved-stack.blend'))
reference_objects, reference_shaders = objects(), shaders()
def particle_images():
    out = {}
    for o in bpy.context.scene.objects:
        if '-enchantment-' not in o.name:
            continue
        tex = next(n for n in o.data.materials[0].node_tree.nodes if n.type == 'TEX_IMAGE')
        assert tex.image.packed_file and tex.interpolation == 'Closest'
        out[o['vanilla_asset_path']] = hashlib.sha256(tex.image.packed_file.data).hexdigest()
    return out
reference_particles = particle_images()
reports = []
for variant,name in module['NAMES'].items():
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/(name+'.blend')))
    s = bpy.context.scene
    assert objects() == reference_objects
    assert shaders() == reference_shaders
    assert particle_images() == reference_particles
    assert s.render.engine == 'CYCLES' and s.cycles.device == 'CPU'
    assert s.render.threads == 2 and s.render.compositor_device == 'CPU'
    assert s.render.resolution_x == s.render.resolution_y == 1024 and s.render.resolution_percentage == 100
    assert {t.name for t in bpy.data.texts} == {name+'.py','render-native.py','author-native-textures.py'}
    assert all(t.as_string() == (ROOT/t.name).read_text() for t in bpy.data.texts)
    packed = []
    for im in bpy.data.images:
        if im.source == 'FILE' and im.users:
            assert im.packed_file and len(im.packed_file.data)>0
            packed.append({'name':im.name,'sha256':hashlib.sha256(im.packed_file.data).hexdigest(),'size':list(im.size)})
    for tier in ['basic','advanced','legendary']:
        filename = f'{tier}_magic_carpet.png'
        expected = sha(ROOT/'textures'/variant/'entity'/filename)
        material = next(m for m in bpy.data.materials if m.name.startswith(tier+' — original texture'))
        tex = next(n for n in material.node_tree.nodes if n.type=='TEX_IMAGE')
        assert tex.interpolation == 'Closest' and tuple(tex.image.size) == (64,14)
        assert hashlib.sha256(tex.image.packed_file.data).hexdigest() == expected
    meshes = [o for o in s.objects if o.type=='MESH' and o.get('role')=='source-carpet-geometry']
    assert len(meshes) == 15
    for o in meshes:
        assert len(o.data.polygons) == 6 and len(o.data.vertices) == 8
        assert all(-1e-6<=v<=1.000001 for uv in o.data.uv_layers.active.data for v in uv.uv)
    particles = [o for o in s.objects if '-enchantment-' in o.name]
    assert len(particles) == 60 and all(o.get('particle_size_factor')==2 for o in particles)
    im = bpy.data.images.load(str(ROOT/(name+'.png')),check_existing=False)
    assert tuple(im.size) == (1024,1024)
    pixels = np.asarray(im.pixels[:],dtype=np.float32).reshape((1024,1024,4))
    assert np.isfinite(pixels).all() and pixels[:,:,:3].std() > .08 and pixels[:,:,3].min() > .99
    reports.append({'name':name,'variant':variant,'packed_images':packed,'all_geometry_UV_camera_lights_effects_unchanged':True,'all_material_parameters_world_compositor_unchanged':True,'particle_texture_bytes_unchanged':True,'embedded_scripts_equal_saved_scripts':True,'native_carpet_images':[64,14],'source_geometry_objects':len(meshes),'particle_objects':len(particles),'pixel_stddev':float(pixels[:,:,:3].std()),'blend_sha256':sha(ROOT/(name+'.blend')),'png_sha256':sha(ROOT/(name+'.png'))})
# Empirical render-from-reopened-packed-blend proof: native B, same camera and seed.
name = module['NAMES']['variant-b']
reference = bpy.data.images.load(str(ROOT/(name+'.png')),check_existing=False)
reference_pixels = np.asarray(reference.pixels[:],dtype=np.float32)
rerender = ROOT/'evidence/reopened-b.png'
s.render.filepath = str(rerender)
bpy.ops.render.render(write_still=True)
actual = bpy.data.images.load(str(rerender),check_existing=False)
actual_pixels = np.asarray(actual.pixels[:],dtype=np.float32)
max_delta = float(np.max(np.abs(actual_pixels-reference_pixels)))
assert max_delta == 0, max_delta
report = {'result':'PASS','scenes':reports,'reopened_B_render_max_pixel_delta':max_delta,'reopened_B_render_file_sha256':sha(rerender),'original_B_render_file_sha256':sha(ROOT/(name+'.png')),'reopened_B_render_bytes_identical':rerender.read_bytes()==(ROOT/(name+'.png')).read_bytes()}
(ROOT/'packed-scene-verification.json').write_text(json.dumps(report,indent=2)+'\n')
print('PACKED_SCENES_PASS '+json.dumps({'scenes':len(reports),'reopened_B_max_pixel_delta':max_delta,'all_textures_packed':True,'scripts_match':True}),flush=True)
