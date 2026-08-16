#version 300 es

// Looks inside the ball through the window: refracts at the glass, ray-marches the liquid, refracts
// again at its surface, and intersects the answer die exactly as a convex solid bounded by its 20
// face planes.
//
// The liquid is the union of two things: an analytic plane, which carries the bulk level and stays
// perfectly smooth however few particles there are, and a coarse volume-fraction grid splatted from
// the SPH particles, which supplies the sloshing, the tongues and the splashes. Combining them with
// a smooth maximum means the surface is always well behaved — the grid only ever adds to it.
//
// Everything is in ball space: the ball sits at the origin and the camera looks down -Z from
// uCameraPos, which is where the camera ends up once the ball's own drift is taken out. The engine
// simulates in the same frame, so the die's position, the liquid grid and the bubbles all arrive
// ready to use. uWindowAxis arrives already turned by the shell's spin, so the hole this pass fills
// is the very one the shell cut.

precision highp float;
precision highp sampler3D;

in vec2 vNdc;

uniform vec3 uCameraPos;
uniform float uTanHalfFov;
uniform float uAspect;

uniform vec3 uWindowAxis;
uniform float uWindowCos;
uniform float uCavityRadius;

uniform vec3 uDieCenter;
uniform mat3 uDieRotation;
uniform float uDiePlaneDistance;
uniform vec3 uFaceNormals[20];

/**
 * Always twenty, and a uniform on purpose.
 *
 * A loop with a literal bound is one a driver will happily unroll, and this one is inlined at every
 * point the die is tested against. Unrolled, that is thousands of lines for the compiler to chew
 * through, and it showed up as the wait before the ball appeared. As a uniform the bound is unknowable
 * at compile time, so the loop stays a loop.
 */
uniform int uFaceCount;

/** Probes across the light the die's shadow is sampled with; the tier sets it, and one is hard. */
uniform int uShadowProbes;

uniform vec3 uLightDir;
uniform vec3 uUp;
uniform float uFluidSurfaceOffset;
uniform vec3 uLiquidColor;
uniform vec3 uDieColor;

/** One cell of answer text per face, white on transparent; only the alpha is read. */
uniform sampler2D uAnswerAtlas;
/** 0 until the atlas holds something worth drawing. */
uniform float uHasAnswers;
/** In-plane axes of each face: the tangent runs right along the text, the bitangent up. */
uniform vec3 uFaceTangents[20];
uniform vec3 uFaceBitangents[20];
/** Cells across and down the sheet. */
uniform ivec2 uAtlasCells;
/** Half the side of the text square on a face, in the die's own units. */
uniform float uTextHalfExtent;
/** Ink colour; the plastic underneath supplies the shading. */
uniform vec3 uTextColor;
/** Mip level for the atlas, chosen on the CPU — see below. */
uniform float uTextLod;

/** Volume fraction per voxel over the whole cavity, x fastest. */
uniform sampler3D uDensity;
/** 0 before the first simulation step has filled the grid, 1 afterwards. */
uniform float uHasDensity;
/** Volume fraction the surface sits at. */
uniform float uIsoLevel;
/** Turns a volume-fraction difference into the length the smooth maximum wants. */
uniform float uFieldScale;
/** World size of one voxel, used as the gradient step. */
uniform float uVoxel;
/** March budget, lowered on weaker devices. */
uniform int uMaxSteps;
/** Beer-Lambert coefficients per channel; red goes first, which is what makes the liquid blue. */
uniform vec3 uAbsorption;
/** How cloudy the liquid is, 0..1: absorption and scattering both rise with it. */
uniform float uTurbidity;
/** How much fine bubbling a shake has whipped up under the surface, 0..1. */
uniform float uFizz;
/** Seconds since the surface was created; only the fizz uses it. */
uniform float uTime;
/** How wet the glass above the waterline still is, 0..1; a shake soaks it and it drains. */
uniform float uWetness;

uniform vec4 uBubbles[12];
uniform int uBubbleCount;

out vec4 fragColor;

/** The air pocket above the liquid, seen through the window. */
const vec3 AIR_COLOR = vec3(0.022, 0.028, 0.042);
const vec3 BUBBLE_COLOR = vec3(0.66, 0.76, 0.92);
/** How softly the plane and the particle blobs are unioned, in world units. */
const float SMOOTH_K = 0.07;

/** How far up the glass the liquid climbs, and over what width, in world units. */
const float MENISCUS_RISE = 0.022;
const float MENISCUS_WIDTH = 0.12;

/** How steeply the edge of a printed letter falls away. */
const float INK_RELIEF = 1.2;

/** Wrap term for the die: how far past the terminator light still reaches through the plastic. */
const float DIE_WRAP = 0.45;

/** Fizz cells per world unit; roughly the size of the smallest analytic bubble. */
const float FIZZ_DENSITY = 26.0;

/** How hard a bulge in the liquid above a point focuses the light into a band. */
const float CAUSTIC_GAIN = 6.0;

/** How much light survives deep inside the die's own shadow. */
const float DIE_SHADOW_MIN = 0.44;

/**
 * Where a shadow in the liquid starts to give way to scattering and where it has gone entirely, as a
 * distance from the die's surface in world units. Past the far one the liquid is lit as if the die
 * were not there — which, a whole cavity of scattering blue later, is the truth.
 */
const float SHADOW_FADE_NEAR = 0.05;
const float SHADOW_FADE_FAR = 0.62;

/** How wide the penumbra probes spread, per unit of distance from the die. */
const float SHADOW_SPREAD = 0.13;

/** Drips around the glass, as a count per radian. */
const float STREAK_DENSITY = 2.6;

/** Near hit of a ray with a sphere at the origin, or -1.0 when it misses. */
float sphereEnter(vec3 origin, vec3 direction, float radius) {
    float b = dot(origin, direction);
    float c = dot(origin, origin) - radius * radius;
    float h = b * b - c;
    if (h < 0.0) return -1.0;
    return -b - sqrt(h);
}

/** Far hit of a ray starting inside a sphere at the origin. */
float sphereExit(vec3 origin, vec3 direction, float radius) {
    float b = dot(origin, direction);
    float c = dot(origin, origin) - radius * radius;
    float h = max(b * b - c, 0.0);
    return -b + sqrt(h);
}

struct DieHit {
    float t;
    vec3 normal;
    int face;
};

/**
 * Slab method over the 20 half-spaces of the icosahedron: the entry is the last plane the ray
 * crosses going in, the exit the first one going out.
 */
DieHit intersectDie(vec3 origin, vec3 direction) {
    DieHit hit;
    hit.t = -1.0;
    hit.normal = vec3(0.0);
    hit.face = -1;

    float tNear = -1e9;
    float tFar = 1e9;
    int nearFace = -1;

    for (int i = 0; i < uFaceCount; i++) {
        vec3 n = uFaceNormals[i];
        float denom = dot(direction, n);
        float dist = uDiePlaneDistance - dot(origin, n);
        if (abs(denom) < 1e-6) {
            // Parallel to this plane: outside it means the ray can never enter.
            if (dist < 0.0) return hit;
            continue;
        }
        float t = dist / denom;
        if (denom < 0.0) {
            if (t > tNear) {
                tNear = t;
                nearFace = i;
            }
        } else {
            tFar = min(tFar, t);
        }
        if (tNear > tFar) return hit;
    }

    if (tFar < 0.0 || nearFace < 0) return hit;
    hit.t = max(tNear, 0.0);
    hit.face = nearFace;
    hit.normal = uFaceNormals[nearFace];
    return hit;
}

/** How close a point on a face is to that face's edges, from 0 in the middle to 1 at the rim. */
float edgeProximity(vec3 point, int face) {
    float best = -1e9;
    for (int i = 0; i < uFaceCount; i++) {
        if (i == face) continue;
        best = max(best, dot(point, uFaceNormals[i]));
    }
    return clamp(best / uDiePlaneDistance, 0.0, 1.0);
}

/** Volume fraction at a point in the cavity. Explicit LOD: the march is in divergent control flow. */
float densityAt(vec3 point) {
    if (uHasDensity < 0.5) return 0.0;
    vec3 uvw = point / (2.0 * uCavityRadius) + 0.5;
    return textureLod(uDensity, uvw, 0.0).r;
}

/** Smooth maximum; the polynomial minimum with the correction flipped. */
float smoothMax(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (a - b) / k, 0.0, 1.0);
    return mix(b, a, h) + k * h * (1.0 - h);
}

/** Positive inside the liquid, negative outside; magnitude is roughly a distance. */
float insideness(vec3 point) {
    float bulk = uFluidSurfaceOffset - dot(point, uUp);
    // Capillary rise. Liquid wets glass, so the surface curves up where it meets the wall instead of
    // ending in a straight line — and the gradient picks that curve up for the normal for free.
    bulk += smoothstep(uCavityRadius - MENISCUS_WIDTH, uCavityRadius, length(point)) * MENISCUS_RISE;
    float lumps = (densityAt(point) - uIsoLevel) * uFieldScale;
    return smoothMax(bulk, lumps, SMOOTH_K);
}

/** Outward surface normal, i.e. away from the liquid, from the field's gradient. */
vec3 surfaceNormal(vec3 point) {
    float h = max(uVoxel, 0.008);
    vec3 gradient = vec3(
        insideness(point + vec3(h, 0.0, 0.0)) - insideness(point - vec3(h, 0.0, 0.0)),
        insideness(point + vec3(0.0, h, 0.0)) - insideness(point - vec3(0.0, h, 0.0)),
        insideness(point + vec3(0.0, 0.0, h)) - insideness(point - vec3(0.0, 0.0, h))
    );
    float len = length(gradient);
    if (len < 1e-5) return uUp;
    return -gradient / len;
}

/**
 * First crossing into the liquid along the ray, or -1.0 when the ray stays dry.
 *
 * Fixed steps rather than sphere tracing: the field is a blend of a plane and a filtered grid, so it
 * is not a true distance field and a distance-based step would happily march straight through a thin
 * sheet. A few bisections afterwards buy back most of the precision a fine step would have cost.
 */
float findSurface(vec3 origin, vec3 direction, float tMax) {
    if (tMax <= 0.0) return -1.0;

    float dt = tMax / float(uMaxSteps);
    for (int i = 1; i <= uMaxSteps; i++) {
        float t = float(i) * dt;
        if (insideness(origin + direction * t) > 0.0) {
            float dry = t - dt;
            float wet = t;
            for (int k = 0; k < 4; k++) {
                float mid = 0.5 * (dry + wet);
                if (insideness(origin + direction * mid) > 0.0) {
                    wet = mid;
                } else {
                    dry = mid;
                }
            }
            return 0.5 * (dry + wet);
        }
    }
    return -1.0;
}

/**
 * How much light reaches a point after crossing whatever liquid is above it, caustics included.
 *
 * The thickness alone says how much was swallowed. The *curvature* of that thickness says where it
 * was focused: a layer that bulges downwards is a converging lens, and the band of light under it is
 * the caustic. The second difference of the samples is that curvature, so the four probes pay for
 * both — and because the field carries the waves, the bands travel with them.
 */
float lightThroughLiquid(vec3 point) {
    vec3 toLight = normalize(uLightDir);
    float a1 = max(insideness(point + toLight * 0.13), 0.0);
    float a2 = max(insideness(point + toLight * 0.26), 0.0);
    float a3 = max(insideness(point + toLight * 0.39), 0.0);
    float a4 = max(insideness(point + toLight * 0.52), 0.0);

    float lit = 1.35 - (a1 + a2 + a3 + a4) * 1.6;
    float focus = clamp(-((a1 + a3) - 2.0 * a2) * CAUSTIC_GAIN, 0.0, 1.0);
    // Stirred liquid has sharper waves and so sharper caustics; still calm liquid keeps a faint set.
    return clamp(lit + focus * (0.35 + 0.65 * uFizz), 0.35, 2.0);
}

/**
 * Ink alpha at a point on face [face]'s text square, 0 anywhere outside it.
 *
 * The face's own basis is what makes the answer read level: `buildRevealTarget` on the engine side
 * turns the die so the chosen face's tangent lands on screen-right, which is the same axis the text
 * runs along here.
 *
 * Explicit LOD, like every other sample in this shader: the die is reached from inside the march, and
 * neighbouring fragments there may not even be looking at the same face.
 */
float answerAlpha(vec2 onFace, int face) {
    vec2 cellUv = onFace / (2.0 * uTextHalfExtent) + 0.5;
    if (any(lessThan(cellUv, vec2(0.0))) || any(greaterThan(cellUv, vec2(1.0)))) return 0.0;
    // The bitmap's first row is at v = 0 while the bitangent points up the face.
    cellUv.y = 1.0 - cellUv.y;

    vec2 cell = vec2(float(face % uAtlasCells.x), float(face / uAtlasCells.x));
    return textureLod(uAnswerAtlas, (cell + cellUv) / vec2(uAtlasCells), uTextLod).a;
}

/**
 * Ink coverage and the slope of its edge, as `(coverage, du, dv)` in the face's own axes.
 *
 * The letters are pressed into the plastic rather than painted on it, so what sells them is the edge:
 * two more taps of the atlas give the alpha's gradient, which becomes a bump in the normal and lets
 * the letters catch the light. Off the text the taps are skipped entirely, so the extra cost is paid
 * only where a ray actually hits ink.
 */
vec3 answerInk(vec3 localPoint, int face) {
    if (uHasAnswers < 0.5) return vec3(0.0);

    vec2 onFace = vec2(dot(localPoint, uFaceTangents[face]), dot(localPoint, uFaceBitangents[face]));
    float coverage = answerAlpha(onFace, face);
    if (coverage <= 0.0) return vec3(0.0);

    float h = uTextHalfExtent * 0.06;
    return vec3(
        coverage,
        answerAlpha(onFace + vec2(h, 0.0), face) - coverage,
        answerAlpha(onFace + vec2(0.0, h), face) - coverage
    );
}

vec3 shadeDie(vec3 localPoint, int face, vec3 normal, vec3 viewDir, float light) {
    vec3 ink = answerInk(localPoint, face);

    // Tilt the normal along the letter's edge: the groove's walls face away from its floor, which is
    // what makes the answer look printed into the plastic instead of masked over it.
    vec3 shadeNormal = normal;
    if (ink.x > 0.0) {
        vec3 worldTangent = normalize(uDieRotation * uFaceTangents[face]);
        vec3 worldBitangent = normalize(uDieRotation * uFaceBitangents[face]);
        shadeNormal = normalize(normal + (worldTangent * ink.y + worldBitangent * ink.z) * INK_RELIEF);
    }

    // Wrap lighting rather than plain Lambert: light gets a little way into white plastic before it
    // comes back out, so the terminator is soft and the die reads as milky instead of chalky.
    float wrapped = max((dot(shadeNormal, normalize(uLightDir)) + DIE_WRAP) / (1.0 + DIE_WRAP), 0.0);
    float facing = max(dot(shadeNormal, -viewDir), 0.0);

    float rim = smoothstep(0.86, 0.99, edgeProximity(localPoint, face));

    vec3 color = uDieColor * (0.34 + 0.66 * wrapped * light);
    // The answer is printed into the plastic, so it takes the same shading as the face around it.
    color = mix(color, uTextColor * (0.30 + 0.70 * wrapped * light), ink.x);
    // Pale bevel along the edges, the way the moulded plastic catches light.
    color = mix(color, vec3(0.88, 0.91, 0.96), rim * 0.75);
    color += vec3(pow(facing, 6.0) * 0.18);
    return color;
}

/** Value hash of a lattice cell, 0..1. */
float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

/**
 * The fine bubbling a shake whips up just under the surface.
 *
 * Too small and too many to simulate, so it is a drifting lattice of specks: one in fourteen cells
 * holds a bubble, and only the shallow ones show — a shake aerates the top of the liquid, not the
 * bottom of the cavity. Three samples along the ray is enough for a cloud.
 */
float fizzSparkle(vec3 origin, vec3 direction, float tMax) {
    if (uFizz <= 0.001 || tMax <= 0.0) return 0.0;

    float total = 0.0;
    for (int i = 1; i <= 3; i++) {
        vec3 point = origin + direction * (tMax * float(i) * 0.25);
        // The specks ride up with the liquid, so the lattice drifts along "up" as time passes.
        vec3 grid = (point - uUp * (uTime * 0.35)) * FIZZ_DENSITY;
        float seed = hash13(floor(grid));
        if (seed < 0.93) continue;

        // Round the cell off, or the fizz reads as voxels.
        float blob = 1.0 - smoothstep(0.15, 0.42, length(fract(grid) - 0.5));
        float shallow = 1.0 - smoothstep(0.05, 0.5, max(insideness(point), 0.0));
        total += blob * shallow;
    }
    return clamp(total * uFizz, 0.0, 1.0);
}

/** Lays the analytic bubbles over whatever the liquid segment already resolved to. */
vec3 compositeBubbles(vec3 color, vec3 origin, vec3 direction, float tMax) {
    for (int i = 0; i < uBubbleCount; i++) {
        vec4 bubble = uBubbles[i];
        vec3 toCenter = bubble.xyz - origin;
        float along = dot(toCenter, direction);
        if (along < 0.0 || along > tMax) continue;

        float offsetSquared = dot(toCenter, toCenter) - along * along;
        float radiusSquared = bubble.w * bubble.w;
        if (offsetSquared >= radiusSquared) continue;

        // A bubble is air in water, so it is a *diverging* lens: it throws the light it gathers down
        // into a caustic beneath itself and leaves its own crown dark, with a bright ring where the
        // wall of it is nearly edge-on.
        float offsetLength = sqrt(max(offsetSquared, 1e-8));
        float edge = offsetLength / bubble.w;
        float vertical = dot((direction * along - toCenter) / offsetLength, uUp);

        float fill = 1.0 - smoothstep(0.86, 1.0, edge);
        float ring = smoothstep(0.55, 0.97, edge) * fill;
        float caustic = clamp(-vertical, 0.0, 1.0) * fill;
        float crown = clamp(vertical, 0.0, 1.0) * fill;

        color = mix(color, AIR_COLOR, fill * 0.35);
        color = mix(color, BUBBLE_COLOR, ring * 0.55);
        color += BUBBLE_COLOR * caustic * 0.35;
        color *= 1.0 - crown * 0.30;
    }
    return color;
}

/**
 * How much of the light reaching a point survives the die standing in the way.
 *
 * The die is an exact solid here rather than a field, so this is a plain shadow test and not a second
 * march. The probes step across the light, over a disc that widens with the distance from the die, and
 * give a penumbra that is tight where the liquid touches the plastic and soft a finger's width away —
 * and the whole thing fades out with depth, because a shadow does not survive a cavity of scattering
 * blue. How many there are is up to the tier: five for a penumbra, one for a hard edge, and too few
 * of them can only step between a handful of levels, which reads as a smear.
 */
float dieShadow(vec3 point) {
    mat3 toDie = transpose(uDieRotation);
    vec3 local = toDie * (point - uDieCenter);
    vec3 toLight = toDie * normalize(uLightDir);

    float gap = length(local) - uDiePlaneDistance;
    float reach = 1.0 - smoothstep(SHADOW_FADE_NEAR, SHADOW_FADE_FAR, gap);
    if (reach <= 0.0) return 1.0;

    // Two axes across the light, so the probes widen the shadow rather than lengthen it.
    vec3 across = normalize(
        abs(toLight.z) < 0.9 ? cross(toLight, vec3(0.0, 0.0, 1.0))
                             : cross(toLight, vec3(1.0, 0.0, 0.0))
    );
    vec3 along = cross(toLight, across);
    float spread = SHADOW_SPREAD * (0.3 + max(gap, 0.0));

    // One loop rather than one call per probe: the die test is a twenty-plane loop of its own, and
    // written out five times over it was the most expensive thing in the shader to compile.
    float blocked = 0.0;
    for (int s = 0; s < uShadowProbes; s++) {
        vec3 offset = vec3(0.0);
        if (s > 0) {
            // 1,2 step either side along one axis; 3,4 along the other.
            vec3 axis = s < 3 ? across : along;
            offset = axis * (mod(float(s), 2.0) < 0.5 ? -spread : spread);
        }
        if (intersectDie(local + offset, toLight).t >= 0.0) blocked += 1.0;
    }

    // Smoothed rather than averaged: five taps still step, and the steps are the part that looks odd.
    float umbra = smoothstep(0.0, 1.0, blocked / float(uShadowProbes));
    return mix(1.0, DIE_SHADOW_MIN, umbra * reach);
}

/** Colour of whatever a ray already inside the liquid reaches, tinted by how far it travelled. */
vec3 shadeSubmerged(vec3 origin, vec3 direction) {
    mat3 toDie = transpose(uDieRotation);
    vec3 localOrigin = toDie * (origin - uDieCenter);
    vec3 localDir = toDie * direction;
    DieHit hit = intersectDie(localOrigin, localDir);

    float depth;
    vec3 beneath;
    if (hit.t >= 0.0) {
        depth = hit.t;
        vec3 point = localOrigin + localDir * hit.t;
        vec3 normal = normalize(uDieRotation * hit.normal);
        beneath = shadeDie(
            point,
            hit.face,
            normal,
            direction,
            lightThroughLiquid(origin + direction * hit.t)
        );
    } else {
        depth = max(sphereExit(origin, direction, uCavityRadius), 0.0);
        // The far wall of the cavity: unlit rubber behind a lot of liquid.
        beneath = uLiquidColor * 0.12;
    }

    // Freshly stirred liquid both swallows and scatters more, so an answer on its way to the window
    // develops out of the murk instead of sliding into view through clear blue.
    vec3 absorption = uAbsorption * (1.0 + uTurbidity * 1.6);
    vec3 scatter = mix(uLiquidColor, uLiquidColor + vec3(0.16, 0.20, 0.26), uTurbidity);

    // Half way along the segment stands in for the whole of it: the liquid beside and under the die
    // is in its shadow, which is what sits the die *in* the water rather than in front of it. The die
    // itself is left alone — it has its own lighting, caustics included.
    float shade = dieShadow(origin + direction * (depth * 0.5));
    scatter *= shade;
    if (hit.t < 0.0) beneath *= shade;

    vec3 transmission = exp(-absorption * depth);
    vec3 color = beneath * transmission + scatter * (1.0 - transmission);
    color = compositeBubbles(color, origin, direction, depth);
    return color + BUBBLE_COLOR * fizzSparkle(origin, direction, depth) * 0.5;
}

/** Any two axes across "up", so a point on the glass can be indexed by where it sits around it. */
void wallBasis(out vec3 right, out vec3 forward) {
    vec3 reference = abs(uUp.z) < 0.9 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
    right = normalize(cross(uUp, reference));
    forward = cross(uUp, right);
}

/**
 * The air pocket over the liquid: dark, but not empty.
 *
 * Shake a real 8-ball and the glass above the waterline is left wet; the film then drains back down
 * over a couple of seconds, each drip at its own pace. Without it the top of the window is a flat
 * fill — the one part of the picture that gives away that this is a rendering.
 */
vec3 shadeAirPocket(vec3 origin, vec3 direction) {
    vec3 wall = origin + direction * max(sphereExit(origin, direction, uCavityRadius), 0.0);
    float above = dot(wall, uUp) - uFluidSurfaceOffset;

    // Not a flat fill. The rubber above the waterline is lit by what comes back up off the liquid, so
    // it is brightest just over the surface and fades to nothing towards the top of the cavity, and
    // the crease where the two meet keeps almost none of it. That gradient is what makes the dark part
    // of the window read as depth rather than as a hole cut in the picture.
    float height = clamp(above / max(uCavityRadius, 1e-3), 0.0, 1.0);
    float bounce = 1.0 - smoothstep(0.0, 0.55, height);
    float crease = 1.0 - smoothstep(0.0, 0.10, max(above, 0.0));
    vec3 color = (AIR_COLOR + uLiquidColor * bounce * 0.15) * (1.0 - crease * 0.35);

    if (uWetness <= 0.002 || above < 0.0) return color;

    vec3 right, forward;
    wallBasis(right, forward);
    // Drips are stuck to the glass and run straight down it, so they are indexed by azimuth alone.
    float column = atan(dot(wall, forward), dot(wall, right)) * STREAK_DENSITY;
    float seed = hash13(vec3(floor(column), 7.13, 1.7));
    float across = abs(fract(column) - 0.5);
    float streak = 1.0 - smoothstep(0.10 + seed * 0.16, 0.44, across);
    if (streak <= 0.0) return color;

    // Every film hangs from the waterline and retreats towards it as it dries.
    float reach = max((uCavityRadius - uFluidSurfaceOffset) * (0.35 + 0.65 * seed) * uWetness, 1e-3);
    float film = streak * (1.0 - smoothstep(reach * 0.45, reach, above));
    if (film <= 0.0) return color;

    // Wet glass catches the light, and the bead at the head of a drip catches most of it.
    vec3 inward = -normalize(wall);
    float sheen = pow(max(dot(reflect(direction, inward), normalize(uLightDir)), 0.0), 26.0);
    float bead = 4.0 * film * (1.0 - film);

    color = mix(color, uLiquidColor * 0.55, film * 0.6);
    color += vec3(sheen) * film * 0.7;
    color += uLiquidColor * bead * 0.35;
    return color;
}

/** Colour of a dry ray: the die poking out of the liquid, or the air pocket behind it. */
vec3 shadeDry(vec3 origin, vec3 direction) {
    mat3 toDie = transpose(uDieRotation);
    vec3 localOrigin = toDie * (origin - uDieCenter);
    vec3 localDir = toDie * direction;
    DieHit hit = intersectDie(localOrigin, localDir);
    if (hit.t < 0.0) return shadeAirPocket(origin, direction);

    vec3 point = localOrigin + localDir * hit.t;
    vec3 normal = normalize(uDieRotation * hit.normal);
    return shadeDie(point, hit.face, normal, direction, 1.0);
}

void main() {
    vec3 rayDir = normalize(vec3(vNdc.x * uTanHalfFov * uAspect, vNdc.y * uTanHalfFov, -1.0));

    float tShell = sphereEnter(uCameraPos, rayDir, 1.0);
    if (tShell < 0.0) discard;

    vec3 entry = uCameraPos + rayDir * tShell;
    vec3 entryNormal = normalize(entry);
    // The hole is a cone around the window axis; anything else belongs to the shell.
    if (dot(entryNormal, uWindowAxis) < uWindowCos) discard;

    // Glass and liquid together bend the view noticeably; one refraction is enough to sell it.
    vec3 innerDir = refract(rayDir, entryNormal, 1.0 / 1.36);
    if (dot(innerDir, innerDir) < 1e-6) innerDir = rayDir;

    vec3 color = vec3(0.014, 0.018, 0.026);
    // The window glass has thickness, so the cavity starts a little way in — and a grazing ray can
    // miss it entirely and see nothing but the inside of the shell.
    float tCavity = sphereEnter(entry + innerDir * 0.001, innerDir, uCavityRadius);
    if (tCavity >= 0.0) {
        vec3 origin = entry + innerDir * (0.001 + tCavity);
        float chord = sphereExit(origin, innerDir, uCavityRadius);

        // A ray that starts wet is the common case: the docked die sits just under the surface.
        bool submerged = insideness(origin) > 0.0;
        float tSurface = submerged ? 0.0 : findSurface(origin, innerDir, chord);

        if (submerged) {
            color = shadeSubmerged(origin, innerDir);
        } else if (tSurface >= 0.0) {
            vec3 point = origin + innerDir * tSurface;
            vec3 normal = surfaceNormal(point);

            vec3 refracted = refract(innerDir, normal, 1.0 / 1.33);
            if (dot(refracted, refracted) < 1e-6) refracted = reflect(innerDir, normal);
            vec3 through = shadeSubmerged(point + refracted * 0.004, refracted);

            // Seen from below the waterline the surface is a mirror, and what it gives back is the
            // dry top of the die hanging upside down over itself — not a flat darkening. The
            // reflected ray is answered by the analytic die test alone, so this costs no second
            // march; where the die is fully under, the air pocket answers and the old behaviour is
            // what comes back.
            vec3 mirrored = reflect(innerDir, normal);
            vec3 mirror = shadeDry(point + mirrored * 0.004, mirrored);

            float fresnel = 0.04 + 0.96 * pow(1.0 - max(dot(normal, -innerDir), 0.0), 5.0);
            vec3 halfway = normalize(normalize(uLightDir) - innerDir);
            float specular = pow(max(dot(normal, halfway), 0.0), 90.0);

            color = mix(through, mirror, fresnel * 0.9) + vec3(specular * 0.9);

            // Where the surface meets the glass it climbs the wall, and that lip of liquid reads as a
            // dark line. Two terms, because one cannot do both jobs: a wide, gentle gathering of
            // shadow into the corner, and a narrow crease right at the glass. A single hard band was
            // the whole of it before, and it read as a ring drawn over the picture.
            float radius = length(point);
            float corner = smoothstep(uCavityRadius - 0.34, uCavityRadius - 0.06, radius);
            float crease = smoothstep(uCavityRadius - 0.09, uCavityRadius - 0.01, radius);
            color *= 1.0 - corner * 0.16 - crease * 0.26;
        } else {
            color = shadeDry(origin, innerDir);
        }
    }

    // A touch of Fresnel on the glass so the window rim stays glassy.
    float glass = pow(1.0 - max(dot(entryNormal, -rayDir), 0.0), 4.0);
    color += vec3(0.42, 0.52, 0.72) * glass * 0.5;

    fragColor = vec4(color, 1.0);
}
