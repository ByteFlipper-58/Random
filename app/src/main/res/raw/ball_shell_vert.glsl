#version 300 es

// The shell is a unit sphere, so the object-space position doubles as its normal.
//
// uModel carries the shell's spin and the offset it has been left behind by, so mat3 of it is a pure
// rotation and the normals come out of it unscaled.

in vec3 aPosition;

uniform mat4 uMvp;
uniform mat4 uModel;

out vec3 vWorldPos;
out vec3 vWorldNormal;
out vec3 vObjectNormal;

void main() {
    vec3 objectNormal = normalize(aPosition);
    vec4 world = uModel * vec4(aPosition, 1.0);

    vWorldPos = world.xyz;
    vObjectNormal = objectNormal;
    vWorldNormal = normalize(mat3(uModel) * objectNormal);

    gl_Position = uMvp * vec4(aPosition, 1.0);
}
