#version 330
// 26.2 variant of projector_composite.fsh. The depth buffers use reversed-Z: they are cleared to 0.0
// (far plane) and a larger value is closer, so "nothing drawn" is 0.0 and shadowing flips direction.
uniform sampler2D MainDepth;
uniform sampler2D ProjectorDepth;
uniform sampler2D ProjectorColor;
layout(std140) uniform ProjectorMatrices {
    mat4 InverseMainCamera;
    mat4 ProjectorCamera;
    // Direction from a surface towards the projector, in the same camera-relative space as world.
    vec4 ProjectorAxis;
    // x: 1.0 when clip space depth is [0, 1] (vanilla default), 0.0 for [-1, 1].
    vec4 Params;
};
in vec2 texCoord;
out vec4 fragColor;

float toNdc(float depth) {
    return Params.x > 0.5 ? depth : depth * 2.0 - 1.0;
}

void main() {
    float depth = texture(MainDepth, texCoord).r;
    if (depth <= 0.0) discard;
    vec4 position = InverseMainCamera * vec4(texCoord * 2.0 - 1.0, toNdc(depth), 1.0);
    vec3 world = position.xyz / position.w;
    vec3 normal = normalize(cross(dFdx(world), dFdy(world)));
    // The world is camera-relative, so a surface faces the viewer when its normal opposes world.
    if (dot(normal, world) > 0.0) normal = -normal;
    vec4 projected = ProjectorCamera * vec4(world, 1.0);
    if (projected.w <= 0.0) discard;
    vec3 clip = projected.xyz / projected.w;
    float zMin = Params.x > 0.5 ? 0.0 : -1.0;
    if (any(greaterThan(abs(clip.xy), vec2(1.0))) || clip.z < zMin || clip.z > 1.0) discard;
    vec2 uv = clip.xy * 0.5 + 0.5;
    float projectedDepth = toNdc(texture(ProjectorDepth, uv).r);
    // Something nearer to the projector was drawn there: this point is in its shadow.
    if (clip.z < projectedDepth - 0.00005) discard;
    float facing = dot(normal, ProjectorAxis.xyz);
    if (facing <= 0.25) discard;
    float attenuation = clamp((facing - 0.25) / 0.75, 0.0, 1.0)
            * clamp((16.0 - projected.w) / 4.0, 0.0, 1.0);
    fragColor = vec4(texture(ProjectorColor, vec2(uv.x, 1.0 - uv.y)).rgb * attenuation, 0.0);
}
