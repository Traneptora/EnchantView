package thebombzen.mods.enchantview;

import io.netty.buffer.Unpooled;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent;
import thebombzen.mods.thebombzenapi.ThebombzenAPI;
import thebombzen.mods.thebombzenapi.ThebombzenAPIBaseMod;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = "enchantview", name = "EnchantView", version = Constants.VERSION, dependencies = "required-after:thebombzenapi", guiFactory = "thebombzen.mods.enchantview.client.ConfigGuiFactory")
public class EnchantView extends ThebombzenAPIBaseMod {

	public static final int STAGE_REQUEST = 0;
	public static final int STAGE_SEND = 1;
	public static final int STAGE_ACCEPT = 2;
	public static final Random random = new Random();
	public static final Minecraft mc = Minecraft.getMinecraft();
	
	
	public FMLEventChannel channel;

	@Instance(value = "enchantview")
	public static EnchantView instance;

	@SidedProxy(clientSide = "thebombzen.mods.enchantview.client.ClientSideSpecificUtilities", serverSide = "thebombzen.mods.enchantview.server.ServerSideSpecificUtilities")
	public static SideSpecificUtilities sideSpecificUtilities;

	private Configuration configuration;

	@SideOnly(Side.CLIENT)
	public ItemStack[] newItemStacks;

	@SideOnly(Side.CLIENT)
	public volatile boolean enchantViewExists;

	@SideOnly(Side.CLIENT)
	public volatile boolean askingIfEnchantViewExists;

	@SideOnly(Side.CLIENT)
	public volatile boolean askingForEnchantments;
	@SideOnly(Side.CLIENT)
	public volatile int prevLevelsHashCode = 0;
	@SideOnly(Side.CLIENT)
	public volatile int drawMe = -1;
	@SideOnly(Side.CLIENT)
	public ContainerEnchantment clientContainerEnchantment;
	
	public int[] prevServerEnchantmentLevels = new int[3];

	private Map<UUID, ItemStack[]> newItemStacksMap = new HashMap<UUID, ItemStack[]>();

	public boolean canPlayerUseCommand(EntityPlayerMP player) {
		return sideSpecificUtilities.canPlayerUseCommand(player);
	}

	public void enchantItem(ContainerEnchantment container,
			EntityPlayerMP player, int slot) {
		ItemStack stack = newItemStacksMap.get(player.getUniqueID())[slot];
		player.addExperienceLevel(-container.enchantLevels[slot]);
		container.tableInventory.setInventorySlotContents(0, stack);
		container.onCraftMatrixChanged(container.tableInventory);
	}

	public ItemStack generateEnchantedItemStack(
			ContainerEnchantment container, EntityPlayerMP player, int slot) {
		ItemStack newItemStack = ItemStack
				.copyItemStack(container.tableInventory.getStackInSlot(0));
		if (container.enchantLevels[slot] > 0
				&& newItemStack != null
				&& (player.experienceLevel >= container.enchantLevels[slot] || player.capabilities.isCreativeMode)) {
			@SuppressWarnings("unchecked")
			List<EnchantmentData> enchList = EnchantmentHelper.buildEnchantmentList(random, newItemStack,
							container.enchantLevels[slot]);
			
			boolean isBook = newItemStack.getItem() == Items.book;

			if (enchList != null) {
				// handler.playerEntity.addExperienceLevel(-container.enchantLevels[slot]);
				if (isBook) {
					// func_150996_a == setItem
					newItemStack.func_150996_a(Items.enchanted_book);
				}
				int enchToPick = random.nextInt(enchList.size());
				for (int i = 0; i < enchList.size(); i++) {
					EnchantmentData enchData = enchList.get(i);
					if (!isBook || i == enchToPick) {
						if (isBook) {
							Items.enchanted_book.addEnchantment(newItemStack, enchData);
						} else {
							newItemStack.addEnchantment(
									enchData.enchantmentobj,
									enchData.enchantmentLevel);
						}
					}
				}
				// this.onCraftMatrixChanged(this.tableInventory);
			}
		}
		return newItemStack;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Configuration getConfiguration() {
		return configuration;
	}
	
	@Override
	public String getDownloadLocationURLString() {
		return "http://is.gd/ThebombzensMods#EnchantView";
	}

	@Override
	public String getLongName() {
		return "EnchantView";
	}

	@Override
	public String getLongVersionString() {
		return "EnchantView, version " + Constants.VERSION + ", Minecraft " + Constants.MC_VERSION;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getNumToggleKeys() {
		return 0;
	}

	@Override
	public String getShortName() {
		return "EV";
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected String getToggleMessageString(int index, boolean enabled) {
		return null;
	}

	@Override
	public String getVersionFileURLString() {
		return "https://dl.dropboxusercontent.com/u/51080973/Mods/EnchantView/EVVersion.txt";
	}

	@Override
	public void init1(FMLPreInitializationEvent event){
		FMLCommonHandler.instance().bus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
		channel = NetworkRegistry.INSTANCE.newEventDrivenChannel("EnchantView");
		channel.register(this);
		configuration = new Configuration(this);
		if (event.getSide().equals(Side.CLIENT)) {
			newItemStacks = new ItemStack[3];
		}
		FMLCommonHandler.instance().findContainerFor(this).getMetadata().authorList = Arrays.asList("Thebombzen");
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientChatReceived(ClientChatReceivedEvent event) {
		if (askingIfEnchantViewExists) {
			enchantViewExists = event.message.getUnformattedText().equals("Yes, EnchantView exists.");
			askingIfEnchantViewExists = false;
			event.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void preDrawScreenEvent(DrawScreenEvent.Pre event){
		if (!(event.gui instanceof GuiEnchantment)){
			return;
		}
		if (enchantViewExists
				&& clientContainerEnchantment.enchantLevels[0] != 0
				&& clientContainerEnchantment.tableInventory.getStackInSlot(0) != null) {
			if (!askingForEnchantments
					&& (newItemStacks[0] == null || prevLevelsHashCode != Arrays
							.hashCode(clientContainerEnchantment.enchantLevels))) {
				requestEnchantmentListFromServer(this.clientContainerEnchantment.windowId);
			}
		} else {
			Arrays.fill(newItemStacks, null);
		}
		int xSize = ThebombzenAPI.getPrivateField((GuiEnchantment)event.gui, GuiContainer.class, "xSize");
		int ySize = ThebombzenAPI.getPrivateField((GuiEnchantment)event.gui, GuiContainer.class, "ySize");
		int xPos = (event.gui.width - xSize) / 2;
		int yPos = (event.gui.width - ySize) / 2;
		drawMe = -1;
		for (int i = 0; i < 3; i++){
			int mouseRelX = event.mouseX - (xPos + 60);
			int mouseRelY = event.mouseY - (yPos + 14 + 19 * i);
			if (mouseRelX >= 0 && mouseRelY >= 0 && mouseRelX < 108 && mouseRelY < 19) {
				drawMe = i;
			}
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void postDrawScreenEvent(DrawScreenEvent.Post event){
		if (drawMe != -1
				&& EnchantView.instance.enchantViewExists
				&& clientContainerEnchantment.tableInventory.getStackInSlot(0) != null
				&& EnchantView.instance.newItemStacks[drawMe] != null) {
			ThebombzenAPI.invokePrivateMethod(event.gui, GuiScreen.class, "renderToolTip", new Class<?>[]{ItemStack.class, int.class, int.class}, newItemStacks[drawMe],
					event.mouseX + 8, event.mouseY + 8);
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onOpenGui(GuiOpenEvent event) {
		if (mc.theWorld == null){
			return;
		}
		if (event.gui instanceof GuiEnchantment){
			boolean shouldAsk = getConfiguration().getSingleMultiProperty(Configuration.ENABLE);
			if (shouldAsk) {
				askingIfEnchantViewExists = true;
				mc.thePlayer.sendChatMessage("/doesenchantviewexist");
				clientContainerEnchantment = (ContainerEnchantment)mc.thePlayer.inventoryContainer;
			} else {
				enchantViewExists = false;
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onPacketToClient(ClientCustomPacketEvent event){
		if (!event.packet.channel().equals("EnchantView")){
			return;
		}
		byte[] payload = new byte[event.packet.payload().readableBytes()];
		event.packet.payload().readBytes(payload);
		receiveEnchantmentsListFromServer(payload);
	}
	
	@SubscribeEvent
	public void onPacketToServer(ServerCustomPacketEvent event) {
		if (!event.packet.channel().equals("EnchantView")){
			return;
		}
		EntityPlayerMP player = ((NetHandlerPlayServer)event.handler).playerEntity;
		try {
			byte[] payload = new byte[event.packet.payload().readableBytes()];
			event.packet.payload().readBytes(payload);
			NBTTagCompound compoundIn;
			compoundIn = CompressedStreamTools.readCompressed(new ByteArrayInputStream(payload));
			int stage = compoundIn.getInteger("stage");
			int windowId = compoundIn.getInteger("windowId");
			if (player.openContainer.windowId != windowId) {
				return;
			}
			if (!(player.openContainer instanceof ContainerEnchantment)) {
				return;
			}
			ContainerEnchantment container = (ContainerEnchantment) player.openContainer;
			if (stage == STAGE_REQUEST) {
				ItemStack[] newItemStacks = new ItemStack[3];
				for (int i = 0; i < 3; i++) {
					newItemStacks[i] = generateEnchantedItemStack(container, player, i);
				}
				newItemStacksMap.put(player.getUniqueID(), newItemStacks);
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				NBTTagCompound compoundOut = new NBTTagCompound();
				compoundOut.setInteger("stage", STAGE_SEND);
				for (int i = 0; i < 3; i++) {
					NBTTagCompound stackTag = new NBTTagCompound();
					newItemStacks[i].writeToNBT(stackTag);
					compoundOut.setTag("stack" + i, stackTag);
				}
				CompressedStreamTools.writeCompressed(compoundOut, byteOut);
				FMLProxyPacket packetSend = new FMLProxyPacket(Unpooled.wrappedBuffer(byteOut.toByteArray()), "EnchantView");
				channel.sendTo(packetSend, player);
			} else if (stage == STAGE_ACCEPT) {
				int slot = compoundIn.getInteger("slot");
				enchantItem(container,
						player, slot);
				newItemStacksMap.put(player.getUniqueID(), new ItemStack[3]);
			}
		} catch (IOException ioe) {
			// never!
		}
	}

	@EventHandler
	public void onServerStarting(FMLServerStartingEvent event){
		event.registerServerCommand(new CommandEnchantViewExists());
	}
	
	@SideOnly(Side.CLIENT)
	public void receiveEnchantmentsListFromServer(byte[] payload) {
		askingForEnchantments = false;
		NBTTagCompound compound = null;
		try {
			compound = CompressedStreamTools.readCompressed(new ByteArrayInputStream(payload));
		} catch (IOException ioe) {
			throwException("Error receiving enchanments list from server.", ioe, false);
			return;
		}
		if (compound.getInteger("stage") != STAGE_SEND) {
			throwException("Error receiving enchanments list from server.", new RuntimeException(), false);
		}
		for (int i = 0; i < 3; i++) {
			NBTTagCompound stackTag = compound.getCompoundTag("stack" + i);
			newItemStacks[i] = ItemStack.loadItemStackFromNBT(stackTag);
		}
	}

	@SideOnly(Side.CLIENT)
	public void requestEnchantmentListFromServer(int windowId) {
		askingForEnchantments = true;
		ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("stage", STAGE_REQUEST);
		compound.setInteger("windowId", windowId);
		try {
			CompressedStreamTools.writeCompressed(compound, dataOut);
		} catch (IOException ioe) {
			throw new RuntimeException(
					"Error receiving enchanments list from server.");
		}
		FMLProxyPacket payload = new FMLProxyPacket(Unpooled.wrappedBuffer(dataOut.toByteArray()), "EnchantView");
		channel.sendToServer(payload);
	}

	@SideOnly(Side.CLIENT)
	public void sendAcceptEnchantment(int windowId, int slot) {
		ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
		NBTTagCompound compound = new NBTTagCompound();
		compound.setInteger("stage", STAGE_ACCEPT);
		compound.setInteger("windowId", windowId);
		compound.setInteger("slot", slot);
		try {
			CompressedStreamTools.writeCompressed(compound, dataOut);
		} catch (IOException ioe) {
			throw new RuntimeException(
					"Error receiving enchanments list from server.");
		}
		FMLProxyPacket payload = new FMLProxyPacket(Unpooled.wrappedBuffer(dataOut.toByteArray()), "EnchantView");
		channel.sendToServer(payload);
	}
	
}
