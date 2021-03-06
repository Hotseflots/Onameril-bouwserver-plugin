package nl.hotseflots.onabouwserver.events;

import nl.hotseflots.onabouwserver.Main;
import nl.hotseflots.onabouwserver.commands.StaffMode;
import nl.hotseflots.onabouwserver.twofactorauth.AuthenticationDetails;
import nl.hotseflots.onabouwserver.twofactorauth.Options;
import nl.hotseflots.onabouwserver.twofactorauth.TOTP;
import nl.hotseflots.onabouwserver.utils.Messages;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.security.GeneralSecurityException;

public class AsyncPlayerChat implements Listener {

    @EventHandler
    public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        if (Main.plugin.hasTwofactorauth(event.getPlayer().getUniqueId()))
        {
            final AuthenticationDetails authenticationDetails = Main.plugin.getAuthenticationDetails(event.getPlayer().getUniqueId());
            event.setCancelled(true);

            if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.MAP) {
                event.getPlayer().getInventory().setItemInMainHand(null);
                StaffMode.EnterStaffMode(event.getPlayer());
            }
            new BukkitRunnable()
            {
                public void run()
                {
                    String validCode;

                    try
                    {
                        validCode = TOTP.generateCurrentNumberString(authenticationDetails.getKey());
                    }
                    catch (GeneralSecurityException e)
                    {
                        e.printStackTrace(); return;
                    }
                    if (validCode.equals(event.getMessage()))
                    {
                        if (authenticationDetails.isSetup()) {
                            Main.plugin.saveAuthenticationDetails(event.getPlayer().getUniqueId(), authenticationDetails);
                        }
                        Main.plugin.unloadAuthenticationDetails(event.getPlayer().getUniqueId());
                        event.getPlayer().sendMessage(Messages.MCAUTH_VALID_CODE);
                    }
                    else
                    {
                        authenticationDetails.attempts += 1;
                        event.getPlayer().sendMessage(Messages.MCAUTH_INVALID_CODE);
                        if (authenticationDetails.attempts > Options.MAX_TRIES.getIntValue()) {
                            if (!authenticationDetails.isSetup())
                            {
                                new BukkitRunnable()
                                {
                                    public void run()
                                    {
                                        event.getPlayer().kickPlayer(Messages.MCAUTH_FAIL_MESSAGE);
                                    }
                                }.runTask(Main.plugin);
                            }
                            else
                            {
                                Main.plugin.unloadAuthenticationDetails(event.getPlayer().getUniqueId());
                                event.getPlayer().sendMessage(Messages.MCAUTH_SETUP_FAIL);
                            }
                        }
                    }
                }
            }.runTaskAsynchronously(Main.plugin);
        }
    }
}
