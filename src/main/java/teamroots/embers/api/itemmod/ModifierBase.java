package teamroots.embers.api.itemmod;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;

public class ModifierBase {
	public enum EnumType {
		ALL,
		TOOL_OR_ARMOR,
		TOOL,
		PROJECTILE,
		ARMOR,
		HELMET,
		CHESTPLATE,
		LEGGINGS,
		BOOTS,
	}
	
	public EnumType type = EnumType.ALL;
	public String name = "";
	public double cost = 0;
	public boolean countTowardsTotalLevel = false;
	public boolean canRemove = true;
	public boolean shouldRenderTooltip = true;

	public ModifierBase(EnumType type, String name, double cost, boolean levelCounts){
		this.type = type;
		this.name = name;
		this.cost = cost;
		this.countTowardsTotalLevel = levelCounts;
	}

	public boolean canApplyTo(ItemStack stack) {
		return canApplyToType(stack,type);
	}

	protected boolean canApplyToType(ItemStack stack, EnumType type) {
		Item item = stack.getItem();
		switch (type) {
			case ALL:
				return true;
			case TOOL_OR_ARMOR:
				return isTool(item) || item instanceof ItemArmor;
			case TOOL:
				return isTool(item);
			case ARMOR:
				return item instanceof ItemArmor;
			case HELMET:
				return item instanceof ItemArmor && ((ItemArmor) item).armorType == EntityEquipmentSlot.HEAD;
			case CHESTPLATE:
				return item instanceof ItemArmor && ((ItemArmor) item).armorType == EntityEquipmentSlot.CHEST;
			case LEGGINGS:
				return item instanceof ItemArmor && ((ItemArmor) item).armorType == EntityEquipmentSlot.LEGS;
			case BOOTS:
				return item instanceof ItemArmor && ((ItemArmor) item).armorType == EntityEquipmentSlot.FEET;
			default:
				return false;
		}
	}

	private boolean isTool(Item item) {
		return item instanceof ItemSword || item instanceof ItemTool || item instanceof ItemHoe;
	}

	public void onApply(ItemStack stack) {
		//NOOP
	}

	public void onRemove(ItemStack stack) {
		//NOOP
	}
}
