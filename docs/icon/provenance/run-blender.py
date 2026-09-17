#!/usr/bin/env python3
"""Offline Blender job: shared three-core / weight-20 cap, two render threads."""
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parent
CGROUP = Path('/sys/fs/cgroup/user.slice/user-1000.slice/user@1000.service/app.slice/render-blender.service')
quota, period = map(int,(CGROUP/'cpu.max').read_text().split())
assert quota/period <= 3 and int((CGROUP/'cpu.weight').read_text()) == 20
(CGROUP/'cgroup.procs').write_text(str(os.getpid()))
for key in ['DISPLAY','WAYLAND_DISPLAY','PULSE_SERVER']:
    os.environ.pop(key,None)
os.environ.update(CUDA_VISIBLE_DEVICES='',HIP_VISIBLE_DEVICES='',OMP_NUM_THREADS='2',OPENBLAS_NUM_THREADS='1',BLENDER_USER_CONFIG=str(ROOT/'blender-config'),PYTHONDONTWRITEBYTECODE='1')
script = ROOT/sys.argv[1]
command = ['nix','shell','nixpkgs#blender','--command','blender','--background','-noaudio','--threads','2','--python-exit-code','1','--python',str(script),'--',*sys.argv[2:]]
log = ROOT/'logs'/(script.stem+'.log')
report = {'command':command,'cwd':str(ROOT),'cpu.max':(CGROUP/'cpu.max').read_text().strip(),'cpu.weight':(CGROUP/'cpu.weight').read_text().strip(),'cgroup_membership':Path('/proc/self/cgroup').read_text().strip(),'pid':os.getpid(),'display':None,'audio':None,'gpu':False}
(ROOT/'evidence'/('resources-'+script.stem+'.json')).write_text(json.dumps(report,indent=2)+'\n')
with log.open('w') as out:
    result = subprocess.run(command,stdout=out,stderr=subprocess.STDOUT)
report['returncode'] = result.returncode
report['stopped'] = True
(ROOT/'evidence'/('resources-'+script.stem+'.json')).write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'returncode':result.returncode,'log':str(log),'stopped':True}),flush=True)
sys.exit(result.returncode)
