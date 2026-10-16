package li.cil.oc2.common.util.world.chunk;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.junit.jupiter.api.Test;

class ChunkUtilsTest {
    @Test
    void unloadCannotResaveClearedBlockEntitiesAfterSuccessfulSnapshot() {
        final AtomicBoolean dirty = new AtomicBoolean();
        final ChunkAccess chunk = chunk(dirty);
        ChunkUtils.setLazyUnsaved(chunk);
        // ChunkMap.save HEAD, while block entities and the running VM still exist.
        ChunkUtils.applyChunkLazyUnsaved(chunk);
        assertTrue(dirty.get(), "The latest VM changes must reach the pre-unload save");
        dirty.set(false); // vanilla consumes the dirty flag and serializes the good snapshot.
        ChunkUtils.handleChunkUnload(chunk);
        assertFalse(dirty.get(), "The post-save unload event must not request another save");
        // VM suspend/close may run after the unload event and request another lazy save.
        ChunkUtils.setLazyUnsaved(chunk);
        ChunkUtils.applyChunkLazyUnsaved();
        ChunkUtils.applyChunkLazyUnsaved(chunk);
        assertFalse(dirty.get(), "A cleared chunk must not overwrite its block-entity snapshot");
        verifyMarkedOnce(chunk);
    }

    @Test
    void lateUnloadRequestIsDiscardedButLoadedChunkCanBecomeDirtyAgain() {
        final AtomicBoolean dirty = new AtomicBoolean();
        final ChunkAccess chunk = chunk(dirty);
        ChunkUtils.setLazyUnsaved(chunk);
        ChunkUtils.handleChunkUnload(chunk);
        ChunkUtils.applyChunkLazyUnsaved();
        assertFalse(dirty.get());
        ChunkUtils.handleChunkLoad(chunk);
        ChunkUtils.setLazyUnsaved(chunk);
        ChunkUtils.applyChunkLazyUnsaved(chunk);
        assertTrue(dirty.get(), "Reload must permit ordinary VM persistence again");
        verifyMarkedOnce(chunk);
    }

    private static ChunkAccess chunk(final AtomicBoolean dirty) {
        final ChunkAccess chunk = mock(ChunkAccess.class);
        //? if >=26.1 {
        /*doAnswer(call -> { dirty.set(true); return null; }).when(chunk).markUnsaved();
        *///?} else {
        doAnswer(call -> { dirty.set(call.getArgument(0)); return null; }).when(chunk).setUnsaved(anyBoolean());
        //?}
        return chunk;
    }

    private static void verifyMarkedOnce(final ChunkAccess chunk) {
        //? if >=26.1 {
        /*verify(chunk).markUnsaved();
        *///?} else {
        verify(chunk).setUnsaved(true);
        //?}
    }
}
