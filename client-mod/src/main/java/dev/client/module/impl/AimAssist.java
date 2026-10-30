package dev.client.module.impl;

import dev.client.module.BoolSetting;
import dev.client.module.Module;
import dev.client.module.NumSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public class AimAssist
extends Module {
    private final NumSetting range = this.add(new NumSetting("Range", 4.5, 1.0, 8.0, 0.1));
    private final NumSetting fov = this.add(new NumSetting("FOV", 360.0, 10.0, 360.0, 5.0));
    private final NumSetting smooth = this.add(new NumSetting("Smoothness", 3.0, 0.5, 20.0, 0.5));
    private final NumSetting inertia = this.add(new NumSetting("Inertia", 6.0, 1.0, 30.0, 0.5));
    private final NumSetting maxSpeed = this.add(new NumSetting("Max deg/s", 180.0, 30.0, 720.0, 10.0));
    private final BoolSetting vertical = this.add(new BoolSetting("Vertical", true));
    private final BoolSetting onlyClick = this.add(new BoolSetting("Only when attacking", true));
    private final BoolSetting playersOnly = this.add(new BoolSetting("Players only", true));
    private float velYaw;
    private float velPitch;

    public AimAssist() {
        super("AimAssist", Module.Category.COMBAT);
    }

    @Override
    public void onDisable() {
        this.velPitch = 0.0f;
        this.velYaw = 0.0f;
    }

    @Override
    public void onFrame(float dt) {
        if (Freecam.active) {
            this.velYaw = 0.0f;
            this.velPitch = 0.0f;
            return;
        }
        boolean active;
        if (dt <= 0.0f) {
            return;
        }
        if (AimAssist.mc.currentScreen != null || AimAssist.mc.player == null || AimAssist.mc.world == null) {
            this.velPitch = 0.0f;
            this.velYaw = 0.0f;
            return;
        }
        float targetVelYaw = 0.0f;
        float targetVelPitch = 0.0f;
        boolean bl = active = !this.onlyClick.value || AimAssist.mc.options.keyAttack.isPressed();
        if (active) {
            Vec3d eye = AimAssist.mc.player.getCameraPosVec(1.0f);
            boolean fullCircle = this.fov.value >= 360.0;
            double bestScore = Double.MAX_VALUE;
            boolean found = false;
            float bestDy = 0.0f;
            float bestDp = 0.0f;
            for (Entity e : AimAssist.mc.world.getEntities()) {
                if (!(e instanceof LivingEntity) || e == AimAssist.mc.player || !e.isAlive() || this.playersOnly.value && !(e instanceof PlayerEntity) || (double)AimAssist.mc.player.distanceTo(e) > this.range.value) continue;
                Vec3d p = new Vec3d(e.getX(), e.getY() + (double)e.getHeight() * 0.7, e.getZ());
                double dx = p.x - eye.x;
                double dy = p.y - eye.y;
                double dz = p.z - eye.z;
                float tYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                float tPitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
                float dYaw = MathHelper.wrapDegrees((float)(tYaw - AimAssist.mc.player.yaw));
                float dPitch = tPitch - AimAssist.mc.player.pitch;
                double ang = Math.hypot(dYaw, dPitch);
                if (!fullCircle && ang > this.fov.value / 2.0) continue;
                double d = fullCircle ? (double)AimAssist.mc.player.distanceTo(e) * 0.5 : 0.0;
                double score = ang + d;
                if (!(score < bestScore)) continue;
                bestScore = score;
                bestDy = dYaw;
                bestDp = dPitch;
                found = true;
            }
            if (found) {
                targetVelYaw = bestDy * this.smooth.f();
                targetVelPitch = this.vertical.value ? bestDp * this.smooth.f() : 0.0f;
            }
        }
        float cap = this.maxSpeed.f();
        targetVelYaw = MathHelper.clamp((float)targetVelYaw, (float)(-cap), (float)cap);
        targetVelPitch = MathHelper.clamp((float)targetVelPitch, (float)(-cap), (float)cap);
        float a = 1.0f - (float)Math.exp(-this.inertia.value * (double)dt);
        this.velYaw += (targetVelYaw - this.velYaw) * a;
        this.velPitch += (targetVelPitch - this.velPitch) * a;
        if (Math.abs(this.velYaw) < 0.01f && Math.abs(this.velPitch) < 0.01f) {
            return;
        }
        AimAssist.mc.player.yaw += this.velYaw * dt;
        AimAssist.mc.player.pitch = MathHelper.clamp((float)(AimAssist.mc.player.pitch + this.velPitch * dt), (float)-90.0f, (float)90.0f);
    }
}

