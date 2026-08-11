package li.cil.oc2.common.bus.device.rpc.item.file;

import java.io.ByteArrayOutputStream;

public final class ExportedFile {
    public final String name;
    public final ByteArrayOutputStream data = new ByteArrayOutputStream();

    public ExportedFile(final String name) {
        this.name = name;
    }
}