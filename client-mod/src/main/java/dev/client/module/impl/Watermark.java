package dev.client.module.impl;

import dev.client.gui.Render2D;
import dev.client.module.HudModule;
import net.minecraft.client.util.math.MatrixStack;

/** Small watermark, by default centered right under the boss bar. */
public class Watermark
extends HudModule {
    private static final String TEXT = "system free";
    private static final int A1 = -7643914;
    private static final int A2 = -13058568;

    public Watermark() {
        // x = -1 means "center horizontally" until the module is dragged somewhere
        super("Watermark", -1, 24, true);
    }

    @Override
    public void render(MatrixStack ms) {
        this.w = Watermark.mc.textRenderer.getWidth(TEXT) + 12;
        this.h = 14;
        if (this.x < 0) {
            this.x = (Watermark.mc.getWindow().getScaledWidth() - this.w) / 2;
        }
        Render2D.shadow(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, 0.9f);
        Render2D.rrect(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, -586018282);
        float cx = (float)(this.x + 6);
        for (int i = 0; i < TEXT.length(); ++i) {
            String ch = String.valueOf(TEXT.charAt(i));
            int col = Render2D.lerp(A1, A2, (float)i / (float)(TEXT.length() - 1));
            Watermark.mc.textRenderer.drawWithShadow(ms, ch, cx, (float)(this.y + 3), col);
            cx += (float)Watermark.mc.textRenderer.getWidth(ch);
        }
    }
}
