package nl.hotseflots.onabouwserver.events;

import nl.hotseflots.onabouwserver.Main;
import nl.hotseflots.onabouwserver.commands.StaffMode;
import nl.hotseflots.onabouwserver.modules.TwoFactorAuth.TwoFA;
import nl.hotseflots.onabouwserver.utils.Options;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.io.File;
import java.util.List;

public class PlayerDropEvent implements Listener {

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (Main.getInstance().getConfig().getString("Modules.TwoFA.Module").equalsIgnoreCase("enabled")) {
            File userPath = new File(Main.getInstance().getDataFolder() + File.separator + "PlayerData" + File.separator + "TwoFA-Data" + File.separator + event.getPlayer().getUniqueId().toString() + ".yml");
            if (TwoFA.hasTwofactorauth(event.getPlayer().getUniqueId()) || (!userPath.exists() && event.getPlayer().hasPermission("bouwserver.2fa.setup"))) {

                if (!Options.DENY_DROPPING.getBooleanValue()) {
                    return;
                }

                event.setCancelled(true);
            }
        }

        if (StaffMode.staffModeList.contains(event.getPlayer().getUniqueId().toString())) {
            event.setCancelled(true);
        }

        /*
        Player will not be able to drop staffitems
         */
        if (StaffMode.isStaffItem(event.getItemDrop().getItemStack())) {
            if (!StaffMode.staffModeList.contains(event.getPlayer().getUniqueId().toString())) {
                event.setCancelled(true);
                event.getItemDrop().remove();
                return;
            }
        }


        if (event.getItemDrop().getItemStack().isSimilar(StaffMode.vanishOnItem ) ) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        } else if (event.getItemDrop().getItemStack().isSimilar(StaffMode.vanishOffItem)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        } else if (event.getItemDrop().getItemStack().isSimilar(StaffMode.randomTPItem)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        } else if (event.getItemDrop().getItemStack().isSimilar(StaffMode.lookupItem)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        } else if (event.getItemDrop().getItemStack().isSimilar(StaffMode.freezeItem)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }
}