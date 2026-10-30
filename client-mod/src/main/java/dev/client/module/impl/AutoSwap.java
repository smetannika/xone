package dev.client.module.impl;

import dev.client.module.BoolSetting;
import dev.client.module.Module;
import dev.client.module.NumSetting;
import java.util.Locale;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class AutoSwap
extends Module {
    private final BoolSetting totems = this.add(new BoolSetting("Totems", true));
    private final BoolSetting spheres = this.add(new BoolSetting("Spheres", true));
    private final NumSetting delay = this.add(new NumSetting("Delay ms", 80.0, 0.0, 500.0, 5.0));
    private Kind lastKind = Kind.NONE;
    private long swapAt = -1L;

    public AutoSwap() {
        super("AutoSwap", Module.Category.COMBAT);
    }

    @Override
    public void onDisable() {
        this.lastKind = Kind.NONE;
        this.swapAt = -1L;
    }

    private static boolean isSphere(ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        String n = s.getName().getString().toLowerCase(Locale.ROOT);
        return n.contains("сфер") || n.contains("sphere");
    }

    private Kind kindOf(ItemStack s) {
        if (s.isEmpty()) {
            return Kind.NONE;
        }
        if (this.totems.value && s.getItem() == Items.TOTEM_OF_UNDYING) {
            return Kind.TOTEM;
        }
        if (this.spheres.value && AutoSwap.isSphere(s)) {
            return Kind.SPHERE;
        }
        return Kind.NONE;
    }

    private boolean matches(ItemStack s, Kind k) {
        if (s.isEmpty()) {
            return false;
        }
        if (k == Kind.TOTEM) {
            return s.getItem() == Items.TOTEM_OF_UNDYING;
        }
        if (k == Kind.SPHERE) {
            return AutoSwap.isSphere(s);
        }
        return false;
    }

    @Override
    public void onTick() {
        if (AutoSwap.mc.player == null || AutoSwap.mc.interactionManager == null) {
            return;
        }
        if (!AutoSwap.mc.player.isAlive()) {
            this.lastKind = Kind.NONE;
            this.swapAt = -1L;
            return;
        }
        if (AutoSwap.mc.currentScreen instanceof HandledScreen) {
            return;
        }
        Kind cur = this.kindOf(AutoSwap.mc.player.getOffHandStack());
        if (cur != Kind.NONE) {
            this.lastKind = cur;
            this.swapAt = -1L;
            return;
        }
        if (!AutoSwap.mc.player.getOffHandStack().isEmpty() || this.lastKind == Kind.NONE) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.swapAt < 0L) {
            this.swapAt = now + (long)this.delay.value;
        }
        if (now < this.swapAt) {
            return;
        }
        int slot = this.findSlot(this.lastKind);
        if (slot >= 0) {
            AutoSwap.mc.interactionManager.clickSlot(AutoSwap.mc.player.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, (PlayerEntity)AutoSwap.mc.player);
        }
        this.lastKind = Kind.NONE;
        this.swapAt = -1L;
    }

    private int findSlot(Kind k) {
        for (int i = 0; i < 36; ++i) {
            ItemStack s = (ItemStack)AutoSwap.mc.player.inventory.main.get(i);
            if (!this.matches(s, k)) continue;
            return i < 9 ? 36 + i : i;
        }
        return -1;
    }

    private static enum Kind {
        NONE,
        TOTEM,
        SPHERE;

    }
}

