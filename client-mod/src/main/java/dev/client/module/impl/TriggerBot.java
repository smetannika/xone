package dev.client.module.impl;

import dev.client.module.BoolSetting;
import dev.client.module.Module;
import dev.client.module.NumSetting;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.EntityHitResult;

public class TriggerBot
extends Module {
    private final NumSetting cooldown = this.add(new NumSetting("Cooldown", 0.9, 0.5, 1.0, 0.05));
    private final NumSetting minDelay = this.add(new NumSetting("Min delay ms", 40.0, 0.0, 500.0, 5.0));
    private final NumSetting maxDelay = this.add(new NumSetting("Max delay ms", 120.0, 0.0, 500.0, 5.0));
    private final BoolSetting playersOnly = this.add(new BoolSetting("Players only", true));
    private final BoolSetting skipInvisible = this.add(new BoolSetting("Skip invisible", false));
    private final BoolSetting onlyCrits = this.add(new BoolSetting("Only crits", false));
    private final NumSetting critChance = this.add(new NumSetting("Crit chance %", 100.0, 0.0, 100.0, 5.0));
    private long attackAt = -1L;
    private boolean needCrit = true;

    public TriggerBot() {
        super("TriggerBot", Module.Category.COMBAT);
    }

    @Override
    public void onDisable() {
        this.attackAt = -1L;
    }

    @Override
    public void onFrame(float dt) {
        if (Freecam.active) {
            this.attackAt = -1L;
            return;
        }
        if (TriggerBot.mc.currentScreen != null || TriggerBot.mc.player == null || TriggerBot.mc.interactionManager == null) {
            return;
        }
        HitResult hit = TriggerBot.mc.crosshairTarget;
        if (!(hit instanceof EntityHitResult)) {
            this.attackAt = -1L;
            return;
        }
        Entity e = ((EntityHitResult)hit).getEntity();
        if (!(e instanceof LivingEntity) || e == TriggerBot.mc.player || !e.isAlive() || this.playersOnly.value && !(e instanceof PlayerEntity) || this.skipInvisible.value && e.isInvisible()) {
            this.attackAt = -1L;
            return;
        }
        boolean crit = this.onlyCrits.value && this.needCrit;
        if (crit && !TriggerBot.canCrit(TriggerBot.mc.player)) {
            this.attackAt = -1L;
            return;
        }
        float need = crit ? Math.max(this.cooldown.f(), 0.9f) : this.cooldown.f();
        if (TriggerBot.mc.player.getAttackCooldownProgress(0.0f) < need) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.attackAt < 0L) {
            int lo = (int)Math.min(this.minDelay.value, this.maxDelay.value);
            int hi = (int)Math.max(this.minDelay.value, this.maxDelay.value);
            this.attackAt = now + (long)(hi > lo ? ThreadLocalRandom.current().nextInt(lo, hi + 1) : lo);
        }
        if (now >= this.attackAt) {
            if (crit && !TriggerBot.canCrit(TriggerBot.mc.player)) {
                this.attackAt = -1L;
                return;
            }
            TriggerBot.mc.interactionManager.attackEntity((PlayerEntity)TriggerBot.mc.player, e);
            TriggerBot.mc.player.swingHand(Hand.MAIN_HAND);
            this.attackAt = -1L;
            this.needCrit = (double)ThreadLocalRandom.current().nextInt(100) < this.critChance.value;
        }
    }

    /** Same conditions the game uses for a critical hit: falling, not on the ground, not sprinting. */
    private static boolean canCrit(PlayerEntity p) {
        return p.fallDistance > 0.0f && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater() && !p.hasStatusEffect(StatusEffects.BLINDNESS) && !p.hasVehicle() && !p.isSprinting();
    }
}

