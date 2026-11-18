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
 * Роналдиньо (Барселона). Король финтов.
 * Способность «Финт без взгляда»: соперник теряется (замедление; у игрока ещё и головокружение), Роналдиньо проскальзывает мимо.
 * Пассивно: прежде чем ударить, чеканит мяч несколько раз.
 */
public class RonaldinhoEntity extends FootballerEntity {
    private int jugglesLeft = 3;

    public RonaldinhoEntity(EntityType<? extends RonaldinhoEntity> type, World world) {
        super(type, world);
    }

    @Override public Team team() { return Team.BARCELONA; }
    @Override protected double kickPower() { return 1.3; }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
        if (target instanceof PlayerEntity) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0));
        }
        Vec3d dir = horizontalDirTo(target);
        Vec3d side = new Vec3d(-dir.z, 0, dir.x).multiply(random.nextBoolean() ? 0.7 : -0.7);
        setVelocity(dir.x * 0.9 + side.x, 0.2, dir.z * 0.9 + side.z);
        velocityModified = true;
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.8, getZ(), 12, 0.4, 0.3, 0.4, 0.05);
        }
        playSound(SoundEvents.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.3f);
        shout(Text.translatable("msg.ronaldomessi.ronaldinho_trick"));
    }

    @Override
    public void kickBall(SoccerBallEntity ball) {
        if (jugglesLeft > 0) {
            jugglesLeft--;
            ball.setVelocity(0, 0.5, 0);   // чеканка
            ball.velocityModified = true;
            swingHand(net.minecraft.util.Hand.MAIN_HAND);
            playSound(SoundEvents.BLOCK_WOOL_STEP, 0.8f, 1.6f);
            return;
        }
        jugglesLeft = 2 + random.nextInt(3);
        super.kickBall(ball);
    }
}
