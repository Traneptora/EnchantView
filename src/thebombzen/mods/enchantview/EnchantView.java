package thebombzen.mods.enchantview;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thebombzen.mods.thebombzenapi.ThebombzenAPI;
import thebombzen.mods.thebombzenapi.ThebombzenAPIBaseMod;

/**
 * The main EnchantView mod
 * The mod itself works entirely on the server
 * It only uses client-side features like Update Notices
 * @author thebombzen
 */
@Mod(modid = "enchantview", name = "EnchantView", version = Constants.VERSION, dependencies = "required-after:thebombzenapi", guiFactory = "thebombzen.mods.enchantview.client.ConfigGuiFactory", acceptedMinecraftVersions = "[1.8, 1.9)")
public class EnchantView extends ThebombzenAPIBaseMod {

	/**
	 * The EnchantView Instance
	 */
	@Instance(value = "enchantview")
	public static EnchantView instance;

	/**
	 * This is the configuration for EnchantView.
	 * It is currently empty.
	 */
	private Configuration configuration;
	
	/**
	 * This stores the number of ticks that have elapsed since the container has been opened.
	 * Every 20 ticks, the enchantment hint will cycle.
	 * It's a function of the player's UUID, so it can keep track
	 * of multiple players at once on the server.
	 */
	private Map<UUID, Long> tickTimes = new HashMap<UUID, Long>();
	
	/**
	 * This stores the previous state of the Craft Matrix.
	 * It is necessary because there is previously no hook for onCraftMatrixChanged.
	 * It needs to be done manually. 
	 * It's a function of the player's UUID, so it can keep track
	 * of multiple players at once on the server.
	 */
	private Map<UUID, TableState> prevTableStates = new HashMap<UUID, TableState>();
	
	/**
	 * This stores the full hint data, rather than just the particular
	 * hint that would have been shown previously.
	 * It's a function of the player's UUID, so it can keep track
	 * of multiple players at once on the server.
	 */
	private Map<UUID, int[][]> hints = new HashMap<UUID, int[][]>();
	
	/**
	 * Do not reject vanilla clients or vanilla servers.
	 */
	@NetworkCheckHandler
	public boolean checkNetwork(Map<String, String> modsList, Side remote){
		return true;
	}
	
	/**
	 * This is the main event ticker. It only ticks when the player has an open container so we can rule out other ticks.
	 * The player usually has an open container (the inventory) but we can simple check for that and return.
	 * This also ticks once per player rather than once per World or once per MinecraftServer, so it provides an extra layer of convenience.
	 */
	@SubscribeEvent
	public void playerHasContainerOpen(PlayerOpenContainerEvent event){
		UUID uuid = event.getEntityPlayer().getUniqueID();
		// If the inventory is not a crafting inventory then clear stuff.
		if (!(event.getEntityPlayer().openContainer instanceof ContainerEnchantment)){
			purgeUUID(uuid);
			return;
		}
		ContainerEnchantment container = (ContainerEnchantment)event.getEntityPlayer().openContainer;
		
		if (!tickTimes.containsKey(uuid)){
			tickTimes.put(uuid, 0L);
		}
		// We set it to zero and immediately increment it so waiting a second
		// is guaranteed. This is simpler than just setting it to one.
		tickTimes.put(uuid, tickTimes.get(uuid) + 1L);
		
		// We have to check for OnCraftMatrixChanged manually.
		TableState state = new TableState(container);
		if (!prevTableStates.containsKey(uuid) || !prevTableStates.get(uuid).equals(state)){
			prevTableStates.put(uuid, state);
			tableUpdated(event.getEntityPlayer());
		}
		
		// 20 ticks is one second.
		if (tickTimes.get(uuid) % 20L == 0L){
			cycleHint(event.getEntityPlayer());
		}
	}
	
	/**
	 * The server tick just clears out old UUIDs from players who have disconnected.
	 */
	@SubscribeEvent
	public void serverTick(ServerTickEvent event){
		for (UUID uuid : tickTimes.keySet()){
			if (FMLCommonHandler.instance().getMinecraftServerInstance().getEntityFromUuid(uuid) == null){
				purgeUUID(uuid);
			}
		}
	}
	
	/**
	 * Remove a player's UUID from our temporary database.
	 * @param uuid The UUID to remove
	 */
	private void purgeUUID(UUID uuid){
		tickTimes.remove(uuid);
		prevTableStates.remove(uuid);
		hints.remove(uuid);
	}
	
	/**
	 * If we've detected an update in the craft matrix, we refresh the xp seed as well.
	 * One of the features of this mod is to rerandomize every time, like it used to.
	 * @param player The player whose table was updated
	 */
	private void tableUpdated(EntityPlayer player){
		
		UUID uuid = player.getUniqueID();
		
		// This causes a change of the tooltip immediately. 
		tickTimes.put(uuid, 0L);
		
		// This causes rerandomization of the XP Seed.
		player.removeExperienceLevel(0);
		
		ContainerEnchantment container = (ContainerEnchantment)player.openContainer;
		container.xpSeed = player.getXPSeed();
		int[][] hints = new int[3][];
		for (int j = 0; j < 3; j++) {
			if (container.enchantLevels[j] > 0) {
				// func_178148_a, a
				List<EnchantmentData> list = ThebombzenAPI.invokePrivateMethod(container, ContainerEnchantment.class, new String[]{"func_178148_a", "a"}, new Class<?>[]{ItemStack.class, int.class, int.class}, container.tableInventory.getStackInSlot(0), j, container.enchantLevels[j]);
				
				if (list != null && !list.isEmpty()) {
					hints[j] = new int[list.size()];
					for (int i = 0; i < list.size(); i++){
						EnchantmentData enchantmentdata = list.get(i);
						hints[j][i] = Enchantment.getEnchantmentID(enchantmentdata.enchantmentobj) | enchantmentdata.enchantmentLevel << 8;
					}
				} else {
					hints[j] = null;
				}
			}
		}
		this.hints.put(uuid, hints);
	}
	
	/**
	 * This cycles the hints once per second.
	 * It might be less than once per second on laggy servers, because it's technically once per 20 ticks.
	 * @param player The player whose tick we must cycle
	 */
	private void cycleHint(EntityPlayer player){
		UUID uuid = player.getUniqueID();
		ContainerEnchantment container = (ContainerEnchantment)player.openContainer;
		int[][] hints = this.hints.get(uuid);
		if (hints == null){
			return;
		}
		for (int j = 0; j < 3; j++){
			int[] currHints = hints[j];
			if (currHints == null){
				return;
			}
			container.enchantClue[j] = currHints[(int)((tickTimes.get(uuid) / 20L) % (long)currHints.length)];
		}
		container.detectAndSendChanges();
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
		configuration = new Configuration(this);
		FMLCommonHandler.instance().findContainerFor(this).getMetadata().authorList = Arrays.asList("Thebombzen");
	}

}
