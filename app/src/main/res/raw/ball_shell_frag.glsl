#version 300 es

// Black glossy shell with the answer window and the "8" badge.
//
// Both features live in *object* space, so they ride the shell: the window and the badge are exactly
// antipodal, and spinning the ball carries the pair around together through a full turn. Keeping the
// answer readable is the simulation's job — it turns the shell until the window faces the camera
// again — not something this shader fakes.

precision highp float;

in vec3 vWorldPos;
in vec3 vWorldNormal;
in vec3 vObjectNormal;

uniform vec3 uCameraPos;
uniform vec3 uLightDir;
uniform vec3 uShellColor;
uniform vec3 uRimColor;

/** Where the window points in the shell's own frame; the badge sits on the far side of it. */
uniform vec3 uWindowAxis;
/** The same axis after the shell's spin, so the bevel's lip can be lit in world space. */
uniform vec3 uWindowAxisWorld;
uniform float uWindowCos;
uniform float uBevelCos;
/** 1.0 once the interior pass exists: the window becomes a real hole. */
uniform float uInteriorEnabled;
uniform vec3 uWindowFillColor;

uniform vec3 uBadgeAxis;
uniform vec3 uBadgeTangent;
uniform vec3 uBadgeBitangent;
uniform float uBadgeCos;

out vec4 fragColor;

const float PI = 3.14159265;

/**
 * Steepest slope of the bevel's lip, as a tangent. The rim is not a painted band but a raised ring of
 * plastic, so its normal has to leave the sphere: this is how far.
 */
const float LIP_SLOPE = 0.85;

float ggxSpecular(float nDotH, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float d = nDotH * nDotH * (a2 - 1.0) + 1.0;
    return a2 / (PI * d * d);
}

/** Two stacked rings: an "8" drawn without a texture. */
float eightGlyph(vec2 uv) {
    float ringRadius = 0.30;
    float thickness = 0.115;
    float upper = abs(length(uv - vec2(0.0, 0.29)) - ringRadius);
    float lower = abs(length(uv - vec2(0.0, -0.29)) - ringRadius);
    float d = min(upper, lower);
    return 1.0 - smoothstep(thickness * 0.65, thickness, d);
}

void main() {
    vec3 n = normalize(vWorldNormal);
    vec3 v = normalize(uCameraPos - vWorldPos);
    vec3 l = normalize(uLightDir);
    vec3 h = normalize(l + v);

    vec3 objectNormal = normalize(vObjectNormal);
    float windowDot = dot(objectNormal, normalize(uWindowAxis));
    bool insideWindow = windowDot > uWindowCos;

    if (insideWindow && uInteriorEnabled > 0.5) {
        // The interior pass fills the hole; leaving far depth here is what lets it in.
        discard;
    }

    float nDotL = max(dot(n, l), 0.0);
    float nDotV = max(dot(n, v), 0.0);
    float specular = ggxSpecular(max(dot(n, h), 0.0), 0.22) * 0.55;
    float fresnel = pow(1.0 - nDotV, 4.0);

    vec3 color = uShellColor * (0.16 + 0.62 * nDotL);
    color += vec3(specular);
    color += uRimColor * fresnel * 0.55;

    if (insideWindow) {
        // Placeholder for as long as there is nothing behind the shell to look at.
        color = mix(uWindowFillColor, color, 0.12);
    } else if (windowDot > uBevelCos) {
        // The rim of a real 8-ball's window is a moulded lip standing proud of the shell, not a decal.
        // Rolling the normal over that lip is what gives it its own highlight on the way up and a
        // shadowed inner wall on the way down into the hole.
        float t = smoothstep(uBevelCos, uWindowCos, windowDot);

        vec3 radial = uWindowAxisWorld - n * dot(n, uWindowAxisWorld);
        float radialLength = length(radial);
        radial = radialLength > 1e-4 ? radial / radialLength : vec3(0.0);

        // Positive on the outer flank, zero at the crest, negative on the inner one.
        float slope = LIP_SLOPE * cos(PI * t);
        vec3 lipNormal = normalize(n - radial * slope);
        float lipNDotL = max(dot(lipNormal, l), 0.0);
        float lipNDotV = max(dot(lipNormal, v), 0.0);
        float lipSpecular = ggxSpecular(max(dot(lipNormal, h), 0.0), 0.30) * 0.9;

        // The inner flank turns away from the room and is shaded by the lip standing over it.
        float recess = smoothstep(0.55, 1.0, t);
        vec3 lip = vec3(0.93, 0.94, 0.97) * (0.22 + 0.78 * lipNDotL) * (1.0 - recess * 0.72);
        lip += vec3(lipSpecular) * (1.0 - recess * 0.5);

        // Thick glass splits the light where it is thinnest, right at the lip of the hole.
        float rimBand = smoothstep(0.82, 1.0, t);
        vec3 fringe = vec3(
            pow(1.0 - lipNDotV, 3.0),
            pow(1.0 - lipNDotV, 4.2),
            pow(1.0 - lipNDotV, 5.6)
        );
        lip += fringe * rimBand * 0.55;

        color = mix(color, lip, smoothstep(0.0, 0.18, t));
    } else {
        float badgeDot = dot(objectNormal, normalize(uBadgeAxis));
        if (badgeDot > uBadgeCos) {
            float spread = sqrt(max(1.0 - uBadgeCos * uBadgeCos, 1e-4));
            vec2 uv = vec2(
                dot(objectNormal, normalize(uBadgeTangent)),
                dot(objectNormal, normalize(uBadgeBitangent))
            ) / spread;
            float disc = 1.0 - smoothstep(0.86, 1.0, length(uv));
            vec3 badge = mix(vec3(0.95, 0.95, 0.97), vec3(0.05, 0.05, 0.06), eightGlyph(uv));
            color = mix(color, badge * (0.35 + 0.65 * nDotL) + vec3(specular * 0.4), disc);
        }
    }

    fragColor = vec4(color, 1.0);
}
