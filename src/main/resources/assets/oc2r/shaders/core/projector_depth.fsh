#version 330
uniform sampler2D Sampler0;
in vec2 texCoord;
out vec4 fragColor;
void main() {
#ifdef CUTOUT
    if (texture(Sampler0, texCoord).a < 0.5) discard;
#endif
    fragColor = vec4(0.0);
}
