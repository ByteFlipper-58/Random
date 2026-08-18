#version 300 es

// Backdrop behind the tray. The floor covers the whole viewport at every aspect ratio the camera is
// fitted for, so this is a safety net as much as a picture — and the gradient it lays down is the one
// the floor fades into at its edges, which is what keeps the two reading as one surface.

in vec2 aPosition;

out vec2 vUv;

void main() {
    vUv = aPosition * 0.5 + 0.5;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
