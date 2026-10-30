package dev.client.module.impl;

import dev.client.gui.ClickGui;
import dev.client.gui.Render2D;
import dev.client.module.HudModule;
import dev.client.module.Module;
import dev.client.module.ModuleManager;
import java.util.ArrayList;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;

public class KeybindHud
extends HudModule {
    public KeybindHud() {
        super("Keybinds", 6, 80, true);
    }

    @Override
    public void render(MatrixStack ms) {
        boolean preview;
        ArrayList<Module> bound = new ArrayList<Module>();
        for (Module m : ModuleManager.modules) {
            if (m.key <= 0 || m instanceof HudModule && m == this) continue;
            bound.add(m);
        }
        boolean bl = preview = bound.isEmpty() && KeybindHud.mc.currentScreen instanceof ClickGui;
        if (bound.isEmpty() && !preview) {
            return;
        }
        String title = "Keybinds";
        int width = KeybindHud.mc.textRenderer.getWidth(title);
        ArrayList<String> left = new ArrayList<String>();
        ArrayList<String> right = new ArrayList<String>();
        for (Module m : bound) {
            String k = InputUtil.fromKeyCode((int)m.key, (int)0).getLocalizedText().getString();
            left.add(m.name);
            right.add("[" + k + "]");
            width = Math.max(width, KeybindHud.mc.textRenderer.getWidth(m.name) + KeybindHud.mc.textRenderer.getWidth("[" + k + "]") + 12);
        }
        if (preview) {
            left.add("No binds yet");
            right.add("");
            width = Math.max(width, KeybindHud.mc.textRenderer.getWidth("No binds yet"));
        }
        this.w = width + 8;
        this.h = 14 + left.size() * 11;
        Render2D.shadow(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, 0.9f);
        Render2D.rrect(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, -586018282);
        KeybindHud.mc.textRenderer.drawWithShadow(ms, title, (float)(this.x + 4), (float)(this.y + 3), -1);
        int ly = this.y + 15;
        for (int i = 0; i < left.size(); ++i) {
            boolean on = i < bound.size() && ((Module)bound.get((int)i)).enabled;
            int col = on ? -11751600 : -5592406;
            KeybindHud.mc.textRenderer.drawWithShadow(ms, (String)left.get(i), (float)(this.x + 4), (float)ly, col);
            KeybindHud.mc.textRenderer.drawWithShadow(ms, (String)right.get(i), (float)(this.x + this.w - 4 - KeybindHud.mc.textRenderer.getWidth((String)right.get(i))), (float)ly, col);
            ly += 11;
        }
    }
}

