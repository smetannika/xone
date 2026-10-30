package dev.client.module.impl;

import dev.client.gui.ClickGui;
import dev.client.gui.Render2D;
import dev.client.module.HudModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.util.math.MatrixStack;

public class TargetHud
extends HudModule {
    private LivingEntity target;
    private long seen;

    public TargetHud() {
        super("Target HUD", 6, 40, true);
        this.w = 130;
        this.h = 34;
    }

    @Override
    public void render(MatrixStack ms) {
        boolean preview;
        Entity e;
        long now = System.currentTimeMillis();
        HitResult hr = TargetHud.mc.crosshairTarget;
        if (hr instanceof EntityHitResult && (e = ((EntityHitResult)hr).getEntity()) instanceof LivingEntity && e != TargetHud.mc.player) {
            this.target = (LivingEntity)e;
            this.seen = now;
        }
        boolean live = this.target != null && this.target.isAlive() && now - this.seen < 3000L;
        boolean bl = preview = !live && TargetHud.mc.currentScreen instanceof ClickGui;
        if (!live && !preview) {
            return;
        }
        String name = live ? this.target.getName().getString() : "Target";
        float hp = live ? this.target.getHealth() : 20.0f;
        float max = live ? Math.max(this.target.getMaxHealth(), 1.0f) : 20.0f;
        float dist = live ? TargetHud.mc.player.distanceTo((Entity)this.target) : 0.0f;
        float ratio = Math.max(0.0f, Math.min(1.0f, hp / max));
        this.w = Math.max(130, TargetHud.mc.textRenderer.getWidth(name) + 12);
        this.h = 34;
        Render2D.shadow(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, 0.9f);
        Render2D.rrect(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, -586018282);
        TargetHud.mc.textRenderer.drawWithShadow(ms, name, (float)(this.x + 4), (float)(this.y + 3), -1);
        int bx = this.x + 4;
        int by = this.y + 15;
        int bw = this.w - 8;
        Render2D.rrect(ms, 3.0f, bx, by, bx + bw, by + 6, -13421773);
        int color = ratio > 0.5f ? -11751600 : (ratio > 0.25f ? -16121 : -769226);
        Render2D.rrect(ms, 3.0f, bx, by, bx + (int)((float)bw * ratio), by + 6, color);
        TargetHud.mc.textRenderer.drawWithShadow(ms, String.format("%.1f / %.1f", Float.valueOf(hp), Float.valueOf(max)), (float)(this.x + 4), (float)(this.y + 23), -2236963);
        String d = String.format("%.1fm", Float.valueOf(dist));
        TargetHud.mc.textRenderer.drawWithShadow(ms, d, (float)(this.x + this.w - 4 - TargetHud.mc.textRenderer.getWidth(d)), (float)(this.y + 23), -5592406);
    }
}

