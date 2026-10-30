package dev.client.module;

import dev.client.gui.ClickGui;
import dev.client.module.HudModule;
import dev.client.module.Module;
import dev.client.module.impl.AimAssist;
import dev.client.module.impl.AutoSwap;
import dev.client.module.impl.Esp;
import dev.client.module.impl.Freecam;
import dev.client.module.impl.FpsHud;
import dev.client.module.impl.KeybindHud;
import dev.client.module.impl.PotionHud;
import dev.client.module.impl.ServerHelper;
import dev.client.module.impl.TargetHud;
import dev.client.module.impl.TriggerBot;
import dev.client.module.impl.UseTracker;
import dev.client.module.impl.Watermark;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;

public class ModuleManager {
    public static final int OPEN_GUI_KEY = 344;
    public static final List<Module> modules = new ArrayList<Module>();
    private static boolean guiKeyWasDown;
    private static final boolean[] keyWasDown;
    private static long lastFrame;

    public static void init() {
        modules.add(new TriggerBot());
        modules.add(new AimAssist());
        modules.add(new AutoSwap());
        modules.add(new ServerHelper());
        modules.add(new Esp());
        modules.add(new Freecam());
        modules.add(new FpsHud());
        modules.add(new TargetHud());
        modules.add(new KeybindHud());
        modules.add(new PotionHud());
        modules.add(new UseTracker());
        modules.add(new Watermark());
        HudRenderCallback.EVENT.register((ms, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null || mc.options.hudHidden) {
                return;
            }
            for (Module m : modules) {
                if (!m.enabled || !(m instanceof HudModule)) continue;
                ((HudModule)m).render(ms);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(ModuleManager::tick);
        WorldRenderEvents.START.register(ctx -> {
            long now = System.nanoTime();
            float dt = Math.min((float)(now - lastFrame) / 1.0E9f, 0.1f);
            lastFrame = now;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) {
                return;
            }
            for (Module m : modules) {
                if (!m.enabled) continue;
                m.onFrame(dt);
            }
        });
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) {
                return;
            }
            for (Module m : modules) {
                if (!m.enabled) continue;
                m.onWorldRender(ctx);
            }
        });
    }

    private static void tick(MinecraftClient mc) {
        if (mc.getWindow() == null) {
            return;
        }
        long handle = mc.getWindow().getHandle();
        boolean guiDown = InputUtil.isKeyPressed((long)handle, (int)344);
        if (guiDown && !guiKeyWasDown && mc.currentScreen == null && mc.player != null) {
            mc.openScreen((Screen)new ClickGui());
        }
        guiKeyWasDown = guiDown;
        if (mc.player == null || mc.world == null) {
            return;
        }
        for (Module m : modules) {
            if (m.key > 0 && m.key < keyWasDown.length) {
                boolean down = InputUtil.isKeyPressed((long)handle, (int)m.key);
                if (down && !keyWasDown[m.key] && mc.currentScreen == null) {
                    m.toggle();
                }
                ModuleManager.keyWasDown[m.key] = down;
            }
            if (!m.enabled) continue;
            m.onTick();
        }
    }

    static {
        keyWasDown = new boolean[512];
        lastFrame = System.nanoTime();
    }
}

