package com.aionemu.gameserver.services.siegeservice;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.callbacks.util.GlobalCallbackHelper;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.SiegeDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.siege.ArtifactLocation;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.siege.GloryPointsRewards;
import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionRank;
import com.aionemu.gameserver.model.templates.siegelocation.SiegeLegionReward;
import com.aionemu.gameserver.model.templates.siegelocation.SiegeReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.abyss.GloryPointsService;
import com.aionemu.gameserver.services.mail.AbyssSiegeLevel;
import com.aionemu.gameserver.services.mail.MailFormatter;
import com.aionemu.gameserver.services.mail.SiegeResult;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.google.common.collect.Lists;

/**
 * Object that controls siege of certain fortress. Siege object is not reusable. New siege = new instance.
 * <p/>
 *
 * @author SoulKeeper
 */
public class FortressSiege extends Siege<FortressLocation> {

   private static final Logger log = LoggerFactory.getLogger("SIEGE_LOG");
   private final AbyssPointsListener addAPListener = new AbyssPointsListener(this);
   private int oldLegionId;

   public FortressSiege(FortressLocation fortress) {
	  super(fortress);
   }

   @Override
   public void onSiegeStart() {
	  if (LoggingConfig.LOG_SIEGE)
		 log.info("[SIEGE] > Siege started. [FORTRESS:" + getSiegeLocationId() + "] [RACE: " + getSiegeLocation().getRace() + "] [LegionId:" + getSiegeLocation().getLegionId() + "]");
	  // Mark fortress as vulnerable
	  getSiegeLocation().setVulnerable(true);

	  // Let the world know where the siege are
	  broadcastState(getSiegeLocation());

	  // Clear fortress from enemys
	  getSiegeLocation().clearLocation();

	  // Register abyss points listener
	  // We should listen for abyss point callbacks that players are earning
	  GlobalCallbackHelper.addCallback(addAPListener);

	  // Remove all and spawn siege NPCs
	  deSpawnNpcs(getSiegeLocationId());
	  spawnNpcs(getSiegeLocationId(), getSiegeLocation().getRace(), SiegeModType.SIEGE);
	  initSiegeBoss();
	  this.oldLegionId = getSiegeLocation().getLegionId();
   }

   @Override
   public void onSiegeFinish() {
	  if (LoggingConfig.LOG_SIEGE) {
		 SiegeRaceCounter winner = getSiegeCounter().getWinnerRaceCounter();
		 if (isBossKilled() && winner != null)
			log.info("[SIEGE] > Siege finished. [FORTRESS:" + getSiegeLocationId() + "] [OLD RACE: " + getSiegeLocation().getRace() + "] [OLD LegionId:" + getSiegeLocation().getLegionId() + "] [NEW RACE: " + winner.getSiegeRace() + "] [NEW LegionId:" + (winner.getWinnerLegionId() == null ? 0 : winner.getWinnerLegionId()) + "]");
		 else
			log.info("[SIEGE] > Siege finished. No winner found [FORTRESS:" + getSiegeLocationId() + "] [RACE: " + getSiegeLocation().getRace() + "] [LegionId:" + getSiegeLocation().getLegionId() + "]");
	  }

	  // Unregister abyss points listener callback
	  // We really don't need to add abyss points anymore
	  GlobalCallbackHelper.removeCallback(addAPListener);

	  // Unregister siege boss listeners
	  // cleanup :)
	  unregisterSiegeBossListeners();

	  // despawn protectors and make fortress invulnerable
	  SiegeService.getInstance().deSpawnNpcs(getSiegeLocationId());
	  getSiegeLocation().setVulnerable(false);
	  getSiegeLocation().setUnderShield(false);

	  // Guardian deity general was not killed, fortress stays with previous
	  if (isBossKilled()) {
		 onCapture();
		 broadcastUpdate(getSiegeLocation());
	  } else {
	  	onDefended();
	  	broadcastState(getSiegeLocation());
	  }

	  SiegeService.getInstance().spawnNpcs(getSiegeLocationId(), getSiegeLocation().getRace(), SiegeModType.PEACE);

		 // Reward players and owning legion
	  // If fortress was not captured by balaur
	  if (SiegeRace.BALAUR != getSiegeLocation().getRace()) {
		 giveRewardsToLegion();
		 giveRewardsToPlayers(getSiegeCounter().getRaceCounter(getSiegeLocation().getRace()));
		 if (GloryPointsRewards.hasRewardForSiege(getSiegeLocationId())) {
			calculateLegionGloryPointsRewards();
			SiegeRace winnerRace = getSiegeLocation().getRace();
			SiegeRace looserRace = winnerRace == SiegeRace.ASMODIANS ? SiegeRace.ELYOS : SiegeRace.ASMODIANS;
			calculateSinglePlayersGloryPoints(getSiegeCounter().getRaceCounter(winnerRace), true);
			calculateSinglePlayersGloryPoints(getSiegeCounter().getRaceCounter(looserRace), false);
		 }
	  }

	  // Update outpost status
	  // Certain fortresses are changing outpost ownership
	  updateOutpostStatusByFortress(getSiegeLocation());

	  // Update data in the DB
	  DAOManager.getDAO(SiegeDAO.class).updateSiegeLocation(getSiegeLocation());

	  if (isBossKilled()) {
		 getSiegeLocation().doOnAllPlayers(new Visitor<Player>() {

			@Override
			public void visit(Player player) {
			   if (SiegeRace.getByRace(player.getRace()) == getSiegeLocation().getRace())
				  QuestEngine.getInstance().onKill(new QuestEnv(getBoss(), player, 0, 0));
			}

		 });
	  }

   }

   public void onCapture() {
	  SiegeRaceCounter winner = getSiegeCounter().getWinnerRaceCounter();

	  try {
		 // Players gain buffs on capture of some fortresses
		 applyWorldBuffs(winner.getSiegeRace(), getSiegeLocation().getRace());
		 for (int i = 1; i < 4; i++) {
			getSiegeLocation().despawnMercenaries(i);
		 }
	  }
	  catch (Exception e) {
		 log.error("Error while despawning mercenaries/applying buffs after capture, location " + getSiegeLocation().getLocationId(), e);
	  }
	  // Set new fortress and artifact owner race
	  getSiegeLocation().setRace(winner.getSiegeRace());
	  getArtifact().setRace(winner.getSiegeRace());
	  
	  if (this.oldLegionId != 0 && GloryPointsRewards.hasRewardForSiege(getSiegeLocationId())) { //make sure holding GP are deducted on Capture
		 int oldLegionGeneral = LegionService.getInstance().getLegionBGeneral(this.oldLegionId);
		 if (oldLegionGeneral != 0) {
			GloryPointsService.decreaseGp(oldLegionGeneral, 1000);
			Legion legion = LegionService.getInstance().getLegion(this.oldLegionId);
			legion.decreaseSiegeGloryPoints(1000);
			PlayerCommonData pcd = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(oldLegionGeneral);
			if (pcd != null) { // who knows? :D
				for (Player p : World.getInstance().getAllPlayers())
					PacketSendUtility.sendPacket(p, SM_SYSTEM_MESSAGE.STR_MSG_GLORY_POINT_LOSE_PERSONAL(pcd.getName(), 1000));
			}
		}
	  }

	  // If new race is balaur
	  if (SiegeRace.BALAUR == winner.getSiegeRace()) {
		 getSiegeLocation().setLegionId(0);
		 getArtifact().setLegionId(0);
	  }
	  else {
		 Integer topLegionId = winner.getWinnerLegionId();
		 getSiegeLocation().setLegionId(topLegionId != null ? topLegionId : 0);
		 getArtifact().setLegionId(topLegionId != null ? topLegionId : 0);
	  }
   }
   
   private void onDefended() {
  	 SiegeRace loserRace = getSiegeLocation().getRace() != SiegeRace.BALAUR ? (getSiegeLocation().getRace() == SiegeRace.ELYOS ? SiegeRace.ASMODIANS : SiegeRace.ELYOS) : null;

  	 try {
  		 // Players gain buffs for successfully defense / failed capture the fortress
  		 applyWorldBuffs(getSiegeLocation().getRace(), loserRace);
  	 }
  	 catch (Exception e) {
  		 log.error("Error while applying buffs after defense, location " + getSiegeLocation().getLocationId(), e);
  	 }
   }

   private void applyWorldBuffs(SiegeRace wRace, SiegeRace lRace) {
	  final int loserSkillId;
	  final int winnerSkillId;
	  final int floc = getSiegeLocation().getLocationId();
	  final Race winningRace = wRace != SiegeRace.BALAUR ? (wRace == SiegeRace.ELYOS ? Race.ELYOS : Race.ASMODIANS) : null;
	  final Race losingRace = lRace != SiegeRace.BALAUR ? (lRace == SiegeRace.ELYOS ? Race.ELYOS : Race.ASMODIANS) : null;

	  switch (floc) {
	  	case 1131: 
	  		loserSkillId = 0;
	  		winnerSkillId = 12147;
	  		break;
	  	case 1132: 
	  		loserSkillId = 0;
	  		winnerSkillId = 12148;
	  		break;
	  	case 1141: 
	  		loserSkillId = 0;
	  		winnerSkillId = 12149;
	  		break;
	  	case 1221:
	  		loserSkillId = 0;
	  		winnerSkillId = 12075;
	  		break;
	  	case 1231:
	  		loserSkillId = 0;
	  		winnerSkillId = 12076;
	  		break;
	  	case 1241:
	  		loserSkillId = 0;
	  		winnerSkillId = 12077;
	  		break;
	  	case 1251:
	  		loserSkillId = 0;
	  		winnerSkillId = 12074;
	  		break;
	  	case 2011:
	  		loserSkillId = 0;
	  		winnerSkillId = 12155;
	  		break;
	  	case 2021:
	  		loserSkillId = 0;
	  		winnerSkillId = 12156;
	  		break;
	  	case 3011:
	  		loserSkillId = 0;
	  		winnerSkillId = 12157;
	  		break;
	  	case 3021:
	  		loserSkillId = 0;
	  		winnerSkillId = 12158;
	  		break;
	  	case 5011:
	  		loserSkillId = 12141;
	  		winnerSkillId = 12135;
	  		break;
	  	case 6011:
	  		loserSkillId = 12142;
	  		winnerSkillId = 12137;
	  		break;
	  	case 6021:
	  		loserSkillId = 12143;
	  		winnerSkillId = 12139;
	  		break;
	  	default:
	  		return;
	  }

	  World.getInstance().doOnAllPlayers(new Visitor<Player>() {

		 @Override
		 public void visit(Player player) {
			 if (floc == 1131 || floc == 1132 || floc == 1141 || floc == 1221 || floc == 1231 || floc == 1241 || floc == 1251) {
				 if (player.getWorldId() == 400010000) {
					 if (winningRace != null && player.getRace().equals(winningRace)) {
						 SkillEngine.getInstance().applyEffectDirectly(winnerSkillId, player, player, 0);
					 }
					 else if (losingRace != null && player.getRace().equals(losingRace) && loserSkillId != 0) {
						 SkillEngine.getInstance().applyEffectDirectly(loserSkillId, player, player, 0);
					 }
				 }
			 } else if (floc == 2011 || floc == 2021 || floc == 3011 || floc == 3021) {
				 if (player.getWorldId() == 220070000 || player.getWorldId() == 600010000 || player.getWorldId() == 210050000) {
					 if (winningRace != null && player.getRace().equals(winningRace)) {
						 SkillEngine.getInstance().applyEffectDirectly(winnerSkillId, player, player, 0);
					 }
					 else if (losingRace != null && player.getRace().equals(losingRace) && loserSkillId != 0) {
						 SkillEngine.getInstance().applyEffectDirectly(loserSkillId, player, player, 0);
					 }
				 }
			 } else if (floc == 5011 || floc == 6011 || floc == 6021) {
				 if (player.getWorldId() == 600050000 || player.getWorldId() == 600060000) {
					 if (winningRace != null && player.getRace().equals(winningRace)) {
						 SkillEngine.getInstance().applyEffectDirectly(winnerSkillId, player, player, 0);
					 }
					 else if (losingRace != null && player.getRace().equals(losingRace) && loserSkillId != 0) {
						 SkillEngine.getInstance().applyEffectDirectly(loserSkillId, player, player, 0);
					 }
				 }
			 } 
		 }		 
	  });
   }

   protected void giveRewardsToLegion() {
	  try {
		 // We do not give rewards if fortress was captured for first time
		 if (isBossKilled()) {
			if (LoggingConfig.LOG_SIEGE)
			   log.info("[SIEGE] > [FORTRESS:" + getSiegeLocationId() + "] [RACE: " + getSiegeLocation().getRace() + "] [LEGION :" + getSiegeLocation().getLegionId() + "] Legion Reward not sending because fortress was captured(siege boss killed).");
			return;
		 }

		 // Legion with id 0 = not exists?
		 if (getSiegeLocation().getLegionId() == 0) {
			if (LoggingConfig.LOG_SIEGE)
			   log.info("[SIEGE] > [FORTRESS:" + getSiegeLocationId() + "] [RACE: " + getSiegeLocation().getRace() + "] [LEGION :" + getSiegeLocation().getLegionId() + "] Legion Reward not sending because fortress not owned by any legion.");
			return;
		 }

		 List<SiegeLegionReward> legionRewards = getSiegeLocation().getLegionReward();
		 int legionBGeneral = LegionService.getInstance().getLegionBGeneral(getSiegeLocation().getLegionId());
		 if (legionBGeneral != 0) {
			PlayerCommonData BGeneral = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(legionBGeneral);
			if (LoggingConfig.LOG_SIEGE) {
			   log.info("[SIEGE] > [FORTRESS:" + getSiegeLocationId() + "] [RACE: " + getSiegeLocation().getRace() + "] Legion Reward in process... LegionId:"
					   + getSiegeLocation().getLegionId() + " General Name:" + BGeneral.getName());
			}
			if (legionRewards != null) {
			   for (SiegeLegionReward medalsType : legionRewards) {
				  if (LoggingConfig.LOG_SIEGE) {
					 log.info("[SIEGE] > [Legion Reward to: " + BGeneral.getName() + "] ITEM RETURN "
							 + medalsType.getItemId() + " ITEM COUNT " + medalsType.getCount() * SiegeConfig.SIEGE_MEDAL_RATE);
				  }
				  MailFormatter.sendAbyssRewardMail(getSiegeLocation(), BGeneral, AbyssSiegeLevel.NONE, SiegeResult.PROTECT, System.currentTimeMillis(), medalsType.getItemId(), medalsType.getCount() * SiegeConfig.SIEGE_MEDAL_RATE, 0);

			   }
			}
		 }
	  }
	  catch (Exception e) {
		 log.error("[SIEGE] Error while calculating legion reward for fortress siege. Location:" + getSiegeLocation().getLocationId(), e);
	  }
   }

   protected void giveRewardsToPlayers(SiegeRaceCounter winnerDamage) {
	  try {
		 // Get the map with playerId to siege reward
		 Map<Integer, Long> playerAbyssPoints = winnerDamage.getPlayerAbyssPoints();
		 List<Integer> topPlayersIds = Lists.newArrayList(playerAbyssPoints.keySet());
		 Map<Integer, String> playerNames = PlayerService.getPlayerNames(playerAbyssPoints.keySet());
		 SiegeResult result = isBossKilled() ? SiegeResult.OCCUPY : SiegeResult.DEFENDER;

		 // Black Magic Here :)
		 int i = 0;
		 List<SiegeReward> playerRewards = getSiegeLocation().getReward();
		 int rewardLevel = 0;
		 for (SiegeReward topGrade : playerRewards) {
			AbyssSiegeLevel level = AbyssSiegeLevel.getLevelById(++rewardLevel);
			for (int rewardedPC = 0; i < topPlayersIds.size() && rewardedPC < topGrade.getTop(); ++i) {
			   Integer playerId = topPlayersIds.get(i);
			   PlayerCommonData pcd = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(playerId);
			   ++rewardedPC;
			   if (LoggingConfig.LOG_SIEGE) {
				  log.info("[SIEGE]  > [FORTRESS:" + getSiegeLocationId() + "] [RACE: " + getSiegeLocation().getRace() + "] Player Reward to: " + playerNames.get(playerId) + "] ITEM RETURN "
						  + topGrade.getItemId() + " ITEM COUNT " + topGrade.getCount() * SiegeConfig.SIEGE_MEDAL_RATE);
			   }
			   MailFormatter.sendAbyssRewardMail(getSiegeLocation(), pcd, level, result, System.currentTimeMillis(), topGrade.getItemId(), topGrade.getCount() * SiegeConfig.SIEGE_MEDAL_RATE, 0);
			}
		 }
		 if (!isBossKilled()) {
			while (i < topPlayersIds.size()) {
			   Integer playerId = topPlayersIds.get(i);
			   PlayerCommonData pcd = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(playerId);
			   //Send Announcement Mails without reward to the rest
			   MailFormatter.sendAbyssRewardMail(getSiegeLocation(), pcd, AbyssSiegeLevel.NONE, SiegeResult.EMPTY, System.currentTimeMillis(), 0, 0, 0);
			   i++;
			}
		 }
	  }
	  catch (Exception e) {
		 log.error("[SIEGE] Error while calculating player reward for fortress siege. Location:" + getSiegeLocation().getLocationId(), e);
	  }
   }

   protected void calculateLegionGloryPointsRewards() {
	  try {
		 int winnerLegionId = getSiegeLocation().getLegionId();
		 if (winnerLegionId == 0)
			return;
		 int legionBGeneral = LegionService.getInstance().getLegionBGeneral(winnerLegionId);

		 boolean defenceSuccessful = winnerLegionId == this.oldLegionId;
		 if (defenceSuccessful) {
			if (legionBGeneral != 0) {
			   List<Integer> deputies = LegionService.getInstance().getMembersByRank(winnerLegionId, LegionRank.DEPUTY);
			   int gpReward = Math.round(500 / (1 + deputies.size()));
			   GloryPointsService.increaseGp(legionBGeneral, gpReward);
			   for (int playerObjId : deputies) {
				  GloryPointsService.increaseGp(playerObjId, gpReward);
			   }
			}
		 }
		 else {
			if (legionBGeneral != 0) {
			   GloryPointsService.increaseGp(legionBGeneral, 1000, false);
			   Legion legion = LegionService.getInstance().getLegion(winnerLegionId);
			   legion.increaseSiegeGloryPoints(1000);
			}
		 }
	  }
	  catch (Exception e) {
		 log.error("Error while calculating glory points reward for fortress siege.", e);
	  }
   }

   protected void calculateSinglePlayersGloryPoints(SiegeRaceCounter damage, boolean isWinner) {
	  try {
		 Map<Integer, Long> playerAbyssPoints = damage.getPlayerAbyssPoints();
		 List<Integer> topPlayersIds = Lists.newArrayList(playerAbyssPoints.keySet());
		 int i = 0;
		 int rewardLevel = 0;
		 for (int j = 0; j < 4; j++) {
			++rewardLevel;
			GloryPointsRewards reward = GloryPointsRewards.getReward(getSiegeLocation().getLocationId(), rewardLevel);
			if (reward == null)
			   break;
			for (int rewardedPC = 0; i < topPlayersIds.size() && rewardedPC < reward.getPlayersCount(); ++i) {
			   Integer playerId = topPlayersIds.get(i);
			   GloryPointsService.increaseGp(playerId, isWinner ? reward.getGpForWin() : reward.getGpForLost());
			   ++rewardedPC;
			}
		 }
	  }
	  catch (Exception e) {
		 log.error("Error while calculating glory points reward for fortress siege.", e);
	  }
   }

   @Override
   public boolean isEndless() {
	  return false;
   }

   @Override
   public void addAbyssPoints(Player player, int abysPoints) {
	  getSiegeCounter().addAbyssPoints(player, abysPoints);
   }

   protected ArtifactLocation getArtifact() {
	  return SiegeService.getInstance().getFortressArtifacts().get(getSiegeLocationId());
   }

   protected boolean hasArtifact() {
	  return getArtifact() != null;
   }

}
