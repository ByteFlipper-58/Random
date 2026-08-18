#version 300 es

// The app theme's gradient, with a soft glow where the tray sits. Colours arrive from Compose so the
// GL surface never fights the surrounding Material palette.

precision mediump float;

in vec2 vUv;

uniform vec3 uTopColor;
uniform vec3 uBottomColor;
uniform vec3 uGlowColor;

out vec4 fragColor;

void main() {
    vec3 color = mix(uBottomColor, uTopColor, smoothstep(0.0, 1.0, vUv.y));

    // Centred a little above the middle, which is where the tray's far half lands on screen.
    float glow = 1.0 - smoothstep(0.0, 0.85, length((vUv - vec2(0.5, 0.58)) * vec2(1.0, 1.6)));
    color += uGlowColor * glow * 0.14;

    fragColor = vec4(color, 1.0);
}
