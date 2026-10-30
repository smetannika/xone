package dev.client.module;

import dev.client.module.Module;
import net.minecraft.client.util.math.MatrixStack;

public abstract class HudModule
extends Module {
    public int x;
    public int y;
    public int w = 80;
    public int h = 14;

    protected HudModule(String name, int x, int y, boolean defaultOn) {
        super(name, Module.Category.HUD);
        this.x = x;
        this.y = y;
        this.enabled = defaultOn;
    }

    public abstract void render(MatrixStack var1);

    public boolean contains(double mx, double my) {
        return mx >= (double)this.x && mx < (double)(this.x + this.w) && my >= (double)this.y && my < (double)(this.y + this.h);
    }
}

