from pathlib import Path
import runpy
module = runpy.run_path(str(Path(__file__).with_name('render-native.py')),run_name='render_native')
module['render']('variant-b')
