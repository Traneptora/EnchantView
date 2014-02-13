package thebombzen.mods.enchantview;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelBook;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.util.EnchantmentNameParts;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.util.glu.Project;

import thebombzen.mods.thebombzenapi.ThebombzenAPI;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EVGuiEnchantment extends GuiEnchantment {

	public static EVGuiEnchantment instance;
	public static int prevLevelsHashCode = 0;
	private int drawMe = -1;
	public static final Minecraft mc = Minecraft.getMinecraft();

	public ContainerEnchantment containerEnchantment;

	public EVGuiEnchantment(GuiEnchantment parent) {
		super(null, null, 0, 0, 0, (String) ThebombzenAPI.getPrivateField(
				parent, GuiEnchantment.class, new String[] { "field_94079_C",
						"C" }));
		this.containerEnchantment = (ContainerEnchantment) ThebombzenAPI
				.getPrivateField(parent, GuiEnchantment.class, new String[] {
						"containerEnchantment", "field_74215_y", "y" });
		ThebombzenAPI.setPrivateField(this, GuiEnchantment.class, new String[] {
				"containerEnchantment", "field_74215_y", "y" },
				this.containerEnchantment);
		this.inventorySlots = this.containerEnchantment;
		instance = this;

		if (System.identityHashCode(mc.theWorld) != EnchantView.instance.currentWorldHashCode) {

			boolean shouldAsk = true;
			if (!EnchantView.instance.getConfiguration().getPropertyBoolean(
					ConfigOption.ENABLE_SP)
					&& mc.isSingleplayer()) {
				shouldAsk = false;
			} else if (!EnchantView.instance.getConfiguration()
					.getPropertyBoolean(ConfigOption.ENABLE_MP)
					&& !mc.isSingleplayer()) {
				shouldAsk = false;
			}

			if (shouldAsk) {
				EnchantView.instance.askingIfEnchantViewExists = true;
				mc.getNetHandler().addToSendQueue(
						new Packet3Chat("/doesenchantviewexist", false));
				EnchantView.instance.currentWorldHashCode = System
						.identityHashCode(mc.theWorld);
			} else {
				EnchantView.instance.enchantViewExists = false;
			}
		}
	}

	@Override
	/**
	 * Draw the background layer for the GuiContainer (everything behind the items)
	 */
	public void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		EVGuiEnchantment.mc.getTextureManager().bindTexture(
				(ResourceLocation) ThebombzenAPI.getPrivateField(this,
						GuiEnchantment.class, new String[] {
								"enchantingTableGuiTextures", "field_110425_B",
								"B" }));
		int k = (this.width - this.xSize) / 2;
		int l = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
		GL11.glPushMatrix();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPushMatrix();
		GL11.glLoadIdentity();
		ScaledResolution scaledresolution = new ScaledResolution(
				EVGuiEnchantment.mc.gameSettings,
				EVGuiEnchantment.mc.displayWidth,
				EVGuiEnchantment.mc.displayHeight);
		GL11.glViewport((scaledresolution.getScaledWidth() - 320) / 2
				* scaledresolution.getScaleFactor(),
				(scaledresolution.getScaledHeight() - 240) / 2
						* scaledresolution.getScaleFactor(),
				320 * scaledresolution.getScaleFactor(),
				240 * scaledresolution.getScaleFactor());
		GL11.glTranslatef(-0.34F, 0.23F, 0.0F);
		Project.gluPerspective(90.0F, 1.3333334F, 9.0F, 80.0F);
		float f1 = 1.0F;
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		RenderHelper.enableStandardItemLighting();
		GL11.glTranslatef(0.0F, 3.3F, -16.0F);
		GL11.glScalef(f1, f1, f1);
		float f2 = 5.0F;
		GL11.glScalef(f2, f2, f2);
		GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
		EVGuiEnchantment.mc.getTextureManager().bindTexture(
				(ResourceLocation) ThebombzenAPI.getPrivateField(this,
						GuiEnchantment.class, new String[] {
								"enchantingTableBookTextures",
								"field_110426_C", "C" }));
		GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
		float f3 = this.field_74208_u
				+ (this.field_74209_t - this.field_74208_u) * par1;
		GL11.glTranslatef((1.0F - f3) * 0.2F, (1.0F - f3) * 0.1F,
				(1.0F - f3) * 0.25F);
		GL11.glRotatef(-(1.0F - f3) * 90.0F - 90.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
		float f4 = this.field_74212_q
				+ (this.field_74213_p - this.field_74212_q) * par1 + 0.25F;
		float f5 = this.field_74212_q
				+ (this.field_74213_p - this.field_74212_q) * par1 + 0.75F;
		f4 = (f4 - MathHelper.truncateDoubleToInt(f4)) * 1.6F - 0.3F;
		f5 = (f5 - MathHelper.truncateDoubleToInt(f5)) * 1.6F - 0.3F;

		if (f4 < 0.0F) {
			f4 = 0.0F;
		}

		if (f5 < 0.0F) {
			f5 = 0.0F;
		}

		if (f4 > 1.0F) {
			f4 = 1.0F;
		}

		if (f5 > 1.0F) {
			f5 = 1.0F;
		}

		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		((ModelBook) ThebombzenAPI.getPrivateField(this, GuiEnchantment.class,
				new String[] { "bookModel", "field_74206_w", "w" })).render(
				(Entity) null, 0.0F, f4, f5, f3, 0.0F, 0.0625F);
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		RenderHelper.disableStandardItemLighting();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glViewport(0, 0, EVGuiEnchantment.mc.displayWidth,
				EVGuiEnchantment.mc.displayHeight);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();
		RenderHelper.disableStandardItemLighting();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		EnchantmentNameParts.instance
				.setRandSeed(this.containerEnchantment.nameSeed);

		if (EnchantView.instance.enchantViewExists
				&& containerEnchantment.enchantLevels[0] != 0
				&& containerEnchantment.tableInventory.getStackInSlot(0) != null) {
			if (!EnchantView.instance.askingForEnchantments
					&& (EnchantView.instance.newItemStacks[0] == null || prevLevelsHashCode != Arrays
							.hashCode(containerEnchantment.enchantLevels))) {
				EnchantView.instance.requestEnchantmentListFromServer(this.containerEnchantment.windowId);
			}
		} else {
			Arrays.fill(EnchantView.instance.newItemStacks, null);
		}
		prevLevelsHashCode = Arrays
				.hashCode(containerEnchantment.enchantLevels);
		drawMe = -1;

		for (int i1 = 0; i1 < 3; ++i1) {
			String s = EnchantmentNameParts.instance
					.generateRandomEnchantName();
			this.zLevel = 0.0F;
			EVGuiEnchantment.mc.getTextureManager().bindTexture(
					(ResourceLocation) ThebombzenAPI.getPrivateField(this,
							GuiEnchantment.class, new String[] {
									"enchantingTableGuiTextures",
									"field_110425_B", "B" }));
			int j1 = this.containerEnchantment.enchantLevels[i1];
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

			if (j1 == 0) {
				this.drawTexturedModalRect(k + 60, l + 14 + 19 * i1, 0, 185,
						108, 19);
			} else {
				String s1 = "" + j1;
				FontRenderer fontrenderer = EVGuiEnchantment.mc.standardGalacticFontRenderer;
				int k1 = 6839882;

				if (EVGuiEnchantment.mc.thePlayer.experienceLevel < j1
						&& !EVGuiEnchantment.mc.thePlayer.capabilities.isCreativeMode) {
					this.drawTexturedModalRect(k + 60, l + 14 + 19 * i1, 0,
							185, 108, 19);
					fontrenderer.drawSplitString(s, k + 62, l + 16 + 19 * i1,
							104, (k1 & 16711422) >> 1);
					fontrenderer = EVGuiEnchantment.mc.fontRenderer;
					k1 = 4226832;
					fontrenderer.drawStringWithShadow(s1, k + 62 + 104
							- fontrenderer.getStringWidth(s1), l + 16 + 19 * i1
							+ 7, k1);
				} else {
					int l1 = par2 - (k + 60);
					int i2 = par3 - (l + 14 + 19 * i1);

					if (l1 >= 0 && i2 >= 0 && l1 < 108 && i2 < 19) {
						this.drawTexturedModalRect(k + 60, l + 14 + 19 * i1, 0,
								204, 108, 19);
						k1 = 16777088;
						drawMe = i1;
					} else {
						this.drawTexturedModalRect(k + 60, l + 14 + 19 * i1, 0,
								166, 108, 19);
					}

					fontrenderer.drawSplitString(s, k + 62, l + 16 + 19 * i1,
							104, k1);
					fontrenderer = EVGuiEnchantment.mc.fontRenderer;
					k1 = 8453920;
					fontrenderer.drawStringWithShadow(s1, k + 62 + 104
							- fontrenderer.getStringWidth(s1), l + 16 + 19 * i1
							+ 7, k1);
				}
			}
		}
	}

	@Override
	public void drawScreen(int par1, int par2, float par3) {
		super.drawScreen(par1, par2, par3);
		if (drawMe != -1
				&& EnchantView.instance.enchantViewExists
				&& containerEnchantment.tableInventory.getStackInSlot(0) != null
				&& EnchantView.instance.newItemStacks[drawMe] != null) {
			this.drawItemStackTooltip(EnchantView.instance.newItemStacks[drawMe],
					par1 + 8, par2 + 8);
		}
	}

	@Override
	protected void mouseClicked(int par1, int par2, int par3) {
		super.mouseClicked(par1, par2, par3);
		int l = (this.width - this.xSize) / 2;
		int i1 = (this.height - this.ySize) / 2;

		for (int j1 = 0; j1 < 3; ++j1) {
			int k1 = par1 - (l + 60);
			int l1 = par2 - (i1 + 14 + 19 * j1);

			if (k1 >= 0
					&& l1 >= 0
					&& k1 < 108
					&& l1 < 19
					&& this.containerEnchantment.enchantItem(
							EVGuiEnchantment.mc.thePlayer, j1)) {
				if (EnchantView.instance.enchantViewExists) {
					EnchantView.instance.sendAcceptEnchantment(
							this.containerEnchantment.windowId, j1);
				} else {
					mc.playerController.sendEnchantPacket(
							this.containerEnchantment.windowId, j1);
				}
			}
		}
	}

}
