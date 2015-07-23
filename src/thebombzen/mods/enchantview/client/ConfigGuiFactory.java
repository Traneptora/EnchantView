package thebombzen.mods.enchantview.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thebombzen.mods.thebombzenapi.client.ThebombzenAPIConfigGuiFactory;

@SideOnly(Side.CLIENT)
public class ConfigGuiFactory extends ThebombzenAPIConfigGuiFactory {
	public ConfigGuiFactory(){
		super(ConfigScreen.class);
	}
}
