#version 300 es

// Moulded plastic with pips sunk into it.
//
// The pips are computed rather than sampled. Six layouts over one face come to a distance against a
// handful of centres, which costs less than the texture fetch it replaces and — the reason it is done
// this way — stays sharp at every size, from one die filling a third of the screen to ten sharing it.
//
// Lighting is one key light for the form plus the room itself for everything else: the sky gradient
// above and the felt below, which is what keeps a die sitting in the tray rather than floating over it.

precision highp float;

in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vUv;
flat in float vPips;

uniform vec3 uCameraPos;
uniform vec3 uLightDir;
uniform vec3 uColor;
uniform vec3 uPipColor;
uniform vec3 uSkyColor;
uniform vec3 uBounceColor;
uniform float uSelected;

out vec4 fragColor;

/**
 * Where the pips sit and how wide they are, as a fraction of the half-face.
 *
 * Both are lifted from the flat dice the 2D mode draws, so switching between the two modes changes the
 * dimension a die is drawn in and nothing else about it.
 */
const float PIP_GRID = 0.52;
const float PIP_RADIUS = 0.155;

/**
 * Distance from [point] to the nearest pip on a face carrying [count] of them.
 *
 * The six layouts are four rules rather than six cases: an odd count has a pip in the middle, two or
 * more take one diagonal pair, four or more take the other, and six alone adds the pair either side of
 * the centre. Returned as a distance rather than a coverage so the shading below can cut the disc, its
 * dished floor and its antialiased edge out of the one number.
 */
float pipDistance(vec2 point, float count) {
    float nearest = 4.0;
    if (mod(count, 2.0) > 0.5) {
        nearest = length(point);
    }
    if (count >= 2.0) {
        nearest = min(nearest, length(point - vec2(-PIP_GRID, PIP_GRID)));
        nearest = min(nearest, length(point - vec2(PIP_GRID, -PIP_GRID)));
    }
    if (count >= 4.0) {
        nearest = min(nearest, length(point - vec2(PIP_GRID, PIP_GRID)));
        nearest = min(nearest, length(point - vec2(-PIP_GRID, -PIP_GRID)));
    }
    if (count >= 6.0) {
        nearest = min(nearest, length(point - vec2(-PIP_GRID, 0.0)));
        nearest = min(nearest, length(point - vec2(PIP_GRID, 0.0)));
    }
    return nearest;
}

void main() {
    vec3 normal = normalize(vNormal);
    vec3 view = normalize(uCameraPos - vWorldPos);
    vec3 light = normalize(uLightDir);

    float pipEdge = max(fwidth(vUv.x), fwidth(vUv.y)) + 0.002;
    float pipNear = pipDistance(vUv, vPips);
    float pip = 1.0 - smoothstep(PIP_RADIUS - pipEdge, PIP_RADIUS + pipEdge, pipNear);

    // A pip is a shallow dish, so its rim sees less of the room than its floor does.
    float dish = 1.0 - smoothstep(PIP_RADIUS * 0.55, PIP_RADIUS, pipNear);
    vec3 albedo = mix(uColor, uPipColor * mix(0.68, 1.0, dish), pip);

    // Wrapped rather than clipped at the terminator: plastic this pale carries light round its own
    // shoulder, and a hard terminator on a rounded edge reads as a crease instead of a curve.
    float key = dot(normal, light) * 0.5 + 0.5;
    float diffuse = key * key;

    // The room: the backdrop's own light from above, the felt's from below, and the die between them.
    // The flat term is what a dark theme leaves a face turned away from the light — enough to still be
    // a die rather than a hole in the tray.
    vec3 room = mix(uBounceColor, uSkyColor, normal.y * 0.5 + 0.5);
    vec3 color = albedo * (0.20 + room * 0.25 + diffuse * 0.62);

    // Highlight, dulled inside a pip so the recess does not shine like the face around it. White,
    // because that is the colour of the light rather than of the room it is standing in.
    vec3 halfway = normalize(light + view);
    float specular = pow(max(dot(normal, halfway), 0.0), 42.0) * 0.3 * (1.0 - pip * 0.75);
    color += vec3(specular);

    // The edge of the silhouette, picking up the backdrop it actually stands against.
    float rim = pow(1.0 - max(dot(normal, view), 0.0), 3.0);
    color += uSkyColor * rim * 0.18;

    // A held die catches the tray's accent light. Kept on the silhouette and broad face light so the
    // pips remain fully readable and the effect feels like selection, not a colour replacement.
    float selectionGlow = uSelected * (0.10 + rim * 0.42);
    color += (uSkyColor * 0.55 + vec3(0.16, 0.22, 0.30)) * selectionGlow;
    color *= 1.0 + uSelected * 0.07;

    fragColor = vec4(color, 1.0);
}
