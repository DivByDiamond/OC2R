package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class StructureUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int DEFAULT_Y_SEARCH_RADIUS = 10;
    public static final String DEFAULT_TEST_STRUCTURES_DIR = "gameteststructures";
    public static String testStructuresDir = "gameteststructures";

    public static Rotation getRotationForRotationSteps(int rotationSteps) {
        switch (rotationSteps) {
            case 0:
                return Rotation.NONE;
            case 1:
                return Rotation.CLOCKWISE_90;
            case 2:
                return Rotation.CLOCKWISE_180;
            case 3:
                return Rotation.COUNTERCLOCKWISE_90;
            default:
                throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + rotationSteps);
        }
    }

    public static int getRotationStepsForRotation(Rotation rotation) {
        switch (rotation) {
            case NONE:
                return 0;
            case CLOCKWISE_90:
                return 1;
            case CLOCKWISE_180:
                return 2;
            case COUNTERCLOCKWISE_90:
                return 3;
            default:
                throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + rotation);
        }
    }

    public static AABB getStructureBounds(StructureBlockEntity structureBlockEntity) {
        return AABB.of(getStructureBoundingBox(structureBlockEntity));
    }

    public static BoundingBox getStructureBoundingBox(StructureBlockEntity structureBlockEntity) {
        BlockPos blockpos = getStructureOrigin(structureBlockEntity);
        BlockPos blockpos1 = getTransformedFarCorner(blockpos, structureBlockEntity.getStructureSize(), structureBlockEntity.getRotation());
        return BoundingBox.fromCorners(blockpos, blockpos1);
    }

    public static BlockPos getStructureOrigin(StructureBlockEntity structureBlockEntity) {
        return structureBlockEntity.getBlockPos().offset(structureBlockEntity.getStructurePos());
    }

    public static void addCommandBlockAndButtonToStartTest(BlockPos structureBlockPos, BlockPos offset, Rotation rotation, ServerLevel serverLevel) {
        BlockPos blockpos = StructureTemplate.transform(structureBlockPos.offset(offset), Mirror.NONE, rotation, structureBlockPos);
        serverLevel.setBlockAndUpdate(blockpos, Blocks.COMMAND_BLOCK.defaultBlockState());
        CommandBlockEntity commandblockentity = (CommandBlockEntity)serverLevel.getBlockEntity(blockpos);
        commandblockentity.getCommandBlock().setCommand("test runclosest");
        BlockPos blockpos1 = StructureTemplate.transform(blockpos.offset(0, 0, -1), Mirror.NONE, rotation, blockpos);
        serverLevel.setBlockAndUpdate(blockpos1, Blocks.STONE_BUTTON.defaultBlockState().rotate(rotation));
    }

    public static void createNewEmptyStructureBlock(String structureName, BlockPos pos, Vec3i size, Rotation rotation, ServerLevel serverLevel) {
        BoundingBox boundingbox = getStructureBoundingBox(pos.above(), size, rotation);
        clearSpaceForStructure(boundingbox, serverLevel);
        serverLevel.setBlockAndUpdate(pos, Blocks.STRUCTURE_BLOCK.defaultBlockState());
        StructureBlockEntity structureblockentity = (StructureBlockEntity)serverLevel.getBlockEntity(pos);
        structureblockentity.setIgnoreEntities(false);
        structureblockentity.setStructureName(ResourceLocation.parse(structureName));
        structureblockentity.setStructureSize(size);
        structureblockentity.setMode(StructureMode.SAVE);
        structureblockentity.setShowBoundingBox(true);
    }

    public static StructureBlockEntity prepareTestStructure(GameTestInfo gameTestInfo, BlockPos pos, Rotation rotation, ServerLevel level) {
        Vec3i vec3i = level.getStructureManager()
            .get(ResourceLocation.parse(gameTestInfo.getStructureName()))
            .orElseThrow(() -> new IllegalStateException("Missing test structure: " + gameTestInfo.getStructureName()))
            .getSize();
        BoundingBox boundingbox = getStructureBoundingBox(pos, vec3i, rotation);
        BlockPos blockpos;
        if (rotation == Rotation.NONE) {
            blockpos = pos;
        } else if (rotation == Rotation.CLOCKWISE_90) {
            blockpos = pos.offset(vec3i.getZ() - 1, 0, 0);
        } else if (rotation == Rotation.CLOCKWISE_180) {
            blockpos = pos.offset(vec3i.getX() - 1, 0, vec3i.getZ() - 1);
        } else {
            if (rotation != Rotation.COUNTERCLOCKWISE_90) {
                throw new IllegalArgumentException("Invalid rotation: " + rotation);
            }

            blockpos = pos.offset(0, 0, vec3i.getX() - 1);
        }

        forceLoadChunks(boundingbox, level);
        clearSpaceForStructure(boundingbox, level);
        return createStructureBlock(gameTestInfo, blockpos.below(), rotation, level);
    }

    public static void encaseStructure(AABB bounds, ServerLevel level, boolean placeBarriers) {
        BlockPos blockpos = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ).offset(-1, 0, -1);
        BlockPos blockpos1 = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        BlockPos.betweenClosedStream(blockpos, blockpos1)
            .forEach(
                p_326745_ -> {
                    boolean flag = p_326745_.getX() == blockpos.getX()
                        || p_326745_.getX() == blockpos1.getX()
                        || p_326745_.getZ() == blockpos.getZ()
                        || p_326745_.getZ() == blockpos1.getZ();
                    boolean flag1 = p_326745_.getY() == blockpos1.getY();
                    if (flag || flag1 && placeBarriers) {
                        level.setBlockAndUpdate(p_326745_, Blocks.BARRIER.defaultBlockState());
                    }
                }
            );
    }

    public static void removeBarriers(AABB bounds, ServerLevel level) {
        BlockPos blockpos = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ).offset(-1, 0, -1);
        BlockPos blockpos1 = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        BlockPos.betweenClosedStream(blockpos, blockpos1)
            .forEach(
                p_326740_ -> {
                    boolean flag = p_326740_.getX() == blockpos.getX()
                        || p_326740_.getX() == blockpos1.getX()
                        || p_326740_.getZ() == blockpos.getZ()
                        || p_326740_.getZ() == blockpos1.getZ();
                    boolean flag1 = p_326740_.getY() == blockpos1.getY();
                    if (level.getBlockState(p_326740_).is(Blocks.BARRIER) && (flag || flag1)) {
                        level.setBlockAndUpdate(p_326740_, Blocks.AIR.defaultBlockState());
                    }
                }
            );
    }

    private static void forceLoadChunks(BoundingBox boundingBox, ServerLevel level) {
        boundingBox.intersectingChunks().forEach(p_308480_ -> level.setChunkForced(p_308480_.x, p_308480_.z, true));
    }

    public static void clearSpaceForStructure(BoundingBox boundingBox, ServerLevel level) {
        int i = boundingBox.minY() - 1;
        BoundingBox boundingbox = new BoundingBox(
            boundingBox.minX() - 2, boundingBox.minY() - 3, boundingBox.minZ() - 3, boundingBox.maxX() + 3, boundingBox.maxY() + 20, boundingBox.maxZ() + 3
        );
        BlockPos.betweenClosedStream(boundingbox).forEach(p_177748_ -> clearBlock(i, p_177748_, level));
        level.getBlockTicks().clearArea(boundingbox);
        level.clearBlockEvents(boundingbox);
        AABB aabb = AABB.of(boundingbox);
        List<Entity> list = level.getEntitiesOfClass(Entity.class, aabb, p_177750_ -> !(p_177750_ instanceof Player));
        list.forEach(Entity::discard);
    }

    public static BlockPos getTransformedFarCorner(BlockPos pos, Vec3i offset, Rotation rotation) {
        BlockPos blockpos = pos.offset(offset).offset(-1, -1, -1);
        return StructureTemplate.transform(blockpos, Mirror.NONE, rotation, pos);
    }

    public static BoundingBox getStructureBoundingBox(BlockPos pos, Vec3i offset, Rotation rotation) {
        BlockPos blockpos = getTransformedFarCorner(pos, offset, rotation);
        BoundingBox boundingbox = BoundingBox.fromCorners(pos, blockpos);
        int i = Math.min(boundingbox.minX(), boundingbox.maxX());
        int j = Math.min(boundingbox.minZ(), boundingbox.maxZ());
        return boundingbox.move(pos.getX() - i, 0, pos.getZ() - j);
    }

    public static Optional<BlockPos> findStructureBlockContainingPos(BlockPos pos, int radius, ServerLevel serverLevel) {
        return findStructureBlocks(pos, radius, serverLevel).filter(p_177756_ -> doesStructureContain(p_177756_, pos, serverLevel)).findFirst();
    }

    public static Optional<BlockPos> findNearestStructureBlock(BlockPos pos, int radius, ServerLevel level) {
        Comparator<BlockPos> comparator = Comparator.comparingInt(p_177759_ -> p_177759_.distManhattan(pos));
        return findStructureBlocks(pos, radius, level).min(comparator);
    }

    public static Stream<BlockPos> findStructureByTestFunction(BlockPos pos, int radius, ServerLevel level, String testName) {
        return findStructureBlocks(pos, radius, level)
            .map(p_340630_ -> (StructureBlockEntity)level.getBlockEntity(p_340630_))
            .filter(Objects::nonNull)
            .filter(p_340628_ -> Objects.equals(p_340628_.getStructureName(), testName))
            .map(BlockEntity::getBlockPos)
            .map(BlockPos::immutable);
    }

    public static Stream<BlockPos> findStructureBlocks(BlockPos pos, int radius, ServerLevel level) {
        BoundingBox boundingbox = getBoundingBoxAtGround(pos, radius, level);
        return BlockPos.betweenClosedStream(boundingbox)
            .filter(p_319470_ -> level.getBlockState(p_319470_).is(Blocks.STRUCTURE_BLOCK))
            .map(BlockPos::immutable);
    }

    private static StructureBlockEntity createStructureBlock(GameTestInfo gameTestInfo, BlockPos pos, Rotation rotation, ServerLevel level) {
        level.setBlockAndUpdate(pos, Blocks.STRUCTURE_BLOCK.defaultBlockState());
        StructureBlockEntity structureblockentity = (StructureBlockEntity)level.getBlockEntity(pos);
        structureblockentity.setMode(StructureMode.LOAD);
        structureblockentity.setRotation(rotation);
        structureblockentity.setIgnoreEntities(false);
        structureblockentity.setStructureName(ResourceLocation.parse(gameTestInfo.getStructureName()));
        structureblockentity.setMetaData(gameTestInfo.getTestName());
        if (!structureblockentity.loadStructureInfo(level)) {
            throw new RuntimeException(
                "Failed to load structure info for test: " + gameTestInfo.getTestName() + ". Structure name: " + gameTestInfo.getStructureName()
            );
        } else {
            return structureblockentity;
        }
    }

    private static BoundingBox getBoundingBoxAtGround(BlockPos pos, int radius, ServerLevel level) {
        BlockPos blockpos = BlockPos.containing(
            (double)pos.getX(), (double)level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos).getY(), (double)pos.getZ()
        );
        return new BoundingBox(blockpos).inflatedBy(radius, 10, radius);
    }

    public static Stream<BlockPos> lookedAtStructureBlockPos(BlockPos pos, Entity entity, ServerLevel level) {
        int i = 200;
        Vec3 vec3 = entity.getEyePosition();
        Vec3 vec31 = vec3.add(entity.getLookAngle().scale(200.0));
        return findStructureBlocks(pos, 200, level)
            .map(p_319477_ -> level.getBlockEntity(p_319477_, BlockEntityType.STRUCTURE_BLOCK))
            .flatMap(Optional::stream)
            .filter(p_319475_ -> getStructureBounds(p_319475_).clip(vec3, vec31).isPresent())
            .map(BlockEntity::getBlockPos)
            .sorted(Comparator.comparing(pos::distSqr))
            .limit(1L);
    }

    private static void clearBlock(int structureBlockY, BlockPos pos, ServerLevel serverLevel) {
        BlockState blockstate;
        if (pos.getY() < structureBlockY) {
            blockstate = Blocks.STONE.defaultBlockState();
        } else {
            blockstate = Blocks.AIR.defaultBlockState();
        }

        BlockInput blockinput = new BlockInput(blockstate, Collections.emptySet(), null);
        blockinput.place(serverLevel, pos, 2);
        serverLevel.blockUpdated(pos, blockstate.getBlock());
    }

    private static boolean doesStructureContain(BlockPos structureBlockPos, BlockPos posToTest, ServerLevel serverLevel) {
        StructureBlockEntity structureblockentity = (StructureBlockEntity)serverLevel.getBlockEntity(structureBlockPos);
        return getStructureBoundingBox(structureblockentity).isInside(posToTest);
    }
}
