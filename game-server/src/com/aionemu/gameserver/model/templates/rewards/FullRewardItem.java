package com.aionemu.gameserver.model.templates.rewards;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Luzien
 * @modified Pad
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FullRewardItem")
public class FullRewardItem extends IdLevelReward {

	@XmlAttribute(name = "count")
	protected int count;

	@XmlAttribute(name = "chance")
	protected float chance;

	public int getCount() {
		return count;
	}

	public float getChance() {
		return chance;
	}
}