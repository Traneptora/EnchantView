package thebombzen.mods.enchantview;

import thebombzen.mods.thebombzenapi.ThebombzenAPIConfiguration;

public class Configuration extends ThebombzenAPIConfiguration<ConfigOption> {
	public Configuration(EnchantView baseMod) {
		super(baseMod, ConfigOption.class);
	}
}
