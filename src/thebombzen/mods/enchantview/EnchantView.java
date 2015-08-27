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

import org.lwjgl.input.Mouse;

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
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This is the main EnchantView mod file
 * @author thebombzen
 */
@Mod(modid = "enchantview", name = "EnchantView", version = Constants.VERSION, dependencies = "required-after:thebombzenapi", guiFactory = "thebombzen.mods.enchantview.client.ConfigGuiFactory")
public class EnchantView extends ThebombzenAPIBaseMod {

	/**
	 * This represents the stage where the client is requesting the enchantment list from the server
	 */
	public static final int STAGE_REQUEST = 0;
	/**
	 * This represents the stage where the server is sending the enchantment list to the client
	 */
	public static final int STAGE_SEND = 1;
	/**
	 * This represents the stage where the client is accepting the given enchantment list
	 */
	public static final int STAGE_ACCEPT = 2;
	
	/**
	 * The Random Number Generator we use
	 */
	public static final Random random = new Random();
	
	/**
	 * This is a shortcut for Minecraft.getMinecraft()
	 */
	public static final Minecraft mc = Minecraft.getMinecraft();
	
	/**
	 * This is the event channel that we use to communicate between client and server
	 */
	public FMLEventChannel channel;

	/**
	 * This field stores the main EnchantView client instance
	 */
	@Instance(value = "enchantview")
	public static EnchantView instance;

	/**
	 * Contains side-specific utilities in a sided proxy
	 */
	@SidedProxy(clientSide = "thebombzen.mods.enchantview.client.ClientSideSpecificUtilities", serverSide = "thebombzen.mods.enchantview.server.ServerSideSpecificUtilities")
	public static SideSpecificUtilities sideSpecificUtilities;

	/**
	 * The configuration for ThebombzenAPI
	 */
	private Configuration configuration;

	/**
	 * The three ItemStacks that the server has provided as potential enchantments
	 */
	@SideOnly(Side.CLIENT)
	public ItemStack[] newItemStacks;
	
	/**
	 * Set to true if the client has detected that EnchantView exists on the server
	 */
	@SideOnly(Side.CLIENT)
	public volatile boolean enchantViewExists;
	
	/**
	 * Set to true while the client is asking if the server has EnchantView
	 */
	@SideOnly(Side.CLIENT)
	public volatile boolean askingIfEnchantViewExists;
	
	/**
	 * Set to true while the client has requested an enchantments list but has not received it yet
	 */
	@SideOnly(Side.CLIENT)
	public volatile boolean askingForEnchantments;
	
	/**
	 * Set to true while the client has accepted the enchantment list but it has not changed yet
	 */
	@SideOnly(Side.CLIENT)
	public volatile boolean acceptingEnchantments;
	
	/**
	 * This stores the Arrays.hashCode of the enchantment levels during the previous tick
	 */
	@SideOnly(Side.CLIENT)
	public volatile int prevLevelsHashCode;
	
	/**
	 * The slot for which a tooltip should be drawn
	 */
	@SideOnly(Side.CLIENT)
	public volatile int drawMe;
	
	/**
	 * The enchantment Container on the client-side
	 */
	@SideOnly(Side.CLIENT)
	public ContainerEnchantment clientContainerEnchantment;
	
	/**
	 * Whether we've artificially changed the touchscreen settings
	 * (used to disable handling mouse input)
	 */
	@SideOnly(Side.CLIENT)
	private volatile boolean touchscreen_changed;
	
	/**
	 * The previous stored value of the touchscreen setting
	 */
	@SideOnly(Side.CLIENT)
	private volatile boolean touchscreen;
	
	/**
	 * The previous stored value of the touchscreen countdown
	 */
	@SideOnly(Side.CLIENT)
	private volatile int field_h;

	/**
	 * This is the map of stored item stacks, based on the EntityPlayer UUID
	 */
	private Map<UUID, ItemStack[]> newItemStacksMap = new HashMap<UUID, ItemStack[]>();
	
	/**
	 * Do not reject vanilla clients or vanilla servers.
	 */
	@NetworkCheckHandler
	public boolean checkNetwork(Map<String, String> modsList, Side remote){
		return true;
	}

	/**
	 * Actually enchant the item on the server-side
	 * @param container The server-side Container
	 * @param player The EntityPlayer for whom we're enchanting
	 * @param slot An integer between 0 and 2, inclusive.
	 */
	public void enchantItem(ContainerEnchantment container,
			EntityPlayerMP player, int slot) {
		ItemStack stack = newItemStacksMap.get(player.getUniqueID())[slot];
		player.addExperienceLevel(-container.enchantLevels[slot]);
		container.tableInventory.setInventorySlotContents(0, stack);
		container.onCraftMatrixChanged(container.tableInventory);
	}

	/**
	 * Build an enchanted item stack the way the Minecraft server would, but don't update the craft matrix.
	 * @param container The server-side Container
	 * @param player The player for whom we're building a stack
	 * @param slot An integer between 0 and 2, inclusive
	 * @return the generated ItemStack
	 */
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
				//container.onCraftMatrixChanged(container.tableInventory);
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

	/**
	 * When we're asking if EnchantView exists, cancel the chat that the server uses to respond.
	 * @param event
	 */
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientChatReceived(ClientChatReceivedEvent event) {
		if (askingIfEnchantViewExists) {
			enchantViewExists = event.message.getUnformattedText().equals("Yes, EnchantView exists.");
			askingIfEnchantViewExists = false;
			event.setCanceled(true);
		}
	}
	
	/**
	 * Tests if any mouse button is down
	 * @return True if any mouse button is down, false otherwise
	 */
	@SideOnly(Side.CLIENT)
	public boolean isMouseDown(){
		for (int i = 0; i < Mouse.getButtonCount(); i++){
			if (Mouse.isButtonDown(i)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Client-side tick loop. Used to cancel the default enchantment-acceptance behavior 
	 * @param event
	 */
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void clientTick(ClientTickEvent event){
		try {
			if (event.phase.equals(Phase.START)) {

				if (mc.currentScreen instanceof GuiEnchantment) {
					if (Arrays
							.hashCode(this.clientContainerEnchantment.enchantLevels) != prevLevelsHashCode) {
						acceptingEnchantments = false;
					}
					if (drawMe != -1 && isMouseDown() && !acceptingEnchantments) {

						this.acceptingEnchantments = true;

						// this is used to disable the mouse event
						// we set the touchscreen delay to be too high so it
						// cancels the event

						touchscreen_changed = true;
						touchscreen = mc.gameSettings.touchscreen;
						field_h = ThebombzenAPI.getPrivateField(
								mc.currentScreen, GuiScreen.class,
								"field_146298_h", "h");

						mc.gameSettings.touchscreen = true;
						ThebombzenAPI.setPrivateField(mc.currentScreen,
								GuiScreen.class, Integer.MAX_VALUE / 2,
								"field_146298_h", "h");

						sendAcceptEnchantment(this.clientContainerEnchantment.windowId, drawMe);
					}
				}
			} else {
				if (touchscreen_changed){
					mc.gameSettings.touchscreen = touchscreen;
					ThebombzenAPI.setPrivateField(mc.currentScreen, GuiScreen.class, field_h, "field_146298_h", "h");
					touchscreen_changed = false;
				}
			}
		} catch (Throwable e) {
			throwException("clientTick", e, false);
		}
	}		
	
	/**
	 * Before the screen is drawn, request the enchantment list
	 * and determine which tooltip we need to draw.
	 */
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void preDrawScreenEvent(DrawScreenEvent.Pre event){
		if (!(event.gui instanceof GuiEnchantment)){
			return;
		}
		drawMe = -1;
		if (enchantViewExists
				&& clientContainerEnchantment.enchantLevels[0] != 0
				&& clientContainerEnchantment.tableInventory.getStackInSlot(0) != null) {
			if (!askingForEnchantments
					&& (newItemStacks[0] == null || prevLevelsHashCode != Arrays
							.hashCode(clientContainerEnchantment.enchantLevels))) {
				requestEnchantmentListFromServer(this.clientContainerEnchantment.windowId);
				prevLevelsHashCode = Arrays.hashCode(clientContainerEnchantment.enchantLevels);
			}
		} else {
			Arrays.fill(newItemStacks, null);
			return;
		}
		int xSize = ThebombzenAPI.getPrivateField((GuiEnchantment)event.gui, GuiContainer.class, "xSize", "field_146999_f", "f");
		int ySize = ThebombzenAPI.getPrivateField((GuiEnchantment)event.gui, GuiContainer.class, "ySize", "field_147000_g", "g");
		int xPos = (event.gui.width - xSize) / 2;
		int yPos = (event.gui.height - ySize) / 2;
		for (int i = 0; i < 3; i++){
			int mouseRelX = event.mouseX - (xPos + 60);
			int mouseRelY = event.mouseY - (yPos + 14 + 19 * i);
			if (mouseRelX >= 0 && mouseRelY >= 0 && mouseRelX < 108 && mouseRelY < 19) {
				drawMe = i;
			}
		}
	}
	
	/**
	 * Draw the correct tooltip
	 */
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
	
	/**
	 * Called when a GUI is open to detect if EnchantView exists.
	 */
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
				clientContainerEnchantment = (ContainerEnchantment)((GuiEnchantment)event.gui).inventorySlots;
			} else {
				enchantViewExists = false;
			}
		}
	}
	
	/**
	 * Used to receive the enchantments list from the server
	 */
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
	
	/**
	 * Receive a packet from the client,
	 * and either send back the enchantments list if it's a request,
	 * or enchant the item if it's an acceptance 
	 */
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
				enchantItem(container, player, slot);
				newItemStacksMap.put(player.getUniqueID(), new ItemStack[3]);
			}
		} catch (IOException ioe) {
			// this will never happen
		}
	}

	/**
	 * Register the command on server start
	 */
	@EventHandler
	public void onServerStarting(FMLServerStartingEvent event){
		event.registerServerCommand(new CommandEnchantViewExists());
	}
	
	/**
	 * Receive the enchantments list from the server and store the necessary data
	 * @param payload the byte array payload sent by the server
	 */
	@SideOnly(Side.CLIENT)
	public void receiveEnchantmentsListFromServer(byte[] payload) {
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
		askingForEnchantments = false;
	}

	/**
	 * Request a list of enchantments from the server
	 * @param windowId The Container id of the current enchantment Container
	 */
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

	/**
	 * Accept the enchantment in the given slot
	 * @param windowId The Container id of the current enchantment Container
	 * @param slot The slot whose enchantment we accept
	 */
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
		drawMe = -1;
	}
	
}
