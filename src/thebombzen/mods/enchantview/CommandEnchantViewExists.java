package thebombzen.mods.enchantview;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

public class CommandEnchantViewExists extends CommandBase {

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender par1ICommandSender) {
		if (par1ICommandSender instanceof EntityPlayerMP) {
			return EnchantView.sideSpecificUtilities.canPlayerUseCommand((EntityPlayerMP) par1ICommandSender);
		} else {
			return true;
		}
	}

	@Override
	public String getCommandName() {
		return "doesenchantviewexist";
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
	public void processCommand(ICommandSender sender, String[] strings) {
		sender.addChatMessage(IChatComponent.Serializer.func_150699_a("{\"text\":\"Yes, EnchantView exists.\"}"));
	}

}
