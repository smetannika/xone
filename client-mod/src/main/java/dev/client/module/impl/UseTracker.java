package dev.client.module.impl;

import dev.client.gui.ClickGui;
import dev.client.gui.Render2D;
import dev.client.module.BoolSetting;
import dev.client.module.HudModule;
import dev.client.module.NumSetting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Shows what the players you are fighting use: food, potions, pearls, snowballs and so on.
 * For thrown splash potions it also shows how strongly the potion landed on you and on him (in %).
 */
public class UseTracker
extends HudModule {
    private static final int BG = -586018282;
    private static final int GREEN = -11751600;
    private static final int AMBER = -16121;
    private static final int RED = -769226;
    private static final int DIM = -5592406;
    private static final int BLUE = -9979649;

    private final NumSetting range = this.add(new NumSetting("Range", 30.0, 8.0, 80.0, 1.0));
    private final NumSetting timeout = this.add(new NumSetting("Fight timeout (s)", 15.0, 3.0, 60.0, 1.0));
    private final NumSetting showFor = this.add(new NumSetting("Show for (s)", 12.0, 3.0, 60.0, 1.0));
    private final NumSetting maxRows = this.add(new NumSetting("Max rows", 6.0, 2.0, 12.0, 1.0));
    private final BoolSetting allPlayers = this.add(new BoolSetting("Track all nearby", false));
    private final BoolSetting food = this.add(new BoolSetting("Food / potions", true));
    private final BoolSetting thrown = this.add(new BoolSetting("Thrown items", true));
    private final BoolSetting splash = this.add(new BoolSetting("Splash hit %", true));

    private static final class Event {
        final String player;
        final String verb;
        final String item;
        final int color;
        final long time;
        String detail;
        int detailColor = DIM;

        Event(String player, String verb, String item, int color) {
            this.player = player;
            this.verb = verb;
            this.item = item;
            this.color = color;
            this.time = System.currentTimeMillis();
        }
    }

    private static final class PotionTrack {
        final Event event;
        final int ownerId;
        Vec3d last;
        Vec3d land;
        int directId = -1;
        int age;

        PotionTrack(Event event, int ownerId, Vec3d start) {
            this.event = event;
            this.ownerId = ownerId;
            this.last = start;
        }
    }

    private final List<Event> events = new ArrayList<Event>();
    private final Map<UUID, Long> engaged = new HashMap<UUID, Long>();
    private final Map<UUID, Integer> lastHurt = new HashMap<UUID, Integer>();
    private final Map<UUID, ItemStack> using = new HashMap<UUID, ItemStack>();
    private final Set<Integer> seenThrown = new HashSet<Integer>();
    private final Map<Integer, PotionTrack> potions = new HashMap<Integer, PotionTrack>();
    private int myLastHurt;

    public UseTracker() {
        super("Use Tracker", 6, 190, true);
        this.w = 150;
        this.h = 14;
    }

    @Override
    public void onDisable() {
        this.events.clear();
        this.engaged.clear();
        this.lastHurt.clear();
        this.using.clear();
        this.seenThrown.clear();
        this.potions.clear();
    }

    private boolean tracked(PlayerEntity p, long now) {
        if (this.allPlayers.value) {
            return true;
        }
        Long t = this.engaged.get(p.getUuid());
        return t != null && (double)(now - t) < this.timeout.value * 1000.0;
    }

    private void push(Event e) {
        this.events.add(e);
        while (this.events.size() > 40) {
            this.events.remove(0);
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) {
            return;
        }
        long now = System.currentTimeMillis();
        double r2 = this.range.value * this.range.value;

        // --- who are we fighting ---
        boolean iWasHurt = mc.player.hurtTime > 0 && this.myLastHurt == 0;
        this.myLastHurt = mc.player.hurtTime;
        List<PlayerEntity> near = new ArrayList<PlayerEntity>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity) || e == mc.player || !e.isAlive()) continue;
            PlayerEntity p = (PlayerEntity)e;
            if ((double)mc.player.squaredDistanceTo((Entity)p) > r2) continue;
            near.add(p);
            UUID id = p.getUuid();
            int prev = this.lastHurt.containsKey(id) ? this.lastHurt.get(id) : 0;
            boolean hurtEdge = p.hurtTime > 0 && prev == 0;
            this.lastHurt.put(id, p.hurtTime);
            double d2 = mc.player.squaredDistanceTo((Entity)p);
            if (d2 < 49.0 && (hurtEdge || iWasHurt)) {
                this.engaged.put(id, now);
            }
        }

        // --- food / drinks ---
        for (PlayerEntity p : near) {
            UUID id = p.getUuid();
            if (!this.tracked(p, now)) {
                this.using.remove(id);
                continue;
            }
            if (p.isUsingItem() && this.food.value) {
                ItemStack st = p.getActiveItem();
                if (st.isEmpty()) continue;
                ItemStack old = this.using.get(id);
                if (old == null || old.getItem() != st.getItem() || !old.getName().getString().equals(st.getName().getString())) {
                    this.using.put(id, st.copy());
                    this.onUse(p, st);
                }
            } else {
                this.using.remove(id);
            }
        }

        // --- thrown things / splash potions ---
        Set<Integer> alive = new HashSet<Integer>();
        Set<Integer> aliveThrown = new HashSet<Integer>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ThrownItemEntity)) continue;
            ThrownItemEntity t = (ThrownItemEntity)e;
            int eid = t.getEntityId();
            aliveThrown.add(eid);
            if (this.seenThrown.contains(eid)) {
                PotionTrack pt = this.potions.get(eid);
                if (pt != null) {
                    alive.add(eid);
                    this.stepPotion(pt, t);
                }
                continue;
            }
            this.seenThrown.add(eid);
            Entity owner = t.getOwner();
            if (!(owner instanceof PlayerEntity) || owner == mc.player) continue;
            PlayerEntity op = (PlayerEntity)owner;
            if (!this.tracked(op, now)) continue;
            ItemStack st = t.getStack();
            String name = st.isEmpty() ? "item" : st.getName().getString();
            if (t instanceof PotionEntity) {
                Event ev = new Event(op.getName().getString(), "threw", name, BLUE);
                ev.detail = "...";
                if (!this.thrown.value && !this.splash.value) continue;
                this.push(ev);
                if (this.splash.value) {
                    this.potions.put(eid, new PotionTrack(ev, op.getEntityId(), t.getPos()));
                    alive.add(eid);
                } else {
                    ev.detail = null;
                }
            } else if (this.thrown.value) {
                this.push(new Event(op.getName().getString(), "threw", name, AMBER));
            }
        }
        this.seenThrown.retainAll(aliveThrown);

        // potions that disappeared have landed
        Iterator<Map.Entry<Integer, PotionTrack>> it = this.potions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PotionTrack> en = it.next();
            if (alive.contains(en.getKey())) continue;
            this.finish(en.getValue());
            it.remove();
        }
    }

    private void onUse(PlayerEntity p, ItemStack st) {
        Item item = st.getItem();
        String name = st.getName().getString();
        if (item == Items.SHIELD || item == Items.BOW || item == Items.CROSSBOW || item == Items.TRIDENT) {
            return;
        }
        String verb;
        int color;
        if (item instanceof PotionItem) {
            verb = "drinks";
            color = BLUE;
        } else if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) {
            verb = "eats";
            color = AMBER;
        } else if (item == Items.CHORUS_FRUIT) {
            verb = "eats";
            color = RED;
        } else if (item == Items.MILK_BUCKET) {
            verb = "drinks";
            color = DIM;
        } else {
            verb = "uses";
            color = GREEN;
        }
        this.push(new Event(p.getName().getString(), verb, name, color));
    }

    /** Moves the potion's landing estimate forward; stops at the first block or player it touches. */
    private void stepPotion(PotionTrack pt, ThrownItemEntity e) {
        Vec3d cur = e.getPos();
        pt.age++;
        if (pt.land != null) {
            return;
        }
        BlockPos bp = new BlockPos(cur.x, cur.y, cur.z);
        BlockState bs = mc.world.getBlockState(bp);
        if (!bs.getCollisionShape((net.minecraft.world.BlockView)mc.world, bp).isEmpty()) {
            pt.land = new Vec3d((pt.last.x + cur.x) / 2.0, (pt.last.y + cur.y) / 2.0, (pt.last.z + cur.z) / 2.0);
            return;
        }
        for (Entity o : mc.world.getEntities()) {
            if (!(o instanceof PlayerEntity)) continue;
            if (o.getEntityId() == pt.ownerId && pt.age < 5) continue;
            if (o.getBoundingBox().expand(0.3).contains(cur)) {
                pt.land = cur;
                pt.directId = o.getEntityId();
                return;
            }
        }
        pt.last = cur;
    }

    private static double power(Entity target, Vec3d pos, int directId) {
        if (target.getEntityId() == directId) {
            return 1.0;
        }
        Box area = new Box(pos, pos).expand(0.125).expand(4.0, 2.0, 4.0);
        if (!target.getBoundingBox().intersects(area)) {
            return 0.0;
        }
        double d = target.squaredDistanceTo(pos.x, pos.y, pos.z);
        if (d >= 16.0) {
            return 0.0;
        }
        return 1.0 - Math.sqrt(d) / 4.0;
    }

    private void finish(PotionTrack pt) {
        Vec3d pos = pt.land != null ? pt.land : pt.last;
        Entity owner = mc.world.getEntityById(pt.ownerId);
        double you = UseTracker.power((Entity)mc.player, pos, pt.directId);
        int youPct = (int)Math.round(you * 100.0);
        String s = "you " + youPct + "%";
        if (owner != null) {
            int hePct = (int)Math.round(UseTracker.power(owner, pos, pt.directId) * 100.0);
            s = s + "  him " + hePct + "%";
            pt.event.detailColor = hePct >= youPct ? GREEN : DIM;
        }
        if (youPct >= 50) {
            pt.event.detailColor = RED;
        }
        pt.event.detail = s;
    }

    @Override
    public void render(MatrixStack ms) {
        long now = System.currentTimeMillis();
        long life = (long)(this.showFor.value * 1000.0);
        List<Event> rows = new ArrayList<Event>();
        for (int i = this.events.size() - 1; i >= 0 && rows.size() < (int)this.maxRows.value; --i) {
            Event ev = this.events.get(i);
            if (now - ev.time > life) continue;
            rows.add(ev);
        }
        boolean preview = rows.isEmpty() && mc.currentScreen instanceof ClickGui;
        if (rows.isEmpty() && !preview) {
            return;
        }
        if (preview) {
            Event a = new Event("Player", "eats", "Golden Apple", AMBER);
            Event b = new Event("Player", "threw", "Splash Potion", BLUE);
            b.detail = "you 62%  him 100%";
            rows.add(b);
            rows.add(a);
        }

        String title = "Use Tracker";
        int width = mc.textRenderer.getWidth(title);
        for (Event ev : rows) {
            int rw = mc.textRenderer.getWidth(UseTracker.line(ev)) + 12;
            if (ev.detail != null) {
                rw += mc.textRenderer.getWidth(ev.detail) + 6;
            }
            width = Math.max(width, rw);
        }
        this.w = Math.max(110, width + 8);
        this.h = 14 + rows.size() * 11;

        Render2D.shadow(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, 0.9f);
        Render2D.rrect(ms, 5.0f, this.x, this.y, this.x + this.w, this.y + this.h, BG);
        mc.textRenderer.drawWithShadow(ms, title, (float)(this.x + 4), (float)(this.y + 3), -1);

        int ly = this.y + 15;
        for (Event ev : rows) {
            Render2D.rrect(ms, 1.0f, this.x + 4, ly + 1, this.x + 6, ly + 8, ev.color);
            String text = UseTracker.line(ev);
            mc.textRenderer.drawWithShadow(ms, text, (float)(this.x + 10), (float)ly, -1);
            if (ev.detail != null) {
                mc.textRenderer.drawWithShadow(ms, ev.detail, (float)(this.x + this.w - 4 - mc.textRenderer.getWidth(ev.detail)), (float)ly, ev.detailColor);
            }
            ly += 11;
        }
    }

    private static String line(Event ev) {
        return ev.player + " " + ev.verb + " " + ev.item;
    }
}
