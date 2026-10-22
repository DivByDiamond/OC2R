#version 330
uniform sampler2D MainDepth;
uniform sampler2D ProjectorDepth;
uniform sampler2D ProjectorColor;
layout(std140) uniform ProjectorMatrices {
    mat4 InverseMainCamera;
    mat4 ProjectorCamera;
};
in vec2 texCoord;
out vec4 fragColor;
void main() {
    float depth = texture(MainDepth, texCoord).r;
    if (depth >= 1.0) discard;
    vec4 position = InverseMainCamera * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 world = position.xyz / position.w;
    vec3 normal = normalize(cross(dFdx(world), dFdy(world)));
    vec4 projected = ProjectorCamera * vec4(world, 1.0);
    if (projected.w <= 0.0) discard;
    vec3 clip = projected.xyz / projected.w;
    if (any(greaterThan(abs(clip), vec3(1.0)))) discard;
    vec2 uv = clip.xy * 0.5 + 0.5;
    float projectedDepth = texture(ProjectorDepth, uv).r * 2.0 - 1.0;
    if (clip.z > projectedDepth + 0.0001) discard;
    float facing = dot((ProjectorCamera * vec4(normal, 0.0)).xyz, vec3(0.0, 0.0, -1.0));
    if (facing <= 0.25) discard;
    float attenuation = clamp((facing - 0.25) / 0.75, 0.0, 1.0)
            * clamp((16.0 - projected.w) / 4.0, 0.0, 1.0);
    fragColor = vec4(texture(ProjectorColor, vec2(uv.x, 1.0 - uv.y)).rgb * attenuation, 0.0);
}
