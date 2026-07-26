
package li.cil.oc2.common.blockentity.projector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ProjectorDecoderWorkers {
    static final ExecutorService INSTANCE = Executors.newCachedThreadPool(r -> {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("Projector Frame Decoder");
        return thread;
    });
}
