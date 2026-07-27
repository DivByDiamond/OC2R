package li.cil.oc2.common.bus.device.rpc.item;

import java.io.ByteArrayInputStream;

final class ImportedFile {
    public final String name;
    public final int size;
    public final ByteArrayInputStream data;

    ImportedFile(final String name, final byte[] data) {
        this.name = name;
        this.size = data.length;
        this.data = new ByteArrayInputStream(data);
    }
}