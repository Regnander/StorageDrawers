package com.jaquadro.minecraft.storagedrawers.inventory;

import com.jaquadro.minecraft.storagedrawers.StorageDrawers;
import com.jaquadro.minecraft.storagedrawers.api.storage.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

public class DrawerItemHandler implements IItemHandler
{
    @CapabilityInject(IDrawerAttributes.class)
    public static Capability<IDrawerAttributes> DRAWER_ATTRIBUTES_CAPABILITY = null;

    private IDrawerGroup group;
    private ICapabilityProvider capProvider;

    public DrawerItemHandler (IDrawerGroup group, ICapabilityProvider capProvider) {
        this.group = group;
        this.capProvider = capProvider;
    }

    @Override
    public int getSlots () {
        return group.getDrawerCount() + 1;
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot (int slot) {
        if (slotIsVirtual(slot))
            return ItemStack.EMPTY;

        slot -= 1;
        if (group instanceof IPriorityGroup) {
            int[] order = ((IPriorityGroup) group).getAccessibleDrawerSlots();
            slot = (slot >= 0 && slot < order.length) ? order[slot] : -1;
        }

        IDrawer drawer = group.getDrawer(slot);
        if (!drawer.isEnabled() || drawer.isEmpty())
            return ItemStack.EMPTY;

        ItemStack stack = drawer.getStoredItemPrototype().copy();
        stack.setCount(drawer.getStoredItemCount());

        return stack;
    }

    @Override
    @Nonnull
    public ItemStack insertItem (int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slotIsVirtual(slot)) {
            if (StorageDrawers.config.cache.enableItemConversion)
                return insertItemFullScan(stack, simulate);
            else
                return stack;
        }

        slot -= 1;
        int orderedSlot = slot;
        int prevSlot = slot - 1;

        if (group instanceof IPriorityGroup) {
            int[] order = ((IPriorityGroup) group).getAccessibleDrawerSlots();
            orderedSlot = (slot >= 0 && slot < order.length) ? order[slot] : -1;
            prevSlot = (slot >= 1 && slot < order.length) ? order[slot - 1] : -1;
        }

        if (StorageDrawers.config.cache.enableItemConversion && orderedSlot > 0) {
            IDrawer drawer = group.getDrawer(orderedSlot);
            if (!drawer.isEnabled() && drawer.isEmpty()) {
                IDrawer prevDrawer = group.getDrawer(prevSlot);
                if (!prevDrawer.isEnabled())
                    return insertItemFullScan(stack, simulate);
                else if (!prevDrawer.isEmpty())
                    return insertItemFullScan(stack, simulate);
            }
        }

        return insertItemInternal(orderedSlot, stack, simulate);
    }

    @Nonnull
    private ItemStack insertItemFullScan (@Nonnull ItemStack stack, boolean simulate) {
        if (group instanceof ISmartGroup) {
            for (int i : ((ISmartGroup) group).enumerateDrawersForInsertion(stack, false)) {
                stack = insertItemInternal(i, stack, simulate);
                if (stack.isEmpty())
                    break;
            }
        }
        else if (group instanceof IPriorityGroup) {
            int[] order = ((IPriorityGroup) group).getAccessibleDrawerSlots();
            for (int i = 0; i < order.length; i++) {
                stack = insertItemInternal(i, stack, simulate);
                if (stack.isEmpty())
                    break;
            }
        }
        else {
            for (int i = 0; i < group.getDrawerCount(); i++) {
                stack = insertItemInternal(i, stack, simulate);
                if (stack.isEmpty())
                    break;
            }
        }

        return stack;
    }

    @Nonnull
    private ItemStack insertItemInternal (int slot, @Nonnull ItemStack stack, boolean simulate) {
        IDrawer drawer = group.getDrawer(slot);
        if (!drawer.isEnabled() || !drawer.canItemBeStored(stack))
            return stack;

        int availableCount = drawer.isEmpty() ? drawer.getMaxCapacity(stack) : drawer.getRemainingCapacity();

        // TODO: May not need void check here with updated storage implementation
        IDrawerAttributes attrs = capProvider.getCapability(DRAWER_ATTRIBUTES_CAPABILITY, null);
        if (attrs != null && attrs.isVoid())
            availableCount = Integer.MAX_VALUE;

        int stackSize = stack.getCount();
        int insertCount = Math.min(stackSize, availableCount);
        int remainder = stackSize - insertCount;

        if (remainder == stackSize)
            return stack;

        if (!simulate) {
            if (drawer.isEmpty())
                drawer = drawer.setStoredItem(stack);
            drawer.setStoredItemCount(drawer.getStoredItemCount() + insertCount);
        }

        if (remainder == 0)
            return ItemStack.EMPTY;

        ItemStack returnStack = stack.copy();
        returnStack.setCount(remainder);

        return returnStack;
    }

    @Override
    @Nonnull
    public ItemStack extractItem (int slot, int amount, boolean simulate) {
        if (slotIsVirtual(slot))
            return ItemStack.EMPTY;

        slot -= 1;
        if (group instanceof IPriorityGroup) {
            int[] order = ((IPriorityGroup) group).getAccessibleDrawerSlots();
            slot = (slot >= 0 && slot < order.length) ? order[slot] : -1;
        }

        IDrawer drawer = group.getDrawer(slot);
        if (!drawer.isEnabled() || drawer.isEmpty() || drawer.getStoredItemCount() == 0)
            return ItemStack.EMPTY;

        ItemStack returnStack = drawer.getStoredItemPrototype().copy();
        int stackSize = Math.min(drawer.getStoredItemCount(), amount);
        stackSize = Math.min(stackSize, returnStack.getMaxStackSize());

        returnStack.setCount(stackSize);

        if (!simulate)
            drawer.setStoredItemCount(drawer.getStoredItemCount() - stackSize);

        return returnStack;
    }

    @Override
    public int getSlotLimit (int slot) {
        if (slotIsVirtual(slot))
            return Integer.MAX_VALUE;

        IDrawer drawer = group.getDrawer(slot);
        if (!drawer.isEnabled())
            return 0;
        if (drawer.isEmpty())
            return drawer.getMaxCapacity(ItemStack.EMPTY);

        return drawer.getMaxCapacity();
    }

    private boolean slotIsVirtual (int slot) {
        return slot == 0;
    }
}
