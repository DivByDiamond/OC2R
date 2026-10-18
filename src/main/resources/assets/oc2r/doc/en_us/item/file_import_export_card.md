# File Import/Export Card
![Between host and guest](item:oc2r:file_import_export_card)

This RPC device, named *file_import_export*, transfers files between the guest and players currently using its terminal. Upload uses the first responding player; downloads are offered to all current terminal users. With no users present, an import request returns false.

The old *import.lua* and *export.lua* helpers are not bundled in the current package. Use the device API from a guest program, or provide your own wrappers. See [scripting](../scripting.md) for Lua setup and discovery.

## API

*beginExportFile(name)* starts an export; *writeExportFile(data)* appends numeric byte arrays; *finishExportFile()* opens the players' save dialogs. The export limit is 1048575 bytes.

*requestImportFile():boolean* opens an upload dialog. Poll *beginImportFile()* until it returns file metadata (*name*, *size*), then call *readImportFile()* for numeric byte chunks until an empty array marks the end. Cancellation raises an error. Calls must follow this order; overlapping transfers are rejected.
