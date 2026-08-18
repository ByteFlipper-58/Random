#version 300 es

// One die. The mesh is a unit rounded cube, so the model matrix carries its rotation, its size and where
// in the tray it has come to rest.
//
// That size is one and the same scale on all three axes, which is why the normal can be turned by the
// upper 3x3 of the same matrix: a uniform scale leaves directions pointing where they did, and its only
// mark is a length the fragment stage divides out anyway.

in vec3 aPosition;
in vec3 aNormal;
in vec2 aUv;
in float aPips;

uniform mat4 uModel;
uniform mat4 uViewProjection;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vUv;

/** The number printed on this face. Flat, because one face carries one number across all of it. */
flat out float vPips;

void main() {
    vec4 world = uModel * vec4(aPosition, 1.0);
    vWorldPos = world.xyz;
    vNormal = mat3(uModel) * aNormal;
    vUv = aUv;
    vPips = aPips;
    gl_Position = uViewProjection * world;
}
