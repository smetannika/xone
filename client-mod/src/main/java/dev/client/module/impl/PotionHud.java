package dev.client.module.impl;

import dev.client.gui.ClickGui;
import dev.client.gui.Render2D;
import dev.client.module.BoolSetting;
import dev.client.module.HudModule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;

public class PotionHud
extends HudModule {
    private static final int BG = -586018282;
    private static final int AMBER = -16121;
    private static final int RED = -769226;
    private static final int DIM = -5592406;
    private static final String[] ROMAN = new String[]{"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private final BoolSetting showLevel = this.add(new BoolSetting("Show level", true));
    private final BoolSetting showTime = this.add(new BoolSetting("Show timer", true));
    private final BoolSetting colored = this.add(new BoolSetting("Effect colors", true));

    public PotionHud() {
        super("Potions", 6, 130, true);
    }

    private static final class Row {
        final String name;
        final int ticks;
        final int color;

        Row(String name, int ticks, int color) {
            this.name = name;
            this.ticks = ticks;
            this.color = color;
        }
    }

    private static String level(int amplifier) {
        int lvl = amplifier + 1;
        if (lvl <= 1) {
            return "";
        }
        return lvl < ROMAN.length ? ROMAN[lvl] : String.valueOf(lvl);
    }

    private static String time(int ticks) {
        int s = (ticks + 19) / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override
    public void render(MatrixStack ms) {
        List<Row> rows = new ArrayList<Row>();
        Collection<StatusEffectInstance> effects = PotionHud.mc.player.getStatusEffects();
        for (StatusEffectInstance e : effects) {
            String name = e.getEffectType().getName().getString();
            if (this.showLevel.value) {
                String lv = PotionHud.level(e.getAmplifier());
                if (!lv.isEmpty()) {
                    name = name + " " + lv;
                }
            }
            rows.add(new Row(name, e.getDuration(), e.getEffectType().getColor()));
        }
        Collections.sort(rows, new Comparator<Row>() {
            @Override
            public int compare(Row a, Row b) {
                return Integer.compare(b.ticks, a.ticks);
            }
        });
        boolean preview = rows.isEmpty() && PotionHud.mc.currentScreen instanceof ClickGui;
        if (rows.isEmpty() && !preview) {
            return;
        }
        if (preview) {
            rows.add(new Row("Speed II", 3600, 8171462));
            rows.add(new Row("Strength", 1600, 9643043));
        }

        String title = "Potions";
        int width = PotionHud.mc.textRenderer.getWidth(title);
        for (Row r : rows) {
            int rw = PotionHud.mc.textRenderer.getWidth(r.name) + 6;
            if (this.showTime.value) {
                rw += PotionHud.mc.textRenderer.getWidth(PotionHud.time(r.ticks)) + 12;
            }
            width = Math.max(width, rw);
        }
        this.w = width + 8;
        this.h = 14 + rows.size() * 11;

        Render2D.shadow(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, 0.9f);
        Render2D.rrect(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, BG);
        PotionHud.mc.textRenderer.drawWithShadow(ms, title, (float)(this.x + 4), (float)(this.y + 3), -1);

        int ly = this.y + 15;
        for (Row r : rows) {
            int rgb = 0xFF000000 | r.color;
            Render2D.rrect(ms, 1.0f, this.x + 4, ly + 1, this.x + 6, ly + 8, rgb);
            int nameColor = this.colored.value ? Render2D.lerp(rgb, -1, 0.35f) : -1;
            PotionHud.mc.textRenderer.drawWithShadow(ms, r.name, (float)(this.x + 10), (float)ly, nameColor);
            if (this.showTime.value) {
                String t = PotionHud.time(r.ticks);
                int tc = r.ticks <= 100 ? RED : (r.ticks <= 200 ? AMBER : DIM);
                PotionHud.mc.textRenderer.drawWithShadow(ms, t, (float)(this.x + this.w - 4 - PotionHud.mc.textRenderer.getWidth(t)), (float)ly, tc);
            }
            ly += 11;
        }
    }
}
