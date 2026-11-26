package ru.footballmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import ru.footballmod.config.FootballConfig;

/**
 * Луис Суарес (Барселона). Мощный нападающий.
 * Способность «Укус»: бросается на соперника, кусает (урон и слабость) и немного лечится. Иногда — красная карточка!
 * Пассивно: самый сильный удар по мячу в Барселоне.
 */
public class SuarezEntity extends FootballerEntity {
    public SuarezEntity(EntityType<? extends SuarezEntity> type, World world) {
        super(type, world);
    }

    @Override public Team team() { return Team.BARCELONA; }
    @Override protected double kickPower() { return FootballConfig.get().suarezKickPower; }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        FootballConfig cfg = FootballConfig.get();
        Vec3d dir = horizontalDirTo(target);
        setVelocity(dir.x * 1.1, 0.15, dir.z * 1.1);
        velocityModified = true;
        boolean bitten = squaredDistanceTo(target) < 9 && tryAttack(target);
        if (bitten) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0));
            heal(cfg.suarezBiteHeal);
            if (getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getBodyY(0.7), target.getZ(), 6, 0.2, 0.2, 0.2, 0.1);
            }
            playSound(SoundEvents.ENTITY_FOX_BITE, 1.2f, 0.8f);
        } else {
            playSound(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
        }
        // Карточку дают только за настоящий укус
        if (bitten && random.nextFloat() < cfg.suarezRedCardChance) {
            addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 0));
            addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 160, 0));
            shout(Text.translatable("msg.ronaldomessi.suarez_red_card"));
        } else {
            shout(Text.translatable(bitten ? "msg.ronaldomessi.suarez_bite" : "msg.ronaldomessi.suarez_rush"));
        }
    }
}
