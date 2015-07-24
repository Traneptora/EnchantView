package thebombzen.mods.enchantview.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class EnchantView extends JavaPlugin {
	//private boolean enabled = false;
	@Override
	public void onEnable(){
		//enabled = true;
	}
	@Override
	public void onDisable(){
		//enabled = false;
	}
	
	@EventHandler
	public void prepareItemEnchant(PrepareItemEnchantEvent event){
		
	}
	
}