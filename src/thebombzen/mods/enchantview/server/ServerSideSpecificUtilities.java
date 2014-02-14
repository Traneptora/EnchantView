package thebombzen.mods.enchantview.server;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thebombzen.mods.enchantview.SideSpecificUtilities;
import net.minecraft.entity.player.EntityPlayerMP;

@SideOnly(Side.SERVER)
public class ServerSideSpecificUtilities implements SideSpecificUtilities {
	@Override
	public boolean canPlayerUseCommand(EntityPlayerMP player) {
		return true;
	}
}
