#version 300 es

// Tray felt with a layered moulded rim, a drop shadow, and soft shadows under every die.
//
// The tray is painted rather than built: one quad and a rounded-trapezoid distance field shared by the
// felt, inner bevel, outer shell and shadow. Geometry would have meant tall walls, and walls seen from
// this angle would hide the dice behind them — the physics keeps its walls, the picture does not.
//
// Outside the felt the floor dissolves into the backdrop's own gradient, recomputed here from the
// fragment's place on screen so the two passes meet without a seam.

precision highp float;

in vec3 vWorldPos;

/** Near half-width, far half-width, and half-length. */
uniform vec3 uTrayHalf;
uniform float uTrayCorner;
uniform vec3 uFeltColor;
uniform vec3 uRimColor;
uniform vec3 uTopColor;
uniform vec3 uBottomColor;
uniform vec3 uGlowColor;
uniform vec2 uViewport;

/** Centres of the dice in play, and the half-extent they all share. */
uniform vec3 uDicePos[10];
uniform int uDiceCount;
uniform int uSelectedDie;
uniform float uDieHalf;

/** 1 when the tier can afford a penumbra that grows with height; 0 for a plain contact patch. */
uniform float uSoftShadows;

out vec4 fragColor;

const int MAX_DICE = 10;

/** Stable cell noise for a restrained felt fibre variation; no texture fetch and no animation crawl. */
float hash21(vec2 point) {
    vec3 p = fract(vec3(point.xyx) * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float feltTexture(vec2 point) {
    float fibre = hash21(floor(point * 38.0)) - 0.5;
    float weave = sin(point.x * 47.0) * sin(point.y * 53.0);
    float diagonal = sin((point.x + point.y) * 112.0) *
        sin((point.x - point.y) * 97.0);
    return fibre * 0.032 + weave * 0.010 + diagonal * 0.004;
}

/** Signed distance to the rounded trapezoid: narrow near the viewer, wide at the far/top edge. */
float trayDistance(vec2 point) {
    // The standard trapezoid SDF expects the near edge at -y and the far edge at +y.
    vec2 p = vec2(point.x, -point.y);
    float nearWidth = max(uTrayHalf.x - uTrayCorner, 0.001);
    float farWidth = max(uTrayHalf.y - uTrayCorner, 0.001);
    float halfLength = max(uTrayHalf.z - uTrayCorner, 0.001);
    vec2 k1 = vec2(farWidth, halfLength);
    vec2 k2 = vec2(farWidth - nearWidth, 2.0 * halfLength);
    p.x = abs(p.x);
    vec2 ca = vec2(
        p.x - min(p.x, p.y < 0.0 ? nearWidth : farWidth),
        abs(p.y) - halfLength
    );
    vec2 cb = p - k1 + k2 * clamp(dot(k1 - p, k2) / max(dot(k2, k2), 0.001), 0.0, 1.0);
    float signDistance = cb.x < 0.0 && ca.y < 0.0 ? -1.0 : 1.0;
    return signDistance * sqrt(min(dot(ca, ca), dot(cb, cb))) - uTrayCorner;
}

/**
 * How much of the light at [point] the dice are keeping off the floor.
 *
 * Taken as the deepest single shadow rather than the sum of them: two dice touching cast one shadow
 * between them, and adding theirs together would leave a dark seam exactly where they meet.
 */
float diceShadow(vec2 point) {
    float darkest = 0.0;
    for (int i = 0; i < MAX_DICE; i++) {
        if (i >= uDiceCount) break;
        vec3 die = uDicePos[i];
        // How far off the floor the die is; zero once it is resting on it.
        float lift = max(die.y - uDieHalf, 0.0);
        float spread = uDieHalf * (1.0 + uSoftShadows * lift * 0.85);
        if (i == uSelectedDie) spread *= 1.22;
        float blot = 1.0 - smoothstep(spread * 0.5, spread * 1.45, length(point - die.xz));
        if (i == uSelectedDie) blot *= 1.16;
        darkest = max(darkest, blot / (1.0 + lift * 1.7));
    }
    return darkest;
}

void main() {
    vec2 screenUv = gl_FragCoord.xy / uViewport;
    vec3 backdrop = mix(uBottomColor, uTopColor, smoothstep(0.0, 1.0, screenUv.y));
    float glow = 1.0 - smoothstep(0.0, 0.85, length((screenUv - vec2(0.5, 0.58)) * vec2(1.0, 1.6)));
    backdrop += uGlowColor * glow * 0.14;

    vec2 point = vWorldPos.xz;
    float edge = trayDistance(point);

    // The shell casts slightly towards the viewer. Restricting it to fragments outside the actual
    // outline keeps it from dirtying the felt and makes the empty space below the tray readable.
    float shiftedEdge = trayDistance(point - vec2(0.0, 0.24));
    float outside = smoothstep(0.02, 0.34, edge);
    float trayShadow = (1.0 - smoothstep(0.02, 0.82, shiftedEdge)) * outside;
    vec3 color = backdrop * (1.0 - trayShadow * 0.34);

    // A broad, dark outer shell gives the tray thickness; the gradient across it is the bevel.
    float shellMask = 1.0 - smoothstep(0.34, 0.48, edge);
    float rimAcross = clamp((edge + 0.16) / 0.64, 0.0, 1.0);
    float shellGrain = hash21(floor(point * vec2(18.0, 7.0))) - 0.5;
    float brushed = sin((point.x * 0.8 + point.y) * 31.0) * 0.5 + 0.5;
    vec3 shell = uRimColor * mix(1.16, 0.48, rimAcross);
    shell *= 0.97 + shellGrain * 0.055 + brushed * 0.025;
    shell += uTopColor * pow(1.0 - rimAcross, 3.0) * 0.14;
    color = mix(color, shell, shellMask);

    // Felt gets subtle depth, fibre variation and a faint far-to-near lighting gradient. It remains
    // quiet enough that the pips stay the highest-frequency detail in the picture.
    float inward = clamp(-edge / max(min(uTrayHalf.x, uTrayHalf.y), 0.001), 0.0, 1.0);
    float farLight = clamp((-point.y / uTrayHalf.z) * 0.5 + 0.5, 0.0, 1.0);
    float centreLight = 1.0 - smoothstep(0.0, uTrayHalf.z, length(point * vec2(1.35, 0.72)));
    vec3 felt = uFeltColor * (0.74 + 0.19 * inward + 0.05 * farLight + 0.06 * centreLight);
    felt *= 1.0 + feltTexture(point);
    // The raised rim softly occludes the felt beside it, grounding it without a tall wall that would
    // hide the dice at the near edge.
    float innerOcclusion = smoothstep(-0.36, -0.055, edge);
    felt *= 1.0 - innerOcclusion * 0.17;
    felt *= 1.0 - diceShadow(point) * 0.55;
    float feltMask = 1.0 - smoothstep(-0.16, -0.07, edge);
    color = mix(color, felt, feltMask);

    // A thin inner lip catches the room light. It is intentionally asymmetric: light on the felt side,
    // darker on the outside, so the band reads as a raised edge instead of a painted outline.
    float innerLip = (1.0 - smoothstep(0.0, 0.055, abs(edge + 0.11))) * shellMask;
    color = mix(color, uRimColor * 1.30 + uTopColor * 0.14, innerLip * 0.78);
    float outerLip = (1.0 - smoothstep(0.0, 0.075, abs(edge - 0.27))) * shellMask;
    color *= 1.0 - outerLip * 0.16;

    // A restrained stitched seam just inside the rim adds scale and makes the tray read as a made
    // object rather than a flat rounded rectangle. The dashes are deliberately low contrast.
    float seamBand = 1.0 - smoothstep(0.0, 0.018, abs(edge + 0.29));
    float seamPhase = sin((point.x * 0.72 + point.y) * 42.0) * 0.5 + 0.5;
    float seamDash = smoothstep(0.38, 0.62, seamPhase);
    vec3 seamColor = mix(uFeltColor * 1.18, uRimColor * 1.12, 0.34);
    color = mix(color, seamColor, seamBand * seamDash * 0.34);

    // Two narrow highlights give the moulded shell a clear top bevel even in very dark themes.
    float crown = (1.0 - smoothstep(0.0, 0.035, abs(edge - 0.02))) * shellMask;
    color += (uTopColor * 0.16 + uGlowColor * 0.035) * crown;

    fragColor = vec4(color, 1.0);
}
