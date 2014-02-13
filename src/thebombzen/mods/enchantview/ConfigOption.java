package thebombzen.mods.enchantview;

import thebombzen.mods.thebombzenapi.ThebombzenAPIConfigOption;

public enum ConfigOption implements ThebombzenAPIConfigOption {

	ENABLE_SP("true", "Use in singleplayer", "This option determines whether",
			"to enable EnchantView in singleplayer."), ENABLE_MP("true",
			"Use in multiplayer", "This option determines whether",
			"to enable EnchantView in multiplayer.",
			"Keep in mind that it only works", "if the server has the mod."), ALLOW_ON_LAN(
			"true", "Enable for LAN players", "This option determines whether",
			"to allow players on your LAN", "world to use EnchantView.");

	private String defaultValue;

	private String[] info;
	private String shortInfo;

	private ConfigOption(String defaultValue, String shortInfo, String... info) {
		this.defaultValue = defaultValue;
		this.info = info;
		this.shortInfo = shortInfo;
	}

	@Override
	public int getDefaultToggleIndex() {
		return -1;
	}

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String[] getFiniteStringOptions() {
		throw new UnsupportedOperationException(
				"Only supported for finite strings!");
	}

	@Override
	public String[] getInfo() {
		return info;
	}

	@Override
	public int getOptionType() {
		return BOOLEAN;
	}

	@Override
	public String getShortInfo() {
		return shortInfo;
	}

}
