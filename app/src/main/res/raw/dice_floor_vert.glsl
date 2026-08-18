#version 300 es

// The floor, as one quad on y = 0. It is already in world space, so the fragment stage gets the
// position straight — the tray's felt, its rim and the shadows under the dice are all painted from it.

in vec3 aPosition;

uniform mat4 uViewProjection;
uniform mat4 uTrayModel;

out vec3 vWorldPos;

void main() {
    vWorldPos = aPosition;
    gl_Position = uViewProjection * uTrayModel * vec4(aPosition, 1.0);
}
