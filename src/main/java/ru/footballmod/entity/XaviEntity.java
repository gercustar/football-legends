package ru.footballmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import ru.footballmod.config.FootballConfig;

/**
 * Хави (Барселона). Мастер паса.
 * Способность «Тики-така»: лечит себя и партнёров рядом и ускоряет их.
 * Пассивно: вместо удара по воротам отдаёт точный пас ближайшему партнёру.
 */
public class XaviEntity extends FootballerEntity {
    public XaviEntity(EntityType<? extends XaviEntity> type, World world) {
        super(type, world);
    }

    @Override public Team team() { return Team.BARCELONA; }
    @Override protected double kickPower() { return FootballConfig.get().xaviKickPower; }

    @Override
    protected void useSpecialAbility(LivingEntity target) {
        int healed = 0;
        support(this);
        for (FootballerEntity mate : nearbyFootballers(16, false)) {
            support(mate);
            healed++;
        }
        playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 1.0f, 1.2f);
        shout(Text.translatable("msg.ronaldomessi.xavi_tiki_taka", healed));
    }

    private void support(FootballerEntity f) {
        f.heal(FootballConfig.get().xaviHeal);
        f.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0));
        if (getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.HEART, f.getX(), f.getY() + 2.0, f.getZ(), 3, 0.3, 0.2, 0.3, 0);
        }
    }

    @Override
    public void kickBall(SoccerBallEntity ball) {
        if (!passToNearestTeammate(ball)) super.kickBall(ball);
    }
}
