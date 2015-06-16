package com.aionemu.gameserver.restrictions;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.templates.item.ItemUseLimits;
import com.aionemu.gameserver.model.templates.panels.SkillPanel;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.SkillType;
import com.aionemu.gameserver.skillengine.model.TransformType;
import com.aionemu.gameserver.skillengine.properties.TargetRelationAttribute;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import static com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_CANNOT_USE_ITEM_INVALID_GENDER;

/**
 * @author lord_rex modified by Sippolo
 */
public class PlayerRestrictions extends AbstractRestrictions {

	@Override
	public boolean canAffectBySkill(Player player, VisibleObject target, Skill skill) {
		if (skill == null)
			return false;

		// dont allow to use skills in Fly Teleport state
		if (target instanceof Player && ((Player) target).isProtectionActive())
			return false;

		if (player != target && target instanceof Player) {
			Player tPlayer = (Player) target;
			if (!tPlayer.getRace().equals(player.getRace())) {
				if (!tPlayer.isEnemyFrom(player))
					return false;
			}
			else if (tPlayer.getController().isDueling(player)) {
				if (skill.getSkillTemplate().getProperties().getTargetRelation() != TargetRelationAttribute.ENEMY) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_TARGET_IS_NOT_VALID);
					return false;
				}
			}
		}

		if (player.isUsingFlyTeleport() || (target instanceof Player && ((Player) target).isUsingFlyTeleport()))
			return false;

		if (((Creature) target).getLifeStats().isAboutToDie() && !skill.checkNonTargetAOE())
			return false;
		
		if (((Creature) target).getLifeStats().isAlreadyDead() && !skill.getSkillTemplate().hasResurrectEffect()
			&& !skill.checkNonTargetAOE()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_TARGET_IS_NOT_VALID);
			return false;
		}
		
		//cant resurrect non players and non dead
		if (skill.getSkillTemplate().hasResurrectEffect() && (!(target instanceof Player) || 
			!((Creature)target).getLifeStats().isAlreadyDead()))
			return false;

		if (!skill.getSkillTemplate().hasEvadeEffect()) {
			if (player.getEffectController().isInAnyAbnormalState(AbnormalState.CANT_ATTACK_STATE))
				return false;
		}

		if (player.getStore() != null) { // You cannot use an item while running a Private Store.
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_USE_ITEM_DURING_PATH_FLYING(new DescriptionId(2800123)));
			return false;
		}

		return true;
	}

	private boolean checkFly(Player player, VisibleObject target) {
		if (player.isUsingFlyTeleport() || player.isInPlayerMode(PlayerMode.WINDSTREAM)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_RESTRICTION_NO_FLY);
			return false;
		}

		if (target != null && target instanceof Player) {
			Player playerTarget = (Player) target;
			if (playerTarget.isUsingFlyTeleport() || playerTarget.isInPlayerMode(PlayerMode.WINDSTREAM)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean canUseSkill(Player player, Skill skill) {
		VisibleObject target = player.getTarget();
		SkillTemplate template = skill.getSkillTemplate();

		if (!checkFly(player, target)) {
			return false;
		}

		// check if is casting to avoid multicast exploit
		// TODO cancel skill if other is used
		if (player.isCasting())
			return false;

		if ((!player.canAttack()) && !template.hasEvadeEffect())
			return false;

		//in 3.0 players can use remove shock even when silenced
		if (template.getType() == SkillType.MAGICAL && player.getEffectController().isAbnormalSet(AbnormalState.SILENCE) && !template.hasEvadeEffect()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_CANT_CAST_MAGIC_SKILL_WHILE_SILENCED);
			return false;
		}

		if (template.getType() == SkillType.PHYSICAL && player.getEffectController().isAbnormalSet(AbnormalState.BIND)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_CANT_CAST_PHYSICAL_SKILL_IN_FEAR);
			return false;
		}
			

		if (player.isSkillDisabled(template))
			return false;

		//cannot use skills while transformed
		if (player.getTransformModel().isActive()) {
			if (player.getTransformModel().getBanUseSkills() == 1) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_CAN_NOT_CAST_IN_SHAPECHANGE);
				return false;
			}
			//can use only panel skills in FORM1
			if (player.getTransformModel().getType() == TransformType.FORM1) {
				SkillPanel panel = DataManager.PANEL_SKILL_DATA.getSkillPanel(player.getTransformModel().getPanelId());
				if (panel == null || !panel.isSkillPresent(skill.getSkillId())) {
					AuditLogger.info(player, "Trying to use non panel skill while transformed in TransformType.FORM1. Player: " + player.getName());
					return false;
				}
			}
		}
		
		// Fix for Summon Group Member, cannot be used while either caster or summoned is actively in combat
		//example skillId: 1606
		if (skill.getSkillTemplate().hasRecallInstant()) {
			if (!(target instanceof Player))
				return false;
			if (player.getController().isInCombat() || 
				((Player) target).getController().isInCombat() ||
				((Player) target).getTransformModel().getRes1() == 1)//cannot be summoned while transformed
			{
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Recall_CANNOT_ACCEPT_EFFECT(target.getName()));
				return false;
			}
		}
		
		if (template.hasResurrectEffect()) {
			if (!(target instanceof Player)) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_TARGET_IS_NOT_VALID);
				return false;
			}
			Player targetPlayer = (Player) target;
			if (!targetPlayer.getLifeStats().isAlreadyDead()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_TARGET_IS_NOT_VALID);
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean canInviteToGroup(Player player, Player target) {
		final com.aionemu.gameserver.model.team2.group.PlayerGroup group = player.getPlayerGroup2();

		if (group != null && group.isFull()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_CANT_ADD_NEW_MEMBER);
			return false;
		}
		else if (group != null && !player.getObjectId().equals(group.getLeader().getObjectId())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_ONLY_LEADER_CAN_INVITE);
			return false;
		}
		else if (target == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_NO_USER_TO_INVITE);
			return false;
		}
		else if (target.getRace() != player.getRace() && !GroupConfig.GROUP_INVITEOTHERFACTION) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_CANT_INVITE_OTHER_RACE);
			return false;
		}
		else if (target.sameObjectId(player.getObjectId())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_CAN_NOT_INVITE_SELF);
			return false;
		}
		else if (target.getLifeStats().isAlreadyDead()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_UI_PARTY_DEAD);
			return false;
		}
		else if (player.getLifeStats().isAlreadyDead()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_CANT_INVITE_WHEN_DEAD);
			return false;
		}
		else if (player.isInGroup2() && target.isInGroup2()
			&& player.getPlayerGroup2().getTeamId() == target.getPlayerGroup2().getTeamId()) {
			PacketSendUtility.sendPacket(player,
				SM_SYSTEM_MESSAGE.STR_PARTY_HE_IS_ALREADY_MEMBER_OF_OUR_PARTY(target.getName()));
		}
		else if (target.isInGroup2()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_HE_IS_ALREADY_MEMBER_OF_OTHER_PARTY(target.getName()));
			return false;
		}
		else if (target.isInAlliance2()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FORCE_ALREADY_OTHER_FORCE(target.getName()));
			return false;
		}

		return true;
	}
	
	public boolean canInviteToAlliance(Player player, Player target) {
		if (target == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FORCE_NO_USER_TO_INVITE);
			return false;
		}

		if (target.getRace() != player.getRace()
			&& !GroupConfig.ALLIANCE_INVITEOTHERFACTION) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_CANT_INVITE_OTHER_RACE);
			return false;
		}

		final com.aionemu.gameserver.model.team2.alliance.PlayerAlliance alliance = player.getPlayerAlliance2();

		if (target.isInAlliance2()) {
			if (target.getPlayerAlliance2() == alliance) {
				PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_PARTY_ALLIANCE_HE_IS_ALREADY_MEMBER_OF_OUR_ALLIANCE(target.getName()));
				return false;
			}
			else {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FORCE_ALREADY_OTHER_FORCE(target.getName()));
				return false;
			}
		}

		if (alliance != null && alliance.isFull()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_ALLIANCE_CANT_ADD_NEW_MEMBER);
			return false;
		}

		if (alliance != null && !alliance.isSomeCaptain(player)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_PARTY_ALLIANCE_ONLY_PARTY_LEADER_CAN_LEAVE_ALLIANCE);
			return false;
		}

		if (target.sameObjectId(player.getObjectId())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FORCE_CAN_NOT_INVITE_SELF);
			return false;
		}

		if (target.getLifeStats().isAlreadyDead()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_UI_PARTY_DEAD);
			return false;
		}

		if (player.getLifeStats().isAlreadyDead()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FORCE_CANT_INVITE_WHEN_DEAD);
			return false;
		}

		if (target.isInGroup2()) {
			PlayerGroup targetGroup = target.getPlayerGroup2();
			if (alliance != null && (targetGroup.size() + alliance.size() >= 24)) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FORCE_INVITE_FAILED_NOT_ENOUGH_SLOT);
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean canAttack(Player player, VisibleObject target) {
		if (target == null)
			return false;

		if (!checkFly(player, target)) {
			return false;
		}

		if (!(target instanceof Creature))
			return false;

		Creature creature = (Creature) target;

		if (creature.getLifeStats().isAlreadyDead() || creature.getLifeStats().isAboutToDie())
			return false;

		//cannot attack while transformed
		if (player.getTransformModel().getRes3() == 1) {
			return false;
		}
		
		return player.isEnemy(creature);
	}

	@Override
	public boolean canUseWarehouse(Player player) {
		if (player == null || !player.isOnline())
			return false;

		// TODO retail message to requestor and player
		if (player.isTrading())
			return false;

		return true;
	}

	@Override
	public boolean canTrade(Player player) {
		if (player == null || !player.isOnline())
			return false;

		if (player.isTrading()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_EXCHANGE_PARTNER_IS_EXCHANGING_WITH_OTHER);
			return false;
		}

		return true;
	}

	@Override
	public boolean canChat(Player player) {
		if (player == null || !player.isOnline())
			return false;

		return !player.isGagged();
	}

	@Override
	public boolean canUseItem(Player player, Item item) {
		if (player == null || !player.isOnline())
			return false;

		if (player.getLifeStats().isAboutToDie())
			return false;
		
		if (player.getEffectController().isInAnyAbnormalState(AbnormalState.CANT_ATTACK_STATE)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_CAN_NOT_USE_ITEM_WHILE_IN_ABNORMAL_STATE);
			return false;
		}
		
		//cannot use item while transformed
		if (player.getTransformModel().getRes5() == 1) {
			//client sends message by itself
			return false;
		}
		
		if (player.getStore() != null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_ITEM_IS_NOT_USABLE);
			return false;
		}
		
		ItemUseLimits limits = item.getItemTemplate().getUseLimits();
		if (limits.getGenderPermitted() != null && limits.getGenderPermitted() != player.getGender()) {
			PacketSendUtility.sendPacket(player, STR_CANNOT_USE_ITEM_INVALID_GENDER);
			return false;
		}

		if (item.getItemTemplate().hasAreaRestriction()) {
			ZoneName restriction = item.getItemTemplate().getUseArea();
			if (restriction == ZoneName.get("_ABYSS_CASTLE_AREA_")) {
				boolean isInFortZone = false;
				for (ZoneInstance zone : player.getPosition().getMapRegion().getZones(player)) {
					if (zone.getZoneTemplate().getZoneType().equals(ZoneClassName.FORT)) {
						isInFortZone = true;
						break;
					}
				}
				if (!isInFortZone) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300143));
					return false;
				}
			}
			else if (restriction != null && !player.isInsideItemUseZone(restriction)) {
				// You cannot use that item here.
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300143));
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean canChangeEquip(Player player) {
		if (player.getController().isUnderStance()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_CAN_NOT_EQUIP_ITEM_WHILE_IN_CURRENT_STANCE);
			return false;
		}
		if (player.getEffectController().isInAnyAbnormalState(AbnormalState.CANT_ATTACK_STATE)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_SKILL_CAN_NOT_EQUIP_ITEM_WHILE_IN_ABNORMAL_STATE);
			return false;
		}
		return true;
	}
	
	@Override
	public boolean canFly(Player player) {
		if (player.getAccessLevel() < AdminConfig.GM_FLIGHT_FREE) {
			if (!player.isInsideZoneType(ZoneType.FLY)) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FLYING_FORBIDDEN_HERE);
				return false;
			}
		}
		// If player is under NoFly Effect, show the retail message for it and return
		if (player.isUnderNoFly()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANT_FLY_NOW_DUE_TO_NOFLY);
			return false;
		}
		//cannot fly in transform
		if (player.getTransformModel().getRes6() == 1) {
			//client sends message itself?
			return false;
		}
		//cannot fly while private store is active
		if (player.getStore() != null)
			return false;
		
		return true;
	}
	
	@Override
	public boolean canGlide(Player player) {
		//cannot glide in transform
		if (player.getTransformModel().getRes6() == 1) {
			//client sends message itself?
			return false;
		}
		//cannot glide while private store is active
		if (player.getStore() != null)
				return false;
		
		return true;
	}
	
	@Override
	public boolean canPrivateStore(Player player) {
		if (player.isFlying())
			return false;
		if (player.getLifeStats().isAlreadyDead())
			return false;
		if (player.isInState(CreatureState.RESTING)) {
			return false;
		}
		
		return true;
	}
	
}
