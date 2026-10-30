package dev.client.module.impl;

import dev.client.module.BindSetting;
import dev.client.module.Module;
import dev.client.module.NumSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Hotkeys for server items: find the item (hotbar first, then inventory), swap to it,
 * use it and swap back.
 */
public class ServerHelper
extends Module {
    private final BindSetting trapBind = this.add(new BindSetting("Трапка (FT)"));
    private final BindSetting disorientBind = this.add(new BindSetting("Дезориентация"));
    private final BindSetting snowballBind = this.add(new BindSetting("Снежок"));
    private final BindSetting killerBind = this.add(new BindSetting("Зелье киллера"));
    private final NumSetting delay = this.add(new NumSetting("Switch delay ms", 60.0, 0.0, 300.0, 5.0));

    private final List<Action> actions = new ArrayList<Action>();
    private Action current;
    private int phase;
    private long at;
    private int prevSlot;
    private boolean usedSwap;
    private int invSlot = -1;

    private static final class Action {
        final BindSetting bind;
        final String[] words;
        final Item item;
        final boolean potionOnly;
        final boolean blockFirst;
        boolean wasDown;

        Action(BindSetting bind, String[] words, Item item, boolean potionOnly, boolean blockFirst) {
            this.bind = bind;
            this.words = words;
            this.item = item;
            this.potionOnly = potionOnly;
            this.blockFirst = blockFirst;
        }

        boolean matches(ItemStack s) {
            if (s.isEmpty()) {
                return false;
            }
            if (this.item != null && s.getItem() == this.item) {
                return true;
            }
            if (this.potionOnly && !(s.getItem() instanceof PotionItem)) {
                return false;
            }
            String n = s.getName().getString().toLowerCase(Locale.ROOT);
            for (String w : this.words) {
                if (n.contains(w)) {
                    return true;
                }
            }
            return false;
        }
    }

    public ServerHelper() {
        super("Server Helper", Module.Category.SERVER);
        this.enabled = true;
        // trap: placed on the block you look at, everything else is a plain right click
        this.actions.add(new Action(this.trapBind, new String[]{"трапк", "trap"}, null, false, true));
        this.actions.add(new Action(this.disorientBind, new String[]{"дезориент", "disorient"}, null, false, false));
        this.actions.add(new Action(this.snowballBind, new String[]{"снежок", "снежк", "snowball"}, Items.SNOWBALL, false, false));
        this.actions.add(new Action(this.killerBind, new String[]{"киллер", "killer"}, null, true, false));
    }

    @Override
    public void onDisable() {
        if (this.phase != 0) {
            this.restore();
        }
        this.phase = 0;
        this.current = null;
    }

    @Override
    public void onTick() {
        if (ServerHelper.mc.player == null || ServerHelper.mc.world == null || ServerHelper.mc.interactionManager == null) {
            this.phase = 0;
            this.current = null;
            return;
        }
        long now = System.currentTimeMillis();
        long handle = ServerHelper.mc.getWindow().getHandle();

        // key edges are tracked every tick so a held key never fires twice
        Action fire = null;
        for (Action a : this.actions) {
            boolean down = a.bind.key > 0 && InputUtil.isKeyPressed(handle, a.bind.key);
            if (down && !a.wasDown && fire == null) {
                fire = a;
            }
            a.wasDown = down;
        }

        if (this.phase == 1 && now >= this.at) {
            this.use(this.current);
            this.phase = 2;
            this.at = now + (long)this.delay.value;
            return;
        }
        if (this.phase == 2 && now >= this.at) {
            this.restore();
            this.phase = 0;
            this.current = null;
            return;
        }
        if (this.phase != 0) {
            return;
        }
        if (fire != null && ServerHelper.mc.currentScreen == null) {
            this.start(fire, now);
        }
    }

    private void start(Action a, long now) {
        int i;
        int hotbar = -1;
        int inv = -1;
        for (i = 0; i < 9; ++i) {
            if (!a.matches(ServerHelper.mc.player.inventory.main.get(i))) continue;
            hotbar = i;
            break;
        }
        if (hotbar < 0) {
            for (i = 9; i < 36; ++i) {
                if (!a.matches(ServerHelper.mc.player.inventory.main.get(i))) continue;
                inv = i;
                break;
            }
        }
        if (hotbar < 0 && inv < 0) {
            return;
        }
        this.current = a;
        this.prevSlot = ServerHelper.mc.player.inventory.selectedSlot;
        if (hotbar >= 0) {
            ServerHelper.mc.player.inventory.selectedSlot = hotbar;
            this.usedSwap = false;
        } else {
            ServerHelper.mc.interactionManager.clickSlot(ServerHelper.mc.player.playerScreenHandler.syncId, inv, this.prevSlot, SlotActionType.SWAP, (PlayerEntity)ServerHelper.mc.player);
            this.invSlot = inv;
            this.usedSwap = true;
        }
        this.phase = 1;
        this.at = now + (long)this.delay.value;
    }

    private void use(Action a) {
        ActionResult r = ActionResult.PASS;
        HitResult hr = ServerHelper.mc.crosshairTarget;
        if (a != null && a.blockFirst && hr instanceof BlockHitResult && hr.getType() == HitResult.Type.BLOCK) {
            r = ServerHelper.mc.interactionManager.interactBlock(ServerHelper.mc.player, ServerHelper.mc.world, Hand.MAIN_HAND, (BlockHitResult)hr);
        }
        if (!r.isAccepted()) {
            ServerHelper.mc.interactionManager.interactItem((PlayerEntity)ServerHelper.mc.player, ServerHelper.mc.world, Hand.MAIN_HAND);
        }
        ServerHelper.mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void restore() {
        if (ServerHelper.mc.player == null || ServerHelper.mc.interactionManager == null) {
            return;
        }
        if (this.usedSwap && this.invSlot >= 0) {
            ServerHelper.mc.interactionManager.clickSlot(ServerHelper.mc.player.playerScreenHandler.syncId, this.invSlot, this.prevSlot, SlotActionType.SWAP, (PlayerEntity)ServerHelper.mc.player);
        } else {
            ServerHelper.mc.player.inventory.selectedSlot = this.prevSlot;
        }
        this.usedSwap = false;
        this.invSlot = -1;
    }
}
