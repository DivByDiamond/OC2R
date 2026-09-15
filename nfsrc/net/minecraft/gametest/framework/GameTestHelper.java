package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GameTestHelper {
    public final GameTestInfo testInfo;
    private boolean finalCheckAdded;

    public GameTestHelper(GameTestInfo testInfo) {
        this.testInfo = testInfo;
    }

    public ServerLevel getLevel() {
        return this.testInfo.getLevel();
    }

    public BlockState getBlockState(BlockPos pos) {
        return this.getLevel().getBlockState(this.absolutePos(pos));
    }

    public <T extends BlockEntity> T getBlockEntity(BlockPos pos) {
        BlockEntity blockentity = this.getLevel().getBlockEntity(this.absolutePos(pos));
        if (blockentity == null) {
            throw new GameTestAssertPosException("Missing block entity", this.absolutePos(pos), pos, this.testInfo.getTick());
        } else {
            return (T)blockentity;
        }
    }

    public void killAllEntities() {
        this.killAllEntitiesOfClass(Entity.class);
    }

    public void killAllEntitiesOfClass(Class entityClass) {
        AABB aabb = this.getBounds();
        List<Entity> list = this.getLevel().getEntitiesOfClass(entityClass, aabb.inflate(1.0), p_177131_ -> !(p_177131_ instanceof Player));
        list.forEach(Entity::kill);
    }

    public ItemEntity spawnItem(Item item, Vec3 pos) {
        ServerLevel serverlevel = this.getLevel();
        Vec3 vec3 = this.absoluteVec(pos);
        ItemEntity itementity = new ItemEntity(serverlevel, vec3.x, vec3.y, vec3.z, new ItemStack(item, 1));
        itementity.setDeltaMovement(0.0, 0.0, 0.0);
        serverlevel.addFreshEntity(itementity);
        return itementity;
    }

    public ItemEntity spawnItem(Item item, float x, float y, float z) {
        return this.spawnItem(item, new Vec3((double)x, (double)y, (double)z));
    }

    public ItemEntity spawnItem(Item item, BlockPos pos) {
        return this.spawnItem(item, (float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
    }

    public <E extends Entity> E spawn(EntityType<E> type, BlockPos pos) {
        return this.spawn(type, Vec3.atBottomCenterOf(pos));
    }

    public <E extends Entity> E spawn(EntityType<E> type, Vec3 pos) {
        ServerLevel serverlevel = this.getLevel();
        E e = type.create(serverlevel);
        if (e == null) {
            throw new NullPointerException("Failed to create entity " + type.builtInRegistryHolder().key().location());
        } else {
            if (e instanceof Mob mob) {
                mob.setPersistenceRequired();
            }

            Vec3 vec3 = this.absoluteVec(pos);
            e.moveTo(vec3.x, vec3.y, vec3.z, e.getYRot(), e.getXRot());
            serverlevel.addFreshEntity(e);
            return e;
        }
    }

    public <E extends Entity> E findOneEntity(EntityType<E> type) {
        return this.findClosestEntity(type, 0, 0, 0, 2.147483647E9);
    }

    public <E extends Entity> E findClosestEntity(EntityType<E> type, int x, int y, int z, double radius) {
        List<E> list = this.findEntities(type, x, y, z, radius);
        if (list.isEmpty()) {
            throw new GameTestAssertException("Expected " + type.toShortString() + " to exist around " + x + "," + y + "," + z);
        } else if (list.size() > 1) {
            throw new GameTestAssertException(
                "Expected only one "
                    + type.toShortString()
                    + " to exist around "
                    + x
                    + ","
                    + y
                    + ","
                    + z
                    + ", but found "
                    + list.size()
            );
        } else {
            Vec3 vec3 = this.absoluteVec(new Vec3((double)x, (double)y, (double)z));
            list.sort((p_319453_, p_319454_) -> {
                double d0 = p_319453_.position().distanceTo(vec3);
                double d1 = p_319454_.position().distanceTo(vec3);
                return Double.compare(d0, d1);
            });
            return list.get(0);
        }
    }

    public <E extends Entity> List<E> findEntities(EntityType<E> type, int x, int y, int z, double radius) {
        return this.findEntities(type, Vec3.atBottomCenterOf(new BlockPos(x, y, z)), radius);
    }

    public <E extends Entity> List<E> findEntities(EntityType<E> type, Vec3 pos, double radius) {
        ServerLevel serverlevel = this.getLevel();
        Vec3 vec3 = this.absoluteVec(pos);
        AABB aabb = this.testInfo.getStructureBounds();
        AABB aabb1 = new AABB(vec3.add(-radius, -radius, -radius), vec3.add(radius, radius, radius));
        return serverlevel.getEntities(type, aabb, p_319451_ -> p_319451_.getBoundingBox().intersects(aabb1) && p_319451_.isAlive());
    }

    public <E extends Entity> E spawn(EntityType<E> type, int x, int y, int z) {
        return this.spawn(type, new BlockPos(x, y, z));
    }

    public <E extends Entity> E spawn(EntityType<E> type, float x, float y, float z) {
        return this.spawn(type, new Vec3((double)x, (double)y, (double)z));
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, BlockPos pos) {
        E e = (E)this.spawn(type, pos);
        e.removeFreeWill();
        return e;
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, int x, int y, int z) {
        return this.spawnWithNoFreeWill(type, new BlockPos(x, y, z));
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, Vec3 pos) {
        E e = (E)this.spawn(type, pos);
        e.removeFreeWill();
        return e;
    }

    public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, float x, float y, float z) {
        return this.spawnWithNoFreeWill(type, new Vec3((double)x, (double)y, (double)z));
    }

    public void moveTo(Mob mob, float x, float y, float z) {
        Vec3 vec3 = this.absoluteVec(new Vec3((double)x, (double)y, (double)z));
        mob.moveTo(vec3.x, vec3.y, vec3.z, mob.getYRot(), mob.getXRot());
    }

    public GameTestSequence walkTo(Mob mob, BlockPos pos, float speed) {
        return this.startSequence().thenExecuteAfter(2, () -> {
            Path path = mob.getNavigation().createPath(this.absolutePos(pos), 0);
            mob.getNavigation().moveTo(path, (double)speed);
        });
    }

    public void pressButton(int x, int y, int z) {
        this.pressButton(new BlockPos(x, y, z));
    }

    public void pressButton(BlockPos pos) {
        this.assertBlockState(pos, p_177212_ -> p_177212_.is(BlockTags.BUTTONS), () -> "Expected button");
        BlockPos blockpos = this.absolutePos(pos);
        BlockState blockstate = this.getLevel().getBlockState(blockpos);
        ButtonBlock buttonblock = (ButtonBlock)blockstate.getBlock();
        buttonblock.press(blockstate, this.getLevel(), blockpos, null);
    }

    public void useBlock(BlockPos pos) {
        this.useBlock(pos, this.makeMockPlayer(GameType.CREATIVE));
    }

    public void useBlock(BlockPos pos, Player player) {
        BlockPos blockpos = this.absolutePos(pos);
        this.useBlock(pos, player, new BlockHitResult(Vec3.atCenterOf(blockpos), Direction.NORTH, blockpos, true));
    }

    public void useBlock(BlockPos pos, Player player, BlockHitResult result) {
        BlockPos blockpos = this.absolutePos(pos);
        BlockState blockstate = this.getLevel().getBlockState(blockpos);
        InteractionHand interactionhand = InteractionHand.MAIN_HAND;
        ItemInteractionResult iteminteractionresult = blockstate.useItemOn(
            player.getItemInHand(interactionhand), this.getLevel(), player, interactionhand, result
        );
        if (!iteminteractionresult.consumesAction()) {
            if (iteminteractionresult != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                || !blockstate.useWithoutItem(this.getLevel(), player, result).consumesAction()) {
                UseOnContext useoncontext = new UseOnContext(player, interactionhand, result);
                player.getItemInHand(interactionhand).useOn(useoncontext);
            }
        }
    }

    public LivingEntity makeAboutToDrown(LivingEntity entity) {
        entity.setAirSupply(0);
        entity.setHealth(0.25F);
        return entity;
    }

    public LivingEntity withLowHealth(LivingEntity entity) {
        entity.setHealth(0.25F);
        return entity;
    }

    public Player makeMockPlayer(final GameType gameType) {
        return new Player(this.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return gameType == GameType.SPECTATOR;
            }

            @Override
            public boolean isCreative() {
                return gameType.isCreative();
            }

            @Override
            public boolean isLocalPlayer() {
                return true;
            }
        };
    }

    @Deprecated(
        forRemoval = true
    )
    public ServerPlayer makeMockServerPlayerInLevel() {
        CommonListenerCookie commonlistenercookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer serverplayer = new ServerPlayer(
            this.getLevel().getServer(), this.getLevel(), commonlistenercookie.gameProfile(), commonlistenercookie.clientInformation()
        ) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        this.getLevel().getServer().getPlayerList().placeNewPlayer(connection, serverplayer, commonlistenercookie);
        return serverplayer;
    }

    public void pullLever(int x, int y, int z) {
        this.pullLever(new BlockPos(x, y, z));
    }

    public void pullLever(BlockPos pos) {
        this.assertBlockPresent(Blocks.LEVER, pos);
        BlockPos blockpos = this.absolutePos(pos);
        BlockState blockstate = this.getLevel().getBlockState(blockpos);
        LeverBlock leverblock = (LeverBlock)blockstate.getBlock();
        leverblock.pull(blockstate, this.getLevel(), blockpos, null);
    }

    public void pulseRedstone(BlockPos pos, long delay) {
        this.setBlock(pos, Blocks.REDSTONE_BLOCK);
        this.runAfterDelay(delay, () -> this.setBlock(pos, Blocks.AIR));
    }

    public void destroyBlock(BlockPos pos) {
        this.getLevel().destroyBlock(this.absolutePos(pos), false, null);
    }

    public void setBlock(int x, int y, int z, Block block) {
        this.setBlock(new BlockPos(x, y, z), block);
    }

    public void setBlock(int x, int y, int z, BlockState state) {
        this.setBlock(new BlockPos(x, y, z), state);
    }

    public void setBlock(BlockPos pos, Block block) {
        this.setBlock(pos, block.defaultBlockState());
    }

    public void setBlock(BlockPos pos, BlockState state) {
        this.getLevel().setBlock(this.absolutePos(pos), state, 3);
    }

    public void setNight() {
        this.setDayTime(13000);
    }

    public void setDayTime(int time) {
        this.getLevel().setDayTime((long)time);
    }

    public void assertBlockPresent(Block block, int x, int y, int z) {
        this.assertBlockPresent(block, new BlockPos(x, y, z));
    }

    public void assertBlockPresent(Block block, BlockPos pos) {
        BlockState blockstate = this.getBlockState(pos);
        this.assertBlock(
            pos,
            p_177216_ -> blockstate.is(block),
            "Expected " + block.getName().getString() + ", got " + blockstate.getBlock().getName().getString()
        );
    }

    public void assertBlockNotPresent(Block block, int x, int y, int z) {
        this.assertBlockNotPresent(block, new BlockPos(x, y, z));
    }

    public void assertBlockNotPresent(Block block, BlockPos pos) {
        this.assertBlock(pos, p_177251_ -> !this.getBlockState(pos).is(block), "Did not expect " + block.getName().getString());
    }

    public void succeedWhenBlockPresent(Block block, int x, int y, int z) {
        this.succeedWhenBlockPresent(block, new BlockPos(x, y, z));
    }

    public void succeedWhenBlockPresent(Block block, BlockPos pos) {
        this.succeedWhen(() -> this.assertBlockPresent(block, pos));
    }

    public void assertBlock(BlockPos pos, Predicate<Block> predicate, String exceptionMessage) {
        this.assertBlock(pos, predicate, () -> exceptionMessage);
    }

    public void assertBlock(BlockPos pos, Predicate<Block> predicate, Supplier<String> exceptionMessage) {
        this.assertBlockState(pos, p_177296_ -> predicate.test(p_177296_.getBlock()), exceptionMessage);
    }

    public <T extends Comparable<T>> void assertBlockProperty(BlockPos pos, Property<T> property, T value) {
        BlockState blockstate = this.getBlockState(pos);
        boolean flag = blockstate.hasProperty(property);
        if (!flag || !blockstate.<T>getValue(property).equals(value)) {
            String s = flag ? "was " + blockstate.getValue(property) : "property " + property.getName() + " is missing";
            String s1 = String.format(Locale.ROOT, "Expected property %s to be %s, %s", property.getName(), value, s);
            throw new GameTestAssertPosException(s1, this.absolutePos(pos), pos, this.testInfo.getTick());
        }
    }

    public <T extends Comparable<T>> void assertBlockProperty(BlockPos pos, Property<T> property, Predicate<T> predicate, String exceptionMessage) {
        this.assertBlockState(pos, p_277264_ -> {
            if (!p_277264_.hasProperty(property)) {
                return false;
            } else {
                T t = p_277264_.getValue(property);
                return predicate.test(t);
            }
        }, () -> exceptionMessage);
    }

    public void assertBlockState(BlockPos pos, Predicate<BlockState> predicate, Supplier<String> exceptionMessage) {
        BlockState blockstate = this.getBlockState(pos);
        if (!predicate.test(blockstate)) {
            throw new GameTestAssertPosException(exceptionMessage.get(), this.absolutePos(pos), pos, this.testInfo.getTick());
        }
    }

    public <T extends BlockEntity> void assertBlockEntityData(BlockPos pos, Predicate<T> predicate, Supplier<String> exceptionMessage) {
        T t = this.getBlockEntity(pos);
        if (!predicate.test(t)) {
            throw new GameTestAssertPosException(exceptionMessage.get(), this.absolutePos(pos), pos, this.testInfo.getTick());
        }
    }

    public void assertRedstoneSignal(BlockPos pos, Direction direction, IntPredicate signalStrengthPredicate, Supplier<String> exceptionMessage) {
        BlockPos blockpos = this.absolutePos(pos);
        ServerLevel serverlevel = this.getLevel();
        BlockState blockstate = serverlevel.getBlockState(blockpos);
        int i = blockstate.getSignal(serverlevel, blockpos, direction);
        if (!signalStrengthPredicate.test(i)) {
            throw new GameTestAssertPosException(exceptionMessage.get(), blockpos, pos, this.testInfo.getTick());
        }
    }

    public void assertEntityPresent(EntityType<?> type) {
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestAssertException("Expected " + type.toShortString() + " to exist");
        }
    }

    public void assertEntityPresent(EntityType<?> type, int x, int y, int z) {
        this.assertEntityPresent(type, new BlockPos(x, y, z));
    }

    public void assertEntityPresent(EntityType<?> type, BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(type, new AABB(blockpos), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestAssertPosException("Expected " + type.toShortString(), blockpos, pos, this.testInfo.getTick());
        }
    }

    public void assertEntityPresent(EntityType<?> entityType, Vec3 startPos, Vec3 endPos) {
        List<? extends Entity> list = this.getLevel().getEntities(entityType, new AABB(startPos, endPos), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestAssertPosException(
                "Expected " + entityType.toShortString() + " between ", BlockPos.containing(startPos), BlockPos.containing(endPos), this.testInfo.getTick()
            );
        }
    }

    public void assertEntitiesPresent(EntityType<?> entityType, int count) {
        List<? extends Entity> list = this.getLevel().getEntities(entityType, this.getBounds(), Entity::isAlive);
        if (list.size() != count) {
            throw new GameTestAssertException("Expected " + count + " of type " + entityType.toShortString() + " to exist, found " + list.size());
        }
    }

    public void assertEntitiesPresent(EntityType<?> entityType, BlockPos pos, int count, double radius) {
        BlockPos blockpos = this.absolutePos(pos);
        List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)entityType, pos, radius);
        if (list.size() != count) {
            throw new GameTestAssertPosException(
                "Expected " + count + " entities of type " + entityType.toShortString() + ", actual number of entities found=" + list.size(),
                blockpos,
                pos,
                this.testInfo.getTick()
            );
        }
    }

    public void assertEntityPresent(EntityType<?> type, BlockPos pos, double expansionAmount) {
        List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)type, pos, expansionAmount);
        if (list.isEmpty()) {
            BlockPos blockpos = this.absolutePos(pos);
            throw new GameTestAssertPosException("Expected " + type.toShortString(), blockpos, pos, this.testInfo.getTick());
        }
    }

    public <T extends Entity> List<T> getEntities(EntityType<T> entityType, BlockPos pos, double radius) {
        BlockPos blockpos = this.absolutePos(pos);
        return this.getLevel().getEntities(entityType, new AABB(blockpos).inflate(radius), Entity::isAlive);
    }

    public <T extends Entity> List<T> getEntities(EntityType<T> entityType) {
        return this.getLevel().getEntities(entityType, this.getBounds(), Entity::isAlive);
    }

    public void assertEntityInstancePresent(Entity entity, int x, int y, int z) {
        this.assertEntityInstancePresent(entity, new BlockPos(x, y, z));
    }

    public void assertEntityInstancePresent(Entity entity, BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(entity.getType(), new AABB(blockpos), Entity::isAlive);
        list.stream()
            .filter(p_177139_ -> p_177139_ == entity)
            .findFirst()
            .orElseThrow(() -> new GameTestAssertPosException("Expected " + entity.getType().toShortString(), blockpos, pos, this.testInfo.getTick()));
    }

    public void assertItemEntityCountIs(Item item, BlockPos pos, double expansionAmount, int count) {
        BlockPos blockpos = this.absolutePos(pos);
        List<ItemEntity> list = this.getLevel().getEntities(EntityType.ITEM, new AABB(blockpos).inflate(expansionAmount), Entity::isAlive);
        int i = 0;

        for (ItemEntity itementity : list) {
            ItemStack itemstack = itementity.getItem();
            if (itemstack.is(item)) {
                i += itemstack.getCount();
            }
        }

        if (i != count) {
            throw new GameTestAssertPosException(
                "Expected " + count + " " + item.getDescription().getString() + " items to exist (found " + i + ")",
                blockpos,
                pos,
                this.testInfo.getTick()
            );
        }
    }

    public void assertItemEntityPresent(Item item, BlockPos pos, double expansionAmount) {
        BlockPos blockpos = this.absolutePos(pos);

        for (Entity entity : this.getLevel().getEntities(EntityType.ITEM, new AABB(blockpos).inflate(expansionAmount), Entity::isAlive)) {
            ItemEntity itementity = (ItemEntity)entity;
            if (itementity.getItem().getItem().equals(item)) {
                return;
            }
        }

        throw new GameTestAssertPosException("Expected " + item.getDescription().getString() + " item", blockpos, pos, this.testInfo.getTick());
    }

    public void assertItemEntityNotPresent(Item item, BlockPos pos, double radius) {
        BlockPos blockpos = this.absolutePos(pos);

        for (Entity entity : this.getLevel().getEntities(EntityType.ITEM, new AABB(blockpos).inflate(radius), Entity::isAlive)) {
            ItemEntity itementity = (ItemEntity)entity;
            if (itementity.getItem().getItem().equals(item)) {
                throw new GameTestAssertPosException(
                    "Did not expect " + item.getDescription().getString() + " item", blockpos, pos, this.testInfo.getTick()
                );
            }
        }
    }

    public void assertItemEntityPresent(Item item) {
        for (Entity entity : this.getLevel().getEntities(EntityType.ITEM, this.getBounds(), Entity::isAlive)) {
            ItemEntity itementity = (ItemEntity)entity;
            if (itementity.getItem().getItem().equals(item)) {
                return;
            }
        }

        throw new GameTestAssertException("Expected " + item.getDescription().getString() + " item");
    }

    public void assertItemEntityNotPresent(Item item) {
        for (Entity entity : this.getLevel().getEntities(EntityType.ITEM, this.getBounds(), Entity::isAlive)) {
            ItemEntity itementity = (ItemEntity)entity;
            if (itementity.getItem().getItem().equals(item)) {
                throw new GameTestAssertException("Did not expect " + item.getDescription().getString() + " item");
            }
        }
    }

    public void assertEntityNotPresent(EntityType<?> type) {
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), Entity::isAlive);
        if (!list.isEmpty()) {
            throw new GameTestAssertException("Did not expect " + type.toShortString() + " to exist");
        }
    }

    public void assertEntityNotPresent(EntityType<?> type, int x, int y, int z) {
        this.assertEntityNotPresent(type, new BlockPos(x, y, z));
    }

    public void assertEntityNotPresent(EntityType<?> type, BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        List<? extends Entity> list = this.getLevel().getEntities(type, new AABB(blockpos), Entity::isAlive);
        if (!list.isEmpty()) {
            throw new GameTestAssertPosException("Did not expect " + type.toShortString(), blockpos, pos, this.testInfo.getTick());
        }
    }

    public void assertEntityNotPresent(EntityType<?> type, Vec3 from, Vec3 to) {
        List<? extends Entity> list = this.getLevel().getEntities(type, new AABB(from, to), Entity::isAlive);
        if (!list.isEmpty()) {
            throw new GameTestAssertPosException(
                "Did not expect " + type.toShortString() + " between ",
                BlockPos.containing(from),
                BlockPos.containing(to),
                this.testInfo.getTick()
            );
        }
    }

    public void assertEntityTouching(EntityType<?> type, double x, double y, double z) {
        Vec3 vec3 = new Vec3(x, y, z);
        Vec3 vec31 = this.absoluteVec(vec3);
        Predicate<? super Entity> predicate = p_177346_ -> p_177346_.getBoundingBox().intersects(vec31, vec31);
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), predicate);
        if (list.isEmpty()) {
            throw new GameTestAssertException("Expected " + type.toShortString() + " to touch " + vec31 + " (relative " + vec3 + ")");
        }
    }

    public void assertEntityNotTouching(EntityType<?> type, double x, double y, double z) {
        Vec3 vec3 = new Vec3(x, y, z);
        Vec3 vec31 = this.absoluteVec(vec3);
        Predicate<? super Entity> predicate = p_177231_ -> !p_177231_.getBoundingBox().intersects(vec31, vec31);
        List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), predicate);
        if (list.isEmpty()) {
            throw new GameTestAssertException("Did not expect " + type.toShortString() + " to touch " + vec31 + " (relative " + vec3 + ")");
        }
    }

    public <E extends Entity, T> void assertEntityData(BlockPos pos, EntityType<E> type, Function<? super E, T> entityDataGetter, @Nullable T testEntityData) {
        BlockPos blockpos = this.absolutePos(pos);
        List<E> list = this.getLevel().getEntities(type, new AABB(blockpos), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestAssertPosException("Expected " + type.toShortString(), blockpos, pos, this.testInfo.getTick());
        } else {
            for (E e : list) {
                T t = entityDataGetter.apply(e);
                if (t == null) {
                    if (testEntityData != null) {
                        throw new GameTestAssertException("Expected entity data to be: " + testEntityData + ", but was: " + t);
                    }
                } else if (!t.equals(testEntityData)) {
                    throw new GameTestAssertException("Expected entity data to be: " + testEntityData + ", but was: " + t);
                }
            }
        }
    }

    public <E extends LivingEntity> void assertEntityIsHolding(BlockPos pos, EntityType<E> entityType, Item item) {
        BlockPos blockpos = this.absolutePos(pos);
        List<E> list = this.getLevel().getEntities(entityType, new AABB(blockpos), Entity::isAlive);
        if (list.isEmpty()) {
            throw new GameTestAssertPosException("Expected entity of type: " + entityType, blockpos, pos, this.getTick());
        } else {
            for (E e : list) {
                if (e.isHolding(item)) {
                    return;
                }
            }

            throw new GameTestAssertPosException("Entity should be holding: " + item, blockpos, pos, this.getTick());
        }
    }

    public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(BlockPos pos, EntityType<E> entityType, Item item) {
        BlockPos blockpos = this.absolutePos(pos);
        List<E> list = this.getLevel().getEntities(entityType, new AABB(blockpos), p_263479_ -> p_263479_.isAlive());
        if (list.isEmpty()) {
            throw new GameTestAssertPosException("Expected " + entityType.toShortString() + " to exist", blockpos, pos, this.getTick());
        } else {
            for (E e : list) {
                if (e.getInventory().hasAnyMatching(p_263481_ -> p_263481_.is(item))) {
                    return;
                }
            }

            throw new GameTestAssertPosException("Entity inventory should contain: " + item, blockpos, pos, this.getTick());
        }
    }

    public void assertContainerEmpty(BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        BlockEntity blockentity = this.getLevel().getBlockEntity(blockpos);
        if (blockentity instanceof BaseContainerBlockEntity && !((BaseContainerBlockEntity)blockentity).isEmpty()) {
            throw new GameTestAssertException("Container should be empty");
        }
    }

    public void assertContainerContains(BlockPos pos, Item item) {
        BlockPos blockpos = this.absolutePos(pos);
        BlockEntity blockentity = this.getLevel().getBlockEntity(blockpos);
        if (!(blockentity instanceof BaseContainerBlockEntity)) {
            throw new GameTestAssertException(
                "Expected a container at " + pos + ", found " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockentity.getType())
            );
        } else if (((BaseContainerBlockEntity)blockentity).countItem(item) != 1) {
            throw new GameTestAssertException("Container should contain: " + item);
        }
    }

    public void assertSameBlockStates(BoundingBox boundingBox, BlockPos pos) {
        BlockPos.betweenClosedStream(boundingBox).forEach(p_177267_ -> {
            BlockPos blockpos = pos.offset(p_177267_.getX() - boundingBox.minX(), p_177267_.getY() - boundingBox.minY(), p_177267_.getZ() - boundingBox.minZ());
            this.assertSameBlockState(p_177267_, blockpos);
        });
    }

    public void assertSameBlockState(BlockPos testPos, BlockPos comparisonPos) {
        BlockState blockstate = this.getBlockState(testPos);
        BlockState blockstate1 = this.getBlockState(comparisonPos);
        if (blockstate != blockstate1) {
            this.fail("Incorrect state. Expected " + blockstate1 + ", got " + blockstate, testPos);
        }
    }

    public void assertAtTickTimeContainerContains(long tickTime, BlockPos pos, Item item) {
        this.runAtTickTime(tickTime, () -> this.assertContainerContains(pos, item));
    }

    public void assertAtTickTimeContainerEmpty(long tickTime, BlockPos pos) {
        this.runAtTickTime(tickTime, () -> this.assertContainerEmpty(pos));
    }

    public <E extends Entity, T> void succeedWhenEntityData(BlockPos pos, EntityType<E> type, Function<E, T> entityDataGetter, T testEntityData) {
        this.succeedWhen(() -> this.assertEntityData(pos, type, entityDataGetter, testEntityData));
    }

    public void assertEntityPosition(Entity entity, AABB box, String exceptionMessage) {
        if (!box.contains(this.relativeVec(entity.position()))) {
            this.fail(exceptionMessage);
        }
    }

    public <E extends Entity> void assertEntityProperty(E entity, Predicate<E> predicate, String name) {
        if (!predicate.test(entity)) {
            throw new GameTestAssertException("Entity " + entity + " failed " + name + " test");
        }
    }

    public <E extends Entity, T> void assertEntityProperty(E entity, Function<E, T> entityPropertyGetter, String valueName, T testEntityProperty) {
        T t = entityPropertyGetter.apply(entity);
        if (!t.equals(testEntityProperty)) {
            throw new GameTestAssertException("Entity " + entity + " value " + valueName + "=" + t + " is not equal to expected " + testEntityProperty);
        }
    }

    public void assertLivingEntityHasMobEffect(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance mobeffectinstance = entity.getEffect(effect);
        if (mobeffectinstance == null || mobeffectinstance.getAmplifier() != amplifier) {
            int i = amplifier + 1;
            throw new GameTestAssertException("Entity " + entity + " failed has " + effect.value().getDescriptionId() + " x " + i + " test");
        }
    }

    public void succeedWhenEntityPresent(EntityType<?> type, int x, int y, int z) {
        this.succeedWhenEntityPresent(type, new BlockPos(x, y, z));
    }

    public void succeedWhenEntityPresent(EntityType<?> type, BlockPos pos) {
        this.succeedWhen(() -> this.assertEntityPresent(type, pos));
    }

    public void succeedWhenEntityNotPresent(EntityType<?> type, int x, int y, int z) {
        this.succeedWhenEntityNotPresent(type, new BlockPos(x, y, z));
    }

    public void succeedWhenEntityNotPresent(EntityType<?> type, BlockPos pos) {
        this.succeedWhen(() -> this.assertEntityNotPresent(type, pos));
    }

    public void succeed() {
        this.testInfo.succeed();
    }

    private void ensureSingleFinalCheck() {
        if (this.finalCheckAdded) {
            throw new IllegalStateException("This test already has final clause");
        } else {
            this.finalCheckAdded = true;
        }
    }

    public void succeedIf(Runnable criterion) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(0L, criterion).thenSucceed();
    }

    public void succeedWhen(Runnable criterion) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil(criterion).thenSucceed();
    }

    public void succeedOnTickWhen(int tick, Runnable criterion) {
        this.ensureSingleFinalCheck();
        this.testInfo.createSequence().thenWaitUntil((long)tick, criterion).thenSucceed();
    }

    public void runAtTickTime(long tickTime, Runnable task) {
        this.testInfo.setRunAtTickTime(tickTime, task);
    }

    public void runAfterDelay(long delay, Runnable task) {
        this.runAtTickTime(this.testInfo.getTick() + delay, task);
    }

    public void randomTick(BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        ServerLevel serverlevel = this.getLevel();
        serverlevel.getBlockState(blockpos).randomTick(serverlevel, blockpos, serverlevel.random);
    }

    public void tickPrecipitation(BlockPos pos) {
        BlockPos blockpos = this.absolutePos(pos);
        ServerLevel serverlevel = this.getLevel();
        serverlevel.tickPrecipitation(blockpos);
    }

    public void tickPrecipitation() {
        AABB aabb = this.getRelativeBounds();
        int i = (int)Math.floor(aabb.maxX);
        int j = (int)Math.floor(aabb.maxZ);
        int k = (int)Math.floor(aabb.maxY);

        for (int l = (int)Math.floor(aabb.minX); l < i; l++) {
            for (int i1 = (int)Math.floor(aabb.minZ); i1 < j; i1++) {
                this.tickPrecipitation(new BlockPos(l, k, i1));
            }
        }
    }

    public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        BlockPos blockpos = this.absolutePos(new BlockPos(x, 0, z));
        return this.relativePos(this.getLevel().getHeightmapPos(heightmapType, blockpos)).getY();
    }

    public void fail(String exceptionMessage, BlockPos pos) {
        throw new GameTestAssertPosException(exceptionMessage, this.absolutePos(pos), pos, this.getTick());
    }

    public void fail(String exceptionMessage, Entity entity) {
        throw new GameTestAssertPosException(exceptionMessage, entity.blockPosition(), this.relativePos(entity.blockPosition()), this.getTick());
    }

    public void fail(String exceptionMessage) {
        throw new GameTestAssertException(exceptionMessage);
    }

    public void failIf(Runnable criterion) {
        this.testInfo.createSequence().thenWaitUntil(criterion).thenFail(() -> new GameTestAssertException("Fail conditions met"));
    }

    public void failIfEver(Runnable criterion) {
        LongStream.range(this.testInfo.getTick(), (long)this.testInfo.getTimeoutTicks())
            .forEach(p_177365_ -> this.testInfo.setRunAtTickTime(p_177365_, criterion::run));
    }

    public GameTestSequence startSequence() {
        return this.testInfo.createSequence();
    }

    public BlockPos absolutePos(BlockPos pos) {
        BlockPos blockpos = this.testInfo.getStructureBlockPos();
        BlockPos blockpos1 = blockpos.offset(pos);
        return StructureTemplate.transform(blockpos1, Mirror.NONE, this.testInfo.getRotation(), blockpos);
    }

    public BlockPos relativePos(BlockPos pos) {
        BlockPos blockpos = this.testInfo.getStructureBlockPos();
        Rotation rotation = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
        BlockPos blockpos1 = StructureTemplate.transform(pos, Mirror.NONE, rotation, blockpos);
        return blockpos1.subtract(blockpos);
    }

    public Vec3 absoluteVec(Vec3 relativeVec3) {
        Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getStructureBlockPos());
        return StructureTemplate.transform(vec3.add(relativeVec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
    }

    public Vec3 relativeVec(Vec3 absoluteVec3) {
        Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getStructureBlockPos());
        return StructureTemplate.transform(absoluteVec3.subtract(vec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
    }

    public Rotation getTestRotation() {
        return this.testInfo.getRotation();
    }

    public void assertTrue(boolean condition, String failureMessage) {
        if (!condition) {
            throw new GameTestAssertException(failureMessage);
        }
    }

    public <N> void assertValueEqual(N actual, N expected, String valueName) {
        if (!actual.equals(expected)) {
            throw new GameTestAssertException("Expected " + valueName + " to be " + expected + ", but was " + actual);
        }
    }

    public void assertFalse(boolean condition, String failureMessage) {
        if (condition) {
            throw new GameTestAssertException(failureMessage);
        }
    }

    public long getTick() {
        return this.testInfo.getTick();
    }

    public AABB getBounds() {
        return this.testInfo.getStructureBounds();
    }

    private AABB getRelativeBounds() {
        AABB aabb = this.testInfo.getStructureBounds();
        return aabb.move(BlockPos.ZERO.subtract(this.absolutePos(BlockPos.ZERO)));
    }

    public void forEveryBlockInStructure(Consumer<BlockPos> consumer) {
        AABB aabb = this.getRelativeBounds().contract(1.0, 1.0, 1.0);
        BlockPos.MutableBlockPos.betweenClosedStream(aabb).forEach(consumer);
    }

    public void onEachTick(Runnable task) {
        LongStream.range(this.testInfo.getTick(), (long)this.testInfo.getTimeoutTicks())
            .forEach(p_177283_ -> this.testInfo.setRunAtTickTime(p_177283_, task::run));
    }

    public void placeAt(Player player, ItemStack stack, BlockPos pos, Direction direction) {
        BlockPos blockpos = this.absolutePos(pos.relative(direction));
        BlockHitResult blockhitresult = new BlockHitResult(Vec3.atCenterOf(blockpos), direction, blockpos, false);
        UseOnContext useoncontext = new UseOnContext(player, InteractionHand.MAIN_HAND, blockhitresult);
        stack.useOn(useoncontext);
    }

    public void setBiome(ResourceKey<Biome> biome) {
        AABB aabb = this.getBounds();
        BlockPos blockpos = BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ);
        BlockPos blockpos1 = BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ);
        Either<Integer, CommandSyntaxException> either = FillBiomeCommand.fill(
            this.getLevel(), blockpos, blockpos1, this.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome)
        );
        if (either.right().isPresent()) {
            this.fail("Failed to set biome for test");
        }
    }
}
