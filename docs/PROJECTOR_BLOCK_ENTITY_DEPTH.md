# Projector block-entity depth on 26.x

`ProjectorBlockEntityDepth` adds an independent depth pass after chunk/entity/particle
occlusion and before projector compositing. Animated block-entity models are not
part of the compiled chunk mesh; omitting their renderer submissions previously
allowed projector light to pass through them.

The pass extracts block entities from the projector-visible sections and the
client level's globally rendered set. Its camera-distance and bounding-box
checks use the projector. It submits actual animated model/model-part and custom
quad vertices to a private collector, preserving the renderer's model pose, UVs
and vertex alpha. The vertex positions remain relative to the main camera, as
required by the existing projector matrix. Models animate synchronously when
submitted, so another instance cannot overwrite shared mutable model state.

Each material keeps its original `Sampler0` texture/sampler and `ALPHA_CUTOUT`
threshold. A depth-only shader discards transparent texels using that threshold;
it does not replace model geometry with the block's bounds. The 26.1 texture
lookup needs a small `RenderType` accessor. The 26.2 path uses prepared material
textures and its normal reversed-Z depth state. Buffers, mesh data and uniforms
are closed after their pass.

The capture intentionally omits blended effects, outlines, materials that do not
write depth, text, item/block/moving-block submissions, non-quad custom geometry,
and untextured materials without `Sampler0`. Projector block entities themselves
are excluded: their solid body already belongs to the chunk mesh, and submitting
their light renderer again would register extra projectors. Custom vertex shader
deformation and scrolling texture transforms are not reproduced. There is no
opaque box fallback for omitted geometry. Limits are 512 block entities and
262,144 captured vertices per projector per frame.

## Verification

The new unit regression captures custom renderer output and checks transformed
positions, UV replacement, vertex alpha and quad indices. Another regression
ensures a blended effect is not promoted to opaque depth. Run on each modern
node:

```sh
./gradlew :26.1.2:test --tests li.cil.oc2.client.renderer.projector.ProjectorBlockEntityDepthTest \
  :26.2:test --tests li.cil.oc2.client.renderer.projector.ProjectorBlockEntityDepthTest
```

The GLSL vertex and fragment stages passed `glslangValidator` individually on
2026-10-18. Java compilation, those regression results and live rendering must be
recorded separately; shader syntax alone does not establish working GPU bindings.

Live acceptance remains required on 26.1.2 and 26.2: project a real guest frame
onto a wall through a chest, with a gap between chest and wall. Compare the
closed/open lid silhouettes and the light around the chest's 14/16-width body.
Add a block entity whose material has transparent cutout texels. Move the player
camera and reload the world; the shadow must stay attached to the actual model.
Record screenshots and client logs, including any GPU validation errors. Keep
Stage 3 open until these checks and the remaining renderer acceptance pass.
