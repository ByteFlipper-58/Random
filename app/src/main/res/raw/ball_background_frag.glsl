#version 300 es

// Backdrop: the app theme's gradient, a soft glow behind the ball and a contact shadow under it.
// Colours arrive from Compose so the GL surface never fights the surrounding Material palette.

precision mediump float;

in vec2 vUv;

uniform vec3 uTopColor;
uniform vec3 uBottomColor;
uniform vec3 uGlowColor;
/** Ball centre in uv. */
uniform vec2 uBallCenter;
/** Ball radius as a fraction of the viewport height. */
uniform float uBallRadius;
/** Viewport width / height, used to keep distances round. */
uniform float uAspect;

out vec4 fragColor;

void main() {
    vec3 color = mix(uBottomColor, uTopColor, smoothstep(0.0, 1.0, vUv.y));

    vec2 toCenter = (vUv - uBallCenter) * vec2(uAspect, 1.0);
    float glow = 1.0 - smoothstep(uBallRadius * 0.5, uBallRadius * 2.2, length(toCenter));
    color += uGlowColor * glow * 0.22;

    // Squashed ellipse just below the ball; it grounds the sphere without a real shadow pass.
    vec2 toShadow = (vUv - vec2(uBallCenter.x, uBallCenter.y - uBallRadius * 1.02))
        * vec2(uAspect * 0.6, 2.0);
    float shadow = 1.0 - smoothstep(0.0, uBallRadius * 0.9, length(toShadow));
    color *= 1.0 - shadow * 0.42;

    fragColor = vec4(color, 1.0);
}
