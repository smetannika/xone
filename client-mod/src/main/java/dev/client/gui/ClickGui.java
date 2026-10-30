package dev.client.gui;

import dev.client.Config;
import dev.client.gui.Render2D;
import dev.client.module.BindSetting;
import dev.client.module.BoolSetting;
import dev.client.module.HudModule;
import dev.client.module.Module;
import dev.client.module.ModuleManager;
import dev.client.module.NumSetting;
import dev.client.module.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.text.LiteralText;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class ClickGui
extends Screen {
    private static final int A1 = -7643914;
    private static final int A2 = -13058568;
    private static final int BG = -233828328;
    private static final int SIDEBAR = -16053487;
    private static final int CARD = -15263968;
    private static final int CARD_HOVER = -14803414;
    private static final int DIM = -7697764;
    private static final int TEXT = -1184267;
    private static final int TRACK_OFF = -13355969;
    private static final int W = 470;
    private static final int H = 290;
    private static final int SIDE_W = 84;
    private static final int LIST_X = 100;
    private static final int LIST_W = 170;
    private static final int ROW_H = 26;
    private static final int ROW_GAP = 4;
    private static final int PANEL_X = 278;
    private static final int PANEL_W = 184;
    private static final int T_MODULE = 0;
    private static final int T_TAB = 1;
    private static final int T_BIND = 2;
    private static final int T_SETTING = 3;
    private final List<Hit> hits = new ArrayList<Hit>();
    private Module.Category selCat = Module.Category.values()[0];
    private Module selMod;
    private Module binding;
    private BindSetting bindingSetting;
    private Hit dragging;
    private HudModule dragHud;
    private double dragOffX;
    private double dragOffY;
    private float tabPillY = -1.0f;
    private float contentFade = 1.0f;
    private float panelFade = 1.0f;
    private float scroll;
    private float scrollTarget;
    private final long openedAt;
    private long lastNs = this.openedAt = System.nanoTime();

    public ClickGui() {
        super((Text)new LiteralText("ClickGUI"));
        this.selMod = ClickGui.firstOf(this.selCat);
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static Module firstOf(Module.Category c) {
        for (Module m : ModuleManager.modules) {
            if (m.category != c) continue;
            return m;
        }
        return null;
    }

    private static List<Module> modulesOf(Module.Category c) {
        ArrayList<Module> l = new ArrayList<Module>();
        for (Module m : ModuleManager.modules) {
            if (m.category != c) continue;
            l.add(m);
        }
        return l;
    }

    private static String pretty(Module.Category c) {
        return c.label;
    }

    private float uiScale() {
        return Math.min(1.0f, Math.min(((float)this.width - 16.0f) / 470.0f, ((float)this.height - 16.0f) / 290.0f));
    }

    private double lx(double mx) {
        return (mx - (double)this.width / 2.0) / (double)this.uiScale() + (double)this.width / 2.0;
    }

    private double ly(double my) {
        return (my - (double)this.height / 2.0) / (double)this.uiScale() + (double)this.height / 2.0;
    }

    private static float ease(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
    }

    private static float approach(float v, float target, float speed) {
        return v + (target - v) * Math.min(1.0f, speed);
    }

    private void text(MatrixStack ms, String t, float x, float y, int color, float alpha) {
        int c = Render2D.ca(color, alpha);
        if ((c >>> 24 & 0xFF) < 8) {
            return;
        }
        this.textRenderer.drawWithShadow(ms, t, x, y, c);
    }

    private void textScaled(MatrixStack ms, String t, float x, float y, float s, int color, float alpha) {
        ms.push();
        ms.translate((double)x, (double)y, 0.0);
        ms.scale(s, s, 1.0f);
        this.text(ms, t, 0.0f, 0.0f, color, alpha);
        ms.pop();
    }

    private void gradientText(MatrixStack ms, String t, float x, float y, float s, float alpha) {
        ms.push();
        ms.translate((double)x, (double)y, 0.0);
        ms.scale(s, s, 1.0f);
        float cx = 0.0f;
        for (int i = 0; i < t.length(); ++i) {
            String ch = String.valueOf(t.charAt(i));
            int col = Render2D.lerp(-7643914, -13058568, t.length() <= 1 ? 0.0f : (float)i / (float)(t.length() - 1));
            this.text(ms, ch, cx, 0.0f, col, alpha);
            cx += (float)this.textRenderer.getWidth(ch);
        }
        ms.pop();
    }

    private Hit addHit(int type, float x, float y, float w, float h) {
        Hit hit = new Hit();
        hit.type = type;
        hit.x = x;
        hit.y = y;
        hit.w = w;
        hit.h = h;
        hit.clipY1 = -10000.0f;
        hit.clipY2 = 10000.0f;
        this.hits.add(hit);
        return hit;
    }

    private static boolean inside(Hit h, double mx, double my) {
        return mx >= (double)h.x && mx < (double)(h.x + h.w) && my >= (double)h.y && my < (double)(h.y + h.h) && my >= (double)h.clipY1 && my < (double)h.clipY2;
    }

    private void selectCategory(Module.Category c) {
        if (c == this.selCat) {
            return;
        }
        this.selCat = c;
        this.selMod = ClickGui.firstOf(c);
        this.contentFade = 0.0f;
        this.panelFade = 0.0f;
        this.scrollTarget = 0.0f;
        this.scroll = 0.0f;
    }

    private void selectModule(Module m) {
        if (m == this.selMod) {
            return;
        }
        this.selMod = m;
        this.panelFade = 0.0f;
        this.scrollTarget = 0.0f;
        this.scroll = 0.0f;
    }

    private static float settingH(Setting s) {
        return s instanceof NumSetting ? 30.0f : 24.0f;
    }

    private static float settingsContentH(Module m) {
        float h = 0.0f;
        for (Setting s : m.settings) {
            h += ClickGui.settingH(s);
        }
        return h;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min((float)(now - this.lastNs) / 1.0E9f, 0.1f);
        this.lastNs = now;
        float k = dt * 14.0f;
        float e = ClickGui.ease((float)(now - this.openedAt) / 1.0E9f / 0.25f);
        float us = this.uiScale();
        float s = us * (0.94f + 0.06f * e);
        float cxm = (float)this.width / 2.0f;
        float cym = (float)this.height / 2.0f;
        float wx = (float)(this.width - 470) / 2.0f;
        float wy = (float)(this.height - 290) / 2.0f;
        double lmx = this.lx(mouseX);
        double lmy = this.ly(mouseY);
        this.contentFade = ClickGui.approach(this.contentFade, 1.0f, dt * 9.0f);
        this.panelFade = ClickGui.approach(this.panelFade, 1.0f, dt * 9.0f);
        for (Module m : ModuleManager.modules) {
            m.toggleAnim = ClickGui.approach(m.toggleAnim, m.enabled ? 1.0f : 0.0f, k);
            for (Setting st : m.settings) {
                float target = 0.0f;
                if (st instanceof BoolSetting) {
                    target = ((BoolSetting)st).value ? 1.0f : 0.0f;
                } else if (st instanceof NumSetting) {
                    NumSetting n = (NumSetting)st;
                    target = (float)((n.value - n.min) / (n.max - n.min));
                }
                st.anim = ClickGui.approach(st.anim, target, k * 1.3f);
            }
        }
        this.scroll = ClickGui.approach(this.scroll, this.scrollTarget, dt * 16.0f);
        this.hits.clear();
        ClickGui.fill((MatrixStack)ms, (int)0, (int)0, (int)this.width, (int)this.height, (int)Render2D.ca(-16777216, 0.5f * e));
        ms.push();
        ms.translate((double)cxm, (double)cym, 0.0);
        ms.scale(s, s, 1.0f);
        ms.translate((double)(-cxm), (double)(-cym), 0.0);
        Render2D.shadow(ms, 12.0f, wx, wy, wx + 470.0f, wy + 290.0f, -7643914, e * 0.55f);
        Render2D.shadow(ms, 12.0f, wx, wy, wx + 470.0f, wy + 290.0f, e * 0.9f);
        Render2D.rrect(ms, 12.0f, wx, wy, wx + 470.0f, wy + 290.0f, Render2D.ca(-233828328, e));
        Render2D.rrectH(ms, 1.0f, wx + 40.0f, wy + 1.0f, wx + 470.0f - 40.0f, wy + 3.0f, Render2D.ca(-7643914, e), Render2D.ca(-13058568, e));
        Render2D.rrect(ms, 9.0f, wx + 8.0f, wy + 8.0f, wx + 8.0f + 84.0f, wy + 290.0f - 8.0f, Render2D.ca(-16053487, e));
        this.gradientText(ms, "CLIENT", wx + 20.0f, wy + 20.0f, 1.25f, e);
        this.text(ms, "1.16.5", wx + 21.0f, wy + 35.0f, -7697764, e);
        Module.Category[] cats = Module.Category.values();
        float tabsTop = wy + 56.0f;
        int selIndex = 0;
        for (int i = 0; i < cats.length; ++i) {
            if (cats[i] != this.selCat) continue;
            selIndex = i;
        }
        float pillTarget = tabsTop + (float)(selIndex * 30);
        this.tabPillY = this.tabPillY < 0.0f ? pillTarget : ClickGui.approach(this.tabPillY, pillTarget, k);
        Render2D.rrectH(ms, 7.0f, wx + 14.0f, this.tabPillY, wx + 14.0f + 72.0f, this.tabPillY + 26.0f, Render2D.ca(-7643914, 0.3f * e), Render2D.ca(-13058568, 0.3f * e));
        Render2D.rrectH(ms, 1.5f, wx + 14.0f, this.tabPillY + 6.0f, wx + 17.0f, this.tabPillY + 20.0f, Render2D.ca(-7643914, e), Render2D.ca(-13058568, e));
        for (int i = 0; i < cats.length; ++i) {
            float ty = tabsTop + (float)(i * 30);
            Hit th = this.addHit(1, wx + 14.0f, ty, 72.0f, 26.0f);
            th.cat = cats[i];
            boolean hover = ClickGui.inside(th, lmx, lmy);
            int tc = Render2D.lerp(-7697764, -1184267, cats[i] == this.selCat ? 1.0f : (hover ? 0.6f : 0.0f));
            this.text(ms, ClickGui.pretty(cats[i]), wx + 26.0f, ty + 9.0f, tc, e);
        }
        float hy = wy + 290.0f - 46.0f;
        this.text(ms, "LMB  toggle", wx + 18.0f, hy, -7697764, e * 0.8f);
        this.text(ms, "RMB  settings", wx + 18.0f, hy + 11.0f, -7697764, e * 0.8f);
        this.text(ms, "MMB  bind", wx + 18.0f, hy + 22.0f, -7697764, e * 0.8f);
        float cx0 = wx + 100.0f;
        List<Module> list = ClickGui.modulesOf(this.selCat);
        this.textScaled(ms, ClickGui.pretty(this.selCat), cx0, wy + 14.0f, 1.45f, -1184267, e);
        this.text(ms, list.size() + (list.size() == 1 ? " module" : " modules"), cx0 + 1.0f, wy + 31.0f, -7697764, e);
        float slide = (1.0f - this.contentFade) * 12.0f;
        float ca = e * this.contentFade;
        for (int i = 0; i < list.size(); ++i) {
            String tag;
            int bg;
            Module m = list.get(i);
            float rx = cx0 + slide;
            float ry = wy + 46.0f + (float)(i * 30);
            Hit mh = this.addHit(0, cx0, ry, 170.0f, 26.0f);
            mh.m = m;
            boolean hover = ClickGui.inside(mh, lmx, lmy);
            boolean sel = m == this.selMod;
            int n = bg = hover || sel ? -14803414 : -15263968;
            if (sel) {
                Render2D.rrectH(ms, 7.0f, rx, ry, rx + 170.0f, ry + 26.0f, Render2D.ca(-7643914, 0.85f * ca), Render2D.ca(-13058568, 0.85f * ca));
                Render2D.rrect(ms, 6.0f, rx + 1.0f, ry + 1.0f, rx + 170.0f - 1.0f, ry + 26.0f - 1.0f, Render2D.ca(bg, ca));
            } else {
                Render2D.rrect(ms, 7.0f, rx, ry, rx + 170.0f, ry + 26.0f, Render2D.ca(bg, ca));
            }
            if (m.toggleAnim > 0.01f) {
                Render2D.rrectH(ms, 6.0f, rx + 1.0f, ry + 1.0f, rx + 170.0f - 1.0f, ry + 26.0f - 1.0f, Render2D.ca(-7643914, 0.22f * m.toggleAnim * ca), Render2D.ca(-13058568, 0.22f * m.toggleAnim * ca));
            }
            this.text(ms, m.name, rx + 12.0f, ry + 9.0f, Render2D.lerp(-4934460, -1184267, m.toggleAnim), ca);
            float sx = rx + 170.0f - 36.0f;
            float sy = ry + 7.0f;
            Render2D.rrectH(ms, 6.0f, sx, sy, sx + 26.0f, sy + 12.0f, Render2D.ca(Render2D.lerp(-13355969, -7643914, m.toggleAnim), ca), Render2D.ca(Render2D.lerp(-13355969, -13058568, m.toggleAnim), ca));
            float kx = sx + 1.5f + m.toggleAnim * 14.0f;
            Render2D.rrect(ms, 4.5f, kx, sy + 1.5f, kx + 9.0f, sy + 10.5f, Render2D.ca(-1, ca));
            String string = this.binding == m ? "..." : (tag = m.key > 0 ? "[" + ClickGui.keyName(m.key) + "]" : "");
            if (tag.isEmpty()) continue;
            this.text(ms, tag, sx - 6.0f - (float)this.textRenderer.getWidth(tag), ry + 9.0f, -7697764, ca);
        }
        float px = wx + 278.0f;
        float py = wy + 14.0f;
        float ph = 268.0f;
        Render2D.rrect(ms, 9.0f, px, py, px + 184.0f, py + ph, Render2D.ca(-15263968, e));
        float pa = e * this.panelFade;
        float pdx = (1.0f - this.panelFade) * 8.0f;
        if (this.selMod == null) {
            this.text(ms, "Select a module", px + 12.0f, py + 14.0f, -7697764, pa);
        } else {
            Module m = this.selMod;
            float ix = px + pdx;
            this.textScaled(ms, m.name, ix + 12.0f, py + 12.0f, 1.3f, -1184267, pa);
            this.text(ms, ClickGui.pretty(m.category) + "  ·  " + (m.enabled ? "ON" : "OFF"), ix + 12.0f, py + 28.0f, m.enabled ? -13058568 : -7697764, pa);
            Hit bh = this.addHit(2, px + 12.0f, py + 42.0f, 160.0f, 20.0f);
            bh.m = m;
            boolean bHover = ClickGui.inside(bh, lmx, lmy);
            Render2D.rrect(ms, 6.0f, ix + 12.0f, py + 42.0f, ix + 184.0f - 12.0f, py + 62.0f, Render2D.ca(bHover || this.binding == m ? -14211275 : -14803414, pa));
            this.text(ms, "Bind", ix + 20.0f, py + 48.0f, -7697764, pa);
            String bt = this.binding == m ? "press a key..." : (m.key > 0 ? "[" + ClickGui.keyName(m.key) + "]" : "none");
            this.text(ms, bt, ix + 184.0f - 20.0f - (float)this.textRenderer.getWidth(bt), py + 48.0f, this.binding == m ? -13058568 : -1184267, pa);
            Render2D.rrect(ms, 0.5f, ix + 12.0f, py + 69.0f, ix + 184.0f - 12.0f, py + 70.0f, Render2D.ca(-14013896, pa));
            float areaY1 = py + 74.0f;
            float areaY2 = py + ph - 8.0f;
            float areaH = areaY2 - areaY1;
            float contentH = ClickGui.settingsContentH(m);
            float maxScroll = Math.max(0.0f, contentH - areaH);
            this.scrollTarget = Math.max(0.0f, Math.min(maxScroll, this.scrollTarget));
            this.scroll = Math.max(0.0f, Math.min(maxScroll, this.scroll));
            if (m.settings.isEmpty()) {
                this.text(ms, "No settings", ix + 12.0f, areaY1 + 4.0f, -7697764, pa);
            } else {
                float sx1 = (px + 2.0f - cxm) * s + cxm;
                float sy1 = (areaY1 - cym) * s + cym;
                Render2D.scissor(sx1, sy1, 180.0f * s, areaH * s);
                float ry = areaY1 - this.scroll;
                for (Setting st : m.settings) {
                    float rh = ClickGui.settingH(st);
                    if (ry + rh > areaY1 - 2.0f && ry < areaY2 + 2.0f) {
                        Hit sh = this.addHit(3, px + 8.0f, ry, 168.0f, rh);
                        sh.m = m;
                        sh.s = st;
                        sh.sx = px + 12.0f;
                        sh.sw = 160.0f;
                        sh.clipY1 = areaY1;
                        sh.clipY2 = areaY2;
                        if (st instanceof BindSetting) {
                            BindSetting bs = (BindSetting)st;
                            boolean bHov = ClickGui.inside(sh, lmx, lmy);
                            this.text(ms, st.name, ix + 12.0f, ry + 8.0f, -3158052, pa);
                            String bt2 = this.bindingSetting == bs ? "press a key..." : (bs.key > 0 ? "[" + ClickGui.keyName(bs.key) + "]" : "none");
                            float bw2 = this.textRenderer.getWidth(bt2) + 12;
                            float bx2 = ix + 184.0f - 12.0f - bw2;
                            Render2D.rrect(ms, 5.0f, bx2, ry + 4.0f, bx2 + bw2, ry + 18.0f, Render2D.ca(bHov || this.bindingSetting == bs ? -14013894 : -14803414, pa));
                            this.text(ms, bt2, bx2 + 6.0f, ry + 8.0f, this.bindingSetting == bs ? -13058568 : -1184267, pa);
                        } else if (st instanceof BoolSetting) {
                            this.text(ms, st.name, ix + 12.0f, ry + 8.0f, -3158052, pa);
                            float tx = ix + 184.0f - 12.0f - 26.0f;
                            float ty = ry + 6.0f;
                            Render2D.rrectH(ms, 6.0f, tx, ty, tx + 26.0f, ty + 12.0f, Render2D.ca(Render2D.lerp(-13355969, -7643914, st.anim), pa), Render2D.ca(Render2D.lerp(-13355969, -13058568, st.anim), pa));
                            float kx = tx + 1.5f + st.anim * 14.0f;
                            Render2D.rrect(ms, 4.5f, kx, ty + 1.5f, kx + 9.0f, ty + 10.5f, Render2D.ca(-1, pa));
                        } else if (st instanceof NumSetting) {
                            NumSetting n = (NumSetting)st;
                            this.text(ms, st.name, ix + 12.0f, ry + 4.0f, -3158052, pa);
                            String val = ClickGui.fmt(n.value);
                            this.text(ms, val, ix + 184.0f - 12.0f - (float)this.textRenderer.getWidth(val), ry + 4.0f, -1184267, pa);
                            float tx = ix + 12.0f;
                            float tw = 160.0f;
                            float ty = ry + 18.0f;
                            Render2D.rrect(ms, 2.5f, tx, ty, tx + tw, ty + 5.0f, Render2D.ca(-13355969, pa));
                            float fw = Math.max(5.0f, tw * st.anim);
                            Render2D.rrectH(ms, 2.5f, tx, ty, tx + fw, ty + 5.0f, Render2D.ca(-7643914, pa), Render2D.ca(-13058568, pa));
                            Render2D.rrect(ms, 5.5f, tx + fw - 5.5f, ty - 3.0f, tx + fw + 5.5f, ty + 8.0f, Render2D.ca(-1, pa));
                        }
                    }
                    ry += rh;
                }
                Render2D.endScissor();
            }
        }
        ms.pop();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = this.lx(mouseX);
        double my = this.ly(mouseY);
        for (Hit h : this.hits) {
            if (!ClickGui.inside(h, mx, my)) continue;
            switch (h.type) {
                case 1: {
                    if (button != 0) break;
                    this.selectCategory(h.cat);
                    break;
                }
                case 0: {
                    if (button == 0) {
                        h.m.toggle();
                        break;
                    }
                    if (button == 1) {
                        this.selectModule(h.m);
                        break;
                    }
                    if (button != 2) break;
                    this.selectModule(h.m);
                    this.binding = h.m;
                    break;
                }
                case 2: {
                    if (button == 0) {
                        this.binding = h.m;
                        break;
                    }
                    if (button != 1) break;
                    h.m.key = -1;
                    break;
                }
                case 3: {
                    if (h.s instanceof BindSetting) {
                        if (button == 0) {
                            this.bindingSetting = (BindSetting)h.s;
                            break;
                        }
                        if (button != 1) break;
                        ((BindSetting)h.s).key = -1;
                        break;
                    }
                    if (button != 0) break;
                    if (h.s instanceof BoolSetting) {
                        ((BoolSetting)h.s).value = !((BoolSetting)h.s).value;
                        break;
                    }
                    if (!(h.s instanceof NumSetting)) break;
                    this.dragging = h;
                    this.slide(h, mx);
                    break;
                }
            }
            return true;
        }
        float wx = (float)(this.width - 470) / 2.0f;
        float wy = (float)(this.height - 290) / 2.0f;
        if (mx >= (double)wx && mx < (double)(wx + 470.0f) && my >= (double)wy && my < (double)(wy + 290.0f)) {
            return true;
        }
        if (button == 0) {
            for (Module m : ModuleManager.modules) {
                if (!(m instanceof HudModule) || !m.enabled || !((HudModule)m).contains(mouseX, mouseY)) continue;
                this.dragHud = (HudModule)m;
                this.dragOffX = mouseX - (double)this.dragHud.x;
                this.dragOffY = mouseY - (double)this.dragHud.y;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.dragging != null) {
            this.slide(this.dragging, this.lx(mouseX));
            return true;
        }
        if (this.dragHud != null) {
            this.dragHud.x = (int)Math.max(0.0, Math.min((double)(this.width - this.dragHud.w), mouseX - this.dragOffX));
            this.dragHud.y = (int)Math.max(0.0, Math.min((double)(this.height - this.dragHud.h), mouseY - this.dragOffY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = null;
        this.dragHud = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.scrollTarget -= (float)amount * 22.0f;
        if (this.scrollTarget < 0.0f) {
            this.scrollTarget = 0.0f;
        }
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.bindingSetting != null) {
            if (keyCode == 259 || keyCode == 261) {
                this.bindingSetting.key = -1;
            } else if (keyCode != 256) {
                this.bindingSetting.key = keyCode;
            }
            this.bindingSetting = null;
            return true;
        }
        if (this.binding != null) {
            if (keyCode == 259 || keyCode == 261) {
                this.binding.key = -1;
            } else if (keyCode != 256) {
                this.binding.key = keyCode;
            }
            this.binding = null;
            return true;
        }
        if (keyCode == 344) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void onClose() {
        Config.save();
        super.onClose();
    }

    private void slide(Hit h, double lmx) {
        NumSetting n = (NumSetting)h.s;
        double frac = Math.max(0.0, Math.min(1.0, (lmx - (double)h.sx) / (double)h.sw));
        n.set(n.min + frac * (n.max - n.min));
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long)v) : String.format("%.2f", v);
    }

    private static String keyName(int key) {
        return InputUtil.fromKeyCode((int)key, (int)0).getLocalizedText().getString();
    }

    private static class Hit {
        int type;
        float x;
        float y;
        float w;
        float h;
        float sx;
        float sw;
        float clipY1;
        float clipY2;
        Module m;
        Module.Category cat;
        Setting s;

        private Hit() {
        }
    }
}

