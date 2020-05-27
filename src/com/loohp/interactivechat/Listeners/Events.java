package com.loohp.interactivechat.Listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.loohp.interactivechat.InteractiveChat;
import com.loohp.interactivechat.ObjectHolders.CommandPlaceholderInfo;
import com.loohp.interactivechat.ObjectHolders.MentionPair;
import com.loohp.interactivechat.Utils.CustomStringUtils;
import com.loohp.interactivechat.Utils.MessageUtils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

public class Events implements Listener {
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		InteractiveChat.mentionCooldown.put(event.getPlayer(), (System.currentTimeMillis() - 3000));
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		InteractiveChat.mentionCooldown.remove(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		String command = event.getMessage();
		for (String parsecommand : InteractiveChat.commandList) {
			if (command.matches(parsecommand)) {
				command = MessageUtils.preprocessMessage(command);
				for (String placeholder : InteractiveChat.placeholderList) {
					if (command.contains(placeholder)) {
						String regexPlaceholder = CustomStringUtils.escapeMetaCharacters(placeholder);
						String uuidmatch = "<" + UUID.randomUUID().toString() + ">";
						command = command.replaceFirst(regexPlaceholder,  uuidmatch);
						InteractiveChat.commandPlaceholderMatch.put(uuidmatch, new CommandPlaceholderInfo(event.getPlayer(), placeholder, uuidmatch, InteractiveChat.commandPlaceholderMatch));
						event.setMessage(command);
						break;
					}
				}
				break;
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGH)
	public void checkChat(AsyncPlayerChatEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (InteractiveChat.ChatManagerHook) {
			return;
		}

		String message = event.getMessage();
		if (InteractiveChat.maxPlacholders >= 0) {
			int count = 0;
			for (String findStr : InteractiveChat.placeholderList) {
				int lastIndex = 0;	
				while(lastIndex != -1) {	
				    lastIndex = message.indexOf(findStr,lastIndex);	
				    if(lastIndex != -1) {
				        count ++;
				        lastIndex += findStr.length();
				    }
				}
			}
			if (count > InteractiveChat.maxPlacholders) {
				event.setCancelled(true);
				String cancelmessage = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(event.getPlayer(), InteractiveChat.limitReachMessage));
				event.getPlayer().sendMessage(cancelmessage);
				return;
			}
		}
		
		event.setMessage(MessageUtils.preprocessMessage(message));
		
		InteractiveChat.messages.put(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', event.getMessage())), event.getPlayer().getUniqueId());
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void checkChatforChatManager(AsyncPlayerChatEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		checkMention(event);
		
		
		if (!InteractiveChat.ChatManagerHook) {
			return;
		}
		
		String message = event.getMessage();
		if (InteractiveChat.maxPlacholders >= 0) {
			int count = 0;
			for (String findStr : InteractiveChat.placeholderList) {
				int lastIndex = 0;	
				while(lastIndex != -1) {	
				    lastIndex = message.indexOf(findStr,lastIndex);	
				    if(lastIndex != -1) {
				        count ++;
				        lastIndex += findStr.length();
				    }
				}
			}
			if (count > InteractiveChat.maxPlacholders) {
				event.setCancelled(true);
				String cancelmessage = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(event.getPlayer(), InteractiveChat.limitReachMessage));
				event.getPlayer().sendMessage(cancelmessage);
				return;
			}
		}
		
		event.setMessage(MessageUtils.preprocessMessage(message));
		
		InteractiveChat.messages.put(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', event.getMessage())), event.getPlayer().getUniqueId());
	}
	
    private void checkMention(AsyncPlayerChatEvent event) {
		String message = event.getMessage();		
		Player sender = event.getPlayer();
		if (InteractiveChat.AllowMention == true && sender.hasPermission("interactivechat.mention.player")) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				List<String> playernames = new ArrayList<String>();
    			playernames.add(ChatColor.stripColor(player.getName()));
    			if (!player.getName().equals(player.getDisplayName())) {
    				playernames.add(ChatColor.stripColor(player.getDisplayName()));
    			}
    			if (InteractiveChat.EssentialsHook) {
    				if (InteractiveChat.essenNick.containsKey(player)) {
    					playernames.add(ChatColor.stripColor(InteractiveChat.essenNick.get(player)));
    				}
    			}
       			for (String name : playernames) {
       				int index = message.toLowerCase().indexOf(name.toLowerCase());
       				if (index >= 0) {
       					char before = (index - 1) < 0 ? ' ' : message.charAt(index - 1);
       					char after = (index + name.length()) >= message.length() ? ' ' : message.charAt(index + name.length());
       					if (String.valueOf(before).matches("[^a-zA-Z0-9]") && String.valueOf(after).matches("[^a-zA-Z0-9]")) {
       						if (!player.equals(sender)) {
       							InteractiveChat.mentionPair.put(player.getUniqueId(), new MentionPair(sender.getUniqueId(), player.getUniqueId(), InteractiveChat.mentionPair));
       						}
       						break;
       					}
       				}
       			}
			}
		}
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getClickedInventory() == null) {
			return;
		}
		if (event.getClickedInventory().getType().equals(InventoryType.CREATIVE)) {
			return;
		}
		if (event.getView().getTopInventory() == null) {
			return;
		}
		if (InteractiveChat.inventoryDisplay.containsValue(event.getView().getTopInventory())) {
			event.setCancelled(true);
			return;
		}
		if (InteractiveChat.enderDisplay.containsValue(event.getView().getTopInventory())) {
			event.setCancelled(true);
			return;
		}
		if (InteractiveChat.itemDisplay.containsValue(event.getView().getTopInventory())) {
			event.setCancelled(true);
			return;
		}
 	}
		
}