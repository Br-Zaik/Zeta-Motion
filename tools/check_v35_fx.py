"""Shader regression for Zeta Motion v35; uses same EGL/GLES3 test harness as check_render."""
import runpy
from pathlib import Path
import numpy as np
from PIL import Image
r = runpy.run_path(str(Path(__file__).with_name('check_render.py')))
uni = r['uniform']; integer = r['integer']; render = r['render']
baseline = r['baseline'].astype('int16')
# model-effect four floats: strength, scale, cycles, zone; masks set to third custom layer (left half).
for fx in ('Smoke', 'Blast', 'Blood', 'Slash', 'Reveal'):
    integer('uMask'+fx,3)
for fx in ('Smoke','Blast','Blood','Slash'):
    uni('uMotion'+fx,0.0,1.0,0.32,0.48)
configs = {
    'Smoke': (.85,2.5,1.0,0.0),
    'Blast': (.95,.9,1.0,0.0),
    'Blood': (.9,1.2,1.0,0.0),
    'Slash': (.9,1.1,1.0,0.0),
    'Reveal': (.9,1.4,1.0,0.0)
}
styles = {'Smoke': 'uSmokeControl', 'Blast': 'uBlastMore', 'Blood': 'uBloodStyle',
          'Slash': 'uSlashStyle','Reveal': 'uRevealStyle'}
counts={'Smoke':5,'Blast':4,'Blood':3,'Slash':4,'Reveal':3}
for fx,val in configs.items():
    uni('uFx'+fx,*val)
    imgs=[]
    for style in range(counts[fx]):
        if fx=='Smoke': styleVals=(style,1.25,.6,1)
        elif fx=='Blast': styleVals=(style,.7,.8,0)
        elif fx=='Blood': styleVals=(style,0,0,0)
        elif fx=='Slash':styleVals=(style,.45,0,0)
        else:styleVals=(style,0,0,0)
        uni(styles[fx],*styleVals)
        img=render(.34).astype('int16');diff=np.abs(img-baseline)[:,:,:3]
        print(fx,style,'active',round(float(diff[:,:110].mean()),4),'leak',int(diff[:,146:].max()),flush=True)
        assert diff[:,:110].mean()>.008, (fx,style,'invisible')
        assert diff[:,146:].max()==0,(fx,style,'leaks outside painted mask')
        assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,(fx,style,'not seamless')
        imgs.append(img)
        Image.fromarray(img.astype('uint8')).save(f'checks/v35_{fx.lower()}{style}.png')
    for style in range(1,len(imgs)):
        d=np.abs(imgs[0]-imgs[style]).mean()
        assert d>.001, (fx,style,'variant not visibly distinct')
    uni('uFx'+fx,0,val[1],val[2],val[3])
    assert np.array_equal(render().astype('int16'),baseline),(fx,'did not return to neutral')
print('v35: new families/variants, masking, loop endpoints and neutral reset PASSED')
