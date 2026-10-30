package dev.client.module.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.client.module.BoolSetting;
import dev.client.module.Module;
import dev.client.module.NumSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;

public class Esp
extends Module {
    private static final int MAX_LIGHT = 15728880;
    private static final int TAG_BG = 0x55000000;
    private static final int GREEN = -11751600;
    private static final int AMBER = -16121;
    private static final int RED = -769226;
    private static final int DIM = -5592406;

    private final BoolSetting players = this.add(new BoolSetting("Players", true));
    private final BoolSetting hostile = this.add(new BoolSetting("Hostile mobs", true));
    private final BoolSetting passive = this.add(new BoolSetting("Passive mobs", false));
    private final BoolSetting walls = this.add(new BoolSetting("Through walls", true));
    private final BoolSetting boxes = this.add(new BoolSetting("Boxes", true));
    private final BoolSetting tags = this.add(new BoolSetting("Name tags", true));
    private final BoolSetting tagHealth = this.add(new BoolSetting("Tag health", true));
    private final BoolSetting tagDistance = this.add(new BoolSetting("Tag distance", true));
    private final NumSetting tagScale = this.add(new NumSetting("Tag scale", 1.0, 0.5, 3.0, 0.1));

    private static final class Tag {
        final LivingEntity entity;
        final double x;
        final double y;
        final double z;

        Tag(LivingEntity entity, double x, double y, double z) {
            this.entity = entity;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public Esp() {
        super("ESP", Module.Category.RENDER);
    }

    @Override
    public void onWorldRender(WorldRenderContext ctx) {
        MatrixStack ms = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        float pt = ctx.tickDelta();
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        List<Tag> pending = new ArrayList<Tag>();
        if (this.walls.value) {
            RenderSystem.disableDepthTest();
        }
        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);

        // pass 1: boxes (and collect the entities that need a name tag)
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLines());
        for (Entity e : Esp.mc.world.getEntities()) {
            float b;
            float g;
            float r;
            if (!(e instanceof LivingEntity) || e == Esp.mc.player || !e.isAlive()) continue;
            if (e instanceof PlayerEntity) {
                if (!this.players.value) continue;
                r = 1.0f;
                g = 0.2f;
                b = 0.2f;
            } else if (e instanceof HostileEntity) {
                if (!this.hostile.value) continue;
                r = 1.0f;
                g = 0.6f;
                b = 0.1f;
            } else {
                if (!this.passive.value) continue;
                r = 0.2f;
                g = 1.0f;
                b = 0.4f;
            }
            double ox = MathHelper.lerp((double)pt, (double)e.lastRenderX, (double)e.getX()) - e.getX();
            double oy = MathHelper.lerp((double)pt, (double)e.lastRenderY, (double)e.getY()) - e.getY();
            double oz = MathHelper.lerp((double)pt, (double)e.lastRenderZ, (double)e.getZ()) - e.getZ();
            if (this.boxes.value) {
                Box box = e.getBoundingBox().offset(ox, oy, oz);
                WorldRenderer.drawBox((MatrixStack)ms, (VertexConsumer)vc, (Box)box, (float)r, (float)g, (float)b, (float)1.0f);
            }
            if (this.tags.value) {
                pending.add(new Tag((LivingEntity)e, e.getX() + ox, e.getY() + oy, e.getZ() + oz));
            }
        }
        vcp.draw(RenderLayer.getLines());

        // pass 2: name tags, drawn in world space and turned to face the camera
        for (Tag t : pending) {
            this.drawTag(ctx, ms, vcp, t);
        }
        if (!pending.isEmpty()) {
            vcp.draw();
        }
        ms.pop();
        if (this.walls.value) {
            RenderSystem.enableDepthTest();
        }
    }

    private void drawTag(WorldRenderContext ctx, MatrixStack ms, VertexConsumerProvider.Immediate vcp, Tag t) {
        LivingEntity e = t.entity;
        float dist = Esp.mc.player.distanceTo((Entity)e);
        List<String> texts = new ArrayList<String>();
        List<Integer> colors = new ArrayList<Integer>();
        texts.add(e.getName().getString());
        colors.add(-1);
        if (this.tagHealth.value) {
            float hp = e.getHealth();
            float ratio = Math.max(0.0f, Math.min(1.0f, hp / Math.max(e.getMaxHealth(), 1.0f)));
            texts.add(" " + String.format(Locale.ROOT, "%.1f", Float.valueOf(hp)));
            colors.add(ratio > 0.5f ? GREEN : (ratio > 0.25f ? AMBER : RED));
        }
        if (this.tagDistance.value) {
            texts.add(" " + String.format(Locale.ROOT, "%.0fm", Float.valueOf(dist)));
            colors.add(DIM);
        }
        int total = 0;
        for (String s : texts) {
            total += Esp.mc.textRenderer.getWidth(s);
        }

        ms.push();
        ms.translate(t.x, t.y + (double)e.getHeight() + 0.5, t.z);
        ms.multiply(ctx.camera().getRotation());
        // size grows with distance so the tag keeps a readable size on screen
        float s = 0.0025f * this.tagScale.f() * (float)Math.max((double)dist, 3.0);
        ms.scale(-s, -s, s);
        Matrix4f m = ms.peek().getModel();
        float cx = -(float)total / 2.0f;
        for (int i = 0; i < texts.size(); ++i) {
            Esp.mc.textRenderer.draw(texts.get(i), cx, 0.0f, colors.get(i).intValue(), false, m, (VertexConsumerProvider)vcp, this.walls.value, TAG_BG, MAX_LIGHT);
            cx += (float)Esp.mc.textRenderer.getWidth(texts.get(i));
        }
        ms.pop();
    }
}
