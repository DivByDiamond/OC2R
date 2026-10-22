#version 330
uniform sampler2D Sampler0;
layout(std140) uniform ModelMatrices {
    mat4 ProjectorCamera;
    vec4 Cutout;
};
in vec2 texCoord;
in float vertexAlpha;
void main() {
    if (Cutout.x >= 0.0 && texture(Sampler0, texCoord).a * vertexAlpha < Cutout.x) discard;
}
