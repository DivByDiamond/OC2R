# Computer-front overlay UV fix — 2026-10-25

The colorful pixel garbage on the computer's world terminal was the complete
block texture atlas drawn over the correct terminal. The GUI terminal and
external RGB565 monitor were separate paths and rendered correctly.

Vanilla SpriteCoordinateExpander.addVertex returns its delegate. Chaining
wrapped.addVertex(...).setUv(...) therefore bypassed the wrapper's sprite UV
mapping in the power/status overlay. Calling addVertex and setUv separately on
the wrapper keeps the intended sprite coordinates. Geometry, materials, terminal
protocol and guest code are unchanged.

The regression uses the real vanilla wrapper: the old fluent calls emitted
U coordinates 0..1 instead of the selected .125..375 range. It fails before the
fix and passes afterward. Actual focused tests passed on 1.21.1, 26.1.2 and
26.2, along with all three main and test Checkstyle tasks. Independent review
verified the relevant vanilla bytecode.

A real Fabric 26.2 client then restored an old Linux world on Sedna 5.2 and
produced a clean computer-front terminal with the intended indicator sprites.
Both fresh guest RAM/disk responses and world reload passed. This is separate
from the Onyx RGB/reload persistence fix and does not infer guest health from
a screenshot. The diagnostic A/B run ruled out projector interference; actual
font texture and terminal mesh were correct before the fix.

Evidence is retained locally in build/reports/terminal-world-diagnosis/ and
build/reports/sedna53-migration/fixture/run/control-26.2/screenshots/.
Diagnostic clients use private Xvfb displays and explicitly set a 1280x720
window. The original 854x480 automation window also explained the low overall
resolution seen during the first visible test run.
