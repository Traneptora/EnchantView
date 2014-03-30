package thebombzen.mods.enchantview;

import thebombzen.mods.thebombzenapi.configuration.ConfigOption;
import thebombzen.mods.thebombzenapi.configuration.SingleMultiBoolean;
import thebombzen.mods.thebombzenapi.configuration.ThebombzenAPIConfiguration;

public class Configuration extends ThebombzenAPIConfiguration {
	
	
	public static ConfigOption ENABLE = new ConfigOption(SingleMultiBoolean.ALWAYS, "ENABLE", "Enable EnchantView",
		"Enable EnchantView.",
		"Keep in mind that it only works",
		"in multiplayer if the",
		"server has the mod.");
	public static ConfigOption ALLOW_ON_LAN = new ConfigOption(true, "ALLOW_ON_LAN", "Enable for LAN players",
		"This option determines whether",
		"to allow players on your LAN",
		"world to use EnchantView.");
	
	public Configuration(EnchantView baseMod) {
		super(baseMod);
	}

	@Override
	public ConfigOption[] getAllOptions() {
		return new ConfigOption[]{ENABLE, ALLOW_ON_LAN};
	}

}
