package thebombzen.mods.enchantview;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

public class CommandEnchantViewExists extends CommandBase {

	@Override
	public boolean canCommandSenderUse(ICommandSender par1ICommandSender) {
		if (par1ICommandSender instanceof EntityPlayerMP) {
			return EnchantView.sideSpecificUtilities.canPlayerUseCommand((EntityPlayerMP) par1ICommandSender);
		} else {
			return true;
		}
	}

	@Override
	public List<String> getAliases() {
		return Collections.singletonList("doesenchantviewexist");
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "Asks whether enchantview exists.";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

	@Override
	public void execute(ICommandSender sender, String[] strings) {
		sender.addChatMessage(IChatComponent.Serializer.jsonToComponent("{\"text\":\"Yes, EnchantView exists.\"}"));
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
