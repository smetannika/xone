package dev.client.module.impl;

import com.mojang.authlib.GameProfile;
import dev.client.module.Module;
import dev.client.module.NumSetting;
import java.util.UUID;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Free camera: the view is moved to a detached camera entity, while the real player
 * stays where it is (no input, no movement packets, aiming and interaction are blocked).
 */
public class Freecam
extends Module {
    /** True while freecam is running; other modules use it to stay passive. */
    public static boolean active;

    private static final double EYE = 1.62;

    private final NumSetting speed = this.add(new NumSetting("Speed", 12.0, 1.0, 60.0, 0.5));

    private OtherClientPlayerEntity cam;
    private Input savedInput;
    private Input blankInput;

    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    private float origYaw;
    private float origPitch;
    private float lockYaw;

    private double px;
    private double py;
    private double pz;

    public Freecam() {
        super("Freecam", Module.Category.RENDER);
    }

    @Override
    public void onEnable() {
        ClientPlayerEntity p = Freecam.mc.player;
        if (p == null || Freecam.mc.world == null) {
            this.enabled = false;
            return;
        }
        this.origYaw = p.yaw;
        this.origPitch = p.pitch;
        this.yaw = p.yaw;
        this.pitch = p.pitch;
        // The real player's pitch is parked at 0 so mouse movement is never clamped;
        // the mouse delta is moved onto the camera every frame instead.
        this.lockYaw = p.yaw;
        p.pitch = 0.0f;
        p.prevPitch = 0.0f;

        Vec3d eye = p.getCameraPosVec(1.0f);
        this.x = eye.x;
        this.y = eye.y;
        this.z = eye.z;
        this.px = p.getX();
        this.py = p.getY();
        this.pz = p.getZ();

        this.cam = new OtherClientPlayerEntity(Freecam.mc.world, new GameProfile(UUID.randomUUID(), "Freecam"));
        this.applyCam();

        this.savedInput = p.input;
        this.blankInput = new Input();
        p.input = this.blankInput;

        Freecam.mc.setCameraEntity(this.cam);
        active = true;
    }

    @Override
    public void onDisable() {
        active = false;
        ClientPlayerEntity p = Freecam.mc.player;
        boolean sameWorld = this.cam != null && Freecam.mc.world != null && this.cam.world == Freecam.mc.world;
        if (p != null && Freecam.mc.world != null) {
            Freecam.mc.setCameraEntity(p);
            if (p.input == this.blankInput && this.savedInput != null) {
                p.input = this.savedInput;
            }
            if (sameWorld) {
                p.yaw = this.origYaw;
                p.pitch = this.origPitch;
                p.prevYaw = this.origYaw;
                p.prevPitch = this.origPitch;
            }
        }
        this.cam = null;
        this.savedInput = null;
        this.blankInput = null;
    }

    private void applyCam() {
        double ey = this.y - EYE;
        this.cam.refreshPositionAndAngles(this.x, ey, this.z, this.yaw, this.pitch);
        this.cam.lastRenderX = this.x;
        this.cam.lastRenderY = ey;
        this.cam.lastRenderZ = this.z;
        this.cam.prevYaw = this.yaw;
        this.cam.prevPitch = this.pitch;
    }

    @Override
    public void onTick() {
        ClientPlayerEntity p = Freecam.mc.player;
        // Enabled from the config at startup, or the world was changed: just switch off.
        if (this.cam == null || p == null || this.cam.world != Freecam.mc.world) {
            this.setEnabled(false);
            return;
        }
        // Keep the real player frozen (no movement packets are sent while the camera is detached).
        Vec3d now = p.getPos();
        if (now.squaredDistanceTo(this.px, this.py, this.pz) > 1.0) {
            // The server moved us (teleport, knockback): follow it instead of dragging the player back.
            this.px = now.x;
            this.py = now.y;
            this.pz = now.z;
        } else {
            p.updatePosition(this.px, this.py, this.pz);
        }
        p.prevX = this.px;
        p.prevY = this.py;
        p.prevZ = this.pz;
        p.setVelocity(Vec3d.ZERO);
    }

    @Override
    public void onFrame(float dt) {
        ClientPlayerEntity p = Freecam.mc.player;
        if (this.cam == null || p == null || dt <= 0.0f) {
            return;
        }
        // Mouse look: take the delta off the real player and give it to the camera.
        float dYaw = p.yaw - this.lockYaw;
        float dPitch = p.pitch;
        if (dYaw != 0.0f || dPitch != 0.0f) {
            this.yaw += dYaw;
            this.pitch = MathHelper.clamp(this.pitch + dPitch, -90.0f, 90.0f);
            p.yaw = this.lockYaw;
            p.pitch = 0.0f;
            p.prevYaw = this.lockYaw;
            p.prevPitch = 0.0f;
        }

        if (Freecam.mc.currentScreen == null) {
            double f = (Freecam.mc.options.keyForward.isPressed() ? 1.0 : 0.0) - (Freecam.mc.options.keyBack.isPressed() ? 1.0 : 0.0);
            double s = (Freecam.mc.options.keyRight.isPressed() ? 1.0 : 0.0) - (Freecam.mc.options.keyLeft.isPressed() ? 1.0 : 0.0);
            double v = (Freecam.mc.options.keyJump.isPressed() ? 1.0 : 0.0) - (Freecam.mc.options.keySneak.isPressed() ? 1.0 : 0.0);
            if (f != 0.0 || s != 0.0 || v != 0.0) {
                double yr = Math.toRadians(this.yaw);
                double pr = Math.toRadians(this.pitch);
                double fx = -Math.sin(yr) * Math.cos(pr);
                double fy = -Math.sin(pr);
                double fz = Math.cos(yr) * Math.cos(pr);
                double rx = -Math.cos(yr);
                double rz = -Math.sin(yr);
                double dx = fx * f + rx * s;
                double dy = fy * f + v;
                double dz = fz * f + rz * s;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len > 1.0E-6) {
                    double k = this.speed.value * (double)dt / len;
                    this.x += dx * k;
                    this.y += dy * k;
                    this.z += dz * k;
                }
            }
        }
        this.applyCam();

        // Nothing can be hit or interacted with from the camera position.
        Freecam.mc.targetedEntity = null;
        Freecam.mc.crosshairTarget = BlockHitResult.createMissed(Vec3d.ZERO, Direction.UP, BlockPos.ORIGIN);
    }
}
