package dev.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

public final class Render2D {
    private static final int SEGMENTS = 10;

    private Render2D() {
    }

    public static int ca(int color, float alpha) {
        int a = (int)((float)(color >>> 24 & 0xFF) * Math.max(0.0f, Math.min(1.0f, alpha)));
        return a << 24 | color & 0xFFFFFF;
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a = (int)((float)(c1 >>> 24 & 0xFF) * (1.0f - t) + (float)(c2 >>> 24 & 0xFF) * t);
        int r = (int)((float)(c1 >> 16 & 0xFF) * (1.0f - t) + (float)(c2 >> 16 & 0xFF) * t);
        int g = (int)((float)(c1 >> 8 & 0xFF) * (1.0f - t) + (float)(c2 >> 8 & 0xFF) * t);
        int b = (int)((float)(c1 & 0xFF) * (1.0f - t) + (float)(c2 & 0xFF) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static void rrect(MatrixStack ms, float r, float x1, float y1, float x2, float y2, int color) {
        Render2D.rrectH(ms, r, x1, y1, x2, y2, color, color);
    }

    public static void rrectH(MatrixStack ms, float r, float x1, float y1, float x2, float y2, int c1, int c2) {
        float w = x2 - x1;
        float h = y2 - y1;
        if (w <= 0.0f || h <= 0.0f || (c1 >>> 24 & 0xFF) == 0 && (c2 >>> 24 & 0xFF) == 0) {
            return;
        }
        r = Math.max(0.0f, Math.min(r, Math.min(w, h) / 2.0f));
        Matrix4f m = ms.peek().getModel();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.defaultBlendFunc();
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        b.begin(6, VertexFormats.POSITION_COLOR);
        Render2D.vtx(b, m, x1 + w / 2.0f, y1 + h / 2.0f, Render2D.lerp(c1, c2, 0.5f));
        float[][] corners = new float[][]{{x1 + r, y1 + r, 180.0f}, {x2 - r, y1 + r, 270.0f}, {x2 - r, y2 - r, 0.0f}, {x1 + r, y2 - r, 90.0f}};
        float firstX = 0.0f;
        float firstY = 0.0f;
        for (int c = 0; c < 4; ++c) {
            for (int i = 0; i <= 10; ++i) {
                double ang = Math.toRadians((double)corners[c][2] + 90.0 * (double)i / 10.0);
                float px = corners[c][0] + (float)Math.cos(ang) * r;
                float py = corners[c][1] + (float)Math.sin(ang) * r;
                if (c == 0 && i == 0) {
                    firstX = px;
                    firstY = py;
                }
                Render2D.vtx(b, m, px, py, Render2D.lerp(c1, c2, (px - x1) / w));
            }
        }
        Render2D.vtx(b, m, firstX, firstY, Render2D.lerp(c1, c2, (firstX - x1) / w));
        b.end();
        BufferRenderer.draw((BufferBuilder)b);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void vtx(BufferBuilder b, Matrix4f m, float x, float y, int col) {
        b.vertex(m, x, y, 0.0f).color(col >> 16 & 0xFF, col >> 8 & 0xFF, col & 0xFF, col >>> 24 & 0xFF).next();
    }

    public static void shadow(MatrixStack ms, float r, float x1, float y1, float x2, float y2, float alpha) {
        Render2D.shadow(ms, r, x1, y1, x2, y2, -16777216, alpha);
    }

    public static void shadow(MatrixStack ms, float r, float x1, float y1, float x2, float y2, int color, float alpha) {
        for (int i = 7; i >= 1; --i) {
            float e = (float)i * 1.8f;
            Render2D.rrect(ms, r + e, x1 - e, y1 - e + 2.0f, x2 + e, y2 + e + 2.0f, Render2D.ca(color, alpha * 0.04f));
        }
    }

    public static void scissor(float x, float y, float w, float h) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double sf = mc.getWindow().getScaleFactor();
        int fbH = mc.getWindow().getFramebufferHeight();
        GL11.glEnable((int)3089);
        GL11.glScissor((int)((int)((double)x * sf)), (int)((int)((double)fbH - (double)(y + h) * sf)), (int)Math.max(0, (int)((double)w * sf)), (int)Math.max(0, (int)((double)h * sf)));
    }

    public static void endScissor() {
        GL11.glDisable((int)3089);
    }
}

