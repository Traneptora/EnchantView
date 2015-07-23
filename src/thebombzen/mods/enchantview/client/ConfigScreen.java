package thebombzen.mods.enchantview.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thebombzen.mods.enchantview.EnchantView;
import thebombzen.mods.thebombzenapi.client.ThebombzenAPIConfigScreen;

@SideOnly(Side.CLIENT)
public class ConfigScreen extends ThebombzenAPIConfigScreen {
	public ConfigScreen(GuiScreen parentScreen) {
		super(EnchantView.instance, parentScreen, EnchantView.instance.getConfiguration());
	}
}
