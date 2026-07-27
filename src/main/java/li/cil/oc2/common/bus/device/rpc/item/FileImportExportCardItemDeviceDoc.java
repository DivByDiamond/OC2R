package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.api.bus.device.object.DocumentedDevice;

final class FileImportExportCardItemDeviceDoc {
    private static final String BEGIN_EXPORT_FILE = "beginExportFile";
    private static final String WRITE_EXPORT_FILE = "writeExportFile";
    private static final String FINISH_EXPORT_FILE = "finishExportFile";
    private static final String BEGIN_IMPORT_FILE = "beginImportFile";
    private static final String READ_IMPORT_FILE = "readImportFile";
    private static final String RESET = "reset";
    private static final String NAME = "name";

    static void visit(final DocumentedDevice.DeviceVisitor visitor) {
        visitor.visitCallback(BEGIN_EXPORT_FILE)
                .description(
                        "Begins exporting a file to external data storage. Requires calls to "
                                + WRITE_EXPORT_FILE
                                + "() to provide data of the exported file and a call "
                                + "to "
                                + FINISH_EXPORT_FILE
                                + "() to complete the export.\n"
                                + "This method may error if the device is currently exporting or"
                                + " importing.")
                .parameterDescription(NAME, "the name of the file being exported.");
        visitor.visitCallback(WRITE_EXPORT_FILE)
                .description(
                        """
                        Appends more data to the currently being exported file.
                        This method may error if the device is not currently exporting or the \
                        export was interrupted.
                        """)
                .parameterDescription("data", "the contents of the file being exported.");
        visitor.visitCallback(FINISH_EXPORT_FILE)
                .description(
                        "Finishes an export. This will prompt present users to select an external"
                            + " file location for the file being exported. If multiple users are"
                            + " present, the file is provided to all users.\n"
                            + "This method may error if the device is not currently exporting or"
                            + " the export was interrupted.");
        visitor.visitCallback(BEGIN_IMPORT_FILE)
                .description(
                        "Begins a file import operation. This will prompt present users to select"
                                + " an externally stored file for import. If multiple users are"
                                + " present, the first user to select a file will have their file"
                                + " uploaded. Use the "
                                + READ_IMPORT_FILE
                                + "() method to read the contents of the file being imported.\n"
                                + "This method may error if the device is currently exporting or"
                                + " importing.");
        visitor.visitCallback(READ_IMPORT_FILE)
                .description(
                        "Tries to read some data from a file being imported. Returns zero length"
                            + " data if no data is available yet. Returns null when no more data is"
                            + " available.\n"
                            + "This method may error if the device is not currently importing or"
                            + " the import was interrupted.")
                .returnValueDescription("data from the file being imported.");
        visitor.visitCallback(RESET)
                .description(
                        "Resets the device and cancels any currently running export or import"
                                + " operation.");
    }
}