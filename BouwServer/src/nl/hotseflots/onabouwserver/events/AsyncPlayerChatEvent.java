package nl.hotseflots.onabouwserver.events;

import net.md_5.bungee.api.ChatColor;
import nl.hotseflots.onabouwserver.Main;
import nl.hotseflots.onabouwserver.modules.RankDetector;
import nl.hotseflots.onabouwserver.modules.StaffUtils.StaffMode;
import nl.hotseflots.onabouwserver.modules.WelcomeMessage;
import nl.hotseflots.onabouwserver.modules.TwoFactorAuth.AuthenticationDetails;
import nl.hotseflots.onabouwserver.modules.TwoFactorAuth.TOTP;
import nl.hotseflots.onabouwserver.modules.TwoFactorAuth.TwoFA;
import nl.hotseflots.onabouwserver.utils.Messages;
import nl.hotseflots.onabouwserver.utils.Options;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.security.GeneralSecurityException;

public class AsyncPlayerChatEvent implements Listener {

    @EventHandler
    public void onAsyncPlayerChatEvent(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        /*
        Check if the player talked in staffmode
         */
        if (Options.MODULE_STAFFCHAT.getStringValue().equalsIgnoreCase("enabled")) {
            if (event.getMessage().startsWith("!")) {
                if (event.getPlayer().hasPermission("bouwserver.commands.staffmode")) {
                    String message = event.getMessage().replaceFirst("!", "");
                    for (Player staffPlayers : Bukkit.getOnlinePlayers()) {
                        if (staffPlayers.hasPermission("bouwserver.commands.staffmode")) {
                            String format = Messages.STAFF_CHAT_FORMAT.getMessage();
                            format = format.replace("%rank%", RankDetector.getRank(event.getPlayer().getUniqueId()));
                            format = format.replace("%playername%", event.getPlayer().getName());
                            format = format.replace("%message%", message);
                            staffPlayers.sendMessage(format);
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }

        /*
        Check if the TwoFA-module is enabled
         */
        if (Options.MODULE_TWOFA.getStringValue().equalsIgnoreCase("enabled")) {

            /*
            Catch the 2fa code if the player needs to verify
             */
            File userPath = new File(Main.getInstance().getDataFolder() + File.separator + "PlayerData" + File.separator + "TwoFA-Data" + File.separator + event.getPlayer().getUniqueId().toString() + ".yml");
            if (TwoFA.hasTwofactorauth(event.getPlayer().getUniqueId()) || (event.getPlayer().hasPermission("bouwserver.2fa.use") && !userPath.exists())) {
                final AuthenticationDetails authenticationDetails = TwoFA.getAuthenticationDetails(event.getPlayer().getUniqueId());
                event.setCancelled(true);

                new BukkitRunnable() {
                    public void run() {
                        String validCode;

                        try {
                            validCode = TOTP.generateCurrentNumberString(authenticationDetails.getKey());
                        } catch (GeneralSecurityException e) {
                            e.printStackTrace();
                            return;
                        }
                        if (validCode.equals(event.getMessage())) {
                            if (authenticationDetails.isSetup()) {
                                TwoFA.saveAuthenticationDetails(event.getPlayer().getUniqueId(), authenticationDetails);
                            }
                            TwoFA.unloadAuthenticationDetails(event.getPlayer().getUniqueId());
                            event.getPlayer().sendMessage(Messages.MCAUTH_VALID_CODE.getMessage());

                            if (event.getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName().equalsIgnoreCase(Messages.MCAUTH_QRMAP_NAME.getMessage())) {
                                event.getPlayer().getInventory().setItemInMainHand(null);
                            }

                            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), new Runnable() {
                                @Override
                                public void run() {
                                    /*
                                    Send the player the servers WelcomeMessage if the WelcomeMessage Module is enabled
                                    */
                                    if (Options.MODULE_WELCOME_MSG.getStringValue().equalsIgnoreCase("enabled")) {
                                        WelcomeMessage.sendDelayedMOTD(event.getPlayer());
                                    }
                                }
                            }, 20 * 1);
                        } else {
                            authenticationDetails.attempts += 1;
                            event.getPlayer().sendMessage(Messages.MCAUTH_INVALID_CODE.getMessage());
                            if (authenticationDetails.attempts > Options.MAX_TRIES.getIntValue()) {
                                if (!authenticationDetails.isSetup()) {
                                    new BukkitRunnable() {
                                        public void run() {
                                            event.getPlayer().kickPlayer(Messages.MCAUTH_FAIL_MESSAGE.getMessage());
                                        }
                                    }.runTask(Main.getInstance());
                                } else {
                                    TwoFA.unloadAuthenticationDetails(event.getPlayer().getUniqueId());
                                    event.getPlayer().sendMessage(Messages.MCAUTH_SETUP_FAIL.getMessage());
                                }
                            }
                        }
                    }
                }.runTaskAsynchronously(Main.getInstance());
            }
        }
    }
}