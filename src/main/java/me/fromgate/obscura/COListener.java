/*  
 *  CameraObscura, Minecraft bukkit plugin
 *  (c)2012-2016, fromgate, fromgate@gmail.com
 *  http://dev.bukkit.org/server-mods/camera-obscura/
 *    
 *  This file is part of CameraObscura.
 *  
 *  CameraObscura is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CameraObscura is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CameraObscura.  If not, see <http://www.gnorg/licenses/>.
 * 
 */

package me.fromgate.obscura;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;

public class COListener implements Listener {
    Obscura plg;
    COUtil u;

    public COListener(Obscura plg) {
        this.plg = plg;
        this.u = plg.u;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPutMapInFrame(PlayerInteractEntityEvent event) {
        if (!plg.hideNameInFrames) return;
        if (event.getRightClicked().getType() != EntityType.ITEM_FRAME) return;
        ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
        if (itemInHand == null) return;
        if (!Album.isObscuraMap(itemInHand)) return;
        ItemMeta im = itemInHand.getItemMeta();
        im.setDisplayName(null);
        itemInHand.setItemMeta(im);
        event.getPlayer().getInventory().setItemInMainHand(itemInHand);
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player p = event.getPlayer();
        ImageCraft.updateSkinCache(p);
        u.updateMsg(p);
        RenderHistory.clearHistory(p);
        WoolSelect.clearSelection(p);
        COCamera.updateInventoryItems(p.getInventory());
        if (plg.personalFolders && plg.autocreatePersonalFolder && p.hasPermission("camera-obscura.files.autocreate")) {
            File dir = new File(plg.dirImages + p.getName() + File.separator);
            if (!dir.exists()) dir.mkdirs();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        RenderHistory.clearHistory(event.getPlayer());
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRotatePicture(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() != EntityType.ITEM_FRAME) return;
        ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
        if (itemFrame.getItem() == null) return;
        if (!Album.isObscuraMap(itemFrame.getItem())) return;
        event.setCancelled(!Album.isRotationAllowed(itemFrame.getItem().getDurability()));
    }


    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBrush(PlayerInteractEvent event) {
        if ((event.getAction() != Action.LEFT_CLICK_BLOCK) && (event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player p = event.getPlayer();
        if ((p.getItemInHand() == null) || (p.getItemInHand().getTypeId() != WoolSelect.wand)) return;
        if (!WoolSelect.getBrushMode(p)) return;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            WoolSelect.setP1(p, event.getClickedBlock().getLocation());
            u.printMSG(p, "msg_selectp1");
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            WoolSelect.setP2(p, event.getClickedBlock().getLocation());
            u.printMSG(p, "msg_selectp2");
        }
        event.setCancelled(true);
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCopyPicure(CraftItemEvent event) {
        if ((event.getCurrentItem().getType() == Material.MAP) &&
                (!Album.isCopyAllowed(event.getCurrentItem().getDurability())))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCraftPicturePaperAndCamera(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;
        ItemStack item = recipe.getResult();
        if ((item.getType() == Material.EMPTY_MAP) && (COCamera.inventoryContainsPicture(event.getInventory())))
            event.getInventory().setResult(new ItemStack(Material.AIR));
        else if (COCamera.isCamera(item))
            event.getInventory().setResult(COCamera.newCamera());
        else if (COCamera.isPhotoPaper(item))
            event.getInventory().setResult(COCamera.newPhotoPaper(item.getAmount()));
        else if ((COCamera.inventoryContainsPaper(event.getInventory())) ||
                (COCamera.inventoryContainsCamera(event.getInventory())))
            event.getInventory().setResult(new ItemStack(Material.AIR));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTakePicture(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        if (!p.hasPermission("camera-obscura.handy.use")) return;
        if (!COCamera.isCameraInHand(p)) return;
        if ((event.getRightClicked() == null) || (!(event.getRightClicked() instanceof Player))) return;
        Player model = (Player) event.getRightClicked();
        double d = p.getLocation().distance(model.getLocation());
        if (d < plg.focus1) Album.takePicturePortrait(p, model.getName());
        else if ((d >= plg.focus1) && (d < plg.focus2)) Album.takePictureTopHalf(p, model.getName());
        else Album.takePicturePhoto(p, model.getName());
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onObscuraShot(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("camera-obscura.tripod-camera.use")) return;
        Block cb = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (cb.getType() != Material.STONE_BUTTON) return;
        if ((player.getInventory().getItemInMainHand() == null) ||
                (!COCamera.isPhotoPaper(player.getInventory().getItemInMainHand()))) return;
        if (COCamera.isClickedButtonIsLens(event.getClickedBlock())) {
            String background = plg.defaultBackground;
            double price = 0;
            String owner = "unknown";
            int focus = 0; // если 0 - [photo]
            // 1 - [head shot]
            // 2 - [top half]
            // 3 - [full length]


            Sign sign = COCamera.getObscuraSign(cb.getRelative(BlockFace.DOWN));
            if (sign != null) {
                background = COCamera.getObscuraBackground(sign);
                price = COCamera.getObscuraPrice(sign);
                owner = COCamera.getObscuraOwner(sign);
                focus = COCamera.getObscuraFocus(sign);

                if (plg.vault_eco && (price > 0) && (!player.getName().equalsIgnoreCase(owner))) {
                    if (plg.economy.has(player.getName(), price)) {
                        if (!owner.equalsIgnoreCase("unknown")) {
                            plg.economy.depositPlayer(owner, price);
                            @SuppressWarnings("deprecation")
                            Player tp = Bukkit.getPlayerExact(owner);
                            if ((tp != null) && (tp.isOnline()))
                                u.printMSG(player, "msg_youreceived", plg.economy.format(price), player.getName(), plg.economy.format(plg.economy.getBalance(player.getName()))); //You have paid %1% for photgraphy! (Balance: %2%)
                        }
                        plg.economy.withdrawPlayer(player.getName(), price);
                        u.printMSG(player, "msg_youpaid", plg.economy.format(price), plg.economy.format(plg.economy.getBalance(player.getName())));
                    } else {
                        u.printMSG(player, "msg_youhavenotmoney");
                        return;
                    }
                }
            }
            if (focus == 0) {
                double d = player.getLocation().distance(cb.getLocation());
                if (d < plg.focus1) focus = 1;
                else if ((d >= plg.focus1) && (d < plg.focus2)) focus = 2;
                else focus = 3;
            }
            switch (focus) {
                case 1:
                    Album.developPortrait(player, owner, player.getName(), background);
                    break;
                case 2:
                    Album.developTopHalfPhoto(player, owner, player.getName(), background);
                    break;
                case 3:
                    Album.developPhoto(player, owner, player.getName(), background);
                    break;
            }
            player.getWorld().playSound(cb.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
            player.getWorld().playSound(cb.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakTripodCamera(BlockBreakEvent event) {
        if (!plg.dropObscura) return;
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) return;
        Block b = event.getBlock();
        if (COCamera.isBlockIsPartOfCamera(b)) {
            if (b.getType() != Material.STONE_BUTTON) b = COCamera.getLensFromTripodCamera(b);
            if (b == null) return;
            b.getDrops().clear();
            b.setType(Material.AIR);
            ItemStack camera = COCamera.newCamera();
            World w = b.getWorld();
            Item dropped = w.dropItemNaturally(b.getLocation(), camera);
            w.playSound(dropped.getLocation(), Sound.BLOCK_LADDER_STEP, 1, 1);
            dropped.setItemStack(camera);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlaceButtonOnTheNoteBlock(BlockPlaceEvent event) {
        if (!plg.blockSbuttonPlace) return;
        if ((event.getBlockPlaced().getType() == Material.STONE_BUTTON) &&
                (event.getBlockAgainst().getType() == Material.NOTE_BLOCK)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSetObscuraPrice(SignChangeEvent event) {
        String l = ChatColor.stripColor(event.getLine(1));
        if (l.equalsIgnoreCase("[photo]") ||
                l.equalsIgnoreCase("[head shot]") ||
                l.equalsIgnoreCase("[top half]") ||
                l.equalsIgnoreCase("[full length]")) {
            if (!event.getPlayer().hasPermission("camera-obscura.set-sign")) {
                l = l.replace("[", "{").replace("]", "}");
                event.setLine(1, l);
                return;
            }
            event.setLine(1, ChatColor.RED + l);
            l = ChatColor.stripColor(event.getLine(0));
            if (l.isEmpty()) l = event.getPlayer().getName();
            event.setLine(0, ChatColor.GREEN + l);
            l = ChatColor.stripColor(event.getLine(2));
            if (l.isEmpty() || (!l.matches("[0-9]*"))) l = "free";
            event.setLine(2, ChatColor.GOLD + l);
            l = ChatColor.stripColor(event.getLine(3));
            if (l.isEmpty()) l = "default";
            event.setLine(3, ChatColor.BLUE + l);
        }
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayNoteAtCamera(NotePlayEvent event) {
        if (COCamera.isNoteBlockIsCamera(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBuildCamera(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!p.hasPermission("camera-obscura.tripod-camera.build")) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getType() != Material.NOTE_BLOCK) return;
        if (COCamera.isButtonPlacedOnTheNoteBlock(event.getClickedBlock())) return;
        if (!COCamera.isCameraInHand(p)) return;
        if (event.getClickedBlock().getRelative(BlockFace.DOWN).getType() != Material.FENCE) return;

        Block b = event.getClickedBlock().getRelative(event.getBlockFace());
        byte dirdata = 0;
        if (event.getBlockFace() == BlockFace.SOUTH) dirdata = 3;
        else if (event.getBlockFace() == BlockFace.NORTH) dirdata = 4;
        else if (event.getBlockFace() == BlockFace.WEST) dirdata = 2;
        else if (event.getBlockFace() == BlockFace.EAST) dirdata = 1;

        if (dirdata == 0) return;
        if (u.placeBlock(b, p, Material.STONE_BUTTON, dirdata, false)) return;
        if (!ItemUtil.removeItemInHand(p, plg.camera)) b.setType(Material.AIR);

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onOpenInventory(InventoryOpenEvent event) {
        try {
            COCamera.updateInventoryItems(event.getInventory());
        } catch (Exception e) {
        }
    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerPickupItemEvent(PlayerPickupItemEvent event) {
        COCamera.updateItemName(event.getItem().getItemStack());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        COCamera.updateItemName(event.getPlayer().getInventory().getItem(event.getNewSlot()));
    }


}
