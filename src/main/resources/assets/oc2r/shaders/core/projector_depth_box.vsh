#version 330
in vec3 Position;
layout(std140) uniform BoxMatrices {
    mat4 ProjectorCamera;
};
void main() {
    gl_Position = ProjectorCamera * vec4(Position, 1.0);
}
