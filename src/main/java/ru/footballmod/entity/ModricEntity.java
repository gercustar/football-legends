package ru.footballmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Лука Модрич (Реал). Мозг команды.
 * Способность «Дирижёр»: вся своя команда рядом получает скорость и силу.
 * Пассивно: вместо удара по воротам отдаёт точный пас ближайшему партнёру.
 */
public class ModricEntity extends FootballerEntity {
    public ModricEntity(EntityType<? extends ModricEntity> type, World world) {
        super(type, world);
    }

    @Override public Team team() { return Team.REAL_MADRID; }
    @Override protected double kickPower() { return 1.2; }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        int buffed = 0;
        for (FootballerEntity mate : nearbyFootballers(16, false)) {
            mate.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 160, 0));
            mate.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 160, 0));
            buffed++;
            if (getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.NOTE, mate.getX(), mate.getY() + 2.2, mate.getZ(), 3, 0.3, 0.2, 0.3, 1);
            }
        }
        addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 160, 0));
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.NOTE, getX(), getY() + 2.2, getZ(), 5, 0.4, 0.2, 0.4, 1);
        }
        playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 1.0f, 1.0f);
        shout(Text.translatable("msg.ronaldomessi.modric_conductor", buffed));
    }

    @Override
    public void kickBall(SoccerBallEntity ball) {
        if (!passToNearestTeammate(ball)) super.kickBall(ball);
    }
}
