import ctypes as C, os, re, json, time
from pathlib import Path
import numpy as np
from PIL import Image
os.environ['EGL_PLATFORM']='surfaceless'
os.environ['LIBGL_ALWAYS_SOFTWARE']='1'
E=C.CDLL('libEGL.so.1'); I=C.c_int; U=C.c_uint; P=C.c_void_p; F=C.c_float
E.eglGetProcAddress.restype=P;E.eglGetProcAddress.argtypes=[C.c_char_p]
def egl(name,rest,args):
 f=getattr(E,name);f.restype=rest;f.argtypes=args;return f
d=egl('eglGetDisplay',P,[P])(None);major=I();minor=I()
assert egl('eglInitialize',U,[P,C.POINTER(I),C.POINTER(I)])(d,C.byref(major),C.byref(minor))
assert egl('eglBindAPI',U,[U])(0x30A0)
attrs=(I*13)(0x3024,8,0x3023,8,0x3022,8,0x3033,1,0x3040,0x40,0x3021,8,0x3038)
config=P();n=I();assert egl('eglChooseConfig',U,[P,C.POINTER(I),C.POINTER(P),I,C.POINTER(I)])(d,attrs,C.byref(config),1,C.byref(n)) and n.value
W=256;H=256
surface=egl('eglCreatePbufferSurface',P,[P,P,C.POINTER(I)])(d,config,(I*5)(0x3057,W,0x3056,H,0x3038))
ctx=egl('eglCreateContext',P,[P,P,P,C.POINTER(I)])(d,config,None,(I*3)(0x3098,3,0x3038))
assert ctx and egl('eglMakeCurrent',U,[P,P,P,P])(d,surface,surface,ctx)
def gl(name,rest,args):return C.CFUNCTYPE(rest,*args)(E.eglGetProcAddress(name.encode()))
def fn(name,rest,*args):globals()[name]=gl(name,rest,list(args))
for name,ret,args in [
('glCreateShader',U,[U]),('glShaderSource',None,[U,I,C.POINTER(C.c_char_p),P]),('glCompileShader',None,[U]),('glGetShaderiv',None,[U,U,C.POINTER(I)]),('glGetShaderInfoLog',None,[U,I,P,P]),('glCreateProgram',U,[]),('glAttachShader',None,[U,U]),('glLinkProgram',None,[U]),('glGetProgramiv',None,[U,U,C.POINTER(I)]),('glGetProgramInfoLog',None,[U,I,P,P]),('glUseProgram',None,[U]),('glGetUniformLocation',I,[U,C.c_char_p]),('glUniform1f',None,[I,F]),('glUniform1i',None,[I,I]),('glUniform2f',None,[I,F,F]),('glUniform3f',None,[I,F,F,F]),('glUniform4f',None,[I,F,F,F,F]),('glGenTextures',None,[I,C.POINTER(U)]),('glBindTexture',None,[U,U]),('glActiveTexture',None,[U]),('glTexParameteri',None,[U,U,I]),('glTexImage2D',None,[U,I,I,I,I,I,U,U,P]),('glTexImage3D',None,[U,I,I,I,I,I,I,U,U,P]),('glGenVertexArrays',None,[I,C.POINTER(U)]),('glBindVertexArray',None,[U]),('glGenBuffers',None,[I,C.POINTER(U)]),('glBindBuffer',None,[U,U]),('glBufferData',None,[U,C.c_ssize_t,P,U]),('glEnableVertexAttribArray',None,[U]),('glVertexAttribPointer',None,[U,I,U,U,I,P]),('glViewport',None,[I,I,I,I]),('glDrawArrays',None,[U,I,I]),('glReadPixels',None,[I,I,I,I,U,U,P]),('glGetError',U,[]),('glFinish',None,[])]:fn(name,ret,*args)
Path('checks').mkdir(exist_ok=True)
root=Path(__file__).resolve().parents[1]/'app/src/main/assets/shaders';program=glCreateProgram()
for kind,file in [(0x8B31,'fondo.vert'),(0x8B30,'fondo.frag')]:
 shader=glCreateShader(kind);src=C.c_char_p((root/file).read_bytes());glShaderSource(shader,1,C.byref(src),None);glCompileShader(shader);ok=I();glGetShaderiv(shader,0x8B81,C.byref(ok));log=C.create_string_buffer(20000);glGetShaderInfoLog(shader,20000,None,log)
 print(file,'compile',ok.value,log.value.decode(),flush=True);assert ok.value;glAttachShader(program,shader)
glLinkProgram(program);ok=I();glGetProgramiv(program,0x8B82,C.byref(ok));log=C.create_string_buffer(20000);glGetProgramInfoLog(program,20000,None,log);print('link',ok.value,log.value.decode(),flush=True);assert ok.value;glUseProgram(program)
def uniform(name,*values):
 loc=glGetUniformLocation(program,name.encode());globals()['glUniform'+str(len(values))+'f'](loc,*values)
def integer(name,value):glUniform1i(glGetUniformLocation(program,name.encode()),value)
ids=(U*6)();glGenTextures(6,ids)
y,x=np.mgrid[0:H,0:W];img=np.zeros((H,W,4),dtype=np.uint8);img[:,:,0]=(x/W*210+20).astype('uint8');img[:,:,1]=(y/H*210+20).astype('uint8');img[:,:,2]=np.where(((x//16+y//16)%2)==0,60,190);img[:,:,3]=255
mask=np.where(x<W//2,255,0).astype('uint8');zeros=np.zeros((H,W),dtype='uint8');atlas=np.zeros((12,H,W),dtype='uint8');atlas[3]=mask
for i in range(6):
 target=0x8C1A if i==5 else 0x0DE1
 glActiveTexture(0x84C0+i);glBindTexture(target,ids[i]);glTexParameteri(target,0x2801,0x2601);glTexParameteri(target,0x2800,0x2601);glTexParameteri(target,0x2802,0x812F);glTexParameteri(target,0x2803,0x812F)
 if i==5:glTexImage3D(target,0,0x8229,W,H,12,0,0x1903,0x1401,atlas.ctypes.data)
 elif i==0:glTexImage2D(target,0,0x8058,W,H,0,0x1908,0x1401,img.ctypes.data)
 else:glTexImage2D(target,0,0x8229,W,H,0,0x1903,0x1401,zeros.ctypes.data)
 integer(['uImage','uFlow','uFreeze','uZone1','uZone2','uMasks'][i],i)
vao=U();vbo=U();glGenVertexArrays(1,C.byref(vao));glBindVertexArray(vao);glGenBuffers(1,C.byref(vbo));glBindBuffer(0x8892,vbo)
quad=np.array([-1,-1,1,-1,-1,1,1,1],dtype='float32');glBufferData(0x8892,quad.nbytes,quad.ctypes.data,0x88E4);glEnableVertexAttribArray(0);glVertexAttribPointer(0,2,0x1406,0,8,None);glViewport(0,0,W,H)
uniform('uAspect',1);uniform('uViewScale',1,1);uniform('uTexSize',W,H);uniform('uFlipY',1)
for name in re.findall(r'uniform int (uMask\w+);',(root/'fondo.frag').read_text()):integer(name,-2)
def render(t=.25):
 uniform('uT',t);glDrawArrays(5,0,4);a=np.zeros((H,W,4),dtype='uint8');glReadPixels(0,0,W,H,0x1908,0x1401,a.ctypes.data);assert glGetError()==0;return a
baseline=render();Image.fromarray(baseline).save('checks/base.png');results=[]
cases=[('sky','uStudioSky',(0.8,1,1,0)),('water','uStudioWater',(0.8,1,0,0)),('dispersion','uStudioDispersion',(0.8,1,0,0))]
uniform('uDispersionMore',28,.8,0,0)
for name,key,val in cases:
 uniform(key,*val);integer('uMask'+name.title(),3);a=render();change=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3];assert change[:,:120].mean()>.2,(name,'invisible');assert change[:,140:].max()==0,(name,'leaks',change[:,140:].max())
 start=render(0);end=render(1);assert np.abs(start.astype('int16')-end.astype('int16')).max()<=1,(name,'loop')
 Image.fromarray(a).save('checks/'+name+'.png');results.append({'effect':name,'masked_change':float(change[:,:120].mean()),'outside_max':int(change[:,140:].max()),'loop':True});uniform(key,0,1,0,0)
for mode in range(6):
 uniform('uCam',.3,.3,1);uniform('uCameraMode',mode,0,0,0);a=render(.2);assert np.abs(a.astype('int16')-baseline.astype('int16')).mean()>.1
 assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1
uniform('uCam',0,0,1)
for typ in range(6):
 uniform('uElementPose[0]',.5,.5,.7,0);uniform('uElementStyle[0]',1,typ,1,0);a=render();assert np.abs(a.astype('int16')-baseline.astype('int16')).mean()>.02,(typ,'invisible element')
 assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1
 Image.fromarray(a).save('checks/element'+str(typ)+'.png')
uniform('uElementStyle[0]',0,0,1,0);assert np.array_equal(render(),baseline)
print(json.dumps({'render_tests':'passed','cases':results,'camera_modes':6,'element_types':6}),flush=True)
# Painting must show the original, independent of the running camera/effect stack.
uniform('uShowMask',1);integer('uActiveMask',3);plain_paint=render()
uniform('uCam',.4,.4,1);uniform('uStudioSky',1,1,2,0);uniform('uStudioDispersion',1,1,0,0)
assert np.array_equal(render(),plain_paint),'painting moved with active effects'
assert np.array_equal(plain_paint[:,140:],baseline[:,140:]),'painting altered the unselected area'
print('paint mode isolation: passed',flush=True)

# v13: every exposed color control must change pixels, and neutral must be lossless.
uniform('uShowMask',0);uniform('uCam',0,0,1);uniform('uStudioSky',0,1,0,0);uniform('uStudioDispersion',0,1,0,0)
color_cases={'brightness':('uColorA',0,.2),'contrast':('uColorA',1,.5),'saturation':('uColorA',2,-1),
'exposure':('uColorA',3,1),'temperature':('uColorB',0,.8),'hue':('uColorB',1,1.2),
'shadows':('uColorB',2,.8),'highlights':('uColorB',3,-.8),'sharpen':('uColorC',0,1),
'glow':('uColorC',1,1),'vignette':('uColorC',2,1),'grain':('uColorC',3,1),
'fade':('uColorD',0,1),'prism':('uColorD',1,1)}
for name,(key,index,value) in color_cases.items():
    v=[0,0,0,0];v[index]=value;uniform(key,*v);a=render()
    change=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3].mean()
    assert change>.01,(name,'does not affect image')
    if name=='saturation':assert np.max(np.abs(a[:,:,0].astype('int16')-a[:,:,1].astype('int16')))<=1
    uniform(key,0,0,0,0);assert np.array_equal(render(),baseline),(name,'neutral changed')
print('14 color controls and neutral reset: passed',flush=True)
# Use the existing drift displacement to exercise the same flow composition as arrows.
uniform('uDrift',.2,.13);uniform('uFlow3',0,1,3);integer('uMaskDrift',-2)
frames=[]
for mode in range(3):
    uniform('uLoopControl',mode,.35,.17,0)
    a=render(0);b=render(1)
    assert np.abs(a.astype('int16')-b.astype('int16')).max()<=1,(mode,'loop seam')
    mid=render(.2);assert np.abs(a.astype('int16')-mid.astype('int16')).mean()>1,(mode,'no animation')
    frames.append(mid)
assert all(np.abs(frames[i].astype('int16')-frames[j].astype('int16')).mean()>1 for i,j in [(0,1),(0,2),(1,2)])
print('3 distinct motion modes, phase, 3 cycles and seamless endpoints: passed',flush=True)

# Regression: extreme displacement must not extend source edge texels into stripes.
for mode in range(3):
    for dx,dy in [(2,0),(-2,0),(0,2),(0,-2),(2,-2)]:
        uniform('uDrift',dx,dy);uniform('uFlow3',0,1,1);uniform('uLoopControl',mode,1,.79,0)
        for t in [0,.2,.5,.8]:
            a=render(t)
            if dy==0:
                assert np.abs(a[:,0].astype('int16')-baseline[:,0].astype('int16')).max()<=1,(mode,'left border moved')
                assert np.abs(a[:,-1].astype('int16')-baseline[:,-1].astype('int16')).max()<=1,(mode,'right border moved')
                assert len(np.unique(a[128,:32,0]))>=8,(mode,'left stripe')
            if dx==0:
                assert np.abs(a[0].astype('int16')-baseline[0].astype('int16')).max()<=1,(mode,'top border moved')
                assert np.abs(a[-1].astype('int16')-baseline[-1].astype('int16')).max()<=1,(mode,'bottom border moved')
print('edge stretch regression: 3 modes, 5 displacement directions, 4 phases: passed',flush=True)
uniform('uDrift',0,0);uniform('uFlow3',0,1,1);integer('uMaskZonemotion',3)
for variant in range(3):
    uniform('uZoneMotion',.035,2,variant,0)
    a=render(.13);change=np.abs(a.astype('int16')-baseline.astype('int16'))
    assert change[:,:110,:3].mean()>.1,(variant,'no zone movement')
    assert change[:,145:,:3].max()==0,(variant,'outside painted area')
    assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,(variant,'zone loop seam')
# A fully protected image must remain still for all zone movements.
glActiveTexture(0x84C0+2);glBindTexture(0x0DE1,ids[2]);white=np.full((H,W),255,dtype='uint8')
glTexImage2D(0x0DE1,0,0x8229,W,H,0,0x1903,0x1401,white.ctypes.data)
assert np.array_equal(render(.13),baseline),'protected area moves'
print('3 painted-zone motions and freeze protection: passed',flush=True)

# v21: parallax 2.5D and soft mesh deformation must be periodic and respect painted zones.
uniform('uZoneMotion',0,1,0,0);uniform('uCam',0,0,1);uniform('uParallax',0,0,1,.65);uniform('uMesh',0,8,2,0)
# Restore freeze to empty.
glActiveTexture(0x84C0+2);glBindTexture(0x0DE1,ids[2]);glTexImage2D(0x0DE1,0,0x8229,W,H,0,0x1903,0x1401,zeros.ctypes.data)
# Background (blue zone) on left third, foreground (green zone) in middle third.
zback=np.where(x<W//3,255,0).astype('uint8');zfront=np.where((x>=W//3)&(x<2*W//3),255,0).astype('uint8')
for unit,data in [(3,zback),(4,zfront)]:
    glActiveTexture(0x84C0+unit);glBindTexture(0x0DE1,ids[unit]);glTexImage2D(0x0DE1,0,0x8229,W,H,0,0x1903,0x1401,data.ctypes.data)
uniform('uParallax',.045,0,1,.65)
para=render(.25);chg=np.abs(para.astype('int16')-baseline.astype('int16'))[:,:,:3]
assert chg[:,:W*2//3].mean()>.1,'parallax invisible'
assert chg[:,W*3//4:].max()==0,'parallax leaks outside painted depth zones'
assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,'parallax loop seam'
uniform('uParallax',0,0,1,.65)
# Custom mask layer 3 still covers the left half of the atlas.
integer('uMaskMesh',3);uniform('uMeshDirection',0,0,0,0);uniform('uMesh',.012,8,2,0)
mesh=render(.13);chg=np.abs(mesh.astype('int16')-baseline.astype('int16'))[:,:,:3]
assert chg[:,:110].mean()>.1,'mesh deformation invisible'
assert chg[:,145:].max()==0,'mesh leaks outside mask'
assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,'mesh loop seam'
print('v21 parallax 2.5D + soft mesh + seamless endpoints: passed',flush=True)

# v22: movimiento natural para árboles/banderas/pasto/cortinas/cabello/ropa.
# Debe respetar la máscara, cerrar el loop y el anclaje debe reducir el movimiento cerca del borde fijo.
uniform('uMesh',0,8,2,0); integer('uMaskNatural',3)
for typ in range(6):
    anchor = [3,1,3,4,4,4][typ]
    uniform('uNatural',.018,0,2,typ)
    uniform('uNaturalMore',.75,.7,.45,anchor)
    a=render(.17);chg=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3]
    assert chg[:,:110].mean()>.08,(typ,'natural invisible')
    assert chg[:,145:].max()==0,(typ,'natural leaks outside mask')
    assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,(typ,'natural loop seam')
# Bandera fijada a la izquierda: el borde sujeto se mueve menos que el extremo libre.
uniform('uNatural',.022,0,2,1);uniform('uNaturalMore',.9,.45,.5,1)
flag=render(.19);chg=np.abs(flag.astype('int16')-baseline.astype('int16'))[:,:,:3]
left=float(chg[:,2:18].mean());free=float(chg[:,90:116].mean())
assert free > left + .05,('flag anchor ineffective',left,free)
uniform('uNatural',0,0,2,0)
print('v22 natural motion: 6 materials + mask + anchor + seamless endpoints: passed',flush=True)

# v24: hojas estacionales deben renderizar cuatro paletas distintas sin cambiar geometría ni loop.
uniform('uFxLeaves',1.25,1.15,3,0);uniform('uMotionLeaves',1,0,.5,.5);integer('uMaskLeaves',-2)
leaf_frames=[]
for style in range(4):
    uniform('uLeavesStyle',style,style,0,0)
    a=render(.31)
    change=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3]
    assert change.mean()>.01,(style,'leaf palette invisible')
    leaf_frames.append(a)
for i,j in [(0,1),(1,2),(2,3),(0,3)]:
    assert np.abs(leaf_frames[i].astype('int16')-leaf_frames[j].astype('int16')).mean()>.01,(i,j,'leaf palettes identical')
assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,'leaf loop seam'
uniform('uFxLeaves',0,1,2,0)
print('v24 seasonal leaves: green/dry/autumn/christmas palettes + seamless loop: passed',flush=True)

# v30: localized anime/manga/manhwa effects must respect their own mask and close the loop.
new_cases = {
    'Bokeh': ('uFxBokeh',(1.0,3.0,2.0,0.0)),
    'Sparkle': ('uFxSparkle',(1.0,1.0,2.0,0.0)),
    'Lightleak': ('uFxLightleak',(0.8,2.0,1.0,0.0)),
    'Waterfall': ('uFxWaterfall',(1.0,8.0,3.0,0.0)),
    'Reflect': ('uFxReflect',(1.0,1.0,2.0,0.0)),
    'Blackaura': ('uFxBlackaura',(0.8,2.5,2.0,0.0)),
    'Inkdrip': ('uFxInkdrip',(0.9,1.0,2.0,0.0)),
    'Dramatic': ('uFxDramatic',(0.8,0.7,1.0,0.0)),
}
for suffix,(key,val) in new_cases.items():
    integer('uMask'+suffix,3); uniform(key,*val)
    a=render(.27);chg=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3]
    assert chg[:,:110].mean()>.01,(suffix,'localized effect invisible')
    assert chg[:,145:].max()==0,(suffix,'localized effect leaks outside mask')
    assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,(suffix,'loop seam')
    uniform(key,0,val[1],val[2],val[3])
# Shore must also remain inside the painted zone and be seamless.
uniform('uShore',.8,3,2,0);uniform('uShoreMore',.55,1,0,0);uniform('uMotionShore',0,1,.5,.5);integer('uMaskShore',3)
a=render(.22);chg=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3]
assert chg[:,:110].mean()>.01,'shore localized effect invisible'
assert chg[:,145:].max()==0,'shore leaks outside painted water zone'
assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,'shore loop seam'
uniform('uShore',0,3,2,0)
print('v30 localized effects: 8 new effects + natural shore mask + seamless loops: passed',flush=True)

# v33: estilos reales sobre motores existentes (no efectos duplicados).
# Rayos: cinco diseños con colores/geométricas distintas, zona limitada y loop continuo.
integer('uMaskRays',3)
uniform('uMotionRays',0,1,.3,0)
uniform('uRays',.9,.3,2,0)
rays_variants=[]
for style in range(5):
    uniform('uLightStyles',style,0,0,0)
    a=render(.31)
    diff=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3]
    assert diff[:,:110].mean()>.015,('rays',style,'invisible')
    assert diff[:,145:].max()==0,('rays',style,'leak')
    assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,('rays',style,'seam')
    rays_variants.append(a)
for style in range(1,5):
    assert np.abs(rays_variants[style].astype('int16')-rays_variants[0].astype('int16')).mean()>.007,('rays',style,'same as default')
uniform('uRays',0,.3,2,0)

# Reflexión de luz en agua: cuatro paletas/geométricas con la misma máscara.
integer('uMaskSunpath',3); uniform('uSunpath',1.1,.3,.26,3)
sun_variants=[]
for style in range(4):
    uniform('uLightStyles',0,style,0,0)
    a=render(.31)
    diff=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3]
    assert diff[:,:110].mean()>.007,('sunpath',style,'invisible')
    assert diff[:,145:].max()==0,('sunpath',style,'leak')
    assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,('sunpath',style,'seam')
    sun_variants.append(a)
for style in range(1,4):
    assert np.abs(sun_variants[style].astype('int16')-sun_variants[0].astype('int16')).mean()>.006,('sunpath',style,'same as default')
uniform('uSunpath',0,.3,.26,3);uniform('uLightStyles',0,0,0,0)

# Tres estilos 2.5D existentes con transformaciones diferentes, solo en zonas de profundidad.
uniform('uParallax',.060,0,1,.65)
para_variants=[]
for style in range(3):
    uniform('uParallaxStyle',style,0,0,0)
    a=render(.23)
    diff=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3]
    assert diff[:,:160].mean()>.04,('parallax',style,'invisible')
    assert diff[:,W*3//4:].max()==0,('parallax',style,'leak')
    assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,('parallax',style,'seam')
    para_variants.append(a)
for style in range(1,3):
    assert np.abs(para_variants[style].astype('int16')-para_variants[0].astype('int16')).mean()>.012,('parallax',style,'same as default')
uniform('uParallax',0,0,1,.65); uniform('uParallaxStyle',0,0,0,0)

# Órbita 3D simulada desde una sola imagen; no implica geometría reconstruida.
uniform('uCam',.2,.8,1);uniform('uCameraMode',5,.6,0,0)
a=render(.23)
assert np.abs(a.astype('int16')-baseline.astype('int16')).mean()>.1,('3D sim','invisible')
assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,('3D sim','seam')
uniform('uCam',0,0,1); uniform('uCameraMode',0,0,0,0)
print('v33 5 ray styles + 4 sun highlights + 3 depth styles + 3D simulated orbit: passed',flush=True)

# Variantes anime de aura y llamas; no se crean IDs de efectos nuevos.
integer('uMaskEnergy',3);uniform('uEnergy',1.2,16,2,0);uniform('uMotionEnergy',0,1,.28,.52)
energy_palettes=[]
for i in range(4):
    uniform('uLightStyles',0,0,i,0)
    a=render(.28)
    delta=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3]
    assert delta[:,:112].mean()>.02,('energy',i,'invisible')
    assert delta[:,145:].max()==0,('energy',i,'mask leak')
    assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,('energy',i,'loop seam')
    energy_palettes.append(a)
for i in range(1,4):
    assert np.abs(energy_palettes[i].astype('int16')-energy_palettes[0].astype('int16')).mean()>.008,('energy',i,'same palette')
uniform('uEnergy',0,16,2,0)

integer('uMaskFlame',3);uniform('uFxFlame',1.0,1.0,2.0,0)
flame_palettes=[]
for i in range(4):
    uniform('uLightStyles',0,0,0,i)
    a=render(.19)
    delta=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3]
    assert delta[:,:112].mean()>.02,('flame',i,'invisible')
    assert delta[:,145:].max()==0,('flame',i,'mask leak')
    assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,('flame',i,'loop seam')
    flame_palettes.append(a)
for i in range(1,4):
    assert np.abs(flame_palettes[i].astype('int16')-flame_palettes[0].astype('int16')).mean()>.008,('flame',i,'same palette')
uniform('uFxFlame',0,1,2,0);uniform('uLightStyles',0,0,0,0)

# Cabello y tela: peso y elasticidad deben cambiar el movimiento sin escapar máscara.
integer('uMaskNatural',3)
for material in (4,5):
    uniform('uNatural',.02,0,2,material); uniform('uNaturalMore',.8,.6,.3,4)
    dynamics=[]
    for weight,spring in ((.05,.1),(.5,.5),(.95,.9)):
        uniform('uNaturalDynamics',weight,spring,0,0)
        a=render(.23)
        delta=np.abs(a.astype('int16')-baseline.astype('int16'))[:,:,:3]
        assert delta[:,:110].mean()>.04,(material,'invisible')
        assert delta[:,145:].max()==0,(material,'leak')
        assert np.abs(render(0).astype('int16')-render(1).astype('int16')).max()<=1,(material,'seam')
        dynamics.append(a)
    assert np.abs(dynamics[0].astype('int16')-dynamics[2].astype('int16')).mean()>.035,(material,'unresponsive dynamics')
    uniform('uNatural',0,0,2,0)
print('v33 4 aura colors + 4 flame colors + hair/cloth inertia and elasticity: passed',flush=True)
