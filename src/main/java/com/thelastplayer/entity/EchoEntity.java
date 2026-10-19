package com.thelastplayer.entity;

import com.thelastplayer.story.StoryManager;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.FlyingMovementController;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.UUID;

/**
 * ECHO - a small floating creature of blue glass.
 * mood: 0 = happy, 1 = uneasy, 2 = afraid, 3 = breaking, 4 = fading
 */
public class EchoEntity extends CreatureEntity {
    private static final DataParameter<Integer> MOOD =
            EntityDataManager.defineId(EchoEntity.class, DataSerializers.INT);

    private UUID ownerId;
    private boolean follow = true;
    private boolean staring;
    private int touchCooldown;

    public EchoEntity(EntityType<? extends EchoEntity> type, World world) {
        super(type, world);
        this.moveControl = new FlyingMovementController(this, 20, true);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MobEntity.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    @Override
    protected PathNavigator createNavigation(World world) {
        FlyingPathNavigator nav = new FlyingPathNavigator(this, world);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new EchoFollowGoal(this));
        this.goalSelector.addGoal(2, new EchoStareGoal(this));
        this.goalSelector.addGoal(3, new LookAtGoal(this, PlayerEntity.class, 10.0F));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MOOD, 0);
    }

    // ---------------------------------------------------------------- state
    public int getMood() { return this.entityData.get(MOOD); }
    public void setMood(int mood) { this.entityData.set(MOOD, Math.max(0, Math.min(4, mood))); }

    public UUID getOwnerId() { return ownerId; }
    public void setOwner(UUID id) { this.ownerId = id; }
    public PlayerEntity getOwnerPlayer() { return ownerId == null ? null : this.level.getPlayerByUUID(ownerId); }

    public boolean isFollowing() { return follow; }
    public void setFollowing(boolean f) { this.follow = f; }

    public boolean isStaring() { return staring; }
    public void setStaring(boolean s) { this.staring = s; }

    // ---------------------------------------------------------------- behaviour
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source == DamageSource.OUT_OF_WORLD) return super.hurt(source, amount);
        return false;   // ECHO can't be hurt
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier) { return false; }

    @Override
    public boolean removeWhenFarAway(double distanceSq) { return false; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.touchCooldown > 0) this.touchCooldown--;
        if (this.level.isClientSide && this.random.nextInt(14) == 0) {
            this.level.addParticle(getMood() >= 3 ? ParticleTypes.SMOKE : ParticleTypes.END_ROD,
                    this.getRandomX(0.4D), this.getY() + 0.4D, this.getRandomZ(0.4D), 0.0D, 0.01D, 0.0D);
        }
    }

    @Override
    protected ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        if (!this.level.isClientSide && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity
                && touchCooldown <= 0) {
            touchCooldown = 40;
            ItemStack held = player.getItemInHand(hand);
            if (!held.isEmpty() && ItemTags.FLOWERS.contains(held.getItem())) {
                if (!player.abilities.instabuild) held.shrink(1);
                StoryManager.onFlowerGift((ServerPlayerEntity) player, this);
            } else {
                StoryManager.onTouchEcho((ServerPlayerEntity) player, this);
            }
        }
        return ActionResultType.sidedSuccess(this.level.isClientSide);
    }

    // ---------------------------------------------------------------- save / net
    @Override
    public void addAdditionalSaveData(CompoundNBT tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putInt("Mood", getMood());
        tag.putBoolean("Follow", follow);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        setMood(tag.getInt("Mood"));
        follow = !tag.contains("Follow") || tag.getBoolean("Follow");
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
