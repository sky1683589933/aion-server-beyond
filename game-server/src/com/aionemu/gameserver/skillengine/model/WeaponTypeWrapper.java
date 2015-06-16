package com.aionemu.gameserver.skillengine.model;

import com.aionemu.gameserver.model.templates.item.enums.ItemGroup;
import com.aionemu.gameserver.services.MotionLoggingService;


/**
 * @author kecimis
 *
 */
public class WeaponTypeWrapper implements Comparable<WeaponTypeWrapper> {

	private ItemGroup mainHand = null;
	private ItemGroup offHand = null;

	public WeaponTypeWrapper(ItemGroup mainHand, ItemGroup offHand) {
		if (mainHand != null && offHand != null) {
			switch (mainHand) {
				case DAGGER:
					this.mainHand = ItemGroup.DAGGER;
					this.offHand = ItemGroup.DAGGER;
					break;
				case SWORD:
					this.mainHand = ItemGroup.SWORD;
					this.offHand = ItemGroup.SWORD;
					break;
			  case TOOLHOES:
					this.mainHand = ItemGroup.TOOLHOES;
					this.offHand = ItemGroup.TOOLHOES;
					break;
			   case GUN:
					this.mainHand = ItemGroup.GUN;
					this.offHand = ItemGroup.GUN;
					break;
			   default:
					this.mainHand = mainHand;
					this.offHand = null;
			}	
		}	
		else {
			this.mainHand = mainHand;
			this.offHand = offHand;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WeaponTypeWrapper other = (WeaponTypeWrapper) obj;
		if (!getOuterType().equals(other.getOuterType()))
			return false;
		if (mainHand != other.mainHand)
			return false;
		if (offHand != other.offHand)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "mainHandType=\"" + (mainHand != null ? mainHand.toString() : "null") + "\"" + " offHandType=\""
			+ (offHand != null ? offHand.toString() : "null");
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getOuterType().hashCode();
		result = prime * result + ((mainHand == null) ? 0 : mainHand.hashCode());
		result = prime * result + ((offHand == null) ? 0 : offHand.hashCode());
		return result;
	}

	@Override
	public int compareTo(WeaponTypeWrapper o) {
		if (mainHand == null || o.getMainHand() == null)
			return 0;
		else if (offHand != null && o.getOffHand() != null)
			return 0;
		else if (offHand != null && o.getOffHand() == null)
			return 1;
		else if (offHand == null && o.getOffHand() != null)
			return -1;
		else
			return mainHand.toString().compareTo(o.getMainHand().toString());
	}

	public ItemGroup getMainHand() {
		return this.mainHand;
	}

	public ItemGroup getOffHand() {
		return this.offHand;
	}

	private MotionLoggingService getOuterType() {
		return MotionLoggingService.getInstance();
	}
}
