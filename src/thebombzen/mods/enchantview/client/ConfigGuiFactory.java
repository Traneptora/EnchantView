package thebombzen.mods.enchantview.client;

import thebombzen.mods.thebombzenapi.client.ThebombzenAPIConfigGuiFactory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ConfigGuiFactory extends ThebombzenAPIConfigGuiFactory {
	public ConfigGuiFactory(){
		super(ConfigScreen.class);
	}
}
