#version 300 es
// Fondo Zeta - shader principal.
// Todo el movimiento depende de uT (0..1 = un loop). Cada efecto usa un número
// ENTERO de ciclos por loop, así el último cuadro empalma con el primero.
precision highp float;
precision highp int;

in vec2 vScreen;
out vec4 fragColor;

uniform sampler2D uImage;
uniform sampler2D uFlow;     // RG: vector de flujo (unidades de imagen por ciclo)
uniform highp sampler2DArray uMasks;
uniform int uActiveMask;


uniform vec2 uViewOffset;
uniform vec2 uViewScale;
uniform float uFlipY;
uniform float uRotate;
uniform float uT;
// Parámetros de efectos en un Uniform Buffer (std140): evita superar el límite
// de vectores uniformes por defecto de GPUs Adreno/Mali económicas (≈224-256).
layout(std140) uniform ZetaParams {
    // ---- v36: forma/comportamiento, color propio y zonas de los efectos mejorados
    vec4 uNaturalBox;    // rectángulo pintado (x0, y0, x1, y1) para fijar la raíz real
    vec4 uFlameBox;      // rectángulo de las llamas: base del fuego
    vec4 uSmokeShape;    // forma, turbulencia, suavizado
    vec4 uFlameShape;    // forma, turbulencia, suavizado, altura
    vec4 uBlastShape;    // forma, destello, onda de choque
    vec4 uBloodShape;    // forma, dirección (rad), apertura
    vec4 uSlashShape;    // forma, grosor
    vec4 uEnergyShape;   // forma, radio, turbulencia
    vec4 uEyesShape;     // forma, halo
    vec4 uFogShape;      // forma, turbulencia
    vec4 uRevealShape;   // forma, humo del borde
    vec4 uTintSmoke;     // rgb + w=1 si el usuario eligió color
    vec4 uTintFlame;
    vec4 uTintBlast;
    vec4 uTintBlood;
    vec4 uTintSlash;
    vec4 uTintEnergy;
    vec4 uTintEyes;
    vec4 uTintFog;
    vec4 uTintBlackaura;
    vec4 uTintReveal;
    float uAspect;       // ancho / alto de la imagen
    float uMono;
    float uPixelSnap;
    vec3 uFlow3;   // encendido, distancia, ciclos
    vec2 uDrift;   // deriva en zona 1 (uv por ciclo)
    vec3 uRipple;  // fuerza, frecuencia, ciclos
    vec3 uGlint;   // intensidad, densidad, ciclos
    vec3 uSway;    // fuerza, frecuencia, ciclos
    vec3 uHeat;    // fuerza, frecuencia, ciclos
    vec4 uRain;    // intensidad, ángulo (rad), ciclos, solo zona 1
    vec4 uSnow;    // intensidad, tamaño, ciclos, solo zona 1
    vec4 uSakura;  // intensidad, tamaño, ciclos, solo zona 1
    vec4 uStars;   // intensidad, densidad, ciclos, solo zona 1
    vec4 uFlies;   // intensidad, densidad, ciclos, solo zona 1
    vec4 uFog;     // intensidad, escala, ciclos, solo zona 1
    vec4 uRays;    // intensidad, origen x, ciclos, solo zona 1
    vec4 uEnergy;  // intensidad, rayos, ciclos, solo zona 1
    vec4 uSpeed;   // intensidad, ángulo, ciclos, solo zona 1
    vec4 uStudioSky;
    vec4 uStudioWater;
    vec4 uStudioDispersion;
    vec4 uDispersionMore;
    vec4 uCameraMode;
    vec4 uLoopControl;
    vec4 uZoneMotion;
    int uMaskZonemotion;
    vec4 uColorA, uColorB, uColorC, uColorD;
    vec4 uElementPose[4];
    vec4 uElementStyle[4];
    int uMaskSky;
    int uMaskWater;
    int uMaskDispersion;
    int uMaskCam;
    vec3 uCam;     // zoom, recorrido, ciclos
    vec4 uParallax;       // cantidad, dirección rad, ciclos, diferencia de profundidad
    vec4 uParallaxStyle;  // 0=planos, 1=órbita suave, 2=dolly
    vec4 uMesh;           // fuerza, tamaño, ciclos, variante
    vec4 uMeshDirection;  // dirección rad en x
    int uMaskMesh;
    vec4 uNatural;        // fuerza, dirección rad, ciclos, material
    vec4 uNaturalMore;    // flexibilidad, variación, ráfagas, borde fijo
    vec4 uNaturalDynamics; // peso, elasticidad (0..1)
    int uMaskNatural;
    vec4 uMotionRain;
    vec4 uMotionSnow;
    vec4 uMotionSakura;
    vec4 uMotionFog;
    vec4 uMotionRays;
    vec4 uMotionEnergy;
    vec4 uMotionSpeed;
    vec4 uMotionAsh;
    vec4 uMotionLeaves;
    vec4 uMotionEmbers;
    vec4 uMotionDust;
    vec4 uMotionBubbles;
    vec4 uMotionElectric;
    vec4 uMotionFocus;
    vec4 uMotionShock;
    vec4 uFxAsh;
    vec4 uFxLeaves;
    vec4 uLeavesStyle; // x=principal, y=copia: 0 verde, 1 seca, 2 otoño, 3 navidad
    vec4 uFxEmbers;
    vec4 uFxDust;
    vec4 uFxBubbles;
    vec4 uFxElectric;
    vec4 uFxFocus;
    vec4 uFxShock;
    vec4 uFxFlash;
    vec4 uFxTone;
    vec4 uFxGrain;
    vec4 uFxInk;
    vec4 uFxAshCopy;
    int uMaskAshCopy;
    vec4 uFxLeavesCopy;
    int uMaskLeavesCopy;
    vec4 uFxEmbersCopy;
    int uMaskEmbersCopy;
    vec4 uFxDustCopy;
    int uMaskDustCopy;
    vec4 uFxBubblesCopy;
    int uMaskBubblesCopy;
    vec4 uFxElectricCopy;
    int uMaskElectricCopy;
    vec4 uFxFocusCopy;
    int uMaskFocusCopy;
    vec4 uFxShockCopy;
    int uMaskShockCopy;
    vec4 uFxFlashCopy;
    int uMaskFlashCopy;
    vec4 uFxToneCopy;
    int uMaskToneCopy;
    vec4 uFxGrainCopy;
    int uMaskGrainCopy;
    vec4 uFxInkCopy;
    int uMaskInkCopy;
    vec4 uMotionAshCopy;
    vec4 uMotionLeavesCopy;
    vec4 uMotionEmbersCopy;
    vec4 uMotionDustCopy;
    vec4 uMotionBubblesCopy;
    vec4 uMotionElectricCopy;
    vec4 uMotionFocusCopy;
    vec4 uMotionShockCopy;
    vec4 uFxComet;
    int uMaskComet;
    vec4 uFxRunes;
    int uMaskRunes;
    vec4 uFxHatch;
    int uMaskHatch;
    vec4 uFxPulse;
    int uMaskPulse;
    vec4 uFxSmoke;
    vec4 uSmokeControl;   // color, densidad, deriva, delante de PNG
    int uMaskSmoke;
    vec4 uFxBlast;
    vec4 uBlastMore;     // estilo, humo, chispas, reservado
    int uMaskBlast;
    vec4 uMotionBlast;
    vec4 uFxBlood;
    vec4 uBloodStyle;
    int uMaskBlood;
    vec4 uMotionBlood;
    vec4 uFxSlash;
    vec4 uSlashStyle;
    int uMaskSlash;
    vec4 uMotionSlash;
    vec4 uFxReveal;
    vec4 uRevealStyle;
    int uMaskReveal;
    vec4 uFxShards;
    int uMaskShards;
    vec4 uFxFlame;
    vec4 uFxEyes;
    vec4 uFxBlink;
    vec4 uFxCloth;
    int uMaskFlame;
    int uMaskEyes;
    int uMaskBlink;
    int uMaskCloth;
    vec4 uMotionComet;
    vec4 uMotionRunes;
    vec4 uMotionPulse;
    vec4 uMotionSmoke;
    vec4 uMotionShards;
    vec4 uFxBokeh;
    int uMaskBokeh;
    vec4 uFxSparkle;
    int uMaskSparkle;
    vec4 uFxLightleak;
    int uMaskLightleak;
    vec4 uFxWaterfall;
    int uMaskWaterfall;
    vec4 uFxReflect;
    int uMaskReflect;
    vec4 uFxBlackaura;
    int uMaskBlackaura;
    vec4 uFxInkdrip;
    int uMaskInkdrip;
    vec4 uFxDramatic;
    int uMaskDramatic;
    int uMaskFlow;
    int uMaskDrift;
    int uMaskRipple;
    int uMaskGlint;
    int uMaskSway;
    int uMaskHeat;
    int uMaskRain;
    int uMaskSnow;
    int uMaskSakura;
    int uMaskStars;
    int uMaskFlies;
    int uMaskFog;
    int uMaskRays;
    int uMaskEnergy;
    int uMaskSpeed;
    int uMaskAsh;
    int uMaskLeaves;
    int uMaskEmbers;
    int uMaskDust;
    int uMaskBubbles;
    int uMaskElectric;
    int uMaskFocus;
    int uMaskShock;
    int uMaskFlash;
    int uMaskTone;
    int uMaskGrain;
    int uMaskInk;
    vec4 uLoopInfo;      // duración del bucle en segundos
    vec4 uBolt;          // intensidad, x, rayos por bucle, destello
    int uMaskBolt;
    vec4 uGust;          // fuerza, momento, duración, velocidad
    vec4 uGustMore;      // hojas y polvo
    vec4 uMotionGust;
    int uMaskGust;
    vec4 uTornado;       // fuerza, radio, giros, tipo
    vec4 uMotionTornado;
    int uMaskTornado;
    vec4 uAppear;        // opacidad, momento, transición, tipo
    vec4 uAppearPose;    // x, y, alto, giro
    float uCharAspect;
    vec4 uClouds3;       // opacidad, tamaño, velocidad, luz
    int uMaskClouds3;
    vec4 uShore;         // intensidad, olas, velocidad, tinte
    vec4 uShoreMore;     // espuma, estilo, reservado, reservado
    vec4 uMotionShore;
    int uMaskShore;
    vec4 uSunpath;       // brillo, x, ancho, titileo
    vec4 uLightStyles;   // rayos, sol, aura y llamas: estilos dentro de los efectos originales
    int uMaskSunpath;
    vec4 uImpact;        // zoom, temblor, momento, golpes
    vec4 uImpactMore;    // destello
};
uniform vec2 uTexSize;
uniform float uShowMask;
uniform float uOverlayOnly;


// Dirección (x,y) y origen normalizado (z,w), compartidos por UI/vista previa/export.


// Segunda instancia independiente de cada efecto anime/manga.


// Nuevos efectos procedurales, independientes de servicios externos.

// v30: fondos localizados anime / manga / manhwa.

const float TAU = 6.28318530718;

// ---- v16: escenas anime con eventos ----
uniform sampler2D uCharacter;


// Capas base del atlas de máscaras: 0 = proteger/personaje, 1 = zona azul, 2 = zona verde.
float maskLayer(vec2 uv, int layer) { return texture(uMasks, vec3(uv, float(layer))).r; }

// slot: -2 toda la imagen, -1 zona heredada, 0..11 máscara propia.
// slot + 128: el efecto queda DETRÁS del personaje (capa Proteger), sin otro shader.
float maskValue(vec2 uv, int slot, float fallback, float enabled) {
    float behind = 1.0;
    if (slot >= 64) { behind = 1.0 - maskLayer(uv, 0); slot -= 128; }
    if (slot == -2) return behind;
    return behind * (slot >= 0 ? texture(uMasks, vec3(uv, float(slot))).r : mix(1.0, fallback, enabled));
}

// Hash sin seno (Dave Hoskins, licencia MIT)
float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

vec3 leafPalette(float style, vec2 p) {
    if (uMono > 0.5) return vec3(0.88);
    float n = hash12(floor(p * 7.0) + vec2(19.0, 73.0));
    if (style < 0.5) {
        return mix(vec3(0.19, 0.58, 0.24), vec3(0.48, 0.90, 0.34), n);
    }
    if (style < 1.5) {
        return mix(vec3(0.48, 0.28, 0.10), vec3(0.82, 0.64, 0.24), n);
    }
    if (style < 2.5) {
        return mix(vec3(0.76, 0.18, 0.07), vec3(0.96, 0.56, 0.08), n);
    }
    return n > 0.82 ? vec3(0.82, 0.08, 0.10) : mix(vec3(0.05, 0.28, 0.10), vec3(0.12, 0.55, 0.18), n);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int k = 0; k < 4; k++) {
        v += a * vnoise(p);
        p = p * 2.03 + vec2(17.1, 9.2);
        a *= 0.5;
    }
    return v;
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

// ---------- v36: utilidades compartidas por los efectos ----------
// Todas son funciones de la posición y de fases ENTERAS del bucle, de modo que el
// cuadro N (que no se exporta) coincide con el 0 y la vista previa = exportación.

// Ruido celular (Worley F1) barato: 3x3 celdas.
float worley(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    float d = 8.0;
    for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
        vec2 g = vec2(float(x), float(y));
        vec2 o = hash22(i + g);
        d = min(d, length(g + o - f));
    }
    return d;
}

// fbm de 3 octavas para efectos grandes: mismo aspecto con un 25% menos de coste.
float fbm3(vec2 p) {
    float v = 0.5 * vnoise(p);
    p = p * 2.03 + vec2(17.1, 9.2);
    v += 0.25 * vnoise(p);
    p = p * 2.03 + vec2(17.1, 9.2);
    v += 0.125 * vnoise(p);
    return v / 0.875;
}

// Dos capas desplazadas media fase (flow map de Valve) con corrección de contraste:
// la mezcla lineal de dos ruidos baja la varianza a mitad de ciclo; se compensa con
// 1/sqrt((1-a)^2+a^2) para que el humo no "respire" ni se aclare en el centro del bucle.
float contrastFix(float n, float a) {
    return 0.5 + (n - 0.5) / sqrt((1.0 - a) * (1.0 - a) + a * a);
}

// Color personalizado (HEX) si el usuario lo eligió; si no, el de la paleta.
vec3 tintOr(vec3 base, vec4 tint) {
    vec3 c = tint.w > 0.5 ? tint.rgb : base;
    return uMono > 0.5 ? vec3(luma(c)) : c;
}

// Sombreado anime de 2 tonos. El gradiente de pantalla se pasa al espacio de la
// imagen con la jacobiana de P, así la luz no cambia con el giro del vídeo vertical,
// el volteo del PNG ni la resolución (solo se usa la dirección).
float celLight(float n, vec2 P) {
    vec2 gs = vec2(dFdx(n), dFdy(n));
    vec2 px = dFdx(P), py = dFdy(P);
    float det = px.x * py.y - px.y * py.x;
    if (abs(det) < 1e-12) return 0.6;
    // J^T * g = gs  ->  g = inverse(J^T) * gs
    vec2 g = vec2(py.y * gs.x - px.y * gs.y, -py.x * gs.x + px.x * gs.y) / det;
    float l = dot(normalize(g + vec2(1e-7)), normalize(vec2(-0.55, -0.8)));
    return smoothstep(-0.25, 0.35, l);
}

// 1 cerca de 0, 0 desde e en adelante (suave)
float falloff(float d, float e) {
    return 1.0 - smoothstep(0.0, e, d);
}

vec3 sourcePixel(vec2 q) { return texture(uImage,clamp(q,vec2(0.0),vec2(1.0))).rgb; }
vec3 sampleImg(vec2 q) {
    q=clamp(q,vec2(0.0),vec2(1.0));
    if(uPixelSnap>0.5)q=(floor(q*uTexSize)+0.5)/uTexSize;
    vec3 c=sourcePixel(q);
    if(uColorD.y>0.0){
        vec2 shift=vec2(uColorD.y*0.009,0.0);
        c.r=sourcePixel(q+shift).r;c.b=sourcePixel(q-shift).b;
    }
    if(uColorC.x>0.0){
        vec2 stepUV=1.0/max(uTexSize,vec2(1.0));
        vec3 neighbors=(sourcePixel(q+vec2(stepUV.x,0.0))+sourcePixel(q-vec2(stepUV.x,0.0))+
            sourcePixel(q+vec2(0.0,stepUV.y))+sourcePixel(q-vec2(0.0,stepUV.y)))*0.25;
        c+=uColorC.x*1.6*(c-neighbors);
    }
    if(uColorC.y>0.0){
        vec2 r=vec2(0.0045);
        vec3 glow=(sourcePixel(q+r)+sourcePixel(q-r)+sourcePixel(q+vec2(r.x,-r.y))+sourcePixel(q+vec2(-r.x,r.y)))*0.25;
        c+=glow*smoothstep(0.45,0.9,dot(glow,vec3(0.2126,0.7152,0.0722)))*uColorC.y*0.45;
    }
    return c;
}
// Keep the sampling footprint inside the source. Pin the outermost pixel centers,
// taper the displacement near edges and saturate it smoothly before the limit.
vec2 safeWarp(vec2 uv, vec2 displacement) {
    vec2 halfPixel=0.5/max(uTexSize,vec2(1.0));
    vec2 center=clamp(uv,halfPixel,1.0-halfPixel);
    vec2 room=max(vec2(0.0),min(center-halfPixel,1.0-halfPixel-center));
    vec2 envelope=smoothstep(vec2(0.0),vec2(0.055),room);
    vec2 delta=displacement*envelope;
    delta=delta/(vec2(1.0)+abs(delta)/max(room*0.25,vec2(0.000001)));
    return center+delta;
}
vec3 warpSample(vec2 uv,vec2 displacement){return sampleImg(safeWarp(uv,displacement));}

vec3 gradeColor(vec3 c,vec2 uv){
    c=c*exp2(uColorA.w)+uColorA.x;
    float lum=dot(c,vec3(0.2126,0.7152,0.0722));
    c+=uColorB.z*0.3*(1.0-smoothstep(0.0,0.6,lum));
    c+=uColorB.w*0.3*smoothstep(0.35,1.0,lum);
    c=(c-0.5)*(1.0+uColorA.y)+0.5;
    c+=uColorB.x*vec3(0.12,0.01,-0.12);
    if(abs(uColorB.y)>0.00001){
        vec3 axis=vec3(0.577350269);
        c=c*cos(uColorB.y)+cross(axis,c)*sin(uColorB.y)+axis*dot(axis,c)*(1.0-cos(uColorB.y));
    }
    lum=dot(c,vec3(0.2126,0.7152,0.0722));
    c=mix(vec3(lum),c,1.0+uColorA.z);
    c=mix(c,c*0.75+0.15,uColorD.x);
    c*=1.0-uColorC.z*smoothstep(0.2,0.7,length(uv-0.5))*0.85;
    if(uColorC.w>0.0)c+=(hash12(floor(uv*uTexSize))-0.5)*uColorC.w*0.16;
    return c;
}

// Rota un campo de caída vertical a la dirección elegida en la UI.
// (0,1) conserva el movimiento hacia abajo del shader original.
vec2 oriented(vec2 p, vec4 m) {
    vec2 d = normalize(m.xy + vec2(0.000001));
    vec2 base = p - m.zw * vec2(uAspect, 1.0);
    return vec2(d.y * base.x - d.x * base.y,
                d.x * base.x + d.y * base.y);
}
float emitArea(vec2 p, vec4 m) {
    vec2 d = normalize(m.xy + vec2(0.000001));
    vec2 from = m.zw * vec2(uAspect, 1.0);
    // El punto central conserva cobertura total; un emisor en un borde entra desde ese lado.
    if (distance(m.zw, vec2(0.5)) < 0.001) return 1.0;
    float edge = dot(p - from, d);
    return smoothstep(-0.04, 0.18, edge);
}

// ---------- Lluvia ----------
float rainLayer(vec2 P, float scale, float cyc, float ang, float seed) {
    float ca = cos(ang);
    float sa = sin(ang);
    vec2 R = vec2(ca * P.x + sa * P.y, -sa * P.x + ca * P.y);
    const float N = 6.0;
    vec2 g = vec2(R.x * scale, R.y * scale * 0.25);
    g.y -= uT * cyc * N;
    vec2 id = floor(g);
    vec2 f = fract(g);
    vec2 idp = vec2(id.x, mod(id.y, N)) + seed;
    vec2 h = hash22(idp);
    float on = step(0.45, hash12(idp + 7.13));
    float x = 0.2 + 0.6 * h.x;
    // Variación por gota (no otra capa FX): profundidad, grosor y longitud no uniformes.
    float len = 0.22 + 0.58 * h.y;
    float y0 = (1.0 - len) * hash12(idp + 3.7);
    float width = mix(0.028, 0.075, hash12(idp + 12.0));
    float w = falloff(abs(f.x - x), width);
    float v = smoothstep(y0, y0 + 0.07, f.y) * (1.0 - smoothstep(y0 + len - 0.10, y0 + len, f.y));
    return w * v * on;
}

// ---------- Nieve ----------
float snowLayer(vec2 P, float scale, float cyc, float size, float seed) {
    const float N = 5.0;
    vec2 g = P * scale;
    g.y -= uT * cyc * N;
    vec2 id = floor(g);
    vec2 f = fract(g);
    vec2 idp = vec2(id.x, mod(id.y, N)) + seed;
    vec2 h = hash22(idp);
    float on = step(0.5, hash12(idp + 1.7));
    float k = 1.0 + floor(h.y * 2.0);
    vec2 c = vec2(0.5 + 0.28 * sin(TAU * (uT * k + h.x)), 0.25 + 0.5 * h.y);
    // Diferentes tamaños y transparencias generan sensación de planos cercanos/lejanos.
    float r = clamp(size * (0.043 + 0.12 * h.x), 0.018, 0.20);
    float d = length(f - c);
    float depthAlpha = mix(0.55, 1.0, h.x);
    return (1.0 - smoothstep(r * 0.35, r, d)) * on * depthAlpha;
}

// ---------- Pétalos de sakura ----------
vec4 sakuraLayer(vec2 P, float scale, float cyc, float size, float seed) {
    const float N = 6.0;
    vec2 g = P * scale;
    g.y -= uT * cyc * N;
    g.x += uT * cyc * N;
    vec2 id = floor(g);
    vec2 f = fract(g);
    vec2 idp = mod(id, N) + seed;
    vec2 h = hash22(idp);
    float on = step(0.55, hash12(idp + 4.1));
    vec2 c = vec2(0.5 + 0.2 * sin(TAU * (uT + h.x)), 0.5 + 0.15 * cos(TAU * (uT + h.y)));
    float a = TAU * (uT * (1.0 + floor(h.x * 2.0)) + h.y);
    vec2 d = f - c;
    float ca = cos(a);
    float sa = sin(a);
    d = vec2(ca * d.x - sa * d.y, sa * d.x + ca * d.y);
    float r = clamp(size * (0.08 + 0.06 * h.y), 0.03, 0.25);
    d.x /= r;
    d.y /= r * 0.55;
    float e = length(d);
    float petal = (1.0 - smoothstep(0.7, 1.0, e)) * on;
    vec3 col = mix(vec3(1.0, 0.72, 0.82), vec3(1.0, 0.93, 0.96), clamp(0.5 - d.x * 0.5, 0.0, 1.0));
    return vec4(col, petal);
}

// ---------- Estrellas ----------
vec3 starsLayer(vec2 P, float dens, float cyc) {
    vec2 g = P * dens;
    vec2 id = floor(g);
    vec2 f = fract(g);
    vec2 h = hash22(id + 11.3);
    float present = step(0.62, hash12(id + 5.9));
    vec2 c = 0.2 + 0.6 * h;
    float d = length(f - c);
    float big = step(0.93, hash12(id + 2.2));
    float k = cyc * (1.0 + floor(h.x * 3.0));
    float tw = 0.45 + 0.55 * (0.5 + 0.5 * sin(TAU * (uT * k + h.y)));
    float core = falloff(d, 0.06 + 0.05 * big);
    float glow = falloff(d, 0.3) * 0.25;
    vec2 a = abs(f - c);
    float spikes = big * (falloff(a.x, 0.015) * falloff(a.y, 0.35) + falloff(a.y, 0.015) * falloff(a.x, 0.35));
    float s = (core + glow + spikes * 0.7) * tw * present;
    vec3 col = mix(vec3(0.8, 0.88, 1.0), vec3(1.0, 0.95, 0.8), h.x);
    return col * s;
}

// ---------- Luciérnagas ----------
vec3 fliesLayer(vec2 P, float dens, float cyc) {
    vec2 g = P * dens;
    vec2 id = floor(g);
    vec2 f = fract(g);
    vec2 h = hash22(id + 21.7);
    float present = step(0.55, hash12(id + 8.8));
    float kx = cyc * (1.0 + floor(h.x * 2.0));
    float ky = cyc * (1.0 + floor(h.y * 2.0));
    vec2 c = vec2(0.5 + 0.3 * sin(TAU * (uT * kx + h.y)), 0.5 + 0.3 * sin(TAU * (uT * ky + h.x) + 1.3));
    float d = length(f - c);
    float blink = smoothstep(-0.2, 1.0, sin(TAU * (uT * cyc * 2.0 + h.x)));
    float core = falloff(d, 0.035);
    float glow = falloff(d, 0.18);
    return vec3(0.85, 1.0, 0.45) * (core + glow * glow * 0.6) * blink * present;
}

// ---------- Niebla (fundido de dos fases = loop perfecto) ----------
float fogField(vec2 P, float scale, float cyc) {
    float p1 = fract(uT * cyc);
    float p2 = fract(p1 + 0.5);
    float w1 = 1.0 - abs(2.0 * p1 - 1.0);
    vec2 dir = vec2(0.9, 0.12);
    float a = fbm(P * scale + dir * (p1 - 0.5));
    float b = fbm(P * scale + dir * (p2 - 0.5));
    return mix(b, a, w1);
}

// ---------- Rayos de luz ----------
float raysField(vec2 P, float ox, float cyc) {
    vec2 o = uMotionRays.zw * vec2(uAspect, 1.0);
    vec2 v = P - o;
    float ang = atan(v.x, v.y) - atan(uMotionRays.y, uMotionRays.x);
    float ph = TAU * uT * cyc;
    float r = sin(ang * 17.0 + ph) + sin(ang * 29.0 - ph * 2.0 + 1.3) + sin(ang * 9.0 + ph + 2.1);
    r = clamp(r / 3.0 * 0.5 + 0.5, 0.0, 1.0);
    r = r * r * r;
    float fall = exp(-length(v) * 1.3);
    return r * fall;
}

float energyField(vec2 P, float rays, float cyc) {
    vec2 q = P - uMotionEnergy.zw * vec2(uAspect, 1.0);
    float r = length(q);
    float a = atan(q.y, q.x);
    float pulse = 0.55 + 0.45 * sin(TAU * uT * cyc);
    float spokes = pow(0.5 + 0.5 * sin(a * rays + sin(uT * TAU * cyc) * 2.0), 10.0);
    return spokes * (1.0 - smoothstep(0.08, 0.58, r)) * pulse;
}

float speedField(vec2 P, float angle, float cyc) {
    float a = radians(angle);
    float ca = cos(a);
    float sa = sin(a);
    vec2 q = vec2(ca * P.x + sa * P.y, -sa * P.x + ca * P.y);
    vec2 g = q * vec2(9.0, 34.0);
    g.y -= uT * cyc * 8.0;
    float line = smoothstep(0.47, 0.49, fract(g.y));
    float fade = smoothstep(0.0, 0.18, q.y) * (1.0 - smoothstep(0.55, 1.0, q.y));
    return line * fade * (0.35 + 0.65 * hash12(floor(g)));
}

// Procedurales anime/manga: todos usan tiempo periódico para bucles limpios.
float particleDot(vec2 p, float density, float cycles, float size, float seed) {
    const float REP = 6.0;
    vec2 g = p * density;
    g.y -= uT * cycles * REP;
    vec2 id = floor(g);
    vec2 f = fract(g);
    vec2 h = hash22(vec2(id.x, mod(id.y, REP)) + seed);
    vec2 c = vec2(0.2 + 0.6 * h.x, 0.2 + 0.6 * h.y);
    float r = clamp(size * 0.09, 0.018, 0.28);
    return (1.0 - smoothstep(r * 0.4, r, length(f - c))) * step(0.42, hash12(vec2(id.x, mod(id.y, REP)) + seed));
}
float electricArc(vec2 p, vec4 motion, float branches, float cycles) {
    vec2 q = p - motion.zw * vec2(uAspect, 1.0);
    float a = atan(q.y, q.x);
    float r = length(q);
    float segments = floor(a * branches / TAU);
    float wobble = 0.07 * sin(a * 23.0 + segments + TAU * uT * cycles);
    float target = 0.27 + 0.12 * sin(segments * 7.4 + TAU * uT * cycles) + wobble;
    float bolt = 1.0 - smoothstep(0.005, 0.024, abs(r - target));
    return bolt * (1.0 - smoothstep(0.2, 0.8, r));
}
float focusLines(vec2 p, vec4 motion, float density, float cycles) {
    vec2 q = p - motion.zw * vec2(uAspect, 1.0);
    float a = atan(q.y, q.x);
    float r = length(q);
    float index = floor(a / TAU * density);
    float phase = fract(a / TAU * density);
    float spike = (1.0 - smoothstep(0.03, 0.14, min(phase, 1.0-phase)));
    float endR = 0.16 + 0.25 * hash12(vec2(index, 27.1));
    return spike * smoothstep(endR, endR + 0.12, r) *
        (0.7 + 0.3 * sin(TAU * uT * cycles));
}


vec3 studioSky(vec2 uv) {
    float ph=TAU*uT*uStudioSky.y;
    vec2 drift=vec2(cos(ph),sin(ph))*0.28;
    float clouds=smoothstep(0.45,0.78,fbm(uv*vec2(4.0*uAspect,6.0)+drift));
    vec3 top=vec3(0.13,0.44,0.83), bottom=vec3(0.79,0.9,1.0);
    if(uStudioSky.z>0.5){top=vec3(0.32,0.22,0.61);bottom=vec3(1.0,0.59,0.38);}
    if(uStudioSky.z>1.5){top=vec3(0.025,0.035,0.13);bottom=vec3(0.16,0.19,0.37);}
    if(uStudioSky.z>2.5){top=vec3(0.1,0.12,0.2);bottom=vec3(0.4,0.44,0.52);}
    vec3 c=mix(top,bottom,smoothstep(0.0,1.0,uv.y));
    c=mix(c,uStudioSky.z>1.5?bottom:vec3(1.0,0.93,0.92),clouds*0.8);
    if(uStudioSky.z>1.5 && uStudioSky.z<2.5){
        vec2 grid=uv*vec2(uAspect,1.0)*90.0;
        vec2 h=hash22(floor(grid));
        float star=(1.0-smoothstep(0.01,0.09,length(fract(grid)-h)))*step(0.96,h.x);
        c+=star*(0.6+0.4*sin(ph+h.y*TAU));
    }
    if(uStudioSky.z>2.5)c+=pow(max(0.0,sin(ph)),40.0)*0.25;
    return c;
}
vec3 studioDisperse(vec3 base, vec2 uv) {
    float strength=uStudioDispersion.x;
    float phase=0.5-0.5*cos(TAU*uT*uStudioDispersion.y);
    if(strength<0.001 || phase<0.0001) return base;
    float count=max(12.0,uDispersionMore.x);
    vec2 scale=vec2(uAspect,1.0)*count;
    vec2 g=uv*scale;
    vec2 cell=floor(g);
    vec2 direction=vec2(cos(uStudioDispersion.w),sin(uStudioDispersion.w));
    float distance=phase*uDispersionMore.y*1.6;
    float zone=maskValue(uv,uMaskDispersion,0.0,1.0);
    // Local color extrapolation, deliberately no claim of semantic/AI reconstruction.
    vec3 fill=vec3(0.0);float weight=0.0;
    for(int k=0;k<8;k++){
        float a=float(k)*TAU/8.0;
        vec2 q=uv+vec2(cos(a),sin(a))/scale*3.0;
        float w=1.0-maskValue(clamp(q,0.0,1.0),uMaskDispersion,0.0,1.0);
        fill+=sampleImg(q)*w;weight+=w;
    }
    fill=weight>0.01?fill/weight:sampleImg(uv+direction/scale*3.0);
    vec3 c=mix(base,fill,zone*phase*strength);
    // Search around the reverse displacement: only nine candidates per pixel.
    vec2 sourceCell=floor(g-direction*distance);
    for(int y=-1;y<=1;y++) for(int x=-1;x<=1;x++) {
        vec2 id=sourceCell+vec2(float(x),float(y));
        vec2 h=hash22(id+31.2);
        vec2 center=id+0.5+direction*distance+(h-0.5)*phase*0.5;
        vec2 local=g-center;
        float radius=mix(0.49,0.19,phase*strength);
        float shape=1.0-smoothstep(radius-0.04,radius,max(abs(local.x),abs(local.y)));
        if(uStudioDispersion.z>0.5)shape=1.0-smoothstep(radius-0.05,radius,length(local*vec2(1.0,1.5)));
        if(uStudioDispersion.z>1.5)shape=1.0-smoothstep(0.03,0.11,length(local));
        vec2 origin=(id+0.5)/scale;
        float valid=step(0.0,origin.x)*step(origin.x,1.0)*step(0.0,origin.y)*step(origin.y,1.0);
        float area=maskValue(clamp(origin,0.0,1.0),uMaskDispersion,0.0,1.0);
        c=mix(c,sampleImg(origin+local/scale),shape*area*valid*phase*strength);
    }
    return c;
}
vec3 studioElements(vec3 col,vec2 uv) {
    for(int i=0;i<4;i++) {
        vec4 style=uElementStyle[i]; if(style.x<0.001)continue;
        vec4 pose=uElementPose[i];
        vec2 q=(uv-pose.xy)*vec2(uAspect,1.0)/pose.z;
        q=mat2(cos(pose.w),-sin(pose.w),sin(pose.w),cos(pose.w))*q;
        if(max(abs(q.x),abs(q.y))>0.8)continue;
        float phase=TAU*uT*style.z;
        float opacity=0.0;vec3 tint=vec3(0.92,0.95,1.0);
        if(style.y<1.5 || style.y>4.5){
            float n=fbm(q*5.0+vec2(sin(phase),cos(phase))*0.35);
            float edge=1.0-smoothstep(0.15,0.58,length(q*vec2(0.8,1.3)));
            opacity=smoothstep(0.3,0.7,n)*edge;
            if(style.y>0.5 && style.y<1.5)tint=vec3(0.42,0.45,0.52);
            if(style.y>4.5)opacity*=0.55;
        } else if(style.y<3.5) {
            for(int j=0;j<8;j++){
                vec2 h=hash22(vec2(float(j),float(i)+41.0));
                vec2 center=(h-0.5)*0.8+vec2(sin(phase+h.x*TAU),cos(phase+h.y*TAU))*0.1;
                vec2 p=q-center;
                if(style.y<2.5){ opacity+=1.0-smoothstep(0.02,0.065,length(p*vec2(0.7,1.4)));tint=vec3(1.0,0.66,0.81); }
                else {opacity+=(1.0-smoothstep(0.0,0.08,abs(p.x)+abs(p.y)))*(0.6+0.4*sin(phase+h.x*TAU));tint=vec3(1.0,0.94,0.65);}
            }
        } else {
            float r=length(q);float ring=0.29+0.05*sin(phase);
            opacity=exp(-abs(r-ring)*65.0)*(0.8+0.2*sin(atan(q.y,q.x)*3.0+phase));
            tint=vec3(0.48,0.74,1.0);
        }
        col=mix(col,tint,clamp(opacity*style.x,0.0,1.0));
    }
    return col;
}


// ================= v16: funciones de escenas =================
// Ventana de un evento que empieza en 'start' y dura 'dur' (fracción del bucle).
float windowEnv(float start, float dur) {
    if (dur >= 0.999) return 1.0;
    float p = fract(uT - start);
    float a = smoothstep(0.0, min(0.08, dur * 0.3), p);
    float b = 1.0 - smoothstep(dur * 0.6, dur, p);
    return a * b;
}

float impactEnv() {
    // uLoopInfo.z >= 0: tiempo local del bloque de la nueva línea de tiempo.
    if (uLoopInfo.z >= 0.0) {
        float tSec = uLoopInfo.z;
        return exp(-tSec * 6.0) * smoothstep(0.0, 0.02, tSec);
    }
    float n = max(1.0, uImpact.w);
    float p = fract(uT * n - uImpact.z);
    float tSec = p * uLoopInfo.x / n;
    return exp(-tSec * 6.0) * smoothstep(0.0, 0.02, tSec);
}

float boltPath(float y, float seed) {
    return (vnoise(vec2(y * 7.0, seed)) - 0.5) * 0.16 + (vnoise(vec2(y * 31.0, seed + 3.3)) - 0.5) * 0.05;
}

vec3 boltLayer(vec3 col, vec2 P, float mask) {
    float n = max(1.0, uBolt.z);
    float k;
    float tSec;
    float seed;
    // uLoopInfo.y >= 0: el rayo empieza exactamente al inicio del bloque elegido.
    if (uLoopInfo.y >= 0.0) {
        k = uLoopInfo.w;
        tSec = uLoopInfo.y;
        seed = mod(k, 997.0) * 0.173 + 3.0;
    } else {
        k = floor(uT * n - 0.3);
        float p = fract(uT * n - 0.3);
        tSec = p * uLoopInfo.x / n;
        seed = mod(k + n, n) * 17.0 + 3.0;
    }
    float vis = (1.0 - smoothstep(0.05, 0.35, tSec)) * (0.55 + 0.45 * step(0.5, fract(tSec * 18.0)));
    if (vis <= 0.001) return col;
    float x0 = uBolt.y * uAspect + (hash12(vec2(seed, 1.0)) - 0.5) * 0.15;
    float len = 0.55 + 0.35 * hash12(vec2(seed, 2.0));
    float grow = clamp(tSec / 0.06, 0.0, 1.0);
    float ymax = len * grow;
    float dm = abs(P.x - (x0 + boltPath(P.y, seed)));
    float along = 1.0 - step(ymax, P.y);
    float core = (1.0 - smoothstep(0.0015, 0.004, dm)) * along;
    float glow = exp(-dm * 55.0) * along * 0.6;
    float by0 = len * (0.25 + 0.3 * hash12(vec2(seed, 4.0)));
    float bdir = hash12(vec2(seed, 5.0)) > 0.5 ? 1.0 : -1.0;
    float bx = x0 + boltPath(by0, seed) + bdir * (P.y - by0) * 0.45 + boltPath(P.y, seed + 9.0) * 0.6;
    float bd = abs(P.x - bx);
    float balong = step(by0, P.y) * (1.0 - step(min(ymax, by0 + len * 0.35), P.y));
    core += (1.0 - smoothstep(0.001, 0.003, bd)) * balong * 0.8;
    glow += exp(-bd * 70.0) * balong * 0.35;
    vec3 bc = uMono > 0.5 ? vec3(1.0) : vec3(0.85, 0.9, 1.0);
    col = mix(col, bc, clamp((core + glow) * vis * uBolt.x * mask, 0.0, 1.0));
    float flash = (1.0 - smoothstep(0.0, 0.18, tSec)) * uBolt.w;
    col += vec3(0.75, 0.8, 1.0) * flash * 0.55 * uBolt.x;
    return col;
}

vec3 gustLayer(vec3 col, vec2 P, float env, float mask) {
    if (env <= 0.001) return col;
    vec2 dir = normalize(uMotionGust.xy + vec2(1e-5, 0.0));
    vec2 nrm = vec2(-dir.y, dir.x);
    float a = dot(P, dir);
    float c = dot(P, nrm);
    float lines = 0.0;
    for (int k = 0; k < 3; k++) {
        float fk = float(k);
        float rows = 5.0 + fk * 4.0;
        float cc = c * rows + 0.35 * sin(a * (2.2 + fk) + fk * 1.7 + TAU * uT);
        float row = floor(cc);
        float fr = fract(cc) - 0.5;
        float h = hash12(vec2(row, fk * 13.1));
        float speed = uGust.w * (2.0 + fk);
        float along = fract(a * 0.55 + h - uT * speed);
        float seg = smoothstep(0.0, 0.08, along) * (1.0 - smoothstep(0.25 + 0.2 * h, 0.45 + 0.2 * h, along));
        float width = 0.035 + 0.02 * h;
        lines += (1.0 - smoothstep(0.0, width, abs(fr))) * seg * step(0.45, h) * (0.9 - fk * 0.2);
    }
    vec3 wc = uMono > 0.5 ? vec3(1.0) : vec3(0.92, 0.97, 1.0);
    col = mix(col, wc, clamp(lines * 0.75, 0.0, 1.0) * env * mask);
    if (uGustMore.x > 0.0) {
        vec2 q = vec2(a, c) * 7.0;
        q.x -= uT * uGust.w * 6.0;
        vec2 id = floor(q);
        vec2 f = fract(q);
        vec2 idp = vec2(mod(id.x, 6.0), id.y) + 5.3;
        vec2 h = hash22(idp);
        float present = step(0.55, hash12(idp + 2.1));
        vec2 cpos = vec2(0.5, 0.3 + 0.4 * h.y + 0.18 * sin(TAU * (uT * 2.0 + h.x)));
        vec2 d = f - cpos;
        float ang = TAU * (uT * 2.0 + h.y);
        d = vec2(cos(ang) * d.x - sin(ang) * d.y, sin(ang) * d.x + cos(ang) * d.y);
        float leaf = 1.0 - smoothstep(0.7, 1.0, length(d / vec2(0.16, 0.07)));
        vec3 lc = h.x > 0.5 ? vec3(0.42, 0.72, 0.30) : vec3(0.78, 0.66, 0.42);
        if (uMono > 0.5) lc = vec3(luma(lc));
        col = mix(col, lc, leaf * present * env * uGustMore.x * mask);
    }
    return col;
}

vec3 tornadoLayer(vec3 col, vec2 P, float mask) {
    vec2 c = uMotionTornado.zw * vec2(uAspect, 1.0);
    vec2 rel = P - c;
    float R = max(0.05, uTornado.y);
    if (uTornado.w < 0.5) {
        float r = length(rel);
        float ang = atan(rel.y, rel.x);
        float rn = r / R;
        float rot = ang + TAU * uT * uTornado.z * 2.0 + 2.5 * log(max(rn, 0.05));
        float arms = pow(0.5 + 0.5 * sin(rot * 3.0), 6.0);
        float band = 1.0 - smoothstep(0.3, 1.1, rn);
        float ripple = 0.7 + 0.3 * sin(rn * 20.0 - TAU * uT * uTornado.z * 3.0);
        vec3 dc = uMono > 0.5 ? vec3(0.9) : vec3(0.88, 0.83, 0.74);
        col = mix(col, dc, clamp(arms * band * ripple * 0.45 * uTornado.x * mask, 0.0, 1.0));
    } else {
        float y = c.y - P.y;
        float hN = clamp(y / (R * 2.2), 0.0, 1.0);
        float w = mix(0.03, R * 0.9, pow(hN, 1.6));
        float x = (P.x - c.x - 0.04 * sin(hN * 6.0 + TAU * uT * uTornado.z)) / w;
        float inside = (1.0 - smoothstep(0.7, 1.05, abs(x))) * step(0.0, y) * (1.0 - smoothstep(0.9, 1.0, hN));
        float a = asin(clamp(x, -1.0, 1.0));
        float stripes = 0.5 + 0.5 * sin(a * 5.0 + y * 28.0 - TAU * uT * uTornado.z * 3.0);
        float shade = 0.55 + 0.45 * (1.0 - abs(clamp(x, -1.0, 1.0)));
        vec3 tc = mix(vec3(0.35, 0.36, 0.40), vec3(0.74, 0.74, 0.77), stripes * shade);
        if (uMono > 0.5) tc = vec3(luma(tc));
        col = mix(col, tc, clamp(inside * (0.55 + 0.35 * stripes) * uTornado.x * mask, 0.0, 1.0));
        float base = 1.0 - smoothstep(0.0, R * 0.6, length(rel * vec2(0.6, 1.8)));
        float churn = 0.7 + 0.3 * sin(TAU * uT * uTornado.z * 4.0 + P.x * 30.0);
        col = mix(col, vec3(0.55, 0.5, 0.45), clamp(base * 0.5 * uTornado.x * mask * churn, 0.0, 1.0));
    }
    return col;
}

vec3 clouds3Layer(vec3 col, vec2 P, float mask) {
    vec3 lit = vec3(1.0);
    vec3 shadow = vec3(0.72, 0.78, 0.9);
    if (uClouds3.w > 0.5 && uClouds3.w < 1.5) { lit = vec3(1.0, 0.82, 0.62); shadow = vec3(0.62, 0.42, 0.52); }
    if (uClouds3.w > 1.5) { lit = vec3(0.55, 0.6, 0.78); shadow = vec3(0.16, 0.18, 0.3); }
    if (uMono > 0.5) { lit = vec3(luma(lit)); shadow = vec3(luma(shadow)); }
    for (int k = 0; k < 3; k++) {
        float fk = float(k);
        float scl = uClouds3.y * (2.2 - fk * 0.6);
        float cyc = uClouds3.z * (1.0 + fk);
        float p1 = fract(uT * cyc);
        float p2 = fract(p1 + 0.5);
        float w1 = 1.0 - abs(2.0 * p1 - 1.0);
        vec2 q = P * scl + vec2(fk * 7.3, fk * 3.1);
        float na = fbm(q - vec2(0.9 * (p1 - 0.5), 0.0));
        float nb = fbm(q - vec2(0.9 * (p2 - 0.5), 0.0));
        float n = mix(nb, na, w1);
        float dens = smoothstep(0.5 - fk * 0.03, 0.62, n);
        float light = clamp(0.35 + (n - 0.5) * 3.0, 0.0, 1.0);
        col = mix(col, mix(shadow, lit, light), dens * uClouds3.x * mask * (0.55 + fk * 0.2));
    }
    return col;
}

vec3 shoreLayer(vec3 col, vec2 P, float mask) {
    vec2 dir = normalize(uMotionShore.xy + vec2(1e-5, 0.0));
    float sAlong = dot(P, dir);
    float across = dot(P, vec2(-dir.y, dir.x));
    float style = clamp(uShoreMore.y, 0.0, 3.0);
    float phase = sAlong * uShore.y - uT * uShore.z;
    float edgeNoise = (fbm(vec2(across * (7.0 + style * 1.3), sAlong * 2.7)) - 0.5) * (0.18 + style * 0.035);
    float f = fract(phase + edgeNoise);
    float frontWidth = mix(0.045, 0.075, style / 3.0);
    float front = 1.0 - smoothstep(0.0, frontWidth, f);
    float lace = smoothstep(0.58, 0.80, vnoise(vec2(across * 34.0, f * 9.0 + style)));
    float trail = (1.0 - smoothstep(frontWidth, 0.28 + style * 0.025, f)) * lace;
    float foam = clamp(front * 0.72 + trail * 0.50, 0.0, 1.0) * clamp(uShoreMore.x, 0.0, 1.0);
    float wet = (1.0 - smoothstep(0.0, 0.52, f)) * 0.055;
    col *= 1.0 - wet * mask * min(uShore.x, 1.0);

    // Natural por defecto: aclara el color original en vez de pegar una franja blanca opaca.
    float tint = uShore.w;
    vec3 foamColor = mix(col, vec3(1.0), 0.58);
    if (tint > 0.5 && tint < 1.5) foamColor = mix(col, vec3(0.58, 0.96, 0.93), 0.62);
    else if (tint < 2.5 && tint > 1.5) foamColor = mix(col, vec3(0.48, 0.76, 1.0), 0.62);
    else if (tint < 3.5 && tint > 2.5) foamColor = mix(col, vec3(1.0, 0.70, 0.54), 0.60);
    else if (tint > 3.5) foamColor = mix(col, vec3(0.42, 0.55, 0.82), 0.58);
    float foamMix = clamp(foam * uShore.x * mask * (0.48 + style * 0.05), 0.0, 0.82);
    col = mix(col, foamColor, foamMix);
    return col;
}

vec3 sunpathLayer(vec3 col, vec2 P, float mask) {
    float cx = uSunpath.y * uAspect;
    float dx = abs(P.x - cx);
    float w = uSunpath.z * (0.4 + 0.8 * P.y);
    float style = floor(uLightStyles.y + 0.5);
    float column = 1.0 - smoothstep(w * 0.3, w * (style > 2.5 ? 0.65 : 1.0), dx);
    vec2 g = P * (style > 2.5 ? vec2(90.0, 190.0) : vec2(55.0, 140.0));
    vec2 id = floor(g);
    vec2 f = fract(g);
    vec2 h = hash22(id + 9.1);
    float tw = pow(0.5 + 0.5 * sin(TAU * (uT * uSunpath.w * (1.0 + floor(h.x * 3.0)) + h.y)), 6.0);
    float spark = (1.0 - smoothstep(0.0, 0.35, length((f - 0.5) * vec2(1.0, 2.5)))) * tw * step(0.35, h.y);
    vec3 scol = vec3(1.0, 0.95, 0.8);
    if (style > 0.5 && style < 1.5) scol = vec3(1.0, 0.60, 0.31); // atardecer
    else if (style > 1.5 && style < 2.5) scol = vec3(0.54, 0.73, 1.0); // luz de luna
    else if (style > 2.5) scol = vec3(0.82, 0.93, 1.0); // destellos finos
    if (uMono > 0.5) scol = vec3(1.0);
    float glimmer = style > 2.5 ? 2.0 : 1.0;
    col += scol * (spark * column * 1.4 * glimmer + column * 0.12) * uSunpath.x * mask;
    return col;
}

vec3 appearLayer(vec3 col, vec2 P) {
    float prog = clamp((uT - uAppear.y) / uAppear.z, 0.0, 1.0);
    float exitK = clamp((1.0 - uT) / (uAppear.z * 0.7), 0.0, 1.0);
    float r = min(prog, exitK);
    if (r <= 0.0) return col;
    vec2 pos = uAppearPose.xy * vec2(uAspect, 1.0);
    float hsz = max(0.02, uAppearPose.z);
    vec2 q = P - pos;
    float ca = cos(-uAppearPose.w);
    float sa = sin(-uAppearPose.w);
    q = vec2(ca * q.x - sa * q.y, sa * q.x + ca * q.y);
    float type = uAppear.w;
    vec2 offset = vec2(0.0);
    float scl = 1.0;
    if (type > 0.5 && type < 1.5) offset = vec2(-(1.0 - r) * (1.0 - r) * 0.35, 0.0);
    if (type > 1.5 && type < 2.5) scl = 1.0 + (1.0 - r) * 0.25;
    q = (q - offset) / scl;
    vec2 tuv = vec2(q.x / (hsz * max(uCharAspect, 0.05)) + 0.5, q.y / hsz + 0.5);
    float inside = step(0.0, tuv.x) * step(tuv.x, 1.0) * step(0.0, tuv.y) * step(tuv.y, 1.0);
    vec4 ch = textureLod(uCharacter, clamp(tuv, vec2(0.0), vec2(1.0)), 0.0) * inside;
    float reveal = r;
    if (type < 0.5) {
        float nz = fbm(tuv * 6.0 + 3.1);
        reveal = smoothstep(nz - 0.15, nz + 0.05, r * 1.2 - 0.1);
    }
    vec3 rgb = ch.a > 0.001 ? ch.rgb / ch.a : vec3(0.0);
    col = mix(col, rgb, clamp(ch.a * reveal * uAppear.x, 0.0, 1.0));
    float tr = r * (1.0 - r) * 4.0;
    if (type < 0.5) {
        vec2 sq = (P - pos) / hsz;
        float dist = length(sq * vec2(1.2, 0.8));
        float cloud = fbm(sq * 3.0 + vec2(0.0, -r * 2.0));
        float puff = (1.0 - smoothstep(0.35, 0.85, dist)) * smoothstep(0.35, 0.6, cloud);
        col = mix(col, vec3(0.92, 0.93, 0.96), clamp(puff * tr * uAppear.x, 0.0, 1.0));
    } else if (type < 1.5) {
        float band = 1.0 - smoothstep(0.0, hsz * 0.7, abs(P.y - pos.y));
        float rowId = floor(P.y * 90.0);
        float streak = step(0.6, hash12(vec2(rowId, 3.0))) *
            smoothstep(0.3, 0.9, fract(P.x * 3.0 - r * 4.0 + hash12(vec2(rowId, 7.0))));
        col = mix(col, vec3(0.95), clamp(streak * band * tr * 0.7 * uAppear.x, 0.0, 1.0));
    } else if (type < 2.5) {
        col = mix(col, vec3(1.0), clamp(tr * 0.85 * uAppear.x, 0.0, 1.0));
    }
    return col;
}


// v36: capas procedurales reutilizables en previsualización y exportación.
// Cada "forma" cambia el generador, el espacio y el movimiento, no solo el color.

// ---------------- Humo ----------------
float smokeField(vec2 x, vec2 wind, float shape, float turb) {
    vec2 side = vec2(-wind.y, wind.x);
    if (shape < 0.5) {                       // Ondulante: nubes con deformación de dominio
        vec2 w = vec2(vnoise(x * 0.7 + 3.1), vnoise(x * 0.7 + vec2(5.2, 1.3))) - 0.5;
        return fbm(x + w * (0.6 + 2.2 * turb));
    } else if (shape < 1.5) {                // Hilos: estirado en la dirección del viento
        vec2 q = vec2(dot(x, wind) * 0.42, dot(x, side) * 1.6);
        float w = vnoise(q * 0.8 + 7.7) - 0.5;
        float r = 1.0 - abs(2.0 * fbm3(q + vec2(w * 0.8, w * (1.0 + 2.5 * turb))) - 1.0);
        float body = smoothstep(0.25, 0.75, fbm3(x * 0.45 + 13.0));   // hilos agrupados, no cortina
        return r * r * r * (0.35 + 0.85 * body);
    } else if (shape < 2.5) {                // Tinta manga: celdas duras con borde
        float c = 1.0 - worley(x * 0.9 + vec2(vnoise(x * 0.5) * turb * 2.0));
        return clamp(c * 0.75 + fbm3(x * 1.3) * 0.45 - 0.08, 0.0, 1.0);
    }
    // Rastrero: aplanado, se arrastra por el suelo.
    vec2 q = vec2(x.x * 0.55, x.y * 1.6);
    vec2 w = vec2(vnoise(q * 0.6 + 2.0), vnoise(q * 0.6 + 9.0)) - 0.5;
    return fbm(q + w * (0.5 + 1.8 * turb)) * 1.12 + 0.06;
}

vec3 zetaSmoke(vec3 col, vec2 P, vec2 uv, float zone) {
    float cyc = max(1.0, uFxSmoke.z);
    float shape = floor(uSmokeShape.x + 0.5);
    float turb = clamp(uSmokeShape.y, 0.0, 1.0);
    float soft = clamp(uSmokeShape.z, 0.0, 1.0);
    vec2 origin = uMotionSmoke.zw * vec2(uAspect, 1.0);
    vec2 wind = normalize(uMotionSmoke.xy + vec2(0.001, 0.003));
    vec2 q = (P - origin) * max(0.5, uFxSmoke.y) * 2.0;
    // Desplazamiento real en la dirección del viento con dos fases fundidas:
    // ambos extremos del bucle usan la misma nube, sin salto al repetir.
    float progress = fract(uT * cyc);
    float ph = TAU * uT * cyc;
    vec2 swirl = vec2(sin(ph), cos(ph)) * 0.12 * (0.4 + turb);
    vec2 travel = wind * (0.35 + uSmokeControl.z * 0.65);
    float a = smokeField(q + swirl - travel * progress, wind, shape, turb);
    float b = smokeField(q + swirl - travel * (progress - 1.0), wind, shape, turb);
    float n = contrastFix(mix(a, b, progress), progress);
    float density = clamp(uSmokeControl.y, 0.2, 2.0);
    float thr = mix(0.62, 0.40, clamp((density - 0.2) / 1.8, 0.0, 1.0));
    float w = mix(0.035, 0.24, soft);
    float mist = smoothstep(thr - w, thr + w, n);
    if (shape > 2.5) {
        // El humo rastrero se concentra bajo el origen y se disipa hacia arriba.
        mist *= 1.0 - smoothstep(-0.10, 0.30, origin.y - P.y);
        mist = max(mist, smoothstep(thr - w - 0.12, thr + w, n) * (1.0 - smoothstep(-0.25, 0.08, origin.y - P.y)) * 0.85);
    }
    float m = maskValue(uv, uMaskSmoke, zone, uFxSmoke.w);
    float alpha = clamp(mist * min(density, 1.25) * uFxSmoke.x * m * emitArea(P, uMotionSmoke), 0.0, 0.92);
    if (alpha <= 0.002) return col;
    float kind = floor(uSmokeControl.x + 0.5);
    vec3 tint = vec3(0.53, 0.57, 0.63);
    if (kind > 0.5 && kind < 1.5) tint = vec3(0.045, 0.043, 0.055);
    else if (kind > 1.5 && kind < 2.5) tint = vec3(0.88, 0.90, 0.94);
    else if (kind > 2.5 && kind < 3.5) tint = vec3(0.31, 0.21, 0.49);
    else if (kind > 3.5) tint = vec3(0.52, 0.35, 0.27);
    tint = tintOr(tint, uTintSmoke);
    // Dos tonos de anime: cara iluminada y sombra; la tinta manga además lleva contorno.
    float lit = celLight(n, P);
    float bright = luma(tint);
    vec3 shadowC = tint * mix(0.55, 0.78, bright);
    vec3 litC = mix(tint, vec3(1.0), mix(0.10, 0.04, bright));
    vec3 smokeC = mix(shadowC, litC, mix(0.5, lit, 0.75));
    if (shape > 1.5 && shape < 2.5) {
        float edge = smoothstep(thr - 0.03, thr, n) * (1.0 - smoothstep(thr + 0.02, thr + 0.06, n));
        smokeC = mix(smokeC, vec3(bright > 0.5 ? 0.12 : 0.0), edge * 0.85);
        alpha = max(alpha, edge * 0.8 * uFxSmoke.x * m);
    }
    return mix(col, smokeC, alpha);
}

// ---------------- Explosión por fases ----------------
vec3 zetaBlast(vec3 col, vec2 P, vec2 uv, float zone) {
    float cycles = max(1.0, uFxBlast.z);
    float shape = floor(uBlastShape.x + 0.5);
    float tau = fract(uT * cycles);
    if (shape > 1.5 && shape < 2.5) {        // estallido manga: animación "en dos"
        float steps = max(4.0, floor(uLoopInfo.x * 12.0 / cycles));
        tau = floor(tau * steps) / steps;
    }
    float seed = floor(uT * cycles);
    vec2 center = uMotionBlast.zw * vec2(uAspect, 1.0);
    float R = max(0.03, uFxBlast.y) * 0.45;
    vec2 q = P - center;
    if (shape > 0.5 && shape < 1.5) q.y += R * 1.1 * pow(tau, 1.3);   // hongo: la bola sube
    float r = length(q);
    float ang = atan(q.y, q.x);
    float a = uFxBlast.x * maskValue(uv, uMaskBlast, zone, uFxBlast.w);
    if (a <= 0.001) return col;

    vec3 hue = vec3(1.0, 0.47, 0.08);
    float kind = uBlastMore.x;
    if (kind > 0.5 && kind < 1.5) hue = vec3(0.25, 0.67, 1.0);
    else if (kind > 1.5 && kind < 2.5) hue = vec3(0.73, 0.37, 1.0);
    else if (kind > 2.5) hue = vec3(0.65, 0.23, 0.23);
    if (shape > 2.5 && uTintBlast.w < 0.5 && kind < 0.5) hue = vec3(0.35, 0.85, 1.0);
    hue = tintOr(hue, uTintBlast);

    // 1) Destello inicial
    float flashAmt = clamp(uBlastShape.y, 0.0, 1.0);
    float flash = pow(1.0 - smoothstep(0.0, 0.07, tau), 2.0) * flashAmt;
    if (shape > 1.5 && shape < 2.5) flash = (tau < 0.06 ? 1.0 : 0.0) * flashAmt;
    col += mix(hue, vec3(1.0), 0.6) * flash * exp(-r * 2.2 / max(R, 0.05)) * a * 1.3;

    // 2) Bola de fuego con borde irregular y bandas de color planas
    float grow = 1.0 - pow(1.0 - clamp(tau / 0.32, 0.0, 1.0), 3.0);
    float rf = R * (0.15 + 0.85 * grow) * (shape > 0.5 && shape < 1.5 ? 1.0 + 0.4 * tau : 1.0);
    float fade = 1.0 - smoothstep(0.28, 0.66, tau);
    float edgeN = (fbm3(vec2(ang * 2.2, r / max(R, 0.02) * 3.0) + seed * 7.1) - 0.5) * 0.55;
    float d = r / max(rf, 0.004) + edgeN;
    float ball = (1.0 - smoothstep(0.92, 1.0, d)) * fade;
    float core = 1.0 - smoothstep(0.38, 0.46, d + tau * 0.5);
    float mid = 1.0 - smoothstep(0.66, 0.74, d + tau * 0.3);
    vec3 fireC = mix(hue * 0.75, mix(hue, vec3(1.0, 0.9, 0.55), 0.6), mid);
    fireC = mix(fireC, vec3(1.0, 0.98, 0.9), core);
    if (shape > 1.5 && shape < 2.5) fireC = mix(vec3(0.02), vec3(1.0), step(0.5, mid + core));
    if (shape > 2.5) fireC = mix(hue, vec3(1.0), core * 0.8 + 0.2 * mid);
    col = mix(col, fireC, clamp(ball * a, 0.0, 1.0));

    // 3) Onda de choque
    float ringAmt = clamp(uBlastShape.z, 0.0, 1.0);
    float rs = R * (0.25 + 2.3 * tau);
    float ring = exp(-pow((r - rs) / max(0.004, R * 0.06), 2.0)) * pow(1.0 - tau, 2.0) * step(tau, 0.6);
    if (shape > 2.5) ring += exp(-pow((r - rs * 0.6) / max(0.004, R * 0.04), 2.0)) * (1.0 - tau);
    col += mix(hue, vec3(1.0), 0.5) * ring * ringAmt * a * 0.9;

    // 4) Chispas radiales estiradas por la velocidad
    float sparksAmt = uBlastMore.z;
    if (sparksAmt > 0.01) {
        float N = 22.0;
        float id = floor((ang + 3.14159) / TAU * N);
        float h = hash12(vec2(id, seed + 3.0));
        float speed = 0.6 + h * 1.4;
        float head = R * (0.3 + 1.9 * speed * pow(tau, 0.7));
        float len = R * 0.35 * speed * (1.0 - tau);
        float along = smoothstep(head - len, head, r) * (1.0 - smoothstep(head, head + 0.006, r));
        float local = fract((ang + 3.14159) / TAU * N) - 0.5;
        float thin = 1.0 - smoothstep(0.03, 0.12, abs(local));
        col += mix(hue, vec3(1.0, 0.95, 0.7), 0.6) * along * thin * step(0.45, h) * (1.0 - smoothstep(0.55, 0.8, tau)) * sparksAmt * a;
    }

    // 5) Humo que queda y se disipa (no en la variante Energía)
    if (shape < 2.5 && uBlastMore.y > 0.01) {
        float rr = R * (0.55 + 0.9 * tau);
        vec2 sq = q / max(R, 0.02) * 2.0;
        float n = fbm(sq + vec2(seed * 3.3, -tau * 1.5));
        float body = smoothstep(rr * 0.35, rr * 0.75, r) * (1.0 - smoothstep(rr * 0.9, rr * 1.35, r + (n - 0.5) * R * 0.6));
        if (shape > 0.5 && shape < 1.5) {        // tallo del hongo
            vec2 st = P - center;
            float top = -R * 1.1 * pow(tau, 1.3);
            float stem = (1.0 - smoothstep(R * 0.10, R * 0.28, abs(st.x + (n - 0.5) * R * 0.25))) *
                smoothstep(top - R * 0.05, top + R * 0.25, st.y) * (1.0 - smoothstep(-R * 0.05, R * 0.15, st.y));
            body = max(body, stem * 0.9 * (1.0 - ball));
        }
        float life = smoothstep(0.12, 0.35, tau) * (1.0 - smoothstep(0.72, 0.99, tau));
        vec3 smokeHue = kind > 2.5 ? vec3(0.035) : mix(vec3(0.20, 0.19, 0.22), hue * 0.25, 0.25);
        if (shape > 1.5) smokeHue = vec3(0.05);
        float lit = smoothstep(0.35, 0.75, n);
        smokeHue = mix(smokeHue * 0.7, smokeHue * 1.6 + 0.04, lit);
        col = mix(col, smokeHue, clamp(body * smoothstep(0.38, 0.6, n + 0.15) * life * uBlastMore.y * a * 0.85, 0.0, 0.85));
    }
    // Líneas de impacto del estallido manga
    if (shape > 1.5 && shape < 2.5) {
        float lines = step(0.72, hash12(vec2(floor((ang + 3.14159) / TAU * 70.0), seed)));
        float reach = smoothstep(R * 0.9, R * 1.6, r) * (1.0 - smoothstep(0.0, 0.5, tau));
        col = mix(col, vec3(0.02), lines * reach * a * 0.8);
    }
    return col;
}

// ---------------- Salpicaduras manga / manhwa ----------------
// Unión suave de círculos (smooth-min): gotas que se funden como tinta, borde duro.
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

vec3 zetaBlood(vec3 col, vec2 P, vec2 uv, float zone) {
    float cycles = max(1.0, uFxBlood.z);
    float tau = fract(uT * cycles);
    float seed = floor(uT * cycles) * 13.0;
    float shape = floor(uBloodShape.x + 0.5);
    vec2 origin = uMotionBlood.zw * vec2(uAspect, 1.0);
    float S = 0.5 * max(0.2, uFxBlood.y);
    vec2 x = P - origin;
    float a = uFxBlood.x * maskValue(uv, uMaskBlood, zone, uFxBlood.w);
    if (a <= 0.001) return col;
    float life = smoothstep(0.0, 0.04, tau) * (1.0 - smoothstep(0.72, 0.97, tau));
    float sd = 1e3;                       // distancia con signo a la tinta (<0 dentro)
    if (shape < 0.5) {
        // Spray: gotas lanzadas en abanico; frenan, caen y se estiran según su velocidad.
        float dirA = uBloodShape.y;
        float open = mix(0.35, 1.6, clamp(uBloodShape.z, 0.0, 1.0));
        float ease = 1.0 - pow(1.0 - min(tau / 0.35, 1.0), 3.0);
        sd = length(x * vec2(1.0, 1.2)) - S * 0.06 * smoothstep(0.0, 0.06, tau);
        for (int i = 0; i < 16; i++) {
            float k = float(i);
            float h1 = hash12(vec2(k, seed + 1.0)), h2 = hash12(vec2(k, seed + 2.0)), h3 = hash12(vec2(k, seed + 3.0));
            float an = dirA + (h1 - 0.5) * open;
            vec2 v = vec2(cos(an), -sin(an)) * S * (0.18 + 0.62 * h2);
            vec2 pos = v * ease + vec2(0.0, 0.10 * S * ease * ease);
            float rad = S * 0.012 * clamp(pow(h3 + 0.05, -0.6), 0.7, 3.4);
            vec2 rel = x - pos;
            vec2 dirv = normalize(v + 1e-5);
            float stretch = 1.0 + 3.0 * (1.0 - ease) * h2;
            float along = dot(rel, dirv) / stretch, across = dot(rel, vec2(-dirv.y, dirv.x));
            // estela hacia el origen mientras vuela
            float tail = max(0.0, -dot(rel, dirv)) / max(length(pos) + 1e-4, 1e-4);
            float dd = length(vec2(along, across)) - rad * (1.0 - 0.6 * clamp(tail, 0.0, 1.0));
            sd = smin(sd, dd, rad * 0.9);
        }
    } else if (shape < 1.5) {
        // Goteo: mancha que aparece y chorrea por gravedad.
        float splat = S * 0.17 * smoothstep(0.0, 0.08, tau);
        float rr = length(x * vec2(1.0, 1.25)) + (vnoise(vec2(atan(x.y, x.x) * 3.0, seed)) - 0.5) * splat * 0.6;
        sd = rr - splat;
        for (int i = 0; i < 6; i++) {
            float k = float(i);
            float hx = hash12(vec2(k, seed + 5.0)) - 0.5;
            float hl = hash12(vec2(k, seed + 6.0));
            float px = hx * splat * 1.5;
            float len = S * (0.12 + 0.5 * hl) * smoothstep(0.06, 0.65, tau);
            float wid = S * 0.014 * (0.6 + hl);
            float y = clamp(x.y, 0.0, len);
            float taper = 1.0 - (y / max(len, 1e-4)) * 0.45;
            float body = length(vec2(x.x - px, x.y - y)) - wid * taper;
            float bulb = length(vec2(x.x - px, x.y - len)) - wid * 1.45;
            sd = smin(sd, min(body, bulb), wid);
        }
    } else {
        // Mancha de tinta: borde irregular que se expande, con gotas satélite.
        float grow = 1.0 - pow(1.0 - min(tau / 0.22, 1.0), 3.0);
        float ang = atan(x.y, x.x);
        float rb = S * 0.2 * grow * (1.0 + 0.55 * (fbm3(vec2(ang * 1.6 + seed, seed)) - 0.5) * 2.0);
        sd = length(x) - rb;
        for (int i = 0; i < 10; i++) {
            float k = float(i);
            float an = hash12(vec2(k, seed + 9.0)) * TAU;
            float dist = S * (0.25 + 0.38 * hash12(vec2(k, seed + 10.0))) * grow;
            vec2 c = vec2(cos(an), sin(an)) * dist;
            float rad = S * 0.016 * (0.5 + hash12(vec2(k, seed + 11.0))) * grow;
            sd = smin(sd, length(x - c) - rad, rad * 0.5);
        }
    }
    float px = max(fwidth(sd), 1e-4);
    float ink = (1.0 - smoothstep(-px, px, sd)) * life;
    vec3 tint = vec3(0.62, 0.035, 0.09);
    if (uBloodStyle.x > 0.5 && uBloodStyle.x < 1.5) tint = vec3(0.025, 0.012, 0.03);
    else if (uBloodStyle.x > 1.5) tint = vec3(0.09, 0.37, 0.75);
    tint = tintOr(tint, uTintBlood);
    return mix(col, tint, clamp(ink * a, 0.0, 0.96));
}

// ---------------- Cortes de energía ----------------
float slashArc(vec2 q, float len, float bend, float reveal, float width) {
    // Media luna: arco de círculo cuyo grosor se afina en los extremos.
    float Rr = len / max(0.2, bend);
    vec2 c = vec2(0.0, Rr);
    float r = length(q - c);
    float ang = atan(q.x, -(q.y - Rr)) / (len / Rr);   // -0.5..0.5 a lo largo del arco
    float s = ang + 0.5;
    float visible = step(0.0, s) * step(s, reveal);
    float thick = width * sin(3.14159 * clamp(s, 0.0, 1.0));
    return visible * (1.0 - smoothstep(thick * 0.6, thick, abs(r - Rr)));
}

vec3 zetaSlash(vec3 col, vec2 P, vec2 uv, float zone) {
    vec2 diff = P - uMotionSlash.zw * vec2(uAspect, 1.0);
    float a0 = uSlashStyle.y;
    float shape = floor(uSlashShape.x + 0.5);
    float phase = fract(uT * max(1.0, uFxSlash.z));
    float len = max(0.10, uFxSlash.y) * 0.44;
    float width = mix(0.004, 0.03, clamp(uSlashShape.y, 0.0, 1.0));
    float m = uFxSlash.x * maskValue(uv, uMaskSlash, zone, uFxSlash.w);
    if (m <= 0.001) return col;
    vec3 hue = vec3(0.14, 0.76, 1.0);
    if (uSlashStyle.x > 0.5 && uSlashStyle.x < 1.5) hue = vec3(0.94);
    else if (uSlashStyle.x > 1.5 && uSlashStyle.x < 2.5) hue = vec3(1.0, 0.70, 0.19);
    else if (uSlashStyle.x > 2.5) hue = vec3(1.0, 0.19, 0.22);
    hue = tintOr(hue, uTintSlash);
    float glow = 0.0, core = 0.0;
    if (shape < 0.5) {
        // Trazo clásico con estela ondulada (v35 mejorado: aparece de un extremo al otro).
        vec2 basis = vec2(cos(a0), sin(a0));
        vec2 q = vec2(dot(diff, basis), dot(diff, vec2(-basis.y, basis.x)));
        float flash = sin(3.14159 * phase);
        float xfade = 1.0 - smoothstep(0.55, 0.96, phase);
        float dist = abs(q.y - 0.10 * len * sin(q.x / len * 3.0 - TAU * phase));
        float reveal = step(q.x, mix(-len, len, clamp(phase * 4.0, 0.0, 1.0)));
        float lengthFade = (1.0 - smoothstep(len * 0.57, len, abs(q.x))) * reveal;
        core = exp(-dist / width * 1.6) * lengthFade * flash * xfade;
        glow = exp(-dist / (width * 5.0)) * lengthFade * flash * xfade;
    } else {
        int count = shape < 1.5 ? 1 : (shape < 2.5 ? 2 : 3);
        for (int k = 0; k < 3; k++) {
            if (k >= count) break;
            float fk = float(k);
            float start = fk * 0.12;
            float local = clamp((phase - start) / 0.5, 0.0, 1.0);
            float reveal = smoothstep(0.0, 0.35, local) * 1.05;
            float life = 1.0 - smoothstep(0.45, 1.0, local);
            float ang = a0;
            vec2 offset = vec2(0.0);
            if (shape > 1.5 && shape < 2.5) ang = a0 + (fk == 0.0 ? 0.785 : -0.785);
            if (shape > 2.5) offset = vec2(-sin(a0), cos(a0)) * (fk - 1.0) * len * 0.35;
            vec2 basis = vec2(cos(ang), sin(ang));
            vec2 d2 = diff - offset;
            vec2 q = vec2(dot(d2, basis), dot(d2, vec2(-basis.y, basis.x)));
            float arc = slashArc(q, len * 2.0, shape < 1.5 ? 0.9 : 0.35, reveal, width * 1.6);
            float halo = slashArc(q, len * 2.0, shape < 1.5 ? 0.9 : 0.35, reveal, width * 3.2);
            core = max(core, arc * life * step(start, phase));
            glow = max(glow, halo * life * step(start, phase));
        }
    }
    col += hue * glow * 0.42 * m;
    col = mix(col, mix(hue, vec3(1.0), 0.75), clamp(core * m, 0.0, 1.0));
    return col;
}

// ---------------- Aparición / revelación entre humo ----------------
vec3 zetaReveal(vec3 col, vec2 P, vec2 uv, float zone) {
    // Revela píxeles existentes sin pretender recomponer el fondo oculto.
    float phase = fract(uT * max(1.0, uFxReveal.z));
    float visibility = smoothstep(0.08, 0.62, phase) * (1.0 - smoothstep(0.86, 1.0, phase));
    float shape = floor(uRevealShape.x + 0.5);
    float ph = TAU * uT;
    float n = fbm(P * (2.0 + uFxReveal.y * 3.0) + vec2(sin(ph), cos(ph)) * 0.28);
    // Frente de revelado: desde el centro, desde abajo o empujado por el viento.
    vec2 c = vec2(0.5 * uAspect, 0.5);
    float front = 0.0;
    if (shape < 0.5) front = length(P - c) * 1.2;
    else if (shape < 1.5) front = 1.0 - uv.y;
    else front = uv.x;
    // Orden de aparición 0..1 (menor = aparece antes) y avance del revelado.
    float order = clamp(front * 0.75 + (n - 0.5) * 0.7 + 0.12, 0.0, 1.0);
    float reveal = visibility * 1.2 - 0.06;
    float hidden = smoothstep(reveal - 0.06, reveal + 0.06, order);
    float m = maskValue(uv, uMaskReveal, zone, uFxReveal.w);
    float a = clamp(hidden * uFxReveal.x * m, 0.0, 1.0);
    vec3 shade = vec3(0.045, 0.052, 0.070);
    if (uRevealStyle.x > 0.5 && uRevealStyle.x < 1.5) shade = vec3(0.48, 0.54, 0.61);
    else if (uRevealStyle.x > 1.5) shade = vec3(0.018, 0.012, 0.030);
    shade = tintOr(shade, uTintReveal);
    col = mix(col, shade, a * (0.75 + 0.25 * smoothstep(0.39, 0.68, n)));
    // Humo que se arremolina justo en el borde del revelado.
    float edgeSmoke = clamp(uRevealShape.y, 0.0, 1.0);
    if (edgeSmoke > 0.01) {
        float band = 1.0 - smoothstep(0.0, 0.14, abs(order - reveal));
        float puff = smoothstep(0.42, 0.7, fbm(P * 6.0 + vec2(cos(ph), sin(ph)) * 0.5));
        vec3 smokeC = mix(shade, vec3(luma(shade) > 0.3 ? 0.95 : 0.35), 0.45);
        col = mix(col, smokeC, clamp(band * puff * edgeSmoke * uFxReveal.x * m * step(0.02, visibility) * 0.85, 0.0, 0.9));
    }
    return col;
}

// ---------------- Fuego natural y mágico ----------------
// La zona pintada es la BASE del fuego: las llamas suben por encima de ella
// (altura configurable) en vez de quedar recortadas dentro del pincel.
vec3 zetaFlame(vec3 col, vec2 P, vec2 uv) {
    vec4 box = uFlameBox;
    if (box.z <= box.x + 0.001) box = vec4(0.0, 0.0, 1.0, 1.0);
    vec2 size = max(box.zw - box.xy, vec2(0.02));
    float H = size.y * (0.6 + 1.6 * clamp(uFlameShape.w, 0.0, 1.0)) + 0.04;
    float hN = (box.w - uv.y) / H;                 // 0 en la base, 1 en la punta
    if (hN < -0.05 || hN > 1.25) return col;
    // Columna de fuego: la máscara en la misma x, medida dentro de la zona pintada.
    float sampleY = clamp(uv.y, box.y + size.y * 0.5, box.w);
    float jitter = (vnoise(vec2(uv.y * 18.0 - uT * 0.0, uv.x * 9.0)) - 0.5) * size.x * 0.12 * clamp(hN, 0.0, 1.0);
    vec2 su = vec2(uv.x + jitter, sampleY);
    float area = maskValue(su, uMaskFlame, maskLayer(su, 2), 1.0);
    area *= 1.0 - maskLayer(uv, 0) * 0.85 * step(uv.y, box.y);   // no tapa al personaje por encima de la zona
    if (area <= 0.001) return col;
    float shape = floor(uFlameShape.x + 0.5);
    float turb = clamp(uFlameShape.y, 0.0, 1.0);
    float soft = clamp(uFlameShape.z, 0.0, 1.0);
    float cx = (uv.x - box.x) / size.x - 0.5;
    float cyc = max(1.0, uFxFlame.z);
    float progress = fract(uT * cyc);
    float ph = TAU * uT * cyc;
    float scale = 9.0 * max(0.3, uFxFlame.y);
    vec2 q = vec2(P.x * scale * 2.3, P.y * scale * 0.95);
    float lean = 0.0;
    if (shape > 1.5 && shape < 2.5) lean = hN * hN * (1.6 + 0.3 * sin(ph));           // antorcha al viento
    if (shape > 2.5 && shape < 3.5) lean = sin(hN * 5.0 - ph) * 0.35 * hN;              // mágica: serpentea
    q.x -= lean * 2.0;
    // Ascenso continuo con dos fases: el fuego sube sin cortes al cerrar el bucle.
    vec2 rise = vec2(0.0, 2.4);
    float wob = (vnoise(q * 0.45 + 11.0 + vec2(0.0, progress * 1.1)) - 0.5) * (0.5 + 1.8 * turb);
    float na = fbm3(q + rise * progress + vec2(wob, 0.0));
    float nb = fbm3(q + rise * (progress - 1.0) + vec2(wob, 0.0));
    float n = contrastFix(mix(na, nb, progress), progress);
    if (shape < 0.5) {
        float wa = 1.0 - worley(q * 0.8 + rise * progress * 0.8);
        float wb = 1.0 - worley(q * 0.8 + rise * (progress - 1.0) * 0.8);
        n = n * 0.7 + mix(wa, wb, progress) * 0.38 - 0.06;                              // lenguas de hoguera
    }
    float width = 1.0;
    if (shape > 0.5 && shape < 1.5) width = 1.0 - smoothstep(0.05 + 0.10 * (1.0 - hN), 0.12 + 0.16 * (1.0 - hN), abs(cx + lean * 0.1)); // vela
    float hc = clamp(hN, 0.0, 1.2);
    float f = (n * 1.2 + (1.0 - hc) * 0.30 - 0.44 - hc * hc * 0.22 - abs(cx) * hc * 0.7) * width;
    float w = mix(0.012, 0.08, soft);
    float outer = smoothstep(0.20 - w, 0.20 + w, f);
    float mid = smoothstep(0.36 - w, 0.36 + w, f);
    float core = smoothstep(0.52 - w, 0.52 + w, f);
    float style = floor(uLightStyles.w + 0.5);
    vec3 base = vec3(1.0, 0.36, 0.06);
    if (style > 0.5 && style < 1.5) base = vec3(0.18, 0.55, 1.0);
    else if (style > 1.5 && style < 2.5) base = vec3(0.24, 0.9, 0.34);
    else if (style > 2.5) base = vec3(0.72, 0.30, 1.0);
    if (shape > 2.5 && shape < 3.5 && uTintFlame.w < 0.5 && style < 0.5) base = vec3(0.45, 0.35, 1.0);
    base = tintOr(base, uTintFlame);
    vec3 outerC = base * 0.8;
    vec3 midC = mix(base, vec3(1.0, 0.85, 0.35), 0.55);
    vec3 coreC = mix(base, vec3(1.0, 0.98, 0.88), 0.85);
    float k = clamp(uFxFlame.x, 0.0, 1.5) * area;
    if (shape > 3.5) {
        // Fuego fantasma: solo contornos luminosos.
        float line = (1.0 - smoothstep(0.0, 0.03 + w, abs(f - 0.22))) + 0.6 * (1.0 - smoothstep(0.0, 0.025 + w, abs(f - 0.42)));
        col += mix(base, vec3(1.0), 0.4) * line * k;
        return col;
    }
    // Resplandor sobre lo que rodea al fuego (luz emitida).
    col += base * smoothstep(-0.1, 0.25, f) * 0.16 * k;
    col = mix(col, outerC, clamp(outer * k, 0.0, 1.0));
    col = mix(col, midC, clamp(mid * k, 0.0, 1.0));
    col = mix(col, coreC, clamp(core * k, 0.0, 1.0));
    if (shape > 2.5 && shape < 3.5) {
        // chispas mágicas que suben (velocidad entera: cierran el bucle)
        vec2 g = P * 24.0 + vec2(0.0, uT * cyc * 24.0);
        vec2 id = floor(g);
        vec2 h = hash22(vec2(id.x, mod(id.y, 24.0)) + 7.0);
        float sp = (1.0 - smoothstep(0.02, 0.09, length(fract(g) - h))) * step(0.8, h.x);
        col += mix(base, vec3(1.0), 0.7) * sp * smoothstep(-0.3, 0.1, f) * k;
    }
    return col;
}

// ---------------- Energía y auras ----------------
// Halo graduado alrededor de la capa Proteger (personaje): media de 16 muestras en
// dos anillos. Da una caída suave hacia fuera que el ruido convierte en llamas.
float silhouetteGlow(vec2 uv, float radius) {
    float acc = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.785398 + 0.39;
        vec2 o = vec2(cos(a) / max(uAspect, 0.01), sin(a)) * radius;
        acc += maskLayer(uv + o, 0) + maskLayer(uv + o * 0.45, 0);
    }
    return acc / 16.0;
}

// Compatibilidad: anillo duro (se usa en el contorno de la variante batalla).
float silhouetteRing(vec2 uv, float radius) {
    return clamp(silhouetteGlow(uv, radius) * 1.6 - maskLayer(uv, 0), 0.0, 1.0);
}

vec3 zetaEnergy(vec3 col, vec2 P, vec2 uv, float g) {
    float shape = floor(uEnergyShape.x + 0.5);
    float style = floor(uLightStyles.z + 0.5);
    vec3 ec = vec3(0.28, 0.62, 1.0);
    if (style > 0.5 && style < 1.5) ec = vec3(0.78, 0.38, 1.0);
    else if (style > 1.5 && style < 2.5) ec = vec3(1.0, 0.78, 0.25);
    else if (style > 2.5) ec = vec3(1.0, 0.29, 0.34);
    ec = tintOr(ec, uTintEnergy);
    float cyc = max(1.0, uEnergy.z);
    float k = uEnergy.x * g;
    if (k <= 0.001) return col;
    if (shape < 0.5) {
        col += ec * energyField(P, uEnergy.y, cyc) * k;
        return col;
    }
    if (shape > 1.5 && shape < 2.5) {
        // Anillos de poder que se expanden desde el origen.
        vec2 q = P - uMotionEnergy.zw * vec2(uAspect, 1.0);
        float d = length(q);
        float R = mix(0.2, 0.9, clamp(uEnergyShape.y, 0.0, 1.0));
        float rings = 0.0;
        for (int i = 0; i < 3; i++) {
            float r = fract(uT * cyc + float(i) / 3.0) * R;
            float th = 0.008 + 0.02 * (1.0 - r / R);
            rings += (1.0 - smoothstep(th * 0.3, th, abs(d - r))) * (1.0 - r / R);
        }
        col += ec * rings * k;
        return col;
    }
    // Aura alrededor del personaje pintado en Proteger: llamas que suben desde la silueta.
    float radius = mix(0.012, 0.05, clamp(uEnergyShape.y, 0.0, 1.0));
    float m0 = maskLayer(uv, 0);
    float near = silhouetteGlow(uv, radius);
    float far = silhouetteGlow(uv + vec2(0.0, radius * 1.6), radius * 2.2);   // desplazada: el aura sube
    float field = clamp(near * 0.9 + far * 0.8, 0.0, 1.2) * (1.0 - m0 * 0.92);
    if (field <= 0.001) return col;
    float turb = clamp(uEnergyShape.z, 0.0, 1.0);
    float progress = fract(uT * cyc);
    if (shape > 2.5) { float st = max(4.0, floor(uLoopInfo.x * 12.0)); progress = fract(floor(uT * st) / st * cyc); }
    vec2 q = vec2(P.x * 14.0, P.y * 5.0);
    vec2 up = vec2(0.0, 2.2);
    float wa = vnoise(q * 0.7 + up * progress) - 0.5, wb = vnoise(q * 0.7 + up * (progress - 1.0)) - 0.5;
    float na = fbm3(q + up * progress + vec2(wa * turb * 2.0, 0.0));
    float nb = fbm3(q + up * (progress - 1.0) + vec2(wb * turb * 2.0, 0.0));
    float n = contrastFix(mix(na, nb, progress), progress);
    float v = field + (n - 0.5) * 1.1;
    float flame = smoothstep(0.40, 0.50, v);
    float hot = smoothstep(0.66, 0.72, v);
    col += ec * flame * 0.85 * k;
    col = mix(col, mix(ec, vec3(1.0), 0.65), clamp(hot * k * (shape > 2.5 ? 1.0 : 0.55), 0.0, 1.0));
    if (shape > 2.5) {
        float edge = smoothstep(0.37, 0.40, v) - smoothstep(0.42, 0.45, v);
        col = mix(col, vec3(0.02), clamp(edge * k * 0.8, 0.0, 1.0));   // contorno de tinta (estilo batalla)
    }
    return col;
}

// ---------------- Ojos brillantes ----------------
vec3 zetaEyes(vec3 col, vec2 uv) {
    float shape = floor(uEyesShape.x + 0.5);
    float cyc = max(1.0, uFxEyes.z);
    float pulse = 0.65 + 0.35 * sin(TAU * uT * cyc);
    if (shape > 0.5 && shape < 1.5) pulse = 0.25 + 0.75 * pow(0.5 + 0.5 * sin(TAU * uT * cyc), 3.0);
    vec3 c = tintOr(vec3(0.20, 0.68, 1.0), uTintEyes);
    float core = maskValue(uv, uMaskEyes, maskLayer(uv, 1), 1.0);
    float haloR = mix(0.004, 0.03, clamp(uEyesShape.y, 0.0, 1.0));
    float halo = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.785398;
        vec2 o = vec2(cos(a) / max(uAspect, 0.01), sin(a)) * haloR;
        halo += maskValue(uv + o, uMaskEyes, maskLayer(uv + o, 1), 1.0);
    }
    halo /= 8.0;
    float streak = 0.0;
    if (shape > 1.5) {
        for (int i = 1; i <= 6; i++) {
            float fi = float(i);
            vec2 o = vec2(fi * haloR * 1.8 / max(uAspect, 0.01), 0.0);
            streak += (maskValue(uv + o, uMaskEyes, maskLayer(uv + o, 1), 1.0) +
                maskValue(uv - o, uMaskEyes, maskLayer(uv - o, 1), 1.0)) * (1.0 - fi / 7.0);
        }
        streak *= 0.22;
    }
    float k = uFxEyes.x * uFxEyes.y * pulse;
    col += c * (core * 0.9 + halo * 0.8 + streak) * k;
    col = mix(col, mix(c, vec3(1.0), 0.7), clamp(core * k * 0.35, 0.0, 0.6));
    return col;
}

// Profundidad de un punto de la imagen para el 2.5D (cerca = +1, lejos = -diferencia).
float parallaxDepth(vec2 fu) {
    float protect = maskLayer(fu, 0);
    float r = max(0.0005, uParallaxStyle.z);
    vec2 rx = vec2(r / max(uAspect, 0.01), 0.0), ry = vec2(0.0, r);
    float front = maskLayer(fu, 2);
    front = max(front, max(maskLayer(fu + rx, 2), maskLayer(fu - rx, 2)));
    front = max(front, max(maskLayer(fu + ry, 2), maskLayer(fu - ry, 2)));
    float back = maskLayer(fu, 1);
    return (front - back * uParallax.w) * (1.0 - protect);
}

void main() {
    // Coordenadas de pantalla con origen arriba-izquierda
    vec2 s = vec2(vScreen.x, mix(1.0 - vScreen.y, vScreen.y, uFlipY));
    if (uRotate > 0.5) {
        s = vec2(1.0 - s.y, s.x);
    }

    vec2 uvFrame = uViewOffset + s * uViewScale;
    // Painting uses the original frame: stable coordinates and no expensive effect work.
    if (uShowMask > 0.5 && uActiveMask >= 0) {
        vec3 original=sourcePixel(uvFrame);
        float selected=texture(uMasks,vec3(uvFrame,float(uActiveMask))).r;
        original=mix(original,vec3(0.72,0.39,1.0),selected*0.48);
        original+=(hash12(gl_FragCoord.xy)-0.5)/255.0;
        fragColor=vec4(clamp(original,0.0,1.0),1.0);
        return;
    }


    // Smooth periodic camera; overscan prevents exposed corners during rotation/pan.
    vec2 sc = s;
    if (uCam.x > 0.0 || uCam.y > 0.0) {
        float phase = TAU * uT * uCam.z;
        float wave = 0.5 - 0.5 * cos(phase);
        float mode = uCameraMode.x;
        float zoom = 1.0 + uCam.x * (mode < 0.5 ? wave : 1.0-wave);
        vec2 offset = vec2(0.0);
        float angle = 0.0;
        if (mode > 1.5 && mode < 2.5) {
            zoom = 1.0 + uCam.x + uCam.y * 0.3;
            vec2 panDir = vec2(cos(uCameraMode.y), -sin(uCameraMode.y));
            offset = panDir * sin(phase) * (0.5 - 0.5 / zoom) * uCam.y;
        }
        if (mode > 2.5 && mode < 3.5) { angle = sin(phase)*uCam.x*0.3; zoom = 1.0 + abs(sin(angle))*max(uAspect,1.0/uAspect)+uCam.x*0.1; }
        if (mode > 3.5 && mode < 4.5) { zoom = 1.0+uCam.x*0.15+uCam.y*0.06; offset = vec2(sin(phase*3.0),sin(phase*5.0))*uCam.y*0.01; }
        if (mode > 4.5) { // órbita 3D simulada, sin inventar geometría oculta
            zoom = 1.04 + uCam.x * 0.7 + uCam.y * 0.16;
            float orbit = sin(phase) * (0.03 + 0.20 * uCam.y);
            vec2 travel = vec2(cos(uCameraMode.y), -sin(uCameraMode.y));
            offset = travel * orbit * 0.15;
        }
        vec2 center = (s-0.5)*vec2(uAspect,1.0);
        center = mat2(cos(angle),-sin(angle),sin(angle),cos(angle))*center;
        if (mode > 4.5) {
            float orbit = sin(phase) * (0.07 + 0.22 * uCam.y);
            center.x = center.x * (1.0 - 0.25 * abs(orbit)) + center.y * orbit;
            center.y = center.y * (1.0 - 0.08 * abs(orbit));
        }
        vec2 moved = center/vec2(uAspect,1.0)/zoom+0.5+offset;
        sc = mix(s,moved,maskValue(uvFrame,uMaskCam,1.0,0.0));
    }
    // Profundidad 2.5D: Zona verde = primer plano, Zona azul = fondo, Proteger = plano de foco.
    // v36: búsqueda iterativa de profundidad (como el parallax occlusion mapping), primer
    // plano dilatado para que el estiramiento caiga en el fondo y no en la silueta, y
    // sobreencuadre automático para que nunca se vean bordes. Cierra exacto en el bucle.
    if (uParallax.x > 0.0) {
        float ph = TAU * uT * uParallax.z;
        float style = floor(uParallaxStyle.x + 0.5);
        vec2 dir = vec2(cos(uParallax.y), -sin(uParallax.y));
        float amt = uParallax.x;
        vec2 cam = dir * sin(ph);
        float dolly = 0.0;
        if (style > 0.5 && style < 1.5) cam = vec2(cos(ph), sin(ph) * 0.5) - vec2(1.0, 0.0);   // órbita, empieza en 0
        else if (style > 1.5) { dolly = 0.5 - 0.5 * cos(ph); cam = dir * dolly * 0.35; }
        float focus = clamp(uParallaxStyle.y, -1.0, 1.0);
        float over = 1.0 + amt * (1.0 + uParallax.w + abs(focus)) * (style > 0.5 && style < 1.5 ? 2.2 : 1.15) + dolly * amt * 1.2;
        vec2 base = (sc - 0.5) / over + 0.5;
        vec2 p = base;
        for (int i = 0; i < 3; i++) {
            vec2 fu = clamp(uViewOffset + p * uViewScale, vec2(0.0), vec2(1.0));
            float d = parallaxDepth(fu) - focus;
            float zoom = 1.0 + dolly * amt * 4.0 * d;
            p = (base - 0.5) / max(zoom, 0.2) + 0.5 - cam * amt * d;
        }
        sc = p;
    }

    float impactE = 0.0;
    if (uImpact.x > 0.0) {
        impactE = impactEnv();
        float kf = max(1.0, floor(uLoopInfo.x * 16.0 + 0.5));
        float z = 1.0 + (uImpact.x + 0.04 * uImpact.y) * impactE;
        vec2 shake = vec2(sin(TAU * uT * kf), sin(TAU * uT * (kf + 3.0) + 1.3)) * uImpact.y * 0.02 * impactE;
        sc = (sc - 0.5) / z + 0.5 + shake;
    }
    vec2 uv = uViewOffset + sc * uViewScale;

    float freeze = maskLayer(uv, 0);
    float z1 = maskLayer(uv, 1);
    float z2 = maskLayer(uv, 2);
    float move = 1.0 - freeze;
    vec2 asp = vec2(1.0 / uAspect, 1.0);
    vec2 P = vec2(uvFrame.x * uAspect, uvFrame.y);
    vec2 Q = vec2(uv.x * uAspect, uv.y);

    vec3 col = vec3(0.0);

    if (uOverlayOnly < 0.5) {
        // Distorsiones
        vec2 d = vec2(0.0);
        if (uRipple.x > 0.0) {
            vec2 q = Q * uRipple.y;
            float ph = TAU * uT * uRipple.z;
            d += vec2(sin(q.y * 1.7 + ph + sin(q.x * 0.9)), cos(q.x * 1.3 - ph + sin(q.y * 1.1))) * uRipple.x * maskValue(uv, uMaskRipple, z1, 1.0);
        }
        if (uSway.x > 0.0) {
            float n = vnoise(Q * uSway.y);
            float ph = TAU * (uT * uSway.z + n);
            float sw = sin(ph) + 0.35 * sin(2.0 * ph + 1.3);
            d += vec2(sw, 0.3 * cos(ph)) * uSway.x * maskValue(uv, uMaskSway, z2, 1.0);
        }
        if (uHeat.x > 0.0) {
            vec2 q = Q * uHeat.y;
            float ph = TAU * uT * uHeat.z;
            d += vec2(sin(q.y * 2.0 + ph + vnoise(q) * 3.0), 0.5 * cos(q.x * 1.5 + ph)) * uHeat.x * maskValue(uv, uMaskHeat, z2, 1.0);
        }
        if (uFxCloth.x > 0.0) {
            float ph = TAU * uT * uFxCloth.z;
            vec2 q = Q * uFxCloth.y;
            vec2 sway = vec2(sin(q.y + ph + 0.4 * sin(q.x)),
                             0.25 * cos(q.x + ph));
            d += sway * uFxCloth.x * maskValue(uv, uMaskCloth, z2, 1.0);
        }
        if (uMesh.x > 0.0) {
            float m = maskValue(uv, uMaskMesh, z2, 1.0);
            float ph = TAU * uT * uMesh.z;
            float a = uMeshDirection.x;
            vec2 dir = vec2(cos(a), -sin(a));
            vec2 side = vec2(-dir.y, dir.x);
            vec2 q = Q * uMesh.y;
            float along = dot(q, side);
            float across = dot(q, dir);
            vec2 deform;
            if (uMesh.w < 0.5) {
                // Ondulación: útil para cabello, cintas, agua o humo.
                deform = dir * sin(along + ph + 0.35 * sin(across * 0.65)) +
                    side * 0.22 * cos(across * 0.7 - ph);
            } else if (uMesh.w < 1.5) {
                // Respiración: expansión suave alrededor de la zona pintada.
                vec2 center = (uv - vec2(0.5)) * vec2(uAspect, 1.0);
                deform = normalize(center + vec2(0.0001)) * sin(ph) * (0.35 + 0.65 * smoothstep(0.0, 0.7, length(center)));
            } else {
                // Tela: dos ondas combinadas para evitar un aspecto rígido.
                deform = dir * (0.72 * sin(along + ph) + 0.28 * sin(along * 1.9 - ph * 0.8)) +
                    side * 0.18 * cos(across + ph * 0.7);
            }
            d += deform * uMesh.x * m;
        }
        if (uNatural.x > 0.0) {
            // v36: movimiento con raíz fija. s = distancia normalizada desde el borde fijo
            // DENTRO del rectángulo pintado (no de toda la imagen). La amplitud crece como
            // s^gamma (viga sujeta en un extremo) y una onda viaja de la raíz a la punta.
            // Fases 1x/2x/3x enteras: el cuadro final empalma con el primero.
            float m = maskValue(uv, uMaskNatural, z2, 1.0);
            if (m > 0.001) {
                float a = uNatural.y;
                vec2 wind = normalize(vec2(cos(a), -sin(a)) + vec2(1e-6, 0.0));
                vec2 side = vec2(-wind.y, wind.x);
                float phase = TAU * uT * uNatural.z;
                float flex = clamp(uNaturalMore.x, 0.0, 1.0);
                float variation = clamp(uNaturalMore.y, 0.0, 1.0);
                float gust = clamp(uNaturalMore.z, 0.0, 1.0);
                float material = floor(uNatural.w + 0.5);
                float anchor = floor(uNaturalMore.w + 0.5);
                float weight = clamp(uNaturalDynamics.x, 0.0, 1.0);
                float spring = clamp(uNaturalDynamics.y, 0.0, 1.0);
                vec4 box = uNaturalBox;
                if (box.z <= box.x + 0.001 || box.w <= box.y + 0.001) box = vec4(0.0, 0.0, 1.0, 1.0);
                vec2 bl = clamp((uv - box.xy) / max(box.zw - box.xy, vec2(0.02)), 0.0, 1.0);
                float s = 1.0;
                if (anchor > 0.5 && anchor < 1.5) s = bl.x;            // izquierda fija
                else if (anchor > 1.5 && anchor < 2.5) s = 1.0 - bl.x; // derecha fija
                else if (anchor > 2.5 && anchor < 3.5) s = 1.0 - bl.y; // abajo fija (árbol, pasto)
                else if (anchor > 3.5) s = bl.y;                       // arriba fija (pelo, cortina)
                // Peso: raíz más rígida y menos altas frecuencias. Flexibilidad: curva más suave.
                float gamma = mix(1.05, 2.2, weight) * mix(1.25, 0.8, flex);
                float pin = anchor < 0.5 ? 1.0 : pow(s, gamma) * smoothstep(0.0, 0.06, s) * 1.35;
                // Fase estática por mechón/rama: vecinos desfasados, sin efecto "gelatina".
                float theta = (vnoise(Q * mix(2.5, 9.0, variation) + 3.7) - 0.5) * 3.2 * variation;
                float kappa = 2.0;
                if (material < 0.5) kappa = 1.1;          // árbol
                else if (material < 1.5) kappa = 7.5;     // bandera
                else if (material < 2.5) kappa = 1.6;     // pasto
                else if (material < 3.5) kappa = 4.0;     // cortina
                else if (material < 4.5) kappa = 3.0;     // cabello
                else kappa = 3.4;                         // ropa
                kappa *= mix(0.6, 1.3, flex);
                float w1 = sin(phase - kappa * s + theta);
                float w2 = sin(2.0 * phase - kappa * 1.7 * s + theta * 1.3 + 1.1);
                float w3 = sin(3.0 * phase - kappa * 2.4 * s + theta * 0.7 + 2.3);
                float bounce = mix(0.10, 0.42, spring) * (1.0 - 0.45 * weight);
                float sway = (1.0 - bounce) * w1 + bounce * w2 + 0.14 * variation * (1.0 - weight) * w3;
                // Ráfagas: armónicos enteros (siguen cerrando el bucle) más fuertes en la punta.
                float gustEnv = 1.0 + gust * (0.34 * sin(phase + 0.7) + 0.16 * sin(2.0 * phase + 2.1)) * (0.5 + 0.5 * s);
                vec2 naturalD;
                if (material < 0.5) {
                    // Árbol: la copa se inclina; las hojas tiemblan con alta frecuencia en la punta.
                    float flutter = sin(4.0 * phase + vnoise(Q * 38.0) * TAU) * 0.22 * variation * smoothstep(0.35, 1.0, s);
                    naturalD = wind * (sway * 0.85 + flutter) + side * 0.16 * w2;
                } else if (material < 1.5) {
                    // Bandera: onda que viaja desde el mástil; la tela se estira en la dirección del viento.
                    float wave = sin(2.0 * phase - kappa * s + theta * 0.5) + 0.35 * sin(3.0 * phase - kappa * 1.9 * s + 0.8);
                    naturalD = side * wave * (0.55 + 0.45 * flex) + wind * (0.18 + 0.12 * w1);
                } else if (material < 2.5) {
                    // Pasto: cada brizna con su propia fase (ruido fino en x), base inmóvil.
                    float blade = (vnoise(vec2(Q.x * 70.0, 3.0)) - 0.5) * 4.0 * variation;
                    float g1 = sin(phase - kappa * s + blade) + 0.25 * sin(2.0 * phase + blade * 1.7);
                    naturalD = wind * g1 + side * 0.06 * w2;
                } else if (material < 3.5) {
                    // Cortina: pliegues verticales que se propagan hacia abajo.
                    float folds = sin(Q.x * 24.0 + phase - s * 2.0) * 0.5 + sway * 0.5;
                    naturalD = wind * folds + side * 0.10 * w2;
                } else if (material < 4.5) {
                    // Cabello: puntas con retraso y rebote; la raíz no se mueve.
                    naturalD = wind * sway + side * 0.14 * w2 * (0.5 + 0.5 * s);
                } else {
                    // Ropa: tela pesada, menos rebote y pliegues amplios.
                    naturalD = wind * sway * mix(1.0, 0.75, weight) + side * 0.10 * w3;
                }
                d += naturalD * uNatural.x * gustEnv * pin * m;
            }
        }
        if(uZoneMotion.x>0.0){
            float zone=maskValue(uv,uMaskZonemotion,0.0,1.0);
            float phase=TAU*uT*uZoneMotion.y;
            vec2 direction=vec2(cos(uZoneMotion.w),sin(uZoneMotion.w));
            vec2 motion=direction*sin(phase)*uZoneMotion.x;
            if(uZoneMotion.z>0.5 && uZoneMotion.z<1.5)
                motion=direction*sin(phase+dot(Q,vec2(-direction.y,direction.x))*18.0)*uZoneMotion.x;
            if(uZoneMotion.z>1.5)
                motion=vec2(0.0,-sin(phase)*uZoneMotion.x*(0.35+0.65*smoothstep(0.0,1.0,uv.y)));
            d+=motion*zone;
        }
        if (uGust.x > 0.0) {
            vec2 gdir = normalize(uMotionGust.xy + vec2(1e-5, 0.0));
            float genv = windowEnv(uGust.y, uGust.z) * uGust.x;
            d += gdir * (0.5 + 0.5 * sin(TAU * uT * uGust.w * 2.0 + Q.y * 18.0)) * genv * 0.004 *
                maskValue(uv, uMaskGust, 1.0, 0.0);
        }
        if (uShore.x > 0.0) {
            vec2 sdir = normalize(uMotionShore.xy + vec2(1e-5, 0.0));
            d += sdir * sin(TAU * (dot(Q, sdir) * uShore.y - uT * uShore.z)) * 0.002 * uShore.x *
                maskValue(uv, uMaskShore, 1.0, 0.0);
        }
        d *= move;

        // Flujo (flechas) + deriva, con fundido de dos fases
        vec2 f = vec2(0.0);
        if (uFlow3.x > 0.5) {
            f += texture(uFlow, uv).rg * uFlow3.y * maskValue(uv, uMaskFlow, z1, 0.0);
        }
        f += uDrift * maskValue(uv, uMaskDrift, z1, 1.0);
        if (uTornado.x > 0.0) {
            vec2 tc = uMotionTornado.zw;
            vec2 rel = (uv - tc) * vec2(uAspect, 1.0);
            float tr = length(rel);
            float TR = max(0.05, uTornado.y);
            float tfall = (1.0 - smoothstep(TR * 0.2, TR, tr)) * smoothstep(0.0, TR * 0.25, tr);
            vec2 tang = vec2(-rel.y, rel.x) / max(tr, 1e-4);
            float kind = uTornado.w > 0.5 ? 0.4 : 1.0;
            f += tang * uTornado.x * 0.18 * tfall * kind / vec2(uAspect, 1.0) * maskValue(uv, uMaskTornado, 1.0, 0.0);
        }
        f *= move;

        vec2 base = uv + d * asp;
        if (dot(f, f) > 1e-12) {
            float phase=uT*uFlow3.z+uLoopControl.z;
            if(uLoopControl.x<0.5){
                float p1=fract(phase),p2=fract(p1+0.5);
                float w1=1.0-abs(2.0*p1-1.0);
                float blend=max(0.05,uLoopControl.y);
                if(blend<0.999)w1=smoothstep(0.5-blend*0.5,0.5+blend*0.5,w1);
                col=mix(warpSample(uv,d*asp-f*(p2-0.5)),warpSample(uv,d*asp-f*(p1-0.5)),w1);
            } else if(uLoopControl.x<1.5){
                col=warpSample(uv,d*asp-f*(0.5*sin(TAU*phase)));
            } else {
                col=warpSample(uv,d*asp-f*(0.5-0.5*cos(TAU*phase)));
            }
        } else {
            col = warpSample(uv,d*asp);
        }

        // Brillos en el agua
        if (uGlint.x > 0.0) {
            vec2 g = Q * uGlint.y;
            vec2 id = floor(g);
            vec2 fq = fract(g);
            vec2 h = hash22(id + 4.4);
            vec2 c = 0.2 + 0.6 * h;
            float dd = length((fq - c) * vec2(0.55, 2.2));
            float tw = 0.5 + 0.5 * sin(TAU * (uT * uGlint.z + h.x));
            float gli = falloff(dd, 0.14) * tw * tw * tw * tw * step(0.5, h.y);
            col += vec3(1.0, 0.98, 0.9) * gli * uGlint.x * maskValue(uv, uMaskGlint, z1, 1.0) * move;
        }
    }

    if (uOverlayOnly < 0.5) {
        if(uStudioWater.x>0.0){
            float ph=TAU*uT*uStudioWater.y;
            vec2 dir=vec2(cos(uStudioWater.w),sin(uStudioWater.w));
            vec2 q=uv*vec2(uAspect,1.0);
            float wave=sin(dot(q,dir)*65.0+ph)*cos(dot(q,vec2(-dir.y,dir.x))*21.0-ph);
            vec2 offset=dir*wave*0.008*uStudioWater.x/vec2(uAspect,1.0);
            if(uStudioWater.z>0.5 && uStudioWater.z<1.5)offset=dir*sin(ph+dot(q,dir)*30.0)*0.014*uStudioWater.x/vec2(uAspect,1.0);
            vec3 water=warpSample(uv,offset);
            if(uStudioWater.z>1.5)water+=pow(max(0.0,wave),8.0)*uStudioWater.x*vec3(0.5,0.7,0.85);
            col=mix(col,water,maskValue(uv,uMaskWater,z1,1.0)*move);
        }
        if(uStudioSky.x>0.0)col=mix(col,studioSky(uv),uStudioSky.x*maskValue(uv,uMaskSky,0.0,1.0));
        if(uStudioDispersion.x>0.0)col=studioDisperse(col,uv);
        if (uClouds3.x > 0.0) col = clouds3Layer(col, P, maskValue(uv, uMaskClouds3, 1.0, 0.0) * move);
        if (uShore.x > 0.0) col = shoreLayer(col, Q, maskValue(uv, uMaskShore, 1.0, 0.0) * move);
        if (uSunpath.x > 0.0) col = sunpathLayer(col, P, maskValue(uv, uMaskSunpath, 1.0, 0.0));
    }
    float bgL = luma(col);

    if (uRays.x > 0.0) {
        float g = maskValue(uv, uMaskRays, z1, uRays.w);
        float style = floor(uLightStyles.x + 0.5);
        float beam = raysField(P, uRays.y, uRays.z);
        vec3 rc = vec3(1.0, 0.92, 0.75);
        if (style > 0.5 && style < 1.5) { // contraluz concentrado
            float radius = length(P - uMotionRays.zw * vec2(uAspect, 1.0));
            beam = pow(max(0.0, beam), 0.70) * exp(-radius * 0.35);
            rc = vec3(1.0, 0.96, 0.87);
        } else if (style > 1.5 && style < 2.5) { // atardecer cálido
            beam = pow(max(0.0, beam), 0.9) * 0.85;
            rc = vec3(1.0, 0.54, 0.25);
        } else if (style > 2.5 && style < 3.5) { // franjas paralelas de ventana
            float bar = 0.5 + 0.5 * sin((P.x * 0.75 + P.y * 0.66) * 24.0);
            beam = pow(bar, 10.0) * (0.5 + 0.5 * sin(TAU * uT * uRays.z)) * 0.65;
            rc = vec3(1.0, 0.96, 0.84);
        } else if (style > 3.5) { // haces volumétricos suaves
            vec2 diff = P - uMotionRays.zw * vec2(uAspect, 1.0);
            float ang = atan(diff.y, diff.x);
            float beams = 0.5 + 0.5 * cos(ang * 11.0 + 0.18 * sin(TAU * uT * uRays.z));
            beam = pow(beams, 7.0) * exp(-length(diff) * 0.9) * 0.70;
            rc = vec3(1.0, 0.90, 0.69);
        }
        if (uMono > 0.5) rc = vec3(1.0);
        col += rc * beam * uRays.x * g;
    }
    if (uEnergy.x > 0.0) {
        col = zetaEnergy(col, P, uv, maskValue(uv, uMaskEnergy, z1, uEnergy.w));
    }
    if (uSpeed.x > 0.0) {
        float g = maskValue(uv, uMaskSpeed, z1, uSpeed.w);
        vec3 sc = uMono > 0.5 ? vec3(1.0) : vec3(0.95, 0.88, 1.0);
        col += sc * speedField(P - uMotionSpeed.zw * vec2(uAspect, 1.0), degrees(atan(uMotionSpeed.y, uMotionSpeed.x)) - 90.0 + uSpeed.y, uSpeed.z) * uSpeed.x * g;
    }
    if (uStars.x > 0.0) {
        float g = maskValue(uv, uMaskStars, z1, uStars.w);
        vec3 st = starsLayer(P, uStars.y, uStars.z);
        if (uMono > 0.5) {
            st = vec3(luma(st));
        }
        col += st * uStars.x * g;
    }
    if (uFog.x > 0.0) {
        float g = maskValue(uv, uMaskFog, z1, uFog.w);
        float shape = floor(uFogShape.x + 0.5);
        float turb = clamp(uFogShape.y, 0.0, 1.0);
        vec2 op = oriented(P, uMotionFog);
        op += (vec2(vnoise(op * 1.3 + 4.0), vnoise(op * 1.3 + 9.0)) - 0.5) * turb * 0.6;
        float fv;
        if (shape < 0.5) {            // bancos que se desplazan
            fv = smoothstep(0.3, 0.85, fogField(op, uFog.y, uFog.z));
        } else if (shape < 1.5) {     // niebla baja pegada al suelo
            float f = fogField(op * vec2(1.0, 2.4), uFog.y * 0.8, uFog.z);
            fv = smoothstep(0.22, 0.75, f) * smoothstep(0.30, 0.95, uv.y) + 0.18 * smoothstep(0.6, 1.0, uv.y);
        } else {                      // bruma fina y uniforme
            fv = 0.25 + 0.55 * smoothstep(0.25, 0.8, fogField(op, uFog.y * 2.6, uFog.z));
        }
        fv *= emitArea(P, uMotionFog);
        vec3 fc = tintOr(vec3(0.86, 0.89, 0.95), uTintFog);
        if (uMono > 0.5) fc = vec3(1.0);
        col = mix(col, fc, clamp(fv * uFog.x * g, 0.0, 0.95));
    }
    if (uRain.x > 0.0) {
        float g = maskValue(uv, uMaskRain, z1, uRain.w);
        float r = rainLayer(oriented(P, uMotionRain), 14.0, uRain.z + 2.0, uRain.y, 1.0)
                + 0.7 * rainLayer(oriented(P, uMotionRain), 24.0, uRain.z + 1.0, uRain.y, 17.0)
                + 0.45 * rainLayer(oriented(P, uMotionRain), 40.0, uRain.z, uRain.y, 41.0);
        vec3 rc = vec3(0.82, 0.88, 1.0);
        if (uMono > 0.5) {
            rc = bgL > 0.55 ? vec3(0.08) : vec3(0.97);
        }
        col = mix(col, rc, clamp(r, 0.0, 1.0) * uRain.x * g * emitArea(P, uMotionRain));
    }
    if (uSnow.x > 0.0) {
        float g = maskValue(uv, uMaskSnow, z1, uSnow.w);
        float sn = snowLayer(oriented(P, uMotionSnow), 10.0, uSnow.z + 1.0, uSnow.y * 1.3, 3.0)
                 + 0.8 * snowLayer(oriented(P, uMotionSnow), 18.0, uSnow.z, uSnow.y, 29.0)
                 + 0.6 * snowLayer(oriented(P, uMotionSnow), 30.0, uSnow.z, uSnow.y * 0.8, 53.0);
        col = mix(col, vec3(1.0), clamp(sn, 0.0, 1.0) * uSnow.x * g * emitArea(P, uMotionSnow));
    }
    if (uSakura.x > 0.0) {
        float g = maskValue(uv, uMaskSakura, z1, uSakura.w);
        vec4 pa = sakuraLayer(oriented(P, uMotionSakura), 5.0, uSakura.z + 1.0, uSakura.y * 1.2, 7.0);
        vec4 pb = sakuraLayer(oriented(P, uMotionSakura), 9.0, uSakura.z, uSakura.y, 61.0);
        vec3 ca = pa.rgb;
        vec3 cb = pb.rgb;
        if (uMono > 0.5) {
            ca = vec3(luma(ca));
            cb = vec3(luma(cb));
        }
        col = mix(col, cb, pb.a * uSakura.x * g * emitArea(P, uMotionSakura));
        col = mix(col, ca, pa.a * uSakura.x * g * emitArea(P, uMotionSakura));
    }
    if (uFlies.x > 0.0) {
        float g = maskValue(uv, uMaskFlies, z1, uFlies.w);
        vec3 fl = fliesLayer(P, uFlies.y, uFlies.z);
        if (uMono > 0.5) {
            fl = vec3(luma(fl));
        }
        col += fl * uFlies.x * g;
    }

    // Partículas de atmósfera con color propio y origen/dirección independientes.
    if (uFxAsh.x > 0.0) {
        float v = particleDot(oriented(P, uMotionAsh), 13.0, uFxAsh.z, uFxAsh.y, 13.0);
        col = mix(col, vec3(0.66), v * uFxAsh.x * maskValue(uv, uMaskAsh, z1, uFxAsh.w) * emitArea(P, uMotionAsh));
    }
    if (uFxLeaves.x > 0.0) {
        float v = particleDot(oriented(P, uMotionLeaves), 7.0, uFxLeaves.z, uFxLeaves.y, 33.0);
        col = mix(col, leafPalette(uLeavesStyle.x, P),
            v * uFxLeaves.x * maskValue(uv, uMaskLeaves, z1, uFxLeaves.w) * emitArea(P, uMotionLeaves));
    }
    if (uFxEmbers.x > 0.0) {
        float v = particleDot(oriented(P, uMotionEmbers), 17.0, uFxEmbers.z, uFxEmbers.y, 51.0);
        col += (uMono > 0.5 ? vec3(1.0) : vec3(1.0, 0.48, 0.09)) * v * uFxEmbers.x *
            maskValue(uv, uMaskEmbers, z1, uFxEmbers.w) * emitArea(P, uMotionEmbers);
    }
    if (uFxDust.x > 0.0) {
        float v = particleDot(oriented(P, uMotionDust), 14.0, uFxDust.z, uFxDust.y, 71.0);
        col += vec3(0.85, 0.81, 0.73) * v * uFxDust.x * maskValue(uv, uMaskDust, z1, uFxDust.w) * emitArea(P, uMotionDust);
    }
    if (uFxBubbles.x > 0.0) {
        vec2 g = oriented(P, uMotionBubbles) * 10.0;
        g.y -= uT * uFxBubbles.z * 6.0;
        vec2 h = hash22(vec2(floor(g.x), mod(floor(g.y), 6.0)) + 81.0);
        float r = length(fract(g) - (0.2 + 0.6 * h));
        float rim = (1.0 - smoothstep(0.008, 0.025, abs(r - uFxBubbles.y * 0.10))) * step(0.5, h.x);
        col += vec3(0.62, 0.87, 1.0) * rim * uFxBubbles.x *
            maskValue(uv, uMaskBubbles, z1, uFxBubbles.w) * emitArea(P, uMotionBubbles);
    }
    if (uFxElectric.x > 0.0) {
        float v = electricArc(P, uMotionElectric, uFxElectric.y, uFxElectric.z);
        col += (uMono > 0.5 ? vec3(1.0) : vec3(0.27, 0.80, 1.0)) * v * uFxElectric.x * maskValue(uv, uMaskElectric, z1, uFxElectric.w);
    }
    if (uFxFocus.x > 0.0) {
        float v = focusLines(P, uMotionFocus, uFxFocus.y, uFxFocus.z);
        col = mix(col, vec3(uMono > 0.5 ? 0.0 : 0.92), clamp(v*uFxFocus.x*maskValue(uv, uMaskFocus, z1, uFxFocus.w),0.0,1.0));
    }
    if (uFxShock.x > 0.0) {
        float r = length(P - uMotionShock.zw * vec2(uAspect, 1.0));
        float phase = fract(uT * uFxShock.z);
        float ring = (1.0 - smoothstep(uFxShock.y * 0.4, uFxShock.y, abs(r-phase*0.9)))*(1.0-phase);
        col += vec3(0.65, 0.86, 1.0) * ring*uFxShock.x*maskValue(uv, uMaskShock, z1, uFxShock.w);
    }
    if (uFxFlash.x > 0.0) {
        float phase = fract(uT*uFxFlash.z);
        float burst = 1.0 - smoothstep(0.0, uFxFlash.y, phase);
        col = mix(col, vec3(1.0), clamp(burst*uFxFlash.x*maskValue(uv, uMaskFlash, z1, uFxFlash.w),0.0,1.0));
    }
    if (uFxTone.x > 0.0) {
        vec2 grid = fract(P * uFxTone.y);
        float dotTone = 1.0 - smoothstep(0.1, 0.38, length(grid-0.5));
        float pulse = 0.9 + 0.1*sin(TAU*uT*uFxTone.z);
        float k = uFxTone.x*maskValue(uv, uMaskTone, z1, uFxTone.w);
        col = mix(col, vec3(dotTone*0.10 + (1.0-dotTone)*0.98)*pulse, k);
    }
    if (uFxGrain.x > 0.0) {
        float n = hash12(floor(P*uFxGrain.y)+vec2(floor(mod(uT*uFxGrain.z*12.0,12.0)),0.0));
        col += (n-0.5)*uFxGrain.x*maskValue(uv, uMaskGrain, z1, uFxGrain.w);
    }
    if (uFxInk.x > 0.0) {
        float ink = smoothstep(uFxInk.y-0.035,uFxInk.y+0.035,luma(col));
        col = mix(col,vec3(ink),uFxInk.x*maskValue(uv, uMaskInk, z1, uFxInk.w));
    }

    // Instancias B; conserva el orden, las máscaras y los parámetros de cada copia.
    if (uFxAshCopy.x > 0.0) {
        float v = particleDot(oriented(P, uMotionAshCopy), 13.0, uFxAshCopy.z, uFxAshCopy.y, 13.0);
        col = mix(col, vec3(0.66), v * uFxAshCopy.x * maskValue(uv, uMaskAshCopy, z1, uFxAshCopy.w) * emitArea(P, uMotionAshCopy));
    }
    if (uFxLeavesCopy.x > 0.0) {
        float v = particleDot(oriented(P, uMotionLeavesCopy), 7.0, uFxLeavesCopy.z, uFxLeavesCopy.y, 33.0);
        col = mix(col, leafPalette(uLeavesStyle.y, P + vec2(0.17, 0.31)),
            v * uFxLeavesCopy.x * maskValue(uv, uMaskLeavesCopy, z1, uFxLeavesCopy.w) * emitArea(P, uMotionLeavesCopy));
    }
    if (uFxEmbersCopy.x > 0.0) {
        float v = particleDot(oriented(P, uMotionEmbersCopy), 17.0, uFxEmbersCopy.z, uFxEmbersCopy.y, 51.0);
        col += (uMono > 0.5 ? vec3(1.0) : vec3(1.0, 0.48, 0.09)) * v * uFxEmbersCopy.x *
            maskValue(uv, uMaskEmbersCopy, z1, uFxEmbersCopy.w) * emitArea(P, uMotionEmbersCopy);
    }
    if (uFxDustCopy.x > 0.0) {
        float v = particleDot(oriented(P, uMotionDustCopy), 14.0, uFxDustCopy.z, uFxDustCopy.y, 71.0);
        col += vec3(0.85, 0.81, 0.73) * v * uFxDustCopy.x * maskValue(uv, uMaskDustCopy, z1, uFxDustCopy.w) * emitArea(P, uMotionDustCopy);
    }
    if (uFxBubblesCopy.x > 0.0) {
        vec2 g = oriented(P, uMotionBubblesCopy) * 10.0;
        g.y -= uT * uFxBubblesCopy.z * 6.0;
        vec2 h = hash22(vec2(floor(g.x), mod(floor(g.y), 6.0)) + 81.0);
        float r = length(fract(g) - (0.2 + 0.6 * h));
        float rim = (1.0 - smoothstep(0.008, 0.025, abs(r - uFxBubblesCopy.y * 0.10))) * step(0.5, h.x);
        col += vec3(0.62, 0.87, 1.0) * rim * uFxBubblesCopy.x *
            maskValue(uv, uMaskBubblesCopy, z1, uFxBubblesCopy.w) * emitArea(P, uMotionBubblesCopy);
    }
    if (uFxElectricCopy.x > 0.0) {
        float v = electricArc(P, uMotionElectricCopy, uFxElectricCopy.y, uFxElectricCopy.z);
        col += (uMono > 0.5 ? vec3(1.0) : vec3(0.27, 0.80, 1.0)) * v * uFxElectricCopy.x * maskValue(uv, uMaskElectricCopy, z1, uFxElectricCopy.w);
    }
    if (uFxFocusCopy.x > 0.0) {
        float v = focusLines(P, uMotionFocusCopy, uFxFocusCopy.y, uFxFocusCopy.z);
        col = mix(col, vec3(uMono > 0.5 ? 0.0 : 0.92), clamp(v*uFxFocusCopy.x*maskValue(uv, uMaskFocusCopy, z1, uFxFocusCopy.w),0.0,1.0));
    }
    if (uFxShockCopy.x > 0.0) {
        float r = length(P - uMotionShockCopy.zw * vec2(uAspect, 1.0));
        float phase = fract(uT * uFxShockCopy.z);
        float ring = (1.0 - smoothstep(uFxShockCopy.y * 0.4, uFxShockCopy.y, abs(r-phase*0.9)))*(1.0-phase);
        col += vec3(0.65, 0.86, 1.0) * ring*uFxShockCopy.x*maskValue(uv, uMaskShockCopy, z1, uFxShockCopy.w);
    }
    if (uFxFlashCopy.x > 0.0) {
        float phase = fract(uT*uFxFlashCopy.z);
        float burst = 1.0 - smoothstep(0.0, uFxFlashCopy.y, phase);
        col = mix(col, vec3(1.0), clamp(burst*uFxFlashCopy.x*maskValue(uv, uMaskFlashCopy, z1, uFxFlashCopy.w),0.0,1.0));
    }
    if (uFxToneCopy.x > 0.0) {
        vec2 grid = fract(P * uFxToneCopy.y);
        float dotTone = 1.0 - smoothstep(0.1, 0.38, length(grid-0.5));
        float pulse = 0.9 + 0.1*sin(TAU*uT*uFxToneCopy.z);
        float k = uFxToneCopy.x*maskValue(uv, uMaskToneCopy, z1, uFxToneCopy.w);
        col = mix(col, vec3(dotTone*0.10 + (1.0-dotTone)*0.98)*pulse, k);
    }
    if (uFxGrainCopy.x > 0.0) {
        float n = hash12(floor(P*uFxGrainCopy.y)+vec2(floor(mod(uT*uFxGrainCopy.z*12.0,12.0)),0.0));
        col += (n-0.5)*uFxGrainCopy.x*maskValue(uv, uMaskGrainCopy, z1, uFxGrainCopy.w);
    }
    if (uFxInkCopy.x > 0.0) {
        float ink = smoothstep(uFxInkCopy.y-0.035,uFxInkCopy.y+0.035,luma(col));
        col = mix(col,vec3(ink),uFxInkCopy.x*maskValue(uv, uMaskInkCopy, z1, uFxInkCopy.w));
    }


    // Segunda familia de efectos: máscara y dirección ajustables en el editor.
    if (uFxComet.x > 0.0) {
        vec2 g = oriented(P, uMotionComet) * vec2(17.0, 7.0);
        g.y -= uT * uFxComet.z * 6.0;
        vec2 id = floor(g), f = fract(g);
        float presence = step(0.87, hash12(vec2(id.x, mod(id.y,6.0))));
        float x = 0.15 + 0.7*hash12(id+7.0);
        float trail = exp(-abs(f.x-x)*105.0) * smoothstep(0.05, 0.3, f.y) * (1.0-smoothstep(0.6,1.0,f.y));
        col += vec3(0.48,0.82,1.0)*trail*presence*uFxComet.y*uFxComet.x*
            maskValue(uv,uMaskComet,z1,uFxComet.w)*emitArea(P,uMotionComet);
    }
    if (uFxRunes.x > 0.0) {
        vec2 q = P-uMotionRunes.zw*vec2(uAspect,1.0);
        float radius = length(q), ang = atan(q.y,q.x)+TAU*uT*uFxRunes.z;
        float r = 0.17*uFxRunes.y;
        float ring = (1.0-smoothstep(0.002,0.009,abs(radius-r)));
        float marks = (1.0-smoothstep(0.01,0.045,abs(radius-r*0.84)))*step(0.7,fract(ang*12.0/TAU));
        float rim = ring + marks;
        col += vec3(0.56,0.79,1.0)*rim*uFxRunes.x*maskValue(uv,uMaskRunes,z1,uFxRunes.w);
    }
    if (uFxHatch.x > 0.0) {
        float lines = abs(fract((P.x+P.y)*uFxHatch.y + uT*uFxHatch.z)-0.5);
        float hatch = (1.0-smoothstep(0.02,0.045,lines));
        col = mix(col, vec3(uMono>0.5?0.0:0.13),hatch*uFxHatch.x*
            maskValue(uv,uMaskHatch,z1,uFxHatch.w));
    }
    if (uFxPulse.x > 0.0) {
        vec2 q=P-uMotionPulse.zw*vec2(uAspect,1.0);
        float d=length(q), phase=fract(uT*uFxPulse.z);
        float rings=0.0;
        for(int k=0;k<3;k++) {
            float r=fract(phase+float(k)/3.0)*0.65;
            rings+= (1.0-smoothstep(uFxPulse.y*0.25,uFxPulse.y,abs(d-r)))*(1.0-r);
        }
        col += vec3(0.52,0.88,1.0)*rings*uFxPulse.x*maskValue(uv,uMaskPulse,z1,uFxPulse.w);
    }
    if (uFxSmoke.x > 0.0 && uSmokeControl.w < 0.5)
        col = zetaSmoke(col, P, uv, z1);
    if (uFxShards.x > 0.0) {
        vec2 g=oriented(P,uMotionShards)*9.0;
        g.y-=uT*uFxShards.z*6.0;
        vec2 id=floor(g),f=fract(g);
        vec2 h=hash22(vec2(id.x,mod(id.y,6.0))+34.0);
        vec2 q=f-(0.2+0.6*h);
        float diamond=abs(q.x)+abs(q.y)*1.6;
        float v=1.0-smoothstep(uFxShards.y*0.035,uFxShards.y*0.06,diamond);
        col+=vec3(0.65,0.91,1.0)*v*step(0.62,h.x)*uFxShards.x*
            maskValue(uv,uMaskShards,z1,uFxShards.w)*emitArea(P,uMotionShards);
    }

    // Fuego por zona, ojos brillantes y cierre de sombra. Solo se calculan activos.
    if (uFxFlame.x > 0.0) col = zetaFlame(col, P, uv);
    if (uFxEyes.x > 0.0) col = zetaEyes(col, uv);
    if (uFxBlink.x > 0.0) {
        float phase = fract(uT * uFxBlink.z);
        float width = max(0.02, uFxBlink.y);
        float close = 1.0 - smoothstep(0.0, width, abs(phase - 0.5));
        col *= 1.0 - clamp(close * uFxBlink.x *
            maskValue(uv, uMaskBlink, z1, 1.0), 0.0, 1.0);
    }

    // v30: efectos de fondo localizados. Son baratos y solo calculan si están activos.
    if (uFxBokeh.x > 0.0) {
        vec2 g = P * vec2(7.5, 9.5);
        vec2 id = floor(g), f = fract(g) - 0.5;
        vec2 h = hash22(id + 17.0);
        float bph = TAU * (uT * uFxBokeh.z + h.y);
        vec2 center = (h - 0.5) * 0.48 + vec2(sin(bph), cos(bph)) * 0.08;
        float r = length(f - center);
        float b = smoothstep(0.31, 0.02, r) * step(0.50, h.x);
        float tw = 0.55 + 0.45 * sin(TAU * (uT * uFxBokeh.z + h.y));
        float style = uFxBokeh.y;
        vec3 bc = vec3(1.0, 0.83, 0.58);
        if (style > 0.5 && style < 1.5) bc = vec3(1.0, 0.58, 0.77);
        else if (style > 1.5 && style < 2.5) bc = vec3(0.55, 0.78, 1.0);
        else if (style > 2.5) bc = mix(vec3(0.63,0.52,1.0), vec3(0.45,1.0,0.84), h.y);
        float a = b * tw * uFxBokeh.x * maskValue(uv, uMaskBokeh, z1, uFxBokeh.w) * 0.46;
        col += bc * a;
    }
    if (uFxSparkle.x > 0.0) {
        vec2 g = P * 13.0;
        vec2 id = floor(g), f = fract(g) - 0.5;
        vec2 h = hash22(id + 61.0);
        float on = step(0.78, h.x);
        float pulse = pow(0.5 + 0.5 * sin(TAU * (uT * uFxSparkle.z + h.y)), 5.0);
        float sx = exp(-abs(f.x - (h.x - 0.5) * 0.45) * (55.0 / max(0.3,uFxSparkle.y)));
        float sy = exp(-abs(f.y - (h.y - 0.5) * 0.45) * (55.0 / max(0.3,uFxSparkle.y)));
        float s = max(sx * exp(-abs(f.y)*8.0), sy * exp(-abs(f.x)*8.0));
        float a = s * on * pulse * uFxSparkle.x * maskValue(uv,uMaskSparkle,z1,uFxSparkle.w);
        col += vec3(1.0,0.96,0.82) * a * 0.72;
    }
    if (uFxLightleak.x > 0.0) {
        float ph = 0.5 + 0.5 * sin(TAU * uT * uFxLightleak.z);
        float side = mix(-0.12, 1.12, ph);
        float band = exp(-abs(uv.x - side) * 4.2) * (0.55 + 0.45 * smoothstep(1.0,0.0,uv.y));
        float style = uFxLightleak.y;
        vec3 lc = vec3(1.0,0.55,0.30);
        if (style > 0.5 && style < 1.5) lc = vec3(1.0,0.43,0.72);
        else if (style > 1.5 && style < 2.5) lc = vec3(0.32,0.72,1.0);
        else if (style > 2.5) lc = vec3(0.62,0.48,1.0);
        float a = band * uFxLightleak.x * maskValue(uv,uMaskLightleak,z1,uFxLightleak.w) * 0.42;
        col += lc * a;
    }
    if (uFxWaterfall.x > 0.0) {
        vec2 q = P * vec2(10.0 + uFxWaterfall.y, 5.0);
        float wph = TAU * uT * uFxWaterfall.z;
        float n = fbm(vec2(q.x*0.42 + sin(wph)*0.28, q.y*0.22 + cos(wph)*0.28));
        float streak = pow(0.5 + 0.5 * sin(q.x * 3.1 + n*8.0 + wph), 6.0);
        float foam = pow(0.5 + 0.5 * sin(q.y * 3.4 + q.x * 0.7 - wph), 8.0);
        float m = maskValue(uv,uMaskWaterfall,z1,uFxWaterfall.w);
        col = mix(col, mix(col, vec3(0.76,0.91,1.0), 0.42), clamp((streak*0.30+foam*0.24)*uFxWaterfall.x*m,0.0,0.62));
    }
    if (uFxReflect.x > 0.0) {
        float s = max(0.3, uFxReflect.y);
        float bands = pow(0.5 + 0.5 * sin(P.y * 125.0 / s + sin(P.x*24.0) * 1.4 - uT*uFxReflect.z*TAU), 7.0);
        float rph = TAU*uT*uFxReflect.z;
        float broken = smoothstep(0.42,0.76,vnoise(P*vec2(28.0,13.0)+vec2(sin(rph),cos(rph))*0.35));
        float a = bands * broken * uFxReflect.x * maskValue(uv,uMaskReflect,z1,uFxReflect.w) * 0.42;
        vec3 naturalHighlight = mix(col, vec3(1.0), 0.48);
        col = mix(col, naturalHighlight, clamp(a,0.0,0.55));
    }
    if (uFxBlackaura.x > 0.0) {
        float ph = TAU * uT * uFxBlackaura.z;
        float n = fbm(P * max(0.5,uFxBlackaura.y) + vec2(sin(ph), cos(ph))*0.32);
        float wisps = smoothstep(0.42,0.79,n);
        float m = maskValue(uv,uMaskBlackaura,z1,uFxBlackaura.w);
        col = mix(col, tintOr(vec3(0.035,0.025,0.055), uTintBlackaura), clamp(wisps*uFxBlackaura.x*m*0.72,0.0,0.78));
    }
    if (uFxInkdrip.x > 0.0) {
        vec2 g = P * vec2(14.0/max(0.4,uFxInkdrip.y), 7.0);
        vec2 id = floor(g), f = fract(g);
        float h = hash12(vec2(id.x, 17.0));
        float len = 0.20 + 0.70*h;
        float iph = TAU * (uT*uFxInkdrip.z + h);
        float y = fract(f.y + 0.16*sin(iph) + h);
        float drip = step(y, len) * (1.0-smoothstep(0.04,0.20,abs(f.x-0.5))) * step(0.57,h);
        float m = maskValue(uv,uMaskInkdrip,z1,uFxInkdrip.w);
        col = mix(col, vec3(0.015), clamp(drip*uFxInkdrip.x*m*0.82,0.0,0.88));
    }
    if (uFxDramatic.x > 0.0) {
        float width = clamp(uFxDramatic.y,0.2,1.0);
        float edge = 1.0 - smoothstep(0.0, width, length((uv-0.5)*vec2(0.86,1.0)));
        float pulse = 0.76 + 0.24*sin(TAU*uT*uFxDramatic.z);
        float m = maskValue(uv,uMaskDramatic,z1,uFxDramatic.w);
        float a = (1.0-edge) * pulse * uFxDramatic.x * m * 0.68;
        col = mix(col, vec3(0.03,0.02,0.04), clamp(a,0.0,0.75));
    }

    if (uTornado.x > 0.0) col = tornadoLayer(col, P, maskValue(uv, uMaskTornado, 1.0, 0.0));
    if (uGust.x > 0.0) col = gustLayer(col, P, windowEnv(uGust.y, uGust.z) * uGust.x, maskValue(uv, uMaskGust, 1.0, 0.0));
    if (uBolt.x > 0.0) col = boltLayer(col, P, maskValue(uv, uMaskBolt, 1.0, 0.0));
    if (uFxBlast.x > 0.0) col = zetaBlast(col, P, uv, z1);
    if (uFxBlood.x > 0.0) col = zetaBlood(col, P, uv, z1);
    if (uFxSlash.x > 0.0) col = zetaSlash(col, P, uv, z1);
    if (uFxReveal.x > 0.0) col = zetaReveal(col, P, uv, z1);
    col=studioElements(col,uvFrame);
    if (uAppear.x > 0.0) col = appearLayer(col, P);
    if (uFxSmoke.x > 0.0 && uSmokeControl.w >= 0.5)
        col = zetaSmoke(col, P, uv, z1);
    if (uImpact.x > 0.0 && uImpactMore.x > 0.0) col = mix(col, vec3(1.0), clamp(impactE * impactE * impactE * uImpactMore.x * 0.8, 0.0, 1.0));
    col=gradeColor(col,uvFrame);
    // Dither suave para evitar bandas en cielos y degradados
    col += (hash12(gl_FragCoord.xy) - 0.5) / 255.0;
    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
