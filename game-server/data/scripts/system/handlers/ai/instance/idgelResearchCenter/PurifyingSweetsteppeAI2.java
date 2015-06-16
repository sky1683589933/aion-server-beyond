package ai.instance.idgelResearchCenter;

import ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;


/**
 * @author Ritsu
 * @modified Luzien
 *
 */
@AIName("purifying_sweetsteppe")
public class PurifyingSweetsteppeAI2 extends AggressiveNpcAI2
{
	
	@Override
	protected void handleSpawned() 
	{
		super.handleSpawned();
		SkillEngine.getInstance().getSkill(getOwner(), 21121, 30, getOwner()).useWithoutPropSkill();
	}
	
}
