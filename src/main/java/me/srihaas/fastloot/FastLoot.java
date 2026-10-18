package me.srihaas.fastloot;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class FastLoot extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("FastLoot enabled!");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!player.isSneaking()) {
            return;
        }

        if (!player.hasPermission("fastloot.use")) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null || !(block.getState() instanceof Container)) {
            return;
        }

        // Let players place blocks while sneaking normally.
        if (isPlaceable(player.getInventory().getItemInMainHand().getType())) {
            return;
        }

        event.setCancelled(true);

        Container container = (Container) block.getState();
        Inventory containerInventory = container.getInventory();
        Inventory playerInventory = player.getInventory();

        int movedItems = 0;
        int movedStacks = 0;

        for (int slot = 0; slot < containerInventory.getSize(); slot++) {
            ItemStack chestItem = containerInventory.getItem(slot);

            if (chestItem == null || chestItem.getType().isAir()) {
                continue;
            }

            int before = chestItem.getAmount();

            // addItem returns any amount that did not fit.
            HashMap<Integer, ItemStack> leftovers =
                    playerInventory.addItem(chestItem.clone());

            int leftOverAmount = 0;

            for (ItemStack leftover : leftovers.values()) {
                leftOverAmount += leftover.getAmount();
            }

            int moved = before - leftOverAmount;

            if (moved > 0) {
                movedItems += moved;
                movedStacks++;

                if (leftOverAmount == 0) {
                    containerInventory.setItem(slot, null);
                } else {
                    ItemStack remaining = chestItem.clone();
                    remaining.setAmount(leftOverAmount);
                    containerInventory.setItem(slot, remaining);
                }
            }
        }

        container.update();

        if (movedItems == 0) {
            player.sendMessage(ChatColor.RED + "Your inventory is full, or this container is empty.");
            return;
        }

        player.sendMessage(
                ChatColor.GREEN + "Looted " + movedItems
                        + " item" + (movedItems == 1 ? "" : "s")
                        + " from " + movedStacks
                        + " stack" + (movedStacks == 1 ? "" : "s") + "."
        );

        if (!hasRoomForAnything(playerInventory)) {
            player.sendMessage(ChatColor.YELLOW + "Your inventory is now full.");
        }
    }

    private boolean isPlaceable(Material material) {
        return material.isBlock();
    }

    private boolean hasRoomForAnything(Inventory inventory) {
        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                return true;
            }

            if (item.getAmount() < item.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }
}
