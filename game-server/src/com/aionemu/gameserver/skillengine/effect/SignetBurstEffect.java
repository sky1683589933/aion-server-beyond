package com.aionemu.gameserver.skillengine.effect;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SignetData;
import com.aionemu.gameserver.skillengine.model.SignetEnum;

/**
 * @author ATracer, kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SignetBurstEffect")
public class SignetBurstEffect extends DamageEffect {

	@XmlAttribute
	protected int signetlvl;
	@XmlAttribute
	protected String signet;

	@Override
	public void calculateDamage(Effect effect) {
		Effect signetEffect = effect.getEffected().getEffectController().getAbnormalEffect(signet);
		int valueWithDelta = calculateBaseValue(effect);

		int effectProb = 0;
		SignetData signetData = DataManager.SIGNET_DATA_TEMPLATES.getSignetData(SignetEnum.valueOf(signet), signetEffect == null ? 0 : signetEffect.getSkillLevel());
		if (signetData != null) {
			valueWithDelta *= signetData.getDamageMultiplier();
			effectProb = signetData.getAddEffectProb();
		}
		AttackUtil.calculateSkillResult(effect, valueWithDelta, this, false);
		effect.setLaunchSubEffect(Rnd.chance() < effectProb);
		if (signetEffect != null)
			signetEffect.endEffect();
	}

	@Override
	public void calculate(Effect effect) {
		Effect signetEffect = effect.getEffected().getEffectController().getAbnormalEffect(signet);
		if (!super.calculate(effect, null, null)) {
			if (signetEffect != null) {
				signetEffect.endEffect();
			}
		}
	}

	/**
	 * @return the signetlvl
	 */
	public int getSignetlvl() {
		return signetlvl;
	}

	/**
	 * @return the signet
	 */
	public String getSignet() {
		return signet;
	}

}
