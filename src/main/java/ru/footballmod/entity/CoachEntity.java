package ru.footballmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import ru.footballmod.academy.Academy;

/** Тренер академии: ПКМ — меню заданий. Стоит на месте, его нельзя ранить (кроме творческого режима). */
public class CoachEntity extends FootballerEntity {
    public CoachEntity(EntityType<? extends CoachEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
        setPersistent();
    }

    @Override public Team team() { return Team.NEUTRAL; }
    @Override protected double kickPower() { return 0; }
    @Override protected void useSpecialAbility(LivingEntity target) {}

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        goalSelector.add(6, new LookAroundGoal(this));
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (player instanceof ServerPlayerEntity sp) Academy.open(sp, this);
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return source.isSourceCreativePlayer() && super.damage(source, amount);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {}

    @Override
    public boolean cannotDespawn() {
        return true;
    }
}
