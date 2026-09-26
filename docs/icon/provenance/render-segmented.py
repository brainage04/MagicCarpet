"""Re-render the approved tier-stack icon with the mod's current segmented carpet models and textures.

The approved scene's camera, lights, world, floor, glyph particles, compositor and carpet material styling are kept.
The flat five-cuboid carpets are replaced by the geometry of MagicCarpetEntityModel.createBodyLayer (built box by box
with Minecraft's box UV layout), posed with its setupAnim at a gentle cruising speed, and textured with the PNGs
written by tools/generate_carpet_assets.py. The luminous perimeter overlays are re-traced around the posed carpets.

    nix shell nixpkgs#blender --command blender --background -noaudio --python-exit-code 1 --python render-segmented.py
"""
from pathlib import Path
import hashlib
import json
import math
import os
import sys
import time

import bmesh
import bpy
import numpy as np
from bpy_extras.object_utils import world_to_camera_view
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parent.parent.parent
TEXTURES = REPO / 'common/src/main/resources/assets/magic_carpet/textures/entity'
NAME = 'magic-carpet-03-segmented-stack'
TIERS = ('basic', 'advanced', 'legendary')

# --- MagicCarpetEntityModel, transcribed (pixels; Y up, front towards -Z) --------------------------------------------
TEX_W, TEX_H = 96, 48
SEGMENTS, SEGMENT_LENGTH, FRONT_Z = 8, 4.0, -16.0
TASSEL_X = (-6.0, -3.0, 0.0, 3.0, 6.0)
TASSEL_DROOP, CURL_PITCH, CURL_ROLL, HOVER_HEIGHT = 0.6, 0.9, 0.5, 1.0
FEATURES = {'basic': (False, False), 'advanced': (True, False), 'legendary': (True, True)}
# icon pose: a gentle cruise, so the ripple, lifted back edge and trailing tassels read at thumbnail size
POSE_SPEED = 0.35
POSE_TIME = {'basic': 12.0, 'advanced': 31.0, 'legendary': 47.0}


def part(offset=(0, 0, 0), rotation=(0, 0, 0), boxes=(), mirror=False):
    return {'offset': list(offset), 'rotation': list(rotation), 'boxes': list(boxes), 'mirror': mirror, 'children': {}}


def body_layer(tassels, curls):
    carpet = part()
    for i in range(SEGMENTS):
        first, last = i == 0, i == SEGMENTS - 1
        cut = first and curls
        boxes = [(56, 40, -8, -1, 0, 16, 1, 4)] if cut else [(0, 5 * i, -12, -1, 0, 24, 1, 4)]
        if not cut:
            boxes += [(56, 5 * i, -12, 0, 0, 2, 1, 4), (56, 5 * i, 10, 0, 0, 2, 1, 4)]
        if cut:
            boxes.append((0, 43, -8, 0, 0, 16, 1, 2))
        elif first:
            boxes.append((0, 40, -10, 0, 0, 20, 1, 2))
        elif last:
            boxes.append((0, 40, -10, 0, 2, 20, 1, 2))
        segment = part((0, 1, FRONT_Z + SEGMENT_LENGTH * i), boxes=boxes)
        carpet['children'][f'segment_{i}'] = segment
        if tassels and first:
            for t, x in enumerate(TASSEL_X):
                segment['children'][f'front_tassel_{t}'] = part((x, -0.5, 0), boxes=[(44, 40, -0.5, -0.5, -3, 1, 1, 3)])
        if tassels and last:
            for t, x in enumerate(TASSEL_X):
                segment['children'][f'back_tassel_{t}'] = part((x, -0.5, SEGMENT_LENGTH), boxes=[(44, 40, -0.5, -0.5, 0, 1, 1, 3)])
        if cut:
            for name, side in (('left_curl', -1), ('right_curl', 1)):
                inner = -4 if side < 0 else 0
                segment['children'][name] = part((8 * side, 0, SEGMENT_LENGTH), (CURL_PITCH, 0, CURL_ROLL * side),
                                                 [(68, 0, inner, -1, -4, 4, 1, 4), (68, 5, inner, -1, -5, 4, 2, 2)],
                                                 mirror=side > 0)
    return {'carpet': carpet}


def setup_anim(root, time_, speed):
    carpet = root['carpet']
    calm = 1 - 0.6 * speed
    carpet['offset'][1] = HOVER_HEIGHT + 0.5 * math.sin(time_ * 0.1) * calm
    carpet['rotation'][2] = 0.03 * math.sin(time_ * 0.07) * calm
    carpet['rotation'][0] = 0.015 * math.sin(time_ * 0.05 + 1) * calm
    amplitude, frequency = 0.4 + 1.1 * speed, 0.08 + 0.42 * speed
    heights = []
    for i in range(SEGMENTS + 1):
        along = i / SEGMENTS
        heights.append(amplitude * (0.5 + along) * math.sin(SEGMENT_LENGTH * i * 0.22 - time_ * frequency) + 1.4 * speed * along * along)
    for i in range(SEGMENTS):
        seg = carpet['children'][f'segment_{i}']
        seg['offset'][1] += heights[i]
        seg['rotation'][0] = -math.atan2(heights[i + 1] - heights[i], SEGMENT_LENGTH)
    front, back = carpet['children']['segment_0'], carpet['children'][f'segment_{SEGMENTS - 1}']
    for i in range(len(TASSEL_X)):
        if f'back_tassel_{i}' not in back['children']:
            break
        flutter = (0.12 + 0.3 * speed) * math.sin(time_ * (0.12 + 0.6 * speed) + i * 1.3)
        sway = 0.12 * math.sin(time_ * 0.09 + i * 2.1) * calm
        back['children'][f'back_tassel_{i}']['rotation'][:2] = [TASSEL_DROOP + (0.2 - TASSEL_DROOP) * speed + flutter, sway]
        front['children'][f'front_tassel_{i}']['rotation'][:2] = [-(TASSEL_DROOP + (2.2 - TASSEL_DROOP) * speed + flutter), sway]
    for name in ('left_curl', 'right_curl'):
        if name in front['children']:
            front['children'][name]['rotation'][0] += 0.05 * math.sin(time_ * 0.1) + 0.1 * speed


def pose_matrix(p):
    x, y, z = p['rotation']
    # ModelPart.translateAndRotate: translate, then rotationZYX(zRot, yRot, xRot)
    return (Matrix.Translation(Vector(p['offset']))
            @ Matrix.Rotation(z, 4, 'Z') @ Matrix.Rotation(y, 4, 'Y') @ Matrix.Rotation(x, 4, 'X'))


def box_faces(u, v, x0, y0, z0, w, h, d, mirror):
    """ModelPart.Cube: the six polygons as [(position, (u, v)) x 4] in texture units."""
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    if mirror:
        x0, x1 = x1, x0
    p = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
    f10, f11, f12 = v, v + d, v + d + h
    faces = []
    for idx, u1, v1, u2, v2 in (((5, 4, 0, 1), f5, f10, f6, f11), ((2, 3, 7, 6), f6, f11, f7, f10),
                                ((0, 4, 7, 3), f4, f11, f5, f12), ((1, 0, 3, 2), f5, f11, f6, f12),
                                ((5, 1, 2, 6), f6, f11, f8, f12), ((4, 5, 6, 7), f8, f11, f9, f12)):
        uvs = ((u2, v1), (u1, v1), (u1, v2), (u2, v2))
        face = [(p[i], uv) for i, uv in zip(idx, uvs)]
        faces.append(face[::-1] if mirror else face)
    return faces


def collect(parts, parent, out):
    for p in parts.values():
        m = parent @ pose_matrix(p)
        for box in p['boxes']:
            for face in box_faces(*box, p['mirror']):
                out.append([(m @ Vector(pos), uv) for pos, uv in face])
        collect(p['children'], m, out)


def outline(root, curls):
    """The posed carpet's bottom perimeter, as points in model space."""
    carpet = root['carpet']
    base = pose_matrix(carpet)
    segs = [base @ pose_matrix(carpet['children'][f'segment_{i}']) for i in range(SEGMENTS)]

    def at(i, x, z):
        return segs[i] @ Vector((x, -1, z))

    right = [at(i, 12, 0) for i in range(1 if curls else 0, SEGMENTS)] + [at(SEGMENTS - 1, 12, SEGMENT_LENGTH)]
    left = [at(SEGMENTS - 1, -12, SEGMENT_LENGTH)] + [at(i, -12, 0) for i in reversed(range(1 if curls else 0, SEGMENTS))]
    if curls:  # trace the notch the lifted corners leave in the front segment
        front = [at(0, -8, SEGMENT_LENGTH), at(0, -8, 0), at(0, 8, 0), at(0, 8, SEGMENT_LENGTH)]
    else:
        front = []
    return front + right + left


# --- scene ----------------------------------------------------------------------------------------------------------

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def to_scene(base_z):
    # model (X, Y up, -Z front) -> scene (X, Z up); a proper rotation (+90 degrees about X), so the carpet front
    # faces away from the camera; the model's resting underside (y = 1 px) sits where the old cuboids' did
    return Matrix.Translation((0, 0, base_z - 1)) @ Matrix.Rotation(math.radians(90), 4, 'X')


def build_mesh(tier, faces, material):
    me = bpy.data.meshes.new(f'{tier} segmented carpet')
    bm = bmesh.new()
    uv_layer = bm.loops.layers.uv.new('Minecraft box UV')
    for face in faces:
        verts = [bm.verts.new(pos) for pos, _ in face]
        f = bm.faces.new(verts)
        for loop, (_, (u, v)) in zip(f.loops, face):
            loop[uv_layer].uv = (u / TEX_W, 1 - v / TEX_H)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(me)
    bm.free()
    me.materials.append(material)
    obj = bpy.data.objects.new(f'{tier}-segmented-carpet', me)
    obj['role'] = 'mod-carpet-geometry'
    obj['tier'] = tier
    bpy.context.scene.collection.objects.link(obj)
    return obj


def bind_texture(tier, material):
    nodes, links = material.node_tree.nodes, material.node_tree.links
    tex = next(n for n in nodes if n.type == 'TEX_IMAGE')
    image = bpy.data.images.load(str(TEXTURES / f'{tier}_magic_carpet.png'), check_existing=False)
    image.name = f'{tier} segmented 96x48 (2x)'
    image.pack()
    tex.image = image
    assert tex.interpolation == 'Closest'
    binding = {'tier': tier, 'texture': f'{tier}_magic_carpet.png', 'size': list(image.size), 'sha256': sha(TEXTURES / f'{tier}_magic_carpet.png')}

    glow_path = TEXTURES / f'{tier}_magic_carpet_glow.png'
    if glow_path.exists():
        # the in-game full-bright pass: glowing threads added on top of the lit surface
        glow = bpy.data.images.load(str(glow_path), check_existing=False)
        glow.name = f'{tier} glow threads'
        glow.pack()
        glow_tex = nodes.new('ShaderNodeTexImage')
        glow_tex.image, glow_tex.interpolation = glow, 'Closest'
        glow_tex.location = tex.location + Vector((0, -320))
        uv = next((l.from_socket for l in links if l.to_socket == tex.inputs['Vector']), None)
        if uv is not None:
            links.new(uv, glow_tex.inputs['Vector'])
        emission = nodes.new('ShaderNodeEmission')
        links.new(glow_tex.outputs['Color'], emission.inputs['Color'])
        strength = nodes.new('ShaderNodeMath')
        strength.operation = 'MULTIPLY'
        strength.inputs[1].default_value = 0.8
        links.new(glow_tex.outputs['Alpha'], strength.inputs[0])
        links.new(strength.outputs['Value'], emission.inputs['Strength'])
        output = next(n for n in nodes if n.type == 'OUTPUT_MATERIAL')
        surface = next(l.from_socket for l in links if l.to_socket == output.inputs['Surface'])
        add = nodes.new('ShaderNodeAddShader')
        links.new(surface, add.inputs[0])
        links.new(emission.outputs['Emission'], add.inputs[1])
        links.new(add.outputs['Shader'], output.inputs['Surface'])
        binding['glow'] = {'texture': glow_path.name, 'sha256': sha(glow_path)}
    return binding


def retrace_perimeter(tier, root, curls, transform):
    curve_obj = next(o for o in bpy.context.scene.objects if o.type == 'CURVE' and o.name.startswith(tier) and 'perimeter' in o.name)
    spline = curve_obj.data.splines[0]
    assert spline.type == 'POLY' and spline.use_cyclic_u
    points = [transform @ p for p in outline(root, curls)]
    curve_obj.data.splines.remove(spline)
    new = curve_obj.data.splines.new('POLY')
    new.points.add(len(points) - 1)
    for point, co in zip(new.points, points):
        point.co = (co.x, co.y, co.z + 0.12, 1.0)
    new.use_cyclic_u = True
    return len(points)


def render():
    assert bpy.app.background and '-noaudio' in sys.argv
    source = ROOT / 'sources/approved-stack.blend'
    bpy.ops.wm.open_mainfile(filepath=str(source))
    s = bpy.context.scene

    report_tiers = []
    for tier in TIERS:
        cuboids = [o for o in s.objects if o.get('tier') == tier and 'source-cuboid' in o.name]
        assert len(cuboids) == 5
        base_z = min((o.matrix_world @ v.co).z for o in cuboids for v in o.data.vertices)
        material = cuboids[0].data.materials[0]
        for o in cuboids:
            bpy.data.objects.remove(o)

        tassels, curls = FEATURES[tier]
        root = body_layer(tassels, curls)
        setup_anim(root, POSE_TIME[tier], POSE_SPEED)
        faces = []
        collect(root, Matrix.Identity(4), faces)
        transform = to_scene(base_z)
        faces = [[(transform @ pos, uv) for pos, uv in face] for face in faces]
        build_mesh(tier, faces, material)
        binding = bind_texture(tier, material)
        binding['perimeter_points'] = retrace_perimeter(tier, root, curls, transform)
        binding['faces'] = len(faces)
        binding['pose'] = {'speed': POSE_SPEED, 'time': POSE_TIME[tier]}
        report_tiers.append(binding)

    particles = [o for o in s.objects if '-enchantment-' in o.name]
    assert len(particles) == 60
    subjects = [o for o in s.objects if o.type == 'MESH' and (o.get('tier') or '-enchantment-' in o.name)]
    points = [world_to_camera_view(s, s.camera, o.matrix_world @ v.co) for o in subjects for v in o.data.vertices]
    bounds = [min(p.x for p in points), min(p.y for p in points), max(p.x for p in points), max(p.y for p in points)]
    assert min(bounds) > .01 and max(bounds) < .99, bounds

    s.render.engine = 'CYCLES'
    s.cycles.device = 'CPU'
    s.cycles.samples = int(os.environ.get('ICON_SAMPLES', '96'))
    s.render.compositor_device = 'CPU'
    s.render.threads_mode = 'FIXED'
    s.render.threads = int(os.environ.get('ICON_RENDER_THREADS', '2'))
    s.render.resolution_x = s.render.resolution_y = 1024
    s.render.resolution_percentage = 100
    s.render.image_settings.file_format = 'PNG'
    s.render.image_settings.color_mode = 'RGBA'
    s.render.filepath = str(ROOT / (NAME + '.png'))
    bpy.context.preferences.filepaths.save_version = 0
    s['reproducible_script'] = 'render-segmented.py'
    s['presentation_note'] = 'Mod carpet models and textures as shipped. Floating vanilla glyphs, bloom and luminous perimeter are the approved preview styling, not in-game behaviour.'
    for text in list(bpy.data.texts):
        bpy.data.texts.remove(text)
    bpy.data.texts.load(str(ROOT / 'render-segmented.py')).use_fake_user = True
    for im in list(bpy.data.images):
        if im.source == 'FILE' and not im.users:
            bpy.data.images.remove(im)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT / (NAME + '.blend')))

    started = time.perf_counter()
    bpy.ops.render.render(write_still=True)
    seconds = time.perf_counter() - started
    decoded = bpy.data.images.load(str(ROOT / (NAME + '.png')), check_existing=False)
    assert tuple(decoded.size) == (1024, 1024)
    pixels = np.asarray(decoded.pixels[:], dtype=np.float32).reshape((1024, 1024, 4))
    assert np.isfinite(pixels).all() and pixels[:, :, :3].std() > .08 and pixels[:, :, 3].min() > .99

    report = {'name': NAME, 'engine': 'CYCLES', 'device': 'CPU', 'threads': s.render.threads, 'samples': s.cycles.samples,
              'blender': bpy.app.version_string, 'resolution': [1024, 1024], 'seconds': seconds,
              'approved_scene_sha256': sha(source), 'tiers': report_tiers, 'bounds': bounds,
              'visible_particles': len(particles), 'png_sha256': sha(ROOT / (NAME + '.png')),
              'blend_sha256': sha(ROOT / (NAME + '.blend'))}
    (ROOT / (NAME + '-metadata.json')).write_text(json.dumps(report, indent=2) + '\n')
    print('RENDER_COMPLETE ' + json.dumps({'name': NAME, 'seconds': seconds}), flush=True)


if __name__ == '__main__':
    render()
