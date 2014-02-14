package thebombzen.mods.enchantview.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import thebombzen.mods.enchantview.ConfigOption;
import thebombzen.mods.enchantview.EnchantView;
import thebombzen.mods.enchantview.SideSpecificUtilities;

@SideOnly(Side.CLIENT)
public class ClientSideSpecificUtilities implements SideSpecificUtilities {
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
