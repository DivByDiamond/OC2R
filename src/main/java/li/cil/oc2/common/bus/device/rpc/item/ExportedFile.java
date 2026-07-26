package li.cil.oc2.common.bus.device.rpc.item;

import java.io.ByteArrayOutputStream;

final class ExportedFile {
    public final String name;
    public final ByteArrayOutputStream data = new ByteArrayOutputStream();

    ExportedFile(final String name) {
        this.name = name;
    }
}
