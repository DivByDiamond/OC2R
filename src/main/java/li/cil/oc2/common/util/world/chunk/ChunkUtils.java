package li.cil.oc2.common.util.world.chunk;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import li.cil.oc2.platform.event.CommonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class ChunkUtils {
    /**
     * All chunks marked for lazy saving. ChunkMap's save hook applies this flag before the
     * save check; the ServerChunkCache hook also flushes it for explicit saves.
     */
    private static final Object SAVE_LOCK = new Object();
    private static final Set<ChunkAccess> UNSAVED_CHUNKS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<ChunkAccess> CLOSING_CHUNKS =
            Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * This will mark a chunk unsaved lazily, right before an attempt to save it would be made due
     * to of these events:
     *
     * <ul>
     *   <li>Chunk about to be saved during unload.
     *   <li>Game paused (singleplayer).
     *   <li>Save command.
     *   <li>Server stopped.
     * </ul>
     *
     * <p>This is intended for things that change every tick, which would lead to saving to NBT
     * every single tick, when setting {@link
     * net.minecraft.world.level.chunk.ChunkAccess#setUnsaved(boolean)} directly.
     *
     * <p>Instead, this sets a flag on the chunk, which, if true, will cause the chunk to be marked
     * as unsaved just before this flag is checked, for the events listed above. I.e. for all cases
     * where an "explicit" save is performed.
     *
     * @param chunkAccess the chunk to set the flag for.
     */
    public static void setLazyUnsaved(final ChunkAccess chunkAccess) {
        synchronized (SAVE_LOCK) {
            if (!CLOSING_CHUNKS.contains(chunkAccess)) UNSAVED_CHUNKS.add(chunkAccess);
        }
    }

    /**
     * This will mark a chunk unsaved lazily, right before an attempt to save it would be made due
     * to of these events:
     *
     * <ul>
     *   <li>Chunk about to be saved during unload.
     *   <li>Game paused (singleplayer).
     *   <li>Save command.
     *   <li>Server stopped.
     * </ul>
     *
     * <p>This is intended for things that change every tick, which would lead to saving to NBT
     * every single tick, when setting {@link
     * net.minecraft.world.level.chunk.ChunkAccess#setUnsaved(boolean)} directly.
     *
     * <p>Instead, this sets a flag on the chunk, which, if true, will cause the chunk to be marked
     * as unsaved just before this flag is checked, for the events listed above. I.e. for all cases
     * where an "explicit" save is performed.
     *
     * @param level the level containing the chunk.
     * @param blockPos the block position contained in the chunk.
     */
    public static void setLazyUnsaved(final LevelAccessor level, final BlockPos blockPos) {
        final int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
        final int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
        if (level.hasChunk(chunkX, chunkZ)) {
            setLazyUnsaved(level.getChunk(chunkX, chunkZ));
        }
    }

    /**
     * This will mark a chunk unsaved lazily, right before an attempt to save it would be made due
     * to of these events:
     *
     * <ul>
     *   <li>Chunk about to be saved during unload.
     *   <li>Game paused (singleplayer).
     *   <li>Save command.
     *   <li>Server stopped.
     * </ul>
     *
     * <p>This is intended for things that change every tick, which would lead to saving to NBT
     * every single tick, when setting {@link
     * net.minecraft.world.level.chunk.ChunkAccess#setUnsaved(boolean)} directly.
     *
     * <p>Instead, this sets a flag on the chunk, which, if true, will cause the chunk to be marked
     * as unsaved just before this flag is checked, for the events listed above. I.e. for all cases
     * where an "explicit" save is performed.
     *
     * @param level the level containing the chunk.
     * @param chunkPos the position of the chunk.
     */
    public static void setLazyUnsaved(final LevelAccessor level, final ChunkPos chunkPos) {
        //? if >=26.1 {
        /*final int chunkX = chunkPos.x();
        final int chunkZ = chunkPos.z();
        *///?} else {
        final int chunkX = chunkPos.x;
        final int chunkZ = chunkPos.z;
        //?}
        if (level.hasChunk(chunkX, chunkZ)) {
            setLazyUnsaved(level.getChunk(chunkX, chunkZ));
        }
    }

    /**
     * Applies pending lazy flags for explicit saves, excluding chunks already closing.
     */
    public static void applyChunkLazyUnsaved() {
        synchronized (SAVE_LOCK) {
            for (final ChunkAccess chunk : UNSAVED_CHUNKS) {
                if (!CLOSING_CHUNKS.contains(chunk)) markUnsaved(chunk);
            }
            UNSAVED_CHUNKS.clear();
        }
    }

    /** Applied by ChunkMap.save before vanilla checks whether this chunk needs saving. */
    public static void applyChunkLazyUnsaved(final ChunkAccess chunk) {
        synchronized (SAVE_LOCK) {
            if (UNSAVED_CHUNKS.remove(chunk) && !CLOSING_CHUNKS.contains(chunk)) markUnsaved(chunk);
        }
    }

    private static void markUnsaved(final ChunkAccess chunk) {
        //? if >=26.1 {
        /*chunk.markUnsaved();
        *///?} else {
        chunk.setUnsaved(true);
        //?}
    }

    public static void register() {
        CommonEvents.CHUNK_LOAD.register((level, chunk) -> handleChunkLoad(chunk));
        CommonEvents.CHUNK_UNLOAD.register((level, chunk) -> handleChunkUnload(chunk));
    }

    static void handleChunkLoad(final ChunkAccess chunk) {
        synchronized (SAVE_LOCK) {
            CLOSING_CHUNKS.remove(chunk);
        }
    }

    static void handleChunkUnload(final ChunkAccess chunk) {
        synchronized (SAVE_LOCK) {
            // NeoForge fires unload AFTER the chunk was saved. Marking it dirty here, or
            // from the VM's ensuing suspend callbacks, can save it again after its block
            // entities have been cleared and overwrite the good snapshot with an empty list.
            CLOSING_CHUNKS.add(chunk);
            UNSAVED_CHUNKS.remove(chunk);
        }
    }
}
