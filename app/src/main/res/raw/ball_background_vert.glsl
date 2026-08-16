#version 300 es

// Full-screen backdrop. Drawn first with depth writes off, so the shell simply covers it.

in vec2 aPosition;

out vec2 vUv;

void main() {
    vUv = aPosition * 0.5 + 0.5;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
