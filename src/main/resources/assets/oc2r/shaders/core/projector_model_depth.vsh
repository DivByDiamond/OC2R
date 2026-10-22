#version 330
in vec3 Position;
in vec2 UV0;
in vec4 Color;
layout(std140) uniform ModelMatrices {
    mat4 ProjectorCamera;
    vec4 Cutout;
};
out vec2 texCoord;
out float vertexAlpha;
void main() {
    gl_Position = ProjectorCamera * vec4(Position, 1.0);
    texCoord = UV0;
    vertexAlpha = Color.a;
}
