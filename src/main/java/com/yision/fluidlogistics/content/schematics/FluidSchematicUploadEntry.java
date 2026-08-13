package com.yision.fluidlogistics.content.schematics;

import com.simibubi.create.content.schematics.ServerSchematicLoader.SchematicUploadEntry;

public final class FluidSchematicUploadEntry extends SchematicUploadEntry {

    public FluidSchematicUploadEntry(SchematicUploadEntry source) {
        super(source.stream, source.totalBytes, source.world, source.tablePos);
        bytesUploaded = source.bytesUploaded;
        idleTime = source.idleTime;
    }
}
