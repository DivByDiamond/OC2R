package li.cil.oc2.common.blockentity.monitor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class MonitorDecoderWorkers {
    static final ExecutorService INSTANCE =
            Executors.newCachedThreadPool(
                    r -> {
                        final Thread thread = new Thread(r);
                        thread.setDaemon(true);
                        thread.setName("Monitor Frame Decoder");
                        return thread;
                    });
}