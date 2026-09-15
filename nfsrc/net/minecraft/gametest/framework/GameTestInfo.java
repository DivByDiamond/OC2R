package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class GameTestInfo {
    private final TestFunction testFunction;
    @Nullable
    private BlockPos structureBlockPos;
    @Nullable
    private BlockPos northWestCorner;
    private final ServerLevel level;
    private final Collection<GameTestListener> listeners = Lists.newArrayList();
    private final int timeoutTicks;
    public final Collection<GameTestSequence> sequences = Lists.newCopyOnWriteArrayList();
    private final Object2LongMap<Runnable> runAtTickTimeMap = new Object2LongOpenHashMap<>();
    private long startTick;
    private int ticksToWaitForChunkLoading = 20;
    private boolean placedStructure;
    private boolean chunksLoaded;
    private long tickCount;
    private boolean started;
    private final RetryOptions retryOptions;
    private final Stopwatch timer = Stopwatch.createUnstarted();
    private boolean done;
    private final Rotation rotation;
    @Nullable
    private Throwable error;
    @Nullable
    private StructureBlockEntity structureBlockEntity;

    public GameTestInfo(TestFunction testFunction, Rotation rotation, ServerLevel level, RetryOptions retryOptions) {
        this.testFunction = testFunction;
        this.level = level;
        this.retryOptions = retryOptions;
        this.timeoutTicks = testFunction.maxTicks();
        this.rotation = testFunction.rotation().getRotated(rotation);
    }

    void setStructureBlockPos(BlockPos pos) {
        this.structureBlockPos = pos;
    }

    public GameTestInfo startExecution(int delay) {
        this.startTick = this.level.getGameTime() + this.testFunction.setupTicks() + (long)delay;
        this.timer.start();
        return this;
    }

    public GameTestInfo placeStructure() {
        if (this.placedStructure) {
            return this;
        } else {
            this.ticksToWaitForChunkLoading = 0;
            this.placedStructure = true;
            StructureBlockEntity structureblockentity = this.getStructureBlockEntity();
            structureblockentity.placeStructure(this.level);
            BoundingBox boundingbox = StructureUtils.getStructureBoundingBox(structureblockentity);
            this.level.getBlockTicks().clearArea(boundingbox);
            this.level.clearBlockEvents(boundingbox);
            return this;
        }
    }

    private boolean ensureStructureIsPlaced() {
        if (this.placedStructure) {
            return true;
        } else if (this.ticksToWaitForChunkLoading > 0) {
            this.ticksToWaitForChunkLoading--;
            return false;
        } else {
            this.placeStructure().startExecution(0);
            return true;
        }
    }

    public void tick(GameTestRunner runner) {
        if (!this.isDone()) {
            if (this.structureBlockEntity == null) {
                this.fail(new IllegalStateException("Running test without structure block entity"));
            }

            if (this.chunksLoaded
                || StructureUtils.getStructureBoundingBox(this.structureBlockEntity)
                    .intersectingChunks()
                    .allMatch(p_309433_ -> this.level.isPositionEntityTicking(p_309433_.getWorldPosition()))) {
                this.chunksLoaded = true;
                if (this.ensureStructureIsPlaced()) {
                    this.tickInternal();
                    if (this.isDone()) {
                        if (this.error != null) {
                            this.listeners.forEach(p_319458_ -> p_319458_.testFailed(this, runner));
                        } else {
                            this.listeners.forEach(p_319456_ -> p_319456_.testPassed(this, runner));
                        }
                    }
                }
            }
        }
    }

    private void tickInternal() {
        this.tickCount = this.level.getGameTime() - this.startTick;
        if (this.tickCount >= 0L) {
            if (!this.started) {
                this.startTest();
            }

            ObjectIterator<Entry<Runnable>> objectiterator = this.runAtTickTimeMap.object2LongEntrySet().iterator();

            while (objectiterator.hasNext()) {
                Entry<Runnable> entry = objectiterator.next();
                if (entry.getLongValue() <= this.tickCount) {
                    try {
                        entry.getKey().run();
                    } catch (Exception exception) {
                        this.fail(exception);
                    }

                    objectiterator.remove();
                }
            }

            if (this.tickCount > (long)this.timeoutTicks) {
                if (this.sequences.isEmpty()) {
                    this.fail(new GameTestTimeoutException("Didn't succeed or fail within " + this.testFunction.maxTicks() + " ticks"));
                } else {
                    this.sequences.forEach(p_177478_ -> p_177478_.tickAndFailIfNotComplete(this.tickCount));
                    if (this.error == null) {
                        this.fail(new GameTestTimeoutException("No sequences finished"));
                    }
                }
            } else {
                this.sequences.forEach(p_177476_ -> p_177476_.tickAndContinue(this.tickCount));
            }
        }
    }

    private void startTest() {
        if (!this.started) {
            this.started = true;

            try {
                this.testFunction.run(new GameTestHelper(this));
            } catch (Exception exception) {
                this.fail(exception);
            }
        }
    }

    public void setRunAtTickTime(long tickTime, Runnable task) {
        this.runAtTickTimeMap.put(task, tickTime);
    }

    public String getTestName() {
        return this.testFunction.testName();
    }

    @Nullable
    public BlockPos getStructureBlockPos() {
        return this.structureBlockPos;
    }

    public AABB getStructureBounds() {
        StructureBlockEntity structureblockentity = this.getStructureBlockEntity();
        return StructureUtils.getStructureBounds(structureblockentity);
    }

    public StructureBlockEntity getStructureBlockEntity() {
        if (this.structureBlockEntity == null) {
            if (this.structureBlockPos == null) {
                throw new IllegalStateException("Could not find a structureBlockEntity for this GameTestInfo");
            }

            this.structureBlockEntity = (StructureBlockEntity)this.level.getBlockEntity(this.structureBlockPos);
            if (this.structureBlockEntity == null) {
                throw new IllegalStateException("Could not find a structureBlockEntity at the given coordinate " + this.structureBlockPos);
            }
        }

        return this.structureBlockEntity;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public boolean hasSucceeded() {
        return this.done && this.error == null;
    }

    public boolean hasFailed() {
        return this.error != null;
    }

    public boolean hasStarted() {
        return this.started;
    }

    public boolean isDone() {
        return this.done;
    }

    public long getRunTime() {
        return this.timer.elapsed(TimeUnit.MILLISECONDS);
    }

    private void finish() {
        if (!this.done) {
            this.done = true;
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
    }

    public void succeed() {
        if (this.error == null) {
            this.finish();
            AABB aabb = this.getStructureBounds();
            List<Entity> list = this.getLevel().getEntitiesOfClass(Entity.class, aabb.inflate(1.0), p_305655_ -> !(p_305655_ instanceof Player));
            list.forEach(p_305656_ -> p_305656_.remove(Entity.RemovalReason.DISCARDED));
        }
    }

    public void fail(Throwable error) {
        this.error = error;
        this.finish();
    }

    @Nullable
    public Throwable getError() {
        return this.error;
    }

    @Override
    public String toString() {
        return this.getTestName();
    }

    public void addListener(GameTestListener listener) {
        this.listeners.add(listener);
    }

    public GameTestInfo prepareTestStructure() {
        BlockPos blockpos = this.getOrCalculateNorthwestCorner();
        this.structureBlockEntity = StructureUtils.prepareTestStructure(this, blockpos, this.getRotation(), this.level);
        this.structureBlockPos = this.structureBlockEntity.getBlockPos();
        this.structureBlockEntity.setMetaData(this.getTestName()); // Neo: set the test name as metadata as it isn't always RL-compatible
        StructureUtils.addCommandBlockAndButtonToStartTest(this.structureBlockPos, new BlockPos(1, 0, -1), this.getRotation(), this.level);
        StructureUtils.encaseStructure(this.getStructureBounds(), this.level, !this.testFunction.skyAccess());
        this.listeners.forEach(p_127630_ -> p_127630_.testStructureLoaded(this));
        return this;
    }

    long getTick() {
        return this.tickCount;
    }

    GameTestSequence createSequence() {
        GameTestSequence gametestsequence = new GameTestSequence(this);
        this.sequences.add(gametestsequence);
        return gametestsequence;
    }

    public boolean isRequired() {
        return this.testFunction.required();
    }

    public boolean isOptional() {
        return !this.testFunction.required();
    }

    public String getStructureName() {
        return this.testFunction.structureName();
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public TestFunction getTestFunction() {
        return this.testFunction;
    }

    public int getTimeoutTicks() {
        return this.timeoutTicks;
    }

    public boolean isFlaky() {
        return this.testFunction.isFlaky();
    }

    public int maxAttempts() {
        return this.testFunction.maxAttempts();
    }

    public int requiredSuccesses() {
        return this.testFunction.requiredSuccesses();
    }

    public RetryOptions retryOptions() {
        return this.retryOptions;
    }

    public Stream<GameTestListener> getListeners() {
        return this.listeners.stream();
    }

    public GameTestInfo copyReset() {
        GameTestInfo gametestinfo = new GameTestInfo(this.testFunction, this.rotation, this.level, this.retryOptions());
        if (this.northWestCorner != null) {
            gametestinfo.setNorthWestCorner(this.northWestCorner);
        }

        if (this.structureBlockPos != null) {
            gametestinfo.setStructureBlockPos(this.structureBlockPos);
        }

        return gametestinfo;
    }

    private BlockPos getOrCalculateNorthwestCorner() {
        if (this.northWestCorner == null) {
            BoundingBox boundingbox = StructureUtils.getStructureBoundingBox(this.getStructureBlockEntity());
            this.northWestCorner = new BlockPos(boundingbox.minX(), boundingbox.minY(), boundingbox.minZ());
        }

        return this.northWestCorner;
    }

    public void setNorthWestCorner(BlockPos northWestCorner) {
        this.northWestCorner = northWestCorner;
    }
}
