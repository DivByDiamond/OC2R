package li.cil.oc2.common.bus.device.rpc.item.file;

import java.io.ByteArrayInputStream;

public final class ImportedFile {
    public final String name;
    public final int size;
    public final ByteArrayInputStream data;

    public ImportedFile(final String name, final byte[] data) {
        this.name = name;
        this.size = data.length;
        this.data = new ByteArrayInputStream(data);
    }
}