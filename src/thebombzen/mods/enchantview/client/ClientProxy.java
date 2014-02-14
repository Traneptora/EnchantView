package thebombzen.mods.enchantview.client;

import thebombzen.mods.enchantview.CommonProxy;
import thebombzen.mods.enchantview.ConfigOption;
import thebombzen.mods.enchantview.EnchantView;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;

public class ClientProxy extends CommonProxy {
	@Override
	public boolean canPlayerUseCommand(EntityPlayerMP player) {
		String ownerName = Minecraft.getMinecraft().thePlayer
				.getCommandSenderName();
		if (ownerName.equals(player.getCommandSenderName())) {
			return true;
		} else if (EnchantView.instance.getConfiguration().getPropertyBoolean(ConfigOption.ALLOW_ON_LAN)) {
			return true;
		} else {
			return false;
		}
	}
}
