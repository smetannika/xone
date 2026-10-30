package dev.client.module;

import dev.client.module.Setting;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    public final String name;
    public final Category category;
    public final List<Setting> settings = new ArrayList<Setting>();
    public boolean enabled;
    public boolean open;
    public int key = -1;
    public float toggleAnim;
    public float openAnim;

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected <T extends Setting> T add(T s) {
        this.settings.add(s);
        return s;
    }

    public void toggle() {
        this.setEnabled(!this.enabled);
    }

    public void setEnabled(boolean e) {
        if (this.enabled == e) {
            return;
        }
        this.enabled = e;
        if (e) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    public void onFrame(float dt) {
    }

    public void onWorldRender(WorldRenderContext ctx) {
    }

    public static enum Category {
        COMBAT("Combat"),
        RENDER("Render"),
        HUD("HUD"),
        SERVER("Server");

        public final String label;

        private Category(String label) {
            this.label = label;
        }
    }
}

