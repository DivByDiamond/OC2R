package li.cil.oc2.common.vm.terminal.render;

import java.util.concurrent.atomic.AtomicInteger;

public interface RendererModel {
    AtomicInteger getDirtyMask();

    void close();
}