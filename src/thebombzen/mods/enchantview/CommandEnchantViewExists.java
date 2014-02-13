package thebombzen.mods.enchantview;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatMessageComponent;

public class CommandEnchantViewExists extends CommandBase {

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender par1ICommandSender) {
		if (par1ICommandSender instanceof EntityPlayerMP) {
			return EnchantView.proxy.canPlayerUseCommand((EntityPlayerMP) par1ICommandSender);
		} else {
			return true;
		}
	}

	@Override
	public String getCommandName() {
		return "doesenchantviewexist";
	}

	@Override
	public String getCommandUsage(ICommandSender var1) {
		return "Asks whether enchantview exists.";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

	@Override
	public void processCommand(ICommandSender var1, String[] var2) {
		var1.sendChatToPlayer(ChatMessageComponent
				.createFromText("Yes, EnchantView exists."));
	}

}
