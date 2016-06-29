package thebombzen.mods.enchantview;

import java.util.Arrays;

import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class TableState {
	private int[] enchantLevels = new int[3];
	private String[] items = new String[2];
	private int[] damages = new int[2];
	private int[] sizes = new int[2];
	public TableState(ContainerEnchantment container){
		System.arraycopy(container.enchantLevels, 0, this.enchantLevels, 0, 3);
		for (int i = 0; i < 2; i++){
			ItemStack stack = container.tableInventory.getStackInSlot(i);
			if (stack == null){
				items[i] = "";
				damages[i] = 0;
				sizes[i] = 0;
			} else {
				items[i] = Item.REGISTRY.getNameForObject(stack.getItem()).toString();
				damages[i] = stack.getItemDamage();
				sizes[i] = stack.stackSize;
			}
		}
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(damages);
		result = prime * result + Arrays.hashCode(enchantLevels);
		result = prime * result + Arrays.hashCode(items);
		result = prime * result + Arrays.hashCode(sizes);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TableState other = (TableState) obj;
		if (!Arrays.equals(damages, other.damages))
			return false;
		if (!Arrays.equals(enchantLevels, other.enchantLevels))
			return false;
		if (!Arrays.equals(items, other.items))
			return false;
		if (!Arrays.equals(sizes, other.sizes))
			return false;
		return true;
	}
}
