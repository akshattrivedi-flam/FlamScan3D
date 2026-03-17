import bpy
import sys
from pathlib import Path

# Usage: blender --background --python ply_to_glb.py -- <input.ply> <output.glb>

args = sys.argv
if "--" not in args:
    raise SystemExit("Expected -- <input.ply> <output.glb>")

input_path = Path(args[args.index("--") + 1])
output_path = Path(args[args.index("--") + 2])

bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)

bpy.ops.import_mesh.ply(filepath=str(input_path))

# Optional: apply basic smoothing/decimation here.

bpy.ops.export_scene.gltf(filepath=str(output_path), export_format='GLB')
