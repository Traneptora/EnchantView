package thebombzen.mods.enchantview.bukkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.server.v1_6_R3.ContainerEnchantTable;
import net.minecraft.server.v1_6_R3.EnchantmentInstance;
import net.minecraft.server.v1_6_R3.EnchantmentManager;
import net.minecraft.server.v1_6_R3.EntityPlayer;
import net.minecraft.server.v1_6_R3.Item;
import net.minecraft.server.v1_6_R3.ItemStack;
import net.minecraft.server.v1_6_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_6_R3.NBTTagCompound;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class EnchantView extends JavaPlugin implements PluginMessageListener {
	
	public static final int STAGE_REQUEST = 0;
	public static final int STAGE_SEND = 1;
	public static final int STAGE_ACCEPT = 2;
	
	public static Random random = new Random();
	
	public boolean enabled = false;
	
	public Map<String, ItemStack[]> newItemStacksMap = new HashMap<String, ItemStack[]>();
	public Map<String, Map<Enchantment, Integer>> enchMaps = new HashMap<String, Map<Enchantment, Integer>>();
	public boolean checkingItems = false;
	
	@Override
    public void onEnable(){
		
		enabled = true;
		
		newItemStacksMap.clear();
		enchMaps.clear();
		
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "EnchantView");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "EnchantView", this);
        
    }
 
    @Override
    public void onDisable() {
    	enabled = false;
    	newItemStacksMap.clear();
		enchMaps.clear();
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    	if(cmd.getName().equalsIgnoreCase("doesenchantviewexist")){
        	if (!enabled){
        		sender.sendMessage("EnchantView is disabled.");
        	} else {
        		sender.sendMessage("Yes, EnchantView exists.");
        	}
    		return true;
    	}
    	return false; 
    }
    
    public EntityPlayer getEntityPlayerForPlayer(Player player){
    	return ((CraftPlayer)player).getHandle();
    }

	@Override
	public void onPluginMessageReceived(String channel, Player bukkitPlayer, byte[] data) {
		if (!enabled){
			return;
		}
		if (channel.equals("EnchantView")){
			NBTTagCompound compoundIn = NBTCompressedStreamTools.a(data);
			int stage = compoundIn.getInt("stage");
			EntityPlayer player = getEntityPlayerForPlayer(bukkitPlayer);
			if (player == null){
				return;
			}
			if (!(player.activeContainer instanceof ContainerEnchantTable)){
				return;
			}

			ContainerEnchantTable container = (ContainerEnchantTable)player.activeContainer;
			if (stage == STAGE_REQUEST){
				ItemStack[] newItemStacks = new ItemStack[3];
				for (int i = 0; i < 3; i++){
	    			newItemStacks[i] = this.generateEnchantedItemStack(container, player, i);
	    		}
				newItemStacksMap.put(player.getName(), newItemStacks);
				NBTTagCompound compoundOut = new NBTTagCompound();
				compoundOut.setInt("stage", STAGE_SEND);
				for (int i = 0; i < 3; i++){
	    			NBTTagCompound stackTag = new NBTTagCompound();
	    			newItemStacks[i].save(stackTag);
	    			compoundOut.setCompound("stack"+i, stackTag);
	    		}
				byte[] toSendData = NBTCompressedStreamTools.a(compoundOut);
				bukkitPlayer.sendPluginMessage(this, "EnchantView", toSendData);
			} else {
				int slot = compoundIn.getInt("slot");
	    		enchantItem(container, player, bukkitPlayer, slot);
	    		newItemStacksMap.remove(player.getName());
			}
		}
	}
	
	private ItemStack generateEnchantedItemStack(
			ContainerEnchantTable container, EntityPlayer player, int slot) {
		ItemStack newItemStack = ItemStack.b(container.getSlot(0).getItem());
		if (container.costs[slot] > 0
				&& newItemStack != null
				&& (player.expLevel >= container.costs[slot] || player
						.getBukkitEntity().getGameMode()
						.equals(GameMode.CREATIVE))) {
			@SuppressWarnings("unchecked")
			List<EnchantmentInstance> enchList = EnchantmentManager.b(random,
					newItemStack, container.costs[slot]);
			if (enchList != null) {
				Map<Enchantment, Integer> enchMap = new HashMap<Enchantment, Integer>();
				enchMaps.put(player.getBukkitEntity().getDisplayName(), enchMap);
				boolean isBook = newItemStack.id == Item.BOOK.id;
				if (isBook) {
					newItemStack.id = Item.ENCHANTED_BOOK.id;
				}
				int enchToPick = isBook ? random.nextInt(enchList.size()) : -1;
				for (int i = 0; i < enchList.size(); ++i) {
					EnchantmentInstance enchData = enchList.get(i);
					if (!isBook || i == enchToPick) {
						if (isBook) {
							Item.ENCHANTED_BOOK.a(newItemStack, enchData);
							enchMap.put(Enchantment.getByName(enchData.enchantment.a()), enchData.level);
						} else {
							newItemStack.addEnchantment(enchData.enchantment,
									enchData.level);
							enchMap.put(Enchantment.getByName(enchData.enchantment.a()), enchData.level);
						}
					}
				}
			}
		}
		return newItemStack;
	}
	
	private void enchantItem(ContainerEnchantTable container, EntityPlayer player, Player bukkitPlayer, int slot){
		org.bukkit.block.Block table = player.getBukkitEntity().getTargetBlock(null, 7);
		if (!table.getType().equals(Material.ENCHANTMENT_TABLE)){
			table = null;
		}
		
		EnchantItemEvent enchantItemEvent = new EnchantItemEvent(bukkitPlayer, container.getBukkitView(), table, container.getBukkitView().getItem(0), container.costs[slot], enchMaps.get(bukkitPlayer.getDisplayName()), slot);
		Bukkit.getServer().getPluginManager().callEvent(enchantItemEvent);
		if (!enchantItemEvent.isCancelled()){
			ItemStack stack = newItemStacksMap.get(player.getName())[slot];
			player.levelDown(-container.costs[slot]);
			container.enchantSlots.setItem(0, stack);
			container.a(container.enchantSlots);
		}
		enchMaps.remove(bukkitPlayer.getDisplayName());
	}
	
}
