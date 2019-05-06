/*
 *  * Copyright © Wynntils - 2019.
 */

package com.wynntils.modules.core.instances;

import com.wynntils.ModCore;
import com.wynntils.core.events.custom.PacketEvent;
import com.wynntils.core.framework.FrameworkManager;
import com.wynntils.core.framework.enums.FilterType;
import com.wynntils.core.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Used for fake opening inventories that are opened by items
 * Just create the instance (title, clickItemPosition)
 * you can receive the items by setting up onReceiveItems
 * call #.open when you are ready to open the inventory and
 * call #.close whenever you finish using the inventory, otherwise
 * everything will bug and catch fire.
 */
public class FakeInventory {

    String windowTitle;
    int itemSlot;

    private Consumer<FakeInventory> onReceiveItems = null;
    private int windowId = -1;
    private short transactionId = 0;

    private HashMap<Integer, ItemStack> items = new HashMap<>();

    boolean open = false;

    public FakeInventory(String windowTitle, int itemSlot) {
        this.windowTitle = windowTitle;
        this.itemSlot = itemSlot;
    }

    public FakeInventory onReceiveItems(Consumer<FakeInventory> onReceiveItems) {
        this.onReceiveItems = onReceiveItems;

        return this;
    }

    /**
     * Request the inventory to be opened
     *
     * @return
     */
    public FakeInventory open() {
        FrameworkManager.getEventBus().register(this);

        Minecraft mc = ModCore.mc();
        int slot = mc.player.inventory.currentItem;

        if(slot == itemSlot) {
            mc.getConnection().sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
            return this;
        }

        mc.getConnection().sendPacket(new CPacketHeldItemChange(itemSlot));
        mc.getConnection().sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
        mc.getConnection().sendPacket(new CPacketHeldItemChange(slot));
        return this;
    }

    /**
     * Closes the fake inventory, it NEEDS to be called, otherwise the inventory
     * will NEVER close and will glitch EVERY SINGLE THING.
     *
     * don't forget to call this at any cost, please.
     */
    public void close() {
        if(!open) return;

        FrameworkManager.getEventBus().unregister(this);
        open = false;
        if(windowId != -1) Minecraft.getMinecraft().getConnection().sendPacket(new CPacketCloseWindow(windowId));
    }

    /**
     * Simulates a inventory click on a certain slot
     *
     * @param slot the input slot
     * @param mouseButton what mouse button was
     * @param type the typeof the click
     */
    public void clickItem(int slot, int mouseButton, ClickType type) {
        if(!open) return;

        transactionId++;
        Minecraft.getMinecraft().getConnection().sendPacket(new CPacketClickWindow(windowId, slot, mouseButton, type, items.get(slot), transactionId));
    }

    /**
     * Returns the ItemStack that is present at the provied slot
     * Should only be called if onReceiveItems was triggered
     *
     * @param slot the input slot
     * @return the ItemStack at the slot
     */
    public ItemStack getItem(int slot) {
        if(!open || !items.containsKey(slot)) return null;

        return items.get(slot);
    }

    /**
     * Returns all the inventory items
     *
     * @return a list containing all the items
     */
    public List<ItemStack> getItems() {
        if(!open) return null;

        return new ArrayList<>(items.values());
    }

    /**
     * Find an specific item at the inventory
     *
     * @param name the item name
     * @param filterType the type of the filter
     * @return An entry with the slot number and the ItemStack
     */
    public Map.Entry<Integer, ItemStack> findItem(String name, FilterType filterType) {
        if(!open) return null;

        if(filterType == FilterType.CONTAINS) return items.entrySet().stream().filter(c -> c.getValue() != null && !c.getValue().isEmpty() && c.getValue().hasDisplayName() && Utils.stripColor(c.getValue().getDisplayName()).contains(name)).findFirst().orElse(null);
        else if(filterType == FilterType.EQUALS) return items.entrySet().stream().filter(c -> c.getValue() != null && !c.getValue().isEmpty() && c.getValue().hasDisplayName() && Utils.stripColor(c.getValue().getDisplayName()).equals(name)).findFirst().orElse(null);
        else return items.entrySet().stream().filter(c -> c.getValue() != null && !c.getValue().isEmpty() && c.getValue().hasDisplayName() && Utils.stripColor(c.getValue().getDisplayName()).equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /**
     * Returns if the current FakeInventory is opened
     *
     * @return if the current FakeInventory is opened
     */
    public boolean isOpen() {
        return open;
    }

    //EVENTS BELOW

    //handles the inventory container receive, sets open to true
    @SubscribeEvent
    public void onInventoryReceive(PacketEvent.InventoryReceived e) {
        if(!"minecraft:container".equals(e.getPacket().getGuiId()) || !e.getPacket().hasSlots() || !e.getPacket().getWindowTitle().getUnformattedText().contains(windowTitle)) {
            close();
            return;
        }

        windowId = e.getPacket().getWindowId();
        open = true;

        e.setCanceled(true);
    }

    //handles the items, calls onReceiveItems
    @SubscribeEvent
    public void onItemsReceive(PacketEvent.InventoryItemsReceived e) {
        if(e.getPacket().getWindowId() != windowId) {
            FrameworkManager.getEventBus().unregister(this);
            open = false;
            return;
        }

        items.clear();

        List<ItemStack> stacks = e.getPacket().getItemStacks();
        for(int i = 0; i < stacks.size(); i++) {
            items.put(i, stacks.get(i));
        }

        if(onReceiveItems != null) onReceiveItems.accept(this);

        e.setCanceled(true);
    }

    //cancel all other interactions to avoid GUI openings while this one is already opened
    @SubscribeEvent
    public void cancelInteract(PacketEvent.PlayerUseItemEvent e) {
        if(!open) return;

        Minecraft.getMinecraft().player.sendMessage(new TextComponentString(TextFormatting.RED + "Your action was canceled because Wynntils is processing a background inventory."));
        e.setCanceled(true);
    }

    //cancel all other interactions to avoid GUI openings while this one is already opened
    @SubscribeEvent
    public void cancelInteract(PacketEvent.PlayerUseItemOnBlockEvent e) {
        if(!open) return;

        Minecraft.getMinecraft().player.sendMessage(new TextComponentString(TextFormatting.RED + "Your action was canceled because Wynntils is processing a background inventory."));
        e.setCanceled(true);
    }

}
