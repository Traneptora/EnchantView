package thebombzen.mods.enchantview.server;

import net.minecraft.entity.player.EntityPlayerMP;
import thebombzen.mods.enchantview.SideSpecificUtilities;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.SERVER)
public class ServerSideSpecificUtilities implements SideSpecificUtilities {
	@Override
	public boolean canPlayerUseCommand(EntityPlayerMP player) {
		return true;
	}
}
