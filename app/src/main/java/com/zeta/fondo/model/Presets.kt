package com.zeta.fondo.model

data class Preset(
    val emoji: String,
    val name: String,
    val hint: String,
    val values: Map<String, Float>,
    val manga: Boolean = false
)

object Presets {
    val all: List<Preset> = listOf(
        Preset("☁️", "Humo negro dramático", "Ajusta Humo > Negro y pinta únicamente el área del humo; no tapes la cara.",
            mapOf("smoke.on" to 1f, "smoke.int" to .67f, "smoke.variant" to 1f,
                "smoke.density" to 1.3f, "smoke.rise" to .45f, "smoke.cycles" to 1f)),
        Preset("🌫️", "Humo blanco suave", "Niebla envolvente con capas delicadas; pinta una máscara para conservar rostros.",
            mapOf("smoke.on" to 1f, "smoke.int" to .45f, "smoke.variant" to 2f,
                "smoke.density" to .8f, "smoke.rise" to .45f, "smoke.cycles" to 1f)),
        Preset("💥", "Explosión anime", "Coloca el centro de la explosión y ajusta humo, radio y chispas.",
            mapOf("blast.on" to 1f, "blast.int" to .9f, "blast.variant" to 0f,
                "blast.smoke" to .65f, "blast.sparks" to .45f)),
        Preset("⚡", "Corte manhwa", "Coloca el centro del corte. Combina estela azul y brillos.",
            mapOf("slash.on" to 1f, "slash.int" to .85f, "slash.variant" to 0f,
                "sparkle.on" to 1f, "sparkle.int" to .25f)),
        Preset("🩸", "Impacto de tinta", "Salpicadura estilo manga; usa Tinta negra o Roja y coloca su origen.",
            mapOf("blood.on" to 1f, "blood.int" to .75f, "blood.variant" to 1f,
                "speed.on" to 1f, "speed.int" to .18f)),
        Preset("◑", "Aparición entre humo", "Pinta el contorno que se revela. No separa al personaje del fondo sin PNG.",
            mapOf("reveal.on" to 1f, "reveal.int" to .8f, "reveal.variant" to 0f,
                "smoke.on" to 1f, "smoke.variant" to 1f, "smoke.int" to .35f)),
        Preset("☁️", "Cielo manhwa vivo", "Pinta solo el cielo y separa las capas si hay edificios o personaje.",
            mapOf("clouds3.on" to 1f, "clouds3.int" to .55f, "clouds3.cycles" to 1f,
                "rays.on" to 1f, "rays.int" to .24f)),
        Preset("👁️", "Ojo entre brasas", "Pinta el ojo para localizar el resplandor. Añade brasas flotantes.",
            mapOf("eyes.on" to 1f, "eyes.int" to .8f,
                "embers.on" to 1f, "embers.int" to .35f)),
        Preset(
            "🏞️", "Río que fluye",
            "Pinta el río en Zona 1 y dibuja flechas en la dirección del agua. Congela orillas y personajes.",
            mapOf(
                "flow.on" to 1f, "flow.amount" to 0.3f, "flow.cycles" to 2f,
                "ripple.on" to 1f, "ripple.amp" to 0.0015f, "ripple.freq" to 60f,
                "glint.on" to 1f, "glint.int" to 0.7f
            )
        ),
        Preset(
            "🌊", "Mar tranquilo",
            "Pinta el mar en Zona 1. Congela la orilla, barcos y personajes.",
            mapOf(
                "ripple.on" to 1f, "ripple.amp" to 0.003f, "ripple.freq" to 35f, "ripple.cycles" to 2f,
                "glint.on" to 1f, "glint.int" to 1f, "glint.dens" to 90f,
                "drift.on" to 1f, "drift.dir" to 0f, "drift.speed" to 0.02f
            )
        ),
        Preset(
            "💧", "Cascada",
            "Dibuja flechas hacia abajo sobre la cascada y congela las rocas de los lados.",
            mapOf(
                "flow.on" to 1f, "flow.amount" to 0.5f, "flow.cycles" to 3f,
                "glint.on" to 1f, "glint.int" to 0.6f, "glint.dens" to 110f
            )
        ),
        Preset(
            "☁️", "Nubes en movimiento",
            "Pinta el cielo en Zona 1. Congela montañas, edificios y personajes.",
            mapOf("drift.on" to 1f, "drift.dir" to 0f, "drift.speed" to 0.05f, "flow.cycles" to 1f)
        ),
        Preset(
            "🌅", "Atardecer time-lapse",
            "Pinta el cielo en Zona 1. Las nubes avanzan rápido y entra luz dorada.",
            mapOf(
                "drift.on" to 1f, "drift.speed" to 0.12f, "flow.cycles" to 2f,
                "rays.on" to 1f, "rays.int" to 0.45f, "rays.x" to 0.7f
            )
        ),
        Preset(
            "🌌", "Noche estrellada",
            "Pinta el cielo en Zona 1: las estrellas solo aparecen ahí.",
            mapOf("stars.on" to 1f, "stars.int" to 1.1f, "stars.dens" to 45f, "stars.zone" to 1f)
        ),
        Preset(
            "🌳", "Viento en árboles",
            "Pinta el follaje en Zona 2. Congela troncos, casas y personajes.",
            mapOf("sway.on" to 1f, "sway.amp" to 0.004f, "sway.freq" to 6f, "sway.cycles" to 2f)
        ),
        Preset(
            "🌾", "Hierba ondulando",
            "Pinta la hierba en Zona 2.",
            mapOf("sway.on" to 1f, "sway.amp" to 0.003f, "sway.freq" to 14f, "sway.cycles" to 3f)
        ),
        Preset(
            "🌧️", "Lluvia",
            "Si pintas los charcos o el agua en Zona 1, también se ondulan.",
            mapOf(
                "rain.on" to 1f, "rain.int" to 0.7f, "rain.cycles" to 8f,
                "ripple.on" to 1f, "ripple.amp" to 0.001f, "ripple.freq" to 90f
            )
        ),
        Preset(
            "⛈️", "Tormenta",
            "Lluvia fuerte e inclinada con bruma.",
            mapOf(
                "rain.on" to 1f, "rain.int" to 1f, "rain.angle" to 20f, "rain.cycles" to 12f,
                "fog.on" to 1f, "fog.int" to 0.25f
            )
        ),
        Preset("❄️", "Nieve", "Nieve cayendo sobre toda la imagen.", mapOf("snow.on" to 1f)),
        Preset(
            "🌸", "Sakura",
            "Pétalos cayendo. Si pintas los árboles en Zona 2, también se mecen.",
            mapOf("sakura.on" to 1f, "sway.on" to 1f, "sway.amp" to 0.002f)
        ),
        Preset("💫", "Luciérnagas", "Ideal para noches de verano.", mapOf("flies.on" to 1f)),
        Preset("🌫️", "Niebla matinal", "Bruma que se desplaza despacio.", mapOf("fog.on" to 1f, "fog.int" to 0.4f)),
        Preset("🔥", "Fuego", "Pinta las llamas en Zona 2.", mapOf("heat.on" to 1f)),
        Preset("☀️", "Rayos de luz", "Mueve el origen en Efectos › Rayos de luz.", mapOf("rays.on" to 1f)),
        Preset("🎥", "Zoom lento", "Acercamiento suave que vuelve al inicio.", mapOf("cam.on" to 1f, "cam.zoom" to 0.06f)),

        // v24 · Biblioteca rápida para fondos anime/manhwa. Son combinaciones editables, no filtros cerrados.
        Preset("🍃", "Brisa suave", "Viento ligero casi continuo, con pocas motas. Ideal para fondos tranquilos.",
            mapOf("gust.on" to 1f, "gust.int" to .25f, "gust.debris" to .12f, "gust.cycles" to 2f,
                "gust.start" to 0f, "gust.dur" to 1f, "gust.variant" to 0f)),
        Preset("💨", "Viento fuerte", "Ráfagas visibles con más velocidad y hojas/polvo.",
            mapOf("gust.on" to 1f, "gust.int" to 1.0f, "gust.debris" to .62f, "gust.cycles" to 5f,
                "gust.start" to 0f, "gust.dur" to 1f, "gust.variant" to 2f)),
        Preset("🌪️", "Viento de tormenta", "Viento muy fuerte con niebla y polvo para escenas agresivas.",
            mapOf("gust.on" to 1f, "gust.int" to 1.35f, "gust.debris" to .9f, "gust.cycles" to 7f,
                "gust.start" to 0f, "gust.dur" to 1f, "gust.variant" to 3f,
                "fog.on" to 1f, "fog.int" to .22f, "dust.on" to 1f, "dust.int" to .25f)),
        Preset("🌳", "Árbol con brisa", "Pinta hojas y ramas finas. La raíz queda fija y el movimiento es pequeño.",
            mapOf("natural.on" to 1f, "natural.variant" to 0f, "natural.int" to .0038f,
                "natural.direction" to 0f, "natural.cycles" to 2f, "natural.flex" to .55f,
                "natural.variation" to .78f, "natural.gust" to .18f, "natural.anchor" to 3f, "natural.mask" to 2f)),
        Preset("🌲", "Árbol viento fuerte", "Para copas y ramas flexibles; mantiene el tronco estable.",
            mapOf("natural.on" to 1f, "natural.variant" to 0f, "natural.int" to .0105f,
                "natural.direction" to 0f, "natural.cycles" to 3f, "natural.flex" to .72f,
                "natural.variation" to .88f, "natural.gust" to .62f, "natural.anchor" to 3f, "natural.mask" to 2f)),

        Preset("🍃", "Hojas verdes", "Hojas verdes ligeras cruzando la escena.",
            mapOf("leaves.on" to 1f, "leaves.variant" to 0f, "leaves.int" to .72f,
                "leaves.size" to .9f, "leaves.cycles" to 2f, "leaves.direction" to 135f)),
        Preset("🍂", "Hojas secas", "Hojas marrones y amarillas para escenas secas o de otoño.",
            mapOf("leaves.on" to 1f, "leaves.variant" to 1f, "leaves.int" to .82f,
                "leaves.size" to 1.0f, "leaves.cycles" to 3f, "leaves.direction" to 135f)),
        Preset("🍁", "Hojas de otoño", "Tonos naranja y rojo con movimiento algo más vivo.",
            mapOf("leaves.on" to 1f, "leaves.variant" to 2f, "leaves.int" to .9f,
                "leaves.size" to 1.05f, "leaves.cycles" to 3f, "leaves.direction" to 145f)),
        Preset("🎄", "Hojas de Navidad", "Verde oscuro con pequeños acentos rojos, pensado para fondos navideños.",
            mapOf("leaves.on" to 1f, "leaves.variant" to 3f, "leaves.int" to .62f,
                "leaves.size" to .85f, "leaves.cycles" to 2f, "leaves.direction" to 135f)),
        Preset("🎄", "Navidad con nieve", "Nieve suave, hojas navideñas y destellos discretos.",
            mapOf("snow.on" to 1f, "snow.variant" to 0f, "snow.int" to .45f, "snow.size" to .85f,
                "snow.cycles" to 1f, "leaves.on" to 1f, "leaves.variant" to 3f, "leaves.int" to .35f,
                "leaves.cycles" to 1f, "stars.on" to 1f, "stars.int" to .35f, "stars.dens" to 30f)),

        Preset("🌊", "Olas suaves", "Espuma lenta y ondas pequeñas para una playa tranquila.",
            mapOf("shore.on" to 1f, "shore.variant" to 0f, "shore.int" to .35f, "shore.freq" to 2f,
                "shore.cycles" to 1f, "ripple.on" to 1f, "ripple.amp" to .0015f, "ripple.freq" to 46f, "ripple.cycles" to 1f)),
        Preset("🌊", "Olas fuertes", "Oleaje más alto y espuma rápida.",
            mapOf("shore.on" to 1f, "shore.variant" to 2f, "shore.int" to 1.05f, "shore.freq" to 5f,
                "shore.cycles" to 3f, "ripple.on" to 1f, "ripple.amp" to .006f, "ripple.freq" to 26f, "ripple.cycles" to 4f)),
        Preset("🌊", "Mar de tormenta", "Oleaje fuerte, niebla y lluvia inclinada.",
            mapOf("shore.on" to 1f, "shore.variant" to 3f, "shore.int" to 1.35f, "shore.freq" to 7f,
                "shore.cycles" to 5f, "ripple.on" to 1f, "ripple.amp" to .009f, "ripple.freq" to 20f, "ripple.cycles" to 5f,
                "rain.on" to 1f, "rain.variant" to 3f, "rain.int" to .9f, "rain.angle" to 28f, "rain.cycles" to 13f,
                "fog.on" to 1f, "fog.int" to .25f)),

        Preset("🌦️", "Lluvia suave", "Gotas finas para escenas tranquilas o melancólicas.",
            mapOf("rain.on" to 1f, "rain.variant" to 0f, "rain.int" to .30f, "rain.angle" to 5f, "rain.cycles" to 4f)),
        Preset("🌧️", "Lluvia fuerte", "Más densidad, velocidad e inclinación.",
            mapOf("rain.on" to 1f, "rain.variant" to 2f, "rain.int" to .95f, "rain.angle" to 18f, "rain.cycles" to 11f)),
        Preset("❄️", "Nieve suave", "Pocos copos grandes y lentos.",
            mapOf("snow.on" to 1f, "snow.variant" to 0f, "snow.int" to .35f, "snow.size" to .75f, "snow.cycles" to 1f)),
        Preset("🌨️", "Nieve fuerte", "Más copos y velocidad, manteniendo un movimiento natural.",
            mapOf("snow.on" to 1f, "snow.variant" to 2f, "snow.int" to 1f, "snow.size" to 1.15f, "snow.cycles" to 4f,
                "snow.direction" to 105f)),
        Preset("🌬️", "Ventisca", "Nieve rápida y diagonal con niebla fría.",
            mapOf("snow.on" to 1f, "snow.variant" to 3f, "snow.int" to 1.15f, "snow.size" to .70f,
                "snow.cycles" to 8f, "snow.direction" to 135f, "fog.on" to 1f, "fog.int" to .32f)),

        Preset("☁️", "Nubes suaves", "Capas lentas de nubes con profundidad y luz discreta.",
            mapOf("clouds3.on" to 1f, "clouds3.int" to .45f, "clouds3.scale" to 1.8f, "clouds3.cycles" to 1f,
                "rays.on" to 1f, "rays.int" to .12f)),
        Preset("🌫️", "Niebla baja", "Bruma suave para calles, bosques y montañas.",
            mapOf("fog.on" to 1f, "fog.int" to .28f, "fog.scale" to 4.5f, "fog.cycles" to 1f)),
        Preset("✨", "Polvo de luz", "Motas lentas y destellos suaves para interiores o atardeceres.",
            mapOf("dust.on" to 1f, "dust.int" to .32f, "dust.size" to .75f, "dust.cycles" to 1f,
                "rays.on" to 1f, "rays.int" to .10f)),
        Preset("🔥", "Fuego y brasas", "Llamas localizadas con chispas ascendentes.",
            mapOf("flame.on" to 1f, "flame.int" to .72f, "flame.size" to 1.15f, "flame.cycles" to 3f,
                "embers.on" to 1f, "embers.int" to .8f, "embers.size" to .85f, "embers.cycles" to 4f,
                "embers.direction" to 270f)),
        Preset("🌙", "Noche mágica", "Estrellas, luciérnagas y niebla muy ligera.",
            mapOf("stars.on" to 1f, "stars.int" to .85f, "stars.dens" to 48f,
                "flies.on" to 1f, "flies.int" to .65f, "flies.dens" to 7f, "fog.on" to 1f, "fog.int" to .12f)),

        // Escenas anime, originales y totalmente editables.
        Preset("⚡", "Despertar de energía", "Coloca el origen sobre el personaje y ajusta la electricidad.",
            mapOf("energy.on" to 1f, "energy.int" to 1.1f, "electric.on" to 1f, "electric.int" to 0.8f)),
        Preset("💥", "Golpe de impacto", "Onda de choque, líneas y destello para batallas.",
            mapOf("shock.on" to 1f, "shock.int" to 1f, "flash.on" to 1f, "flash.int" to 0.4f,
                "focus.on" to 1f, "focus.int" to 0.4f)),
        Preset("🍃", "Viento de otoño", "Hojas desde la derecha superior, editables en Efectos.",
            mapOf("leaves.on" to 1f, "leaves.origin" to 6f, "leaves.direction" to 135f,
                "sway.on" to 1f, "sway.amp" to 0.003f)),
        Preset("🌋", "Brasas ascendentes", "Elige abajo como origen y controla su velocidad.",
            mapOf("embers.on" to 1f, "embers.origin" to 2f, "embers.direction" to 270f,
                "ash.on" to 1f, "ash.int" to 0.35f)),
        Preset("🌊", "Bajo el agua", "Ondas, burbujas y brillos para anime submarino.",
            mapOf("bubbles.on" to 1f, "bubbles.origin" to 2f, "bubbles.direction" to 270f,
                "ripple.on" to 1f, "glint.on" to 1f)),
        Preset("🌫️", "Calle polvorienta", "Polvo y niebla ambiental suave.",
            mapOf("dust.on" to 1f, "dust.int" to 0.45f, "fog.on" to 1f, "fog.int" to 0.23f)),
        Preset("💨", "Carrera extrema", "Líneas de velocidad con impacto y cámara.",
            mapOf("speed.on" to 1f, "speed.int" to 0.9f, "focus.on" to 1f,
                "focus.int" to 0.3f, "cam.on" to 1f, "cam.zoom" to 0.09f)),

        // Presets rápidos para fondos de anime/manhwa: pocos controles y bucle cerrado.
        Preset("◫", "Fondo con profundidad", "Pinta fondo azul y primer plano verde. Añade parallax y un zoom muy suave.",
            mapOf("parallax.on" to 1f, "parallax.amount" to 0.024f, "parallax.direction" to 0f,
                "parallax.cycles" to 1f, "parallax.depth" to 0.65f,
                "cam.on" to 1f, "cam.zoom" to 0.025f, "cam.cycles" to 1f)),
        Preset("🌳", "Árbol natural", "Pinta hojas y ramas finas en verde; la base queda fija y el movimiento cambia ligeramente con cada zona.",
            mapOf("natural.on" to 1f, "natural.int" to 0.0065f, "natural.direction" to 0f,
                "natural.cycles" to 2f, "natural.flex" to 0.62f, "natural.variation" to 0.82f,
                "natural.gust" to 0.38f, "natural.variant" to 0f, "natural.anchor" to 3f, "natural.mask" to 2f)),
        Preset("🚩", "Bandera realista", "Pinta solo la bandera en verde. El borde izquierdo queda sujeto al poste; puedes cambiarlo a derecha si hace falta.",
            mapOf("natural.on" to 1f, "natural.int" to 0.009f, "natural.direction" to 0f,
                "natural.cycles" to 2f, "natural.flex" to 0.92f, "natural.variation" to 0.48f,
                "natural.gust" to 0.52f, "natural.variant" to 1f, "natural.anchor" to 1f, "natural.mask" to 2f)),
        Preset("🌾", "Pasto al viento", "Pinta el pasto en verde. La base se mantiene quieta y cada grupo se balancea con una fase distinta.",
            mapOf("natural.on" to 1f, "natural.int" to 0.0048f, "natural.direction" to 0f,
                "natural.cycles" to 3f, "natural.flex" to 0.72f, "natural.variation" to 0.88f,
                "natural.gust" to 0.30f, "natural.variant" to 2f, "natural.anchor" to 3f, "natural.mask" to 2f)),
        Preset("▥", "Cortina suave", "Pinta la cortina en verde. La parte superior queda fija y los pliegues reaccionan al viento.",
            mapOf("natural.on" to 1f, "natural.int" to 0.0055f, "natural.direction" to 0f,
                "natural.cycles" to 2f, "natural.flex" to 0.88f, "natural.variation" to 0.36f,
                "natural.gust" to 0.20f, "natural.variant" to 3f, "natural.anchor" to 4f, "natural.mask" to 2f)),
        Preset("〰", "Cabello natural", "Pinta únicamente los mechones que deben moverse; la raíz queda estable y las puntas reaccionan más.",
            mapOf("natural.on" to 1f, "natural.int" to 0.0048f, "natural.direction" to 0f,
                "natural.cycles" to 2f, "natural.flex" to 0.78f, "natural.variation" to 0.52f,
                "natural.gust" to 0.28f, "natural.variant" to 4f, "natural.anchor" to 4f, "natural.mask" to 2f)),
        Preset("👕", "Ropa al viento", "Pinta mangas, falda, capa o tela suelta. Mantiene la unión al cuerpo estable y deforma más el extremo libre.",
            mapOf("natural.on" to 1f, "natural.int" to 0.0042f, "natural.direction" to 0f,
                "natural.cycles" to 2f, "natural.flex" to 0.62f, "natural.variation" to 0.42f,
                "natural.gust" to 0.24f, "natural.variant" to 5f, "natural.anchor" to 4f, "natural.mask" to 2f)),
        Preset("〰", "Cabello al viento", "Pinta el cabello en Zona verde y aplica una ondulación corta y suave.",
            mapOf("mesh.on" to 1f, "mesh.int" to 0.0055f, "mesh.size" to 10f,
                "mesh.cycles" to 2f, "mesh.variant" to 0f, "mesh.direction" to 0f, "mesh.mask" to 2f)),
        Preset("👕", "Ropa suave", "Pinta la ropa en Zona verde. Dos ondas combinadas evitan un movimiento rígido.",
            mapOf("mesh.on" to 1f, "mesh.int" to 0.0045f, "mesh.size" to 8f,
                "mesh.cycles" to 2f, "mesh.variant" to 2f, "mesh.direction" to 0f, "mesh.mask" to 2f)),
        Preset("☁️", "Cielo vivo", "Nubes lentas, luz y cámara mínima para fondos tranquilos.",
            mapOf("drift.on" to 1f, "drift.speed" to 0.025f, "drift.dir" to 0f,
                "rays.on" to 1f, "rays.int" to 0.18f, "cam.on" to 1f,
                "cam.zoom" to 0.025f, "cam.cycles" to 1f)),
        Preset("🌧️", "Lluvia nocturna anime", "Lluvia, niebla ligera y estrellas suaves sin sobrecargar la escena.",
            mapOf("rain.on" to 1f, "rain.int" to 0.72f, "rain.cycles" to 8f,
                "fog.on" to 1f, "fog.int" to 0.2f, "stars.on" to 1f,
                "stars.int" to 0.45f, "stars.dens" to 36f)),
        Preset("🖋️", "Manga respirando", "Zoom mínimo y deformación suave para una viñeta sin destruir las tramas.",
            mapOf("style.mono" to 1f, "style.snap" to 1f,
                "cam.on" to 1f, "cam.zoom" to 0.02f, "cam.cycles" to 1f,
                "mesh.on" to 1f, "mesh.int" to 0.0025f, "mesh.size" to 12f,
                "mesh.cycles" to 1f, "mesh.variant" to 1f, "mesh.mask" to -2f)),

        // Manga blanco y negro
        Preset(
            "🖋️", "Lluvia en tinta",
            "Líneas de lluvia en blanco o negro según el fondo.",
            mapOf("style.mono" to 1f, "rain.on" to 1f, "rain.int" to 0.8f, "rain.cycles" to 10f),
            manga = true
        ),
        Preset(
            "🖋️", "Nieve manga",
            "Copos blancos sobre la página.",
            mapOf("style.mono" to 1f, "snow.on" to 1f),
            manga = true
        ),
        Preset(
            "🖋️", "Destellos",
            "Pinta el cielo o el fondo oscuro en Zona 1.",
            mapOf("style.mono" to 1f, "stars.on" to 1f, "stars.zone" to 1f, "stars.int" to 0.9f),
            manga = true
        ),
        Preset("▦", "Trama de tinta", "Screentone animado con impresión B/N.",
            mapOf("style.mono" to 1f, "tone.on" to 1f, "grain.on" to 1f, "grain.int" to 0.1f),
            manga = true),
        Preset("◎", "Enfoque dramático", "Líneas que convergen en el punto elegido.",
            mapOf("style.mono" to 1f, "focus.on" to 1f, "focus.int" to 1f, "ink.on" to 1f,
                "ink.int" to 0.18f), manga = true),
        Preset("◉", "Viñeta de choque", "Flash, choque y contraste en una escena manga.",
            mapOf("style.mono" to 1f, "shock.on" to 1f, "flash.on" to 1f,
                "flash.int" to 0.35f), manga = true),
        Preset(
            "🖋️", "Tramas intactas",
            "Mueve la imagen píxel por píxel para no deformar las tramas. Anima con flechas cortas.",
            mapOf("style.mono" to 1f, "style.snap" to 1f, "flow.amount" to 0.2f),
            manga = true
        )
    )

    /**
     * v36 · Escenas rápidas: un toque las activa con valores ya ajustados y abre el
     * efecto principal para personalizarlo. Usan las formas nuevas (no solo colores).
     */
    val scenes: List<Preset> = listOf(
        Preset("◑", "Aparición entre humo", "Pinta al personaje en Proteger: emerge del humo y el humo pasa por detrás.",
            mapOf("reveal.on" to 1f, "reveal.int" to .85f, "reveal.variant" to 0f, "reveal.shape" to 1f,
                "reveal.edge" to .8f, "reveal.mask" to 0f, "reveal.cycles" to 1f,
                "smoke.on" to 1f, "smoke.int" to .38f, "smoke.variant" to 1f, "smoke.shape" to 3f,
                "smoke.behind" to 1f, "smoke.mask" to -2f)),
        Preset("💥", "Explosión anime", "Coloca el centro: destello, bola de fuego, onda, chispas y humo por fases.",
            mapOf("blast.on" to 1f, "blast.int" to 1f, "blast.shape" to 0f, "blast.variant" to 0f,
                "blast.flash" to .85f, "blast.ring" to .75f, "blast.smoke" to .7f, "blast.sparks" to .65f,
                "blast.mask" to -2f, "impact.on" to 1f, "impact.int" to .08f, "impact.shake" to .5f)),
        Preset("☁", "Cielo animado", "Pinta el cielo en Zona azul: nubes en capas y luz suave.",
            mapOf("clouds3.on" to 1f, "clouds3.int" to .62f, "clouds3.cycles" to 1f, "clouds3.mask" to 1f,
                "rays.on" to 1f, "rays.int" to .18f, "rays.mask" to 1f)),
        Preset("◉", "Aura oscura", "Pinta al personaje en Proteger: llamas de aura y sombra detrás de él.",
            mapOf("energy.on" to 1f, "energy.shape" to 1f, "energy.variant" to 1f, "energy.int" to 1.05f,
                "energy.size" to .55f, "energy.mask" to -2f,
                "blackaura.on" to 1f, "blackaura.int" to .5f, "blackaura.behind" to 1f, "blackaura.mask" to -2f)),
        Preset("👁️", "Ojo brillante", "Pinta solo los ojos: brillo con halo y destello.",
            mapOf("eyes.on" to 1f, "eyes.int" to 1.1f, "eyes.shape" to 2f, "eyes.halo" to .65f,
                "embers.on" to 1f, "embers.int" to .25f)),
        Preset("✹", "Impacto manga", "Líneas de enfoque, golpe con temblor y destello.",
            mapOf("focus.on" to 1f, "focus.int" to .85f, "focus.mask" to -2f,
                "impact.on" to 1f, "impact.int" to .14f, "impact.shake" to .75f, "impact.flash" to .45f,
                "blood.on" to 1f, "blood.shape" to 2f, "blood.variant" to 1f, "blood.int" to .8f, "blood.mask" to -2f)),
        Preset("🍃", "Viento en cabello", "Pinta solo el cabello: raíz fija arriba y puntas con rebote.",
            mapOf("natural.on" to 1f, "natural.variant" to 4f, "natural.anchor" to 4f, "natural.int" to .0055f,
                "natural.cycles" to 2f, "natural.flex" to .8f, "natural.variation" to .6f, "natural.gust" to .45f,
                "natural.weight" to .4f, "natural.spring" to .7f, "natural.direction" to 0f,
                "gust.on" to 1f, "gust.int" to .3f, "gust.debris" to .2f, "gust.cycles" to 2f)),
        Preset("🔥", "Fuego y humo", "Pinta la base del fuego: llamas que suben, humo y brasas.",
            mapOf("flame.on" to 1f, "flame.int" to .95f, "flame.shape" to 0f, "flame.height" to .55f,
                "smoke.on" to 1f, "smoke.int" to .35f, "smoke.variant" to 0f, "smoke.shape" to 0f,
                "smoke.rise" to .9f, "smoke.direction" to 270f, "smoke.mask" to -2f,
                "embers.on" to 1f, "embers.int" to .55f)),
        Preset("☁", "Nubes detrás del personaje", "Pinta al personaje en Proteger: las nubes pasan por detrás.",
            mapOf("clouds3.on" to 1f, "clouds3.int" to .7f, "clouds3.scale" to 1.6f, "clouds3.mask" to -2f,
                "clouds3.behind" to 1f, "fog.on" to 1f, "fog.shape" to 1f, "fog.int" to .3f,
                "fog.behind" to 1f, "fog.mask" to -2f))
    )

    /** Efecto que se abre para ajustar después de aplicar la escena. */
    fun sceneMainEffect(p: Preset): String = when (p.name) {
        "Aparición entre humo" -> "reveal"
        "Explosión anime" -> "blast"
        "Cielo animado", "Nubes detrás del personaje" -> "clouds3"
        "Aura oscura" -> "energy"
        "Ojo brillante" -> "eyes"
        "Impacto manga" -> "focus"
        "Viento en cabello" -> "natural"
        "Fuego y humo" -> "flame"
        else -> p.values.keys.firstOrNull { it.endsWith(".on") }?.substringBefore('.') ?: "smoke"
    }
}
