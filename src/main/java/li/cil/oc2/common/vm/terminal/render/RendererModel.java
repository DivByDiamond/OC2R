package li.cil.oc2.common.vm.terminal.render;

import java.util.concurrent.atomic.AtomicLong;

public interface RendererModel {
    AtomicLong getDirtyMask();

    void close();
}
