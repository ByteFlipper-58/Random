#version 300 es

// The interior pass runs on a screen-filling quad pushed to the far end of the depth range, so it
// only survives where the shell discarded its fragments — the answer window.

in vec2 aPosition;

out vec2 vNdc;

void main() {
    vNdc = aPosition;
    // Just short of the far plane: passes the depth test inside the window's hole, fails against
    // the shell everywhere else.
    gl_Position = vec4(aPosition, 0.999, 1.0);
}
