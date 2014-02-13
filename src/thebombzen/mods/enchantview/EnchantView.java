package thebombzen.mods.enchantview;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import thebombzen.mods.thebombzenapi.ThebombzenAPI;
import thebombzen.mods.thebombzenapi.ThebombzenAPIBaseMod;
import thebombzen.mods.thebombzenapi.ThebombzenAPIConfiguration;
import thebombzen.mods.thebombzenapi.client.ThebombzenAPIConfigScreen;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.IChatListener;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = "EnchantView", name = "EnchantView", version = "3.12.0", dependencies = "required-after:ThebombzenAPI")
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public class EnchantView extends ThebombzenAPIBaseMod implements IPacketHandler {

	public static final int STAGE_REQUEST = 0;
	public static final int STAGE_SEND = 1;
	public static final int STAGE_ACCEPT = 2;
	public static final Random random = new Random();

	@Instance(value = "EnchantView")
	public static EnchantView instance;

	@SidedProxy(clientSide = "thebombzen.mods.enchantview.ClientProxy", serverSide = "thebombzen.mods.enchantview.CommonProxy")
	public static CommonProxy proxy;

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
	public volatile int currentWorldHashCode;

	public Map<UUID, ItemStack[]> newItemStacksMap = new HashMap<UUID, ItemStack[]>();

	public boolean canPlayerUseCommand(EntityPlayerMP player) {
		return proxy.canPlayerUseCommand(player);
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
			List<EnchantmentData> enchList = EnchantmentHelper
					.buildEnchantmentList(random, newItemStack,
							container.enchantLevels[slot]);
			boolean isBook = newItemStack.itemID == Item.book.itemID;

			if (enchList != null) {
				// handler.playerEntity.addExperienceLevel(-container.enchantLevels[slot]);
				if (isBook) {
					newItemStack.itemID = Item.enchantedBook.itemID;
				}
				int enchToPick = random.nextInt(enchList.size());
				for (int i = 0; i < enchList.size(); i++) {
					EnchantmentData enchData = enchList.get(i);
					if (!isBook || i == enchToPick) {
						if (isBook) {
							Item.enchantedBook.addEnchantment(newItemStack,
									enchData);
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

	@SideOnly(Side.CLIENT)
	public void receiveEnchantmentListFromServer(
			Packet250CustomPayload payload) {
		askingForEnchantments = false;
		NBTTagCompound compound = null;
		try {
			compound = CompressedStreamTools
					.readCompressed(new ByteArrayInputStream(payload.data, 0,
							payload.length));
		} catch (IOException ioe) {
			throw new RuntimeException(
					"Error receiving enchanments list from server.");
		}
		if (compound.getInteger("stage") != STAGE_SEND) {
			throw new RuntimeException(
					"Error receiving enchanments list from server.");
		}
		for (int i = 0; i < 3; i++) {
			NBTTagCompound stackTag = compound.getCompoundTag("stack" + i);
			newItemStacks[i] = ItemStack.loadItemStackFromNBT(stackTag);
		}
	}

	@SideOnly(Side.CLIENT)
	public void requestEnchantmentListFromServer(int windowId) {
		askingForEnchantments = true;
		Packet250CustomPayload payload = new Packet250CustomPayload();
		payload.channel = "EnchantView";
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
		payload.data = dataOut.toByteArray();
		payload.length = payload.data.length;
		Minecraft.getMinecraft().getNetHandler().addToSendQueue(payload);
	}

	@SideOnly(Side.CLIENT)
	public void sendAcceptEnchantment(int windowId, int slot) {
		Packet250CustomPayload payload = new Packet250CustomPayload();
		payload.channel = "EnchantView";
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
		payload.data = dataOut.toByteArray();
		payload.length = payload.data.length;
		Minecraft.getMinecraft().getNetHandler().addToSendQueue(payload);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ThebombzenAPIConfigScreen createConfigScreen(GuiScreen base) {
		return new ConfigScreen(this, base, configuration);
	}

	@Override
	public ThebombzenAPIConfiguration<?> getConfiguration() {
		return configuration;
	}

	@Override
	public String getLongName() {
		return "EnchantView";
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getNumActiveKeys() {
		return 0;
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
	public String getLongVersionString() {
		return "EnchantView v3.12.0 for Minecraft 1.6.4";
	}

	@Override
	public String getVersionFileURLString() {
		return "https://dl.dropboxusercontent.com/u/51080973/EnchantView/EVVersion.txt";
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean hasConfigScreen() {
		return true;
	}
	
	@EventHandler
	public void load(FMLInitializationEvent fmlie) {
		NetworkRegistry.instance().registerChannel(this, "EnchantView");
		MinecraftForge.EVENT_BUS.register(this);
	}

	@ForgeSubscribe
	@SideOnly(Side.CLIENT)
	public void onClientChatReceived(ClientChatReceivedEvent event) {
		if (askingIfEnchantViewExists) {
			enchantViewExists = event.message
					.equals("{\"text\":\"Yes, EnchantView exists.\"}");
			askingIfEnchantViewExists = false;
			event.setCanceled(true);
		}
	}

	@SideOnly(Side.CLIENT)
	public void onClientPacket(Packet250CustomPayload payload) {
		receiveEnchantmentListFromServer(payload);
	}

	@ForgeSubscribe
	@SideOnly(Side.CLIENT)
	public void onOpenGui(GuiOpenEvent event) {
		if (event.gui instanceof GuiEnchantment
				&& !(event.gui instanceof EVGuiEnchantment)) {
			event.gui = new EVGuiEnchantment((GuiEnchantment) event.gui);
		}
	}

	@Override
	public void onPacketData(INetworkManager manager,
			Packet250CustomPayload payload, Player player) {
		if (player instanceof EntityPlayerMP) {
			onServerPacket(manager, payload, (EntityPlayerMP) player);
		} else {
			onClientPacket(payload);
		}
	}

	public void onServerPacket(INetworkManager manager,
			Packet250CustomPayload payload, EntityPlayerMP player) {
		try {
			NBTTagCompound compoundIn;
			ByteArrayInputStream byteIn = new ByteArrayInputStream(
					payload.data, 0, payload.length);
			compoundIn = CompressedStreamTools.readCompressed(byteIn);
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
				ItemStack stack = (ItemStack) container.getInventory().get(0);
				for (int i = 0; i < 3; i++) {
					int level = container.enchantLevels[i];
					newItemStacks[i] = generateEnchantedItemStack(container, player, i);
				}
				newItemStacksMap.put(player.getUniqueID(), newItemStacks);
				Packet250CustomPayload payloadSend = new Packet250CustomPayload();
				payloadSend.channel = "EnchantView";
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				NBTTagCompound compoundOut = new NBTTagCompound();
				compoundOut.setInteger("stage", STAGE_SEND);
				for (int i = 0; i < 3; i++) {
					NBTTagCompound stackTag = new NBTTagCompound();
					newItemStacks[i].writeToNBT(stackTag);
					compoundOut.setCompoundTag("stack" + i, stackTag);
				}
				CompressedStreamTools.writeCompressed(compoundOut, byteOut);
				payloadSend.data = byteOut.toByteArray();
				payloadSend.length = payloadSend.data.length;
				manager.addToSendQueue(payloadSend);
			} else if (stage == STAGE_ACCEPT) {
				int slot = compoundIn.getInteger("slot");
				enchantItem((ContainerEnchantment) player.openContainer,
						player, slot);
				newItemStacksMap.put(player.getUniqueID(), new ItemStack[3]);
			}
		} catch (IOException ioe) {
			// never!
		}
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		configuration = new Configuration(this);
		if (event.getSide().equals(Side.CLIENT)) {
			newItemStacks = new ItemStack[3];
		}
		super.preInit(event);
	}
	
	@EventHandler
	public void onServerStarting(FMLServerStartingEvent event){
		event.registerServerCommand(new CommandEnchantViewExists());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void activeKeyPressed(int keyCode) {
		
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected String getToggleMessageString(int index, boolean enabled) {
		return "";
	}

}
