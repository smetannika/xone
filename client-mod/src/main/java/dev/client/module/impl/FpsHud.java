package dev.client.module.impl;

import dev.client.gui.Render2D;
import dev.client.module.HudModule;
import net.minecraft.client.util.math.MatrixStack;

public class FpsHud
extends HudModule {
    private int frames;
    private int fps;
    private long last = System.currentTimeMillis();

    public FpsHud() {
        super("FPS", 6, 6, true);
    }

    @Override
    public void render(MatrixStack ms) {
        ++this.frames;
        long now = System.currentTimeMillis();
        if (now - this.last >= 1000L) {
            this.fps = this.frames;
            this.frames = 0;
            this.last = now;
        }
        String t = "FPS: " + this.fps;
        this.w = FpsHud.mc.textRenderer.getWidth(t) + 8;
        this.h = 14;
        Render2D.shadow(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, 0.9f);
        Render2D.rrect(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, -586018282);
        FpsHud.mc.textRenderer.drawWithShadow(ms, t, (float)(this.x + 4), (float)(this.y + 3), -1);
    }
}

