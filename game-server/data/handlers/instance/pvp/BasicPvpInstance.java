package instance.pvp;

import static com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.Rates;
import com.aionemu.gameserver.model.instance.InstanceProgressionType;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.PvpInstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.PvpInstancePlayerReward;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.instanceinfo.PvpInstanceScoreInfo;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.services.abyss.GloryPointsService;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * Super class for all newer PvP instances introduced by 4.x updates:<br>
 * - Idgel Dome<br>
 * - Engulfed Ophidan Bridge<br>
 * - Kamar Battlefield<br>
 * - Iron Wall Warfront
 *
 * @author Estrayl
 */
public class BasicPvpInstance extends GeneralInstanceHandler {

	protected final List<Future<?>> tasks = new ArrayList<>();
	protected final int raceStartPosition = Rnd.get(2);
	protected PvpInstanceReward<PvpInstancePlayerReward> pInstanceReward;
	protected long startTime;

	public BasicPvpInstance(WorldMapInstance instance) {
		super(instance);
	}

	@Override
	public void onInstanceCreate() {
		if (pInstanceReward == null)
			return;

		pInstanceReward.addPointsByRace(Race.ELYOS, 1000);
		pInstanceReward.addPointsByRace(Race.ASMODIANS, 1000);
		updateProgress(InstanceProgressionType.REINFORCE_MEMBER);
		tasks.add(ThreadPoolManager.getInstance().schedule(this::startPreparation, getReinforceMemberPhaseDelay()));
	}

	private void startPreparation() {
		updateProgress(InstanceProgressionType.PREPARING);
		tasks.add(ThreadPoolManager.getInstance().schedule(this::onStart, 60000));
	}

	protected void onStart() {

	}

	protected void onStop(boolean isBossKilled) {
		cancelTasks();
		updateProgress(InstanceProgressionType.END_PROGRESS);

		Race winningRace = pInstanceReward.getRaceWithHighestPoints();
		instance.forEachPlayer(p -> setAndDistributeRewards(p, pInstanceReward.getPlayerReward(p.getObjectId()), winningRace, isBossKilled));
		instance.forEachNpc(npc -> npc.getController().delete());
		tasks.add(ThreadPoolManager.getInstance().schedule(() -> instance.getPlayersInside().forEach(this::revivePlayerOnEnd), 10000));
		tasks.add(ThreadPoolManager.getInstance().schedule(() -> instance.getPlayersInside().forEach(this::onExitInstance), 60000));
	}

	protected void setAndDistributeRewards(Player player, PvpInstancePlayerReward reward, Race winningRace, boolean isBossKilled) {

	}

	protected void distributeRewards(Player player, PvpInstancePlayerReward reward) {
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(instance.getMapId(),
			new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.SHOW_REWARD, player.getObjectId(), 0), getTime()));
		AbyssPointsService.addAp(player, (int) Rates.AP_DREDGION.calcResult(player, reward.getBaseAp() + reward.getBonusAp()));
		int gpToAdd = reward.getBaseGp() + reward.getBonusGp();
		if (gpToAdd > 0)
			GloryPointsService.increaseGpBy(player.getObjectId(), gpToAdd);
		if (reward.getReward1ItemId() > 0)
			ItemService.addItem(player, reward.getReward1ItemId(), reward.getReward1Count() + reward.getReward1BonusCount(), true);
		if (reward.getReward2ItemId() > 0)
			ItemService.addItem(player, reward.getReward2ItemId(), reward.getReward2Count() + reward.getReward2BonusCount(), true);
		if (reward.getReward3ItemId() > 0)
			ItemService.addItem(player, reward.getReward3ItemId(), reward.getReward3Count(), true);
		if (reward.getReward4ItemId() > 0)
			ItemService.addItem(player, reward.getReward4ItemId(), reward.getReward4Count(), true);
		if (reward.getBonusRewardItemId() > 0)
			ItemService.addItem(player, reward.getBonusRewardItemId(), reward.getBonusRewardCount(), true);
	}

	@Override
	public void onExitInstance(Player player) {
		if (player.getPosition().getMapId() != mapId) // Check if player has not already left
			return;

		TeleportService.moveToInstanceExit(player, mapId, player.getRace());
		sendPacket(new SM_INSTANCE_SCORE(instance.getMapId(),
			new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.PLAYER_QUIT, player.getObjectId(), 0), getTime()));
	}

	@Override
	public boolean onDie(Player victim, Creature lastAttacker) {
		sendPacket(new SM_INSTANCE_SCORE(instance.getMapId(),
			new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.UPDATE_PLAYER_BUFF_STATUS, victim.getObjectId(), 60), getTime()));
		PacketSendUtility.sendPacket(victim, new SM_DIE(victim.canUseRebirthRevive(), false, 0, 8));
		if (lastAttacker instanceof Player killer && killer.getRace() != victim.getRace()) {
			int killPoints = 200;
			if (pInstanceReward.isStartProgress() && getTime() <= 600000 && victim.getRace() != pInstanceReward.getRaceWithHighestPoints())
				killPoints += 100; // After 10 minutes the outplayed faction gets bonus points

			if (victim.getAbyssRank().getRank().getId() - killer.getAbyssRank().getRank().getId() >= 4)
				killPoints *= 1.6f;

			updatePoints(killer, killer.getRace(), null, killPoints, false, true);
		}

		updatePoints(victim, victim.getRace(), null, -100, true, false);
		return true;
	}

	@Override
	public boolean onReviveEvent(Player player) {
		PacketSendUtility.sendPacket(player, STR_REBIRTH_MASSAGE_ME());
		PlayerReviveService.revive(player, 100, 100, false, 0);
		player.getGameStats().updateStatsAndSpeedVisually();
		portToStartPosition(player);
		sendPacket(new SM_INSTANCE_SCORE(instance.getMapId(),
			new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.UPDATE_PLAYER_BUFF_STATUS, player.getObjectId(), 0), getTime()));
		return true;
	}

	@Override
	public void onPlayerLogOut(Player player) {
		if (player.isDead())
			onReviveEvent(player);
	}

	@Override
	public void onEnterInstance(Player player) {
		if (!pInstanceReward.containsPlayer(player.getObjectId()))
			pInstanceReward.addPlayerReward(new PvpInstancePlayerReward(player.getObjectId(), player.getRace()));

		sendPacket(new SM_INSTANCE_SCORE(instance.getMapId(),
			new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.INIT_PLAYER, player.getObjectId(), 0), getTime()));

		sendPacket(new SM_INSTANCE_SCORE(instance.getMapId(),
			new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.UPDATE_PLAYER_BUFF_STATUS, player.getObjectId(), 0), getTime()));
	}

	@Override
	public void onLeaveInstance(Player player) {
		PvpInstancePlayerReward reward = pInstanceReward.getPlayerReward(player.getObjectId());
		if (reward != null)
			pInstanceReward.removePlayerReward(reward);
		sendPacket(new SM_INSTANCE_SCORE(instance.getMapId(),
			new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.PLAYER_QUIT, player.getObjectId(), 0), getTime()));
	}

	@Override
	public void onInstanceDestroy() {
		cancelTasks();
	}

	protected int getReinforceMemberPhaseDelay() {
		return 60000;
	}

	protected int getTime() {
		int current = (int) (System.currentTimeMillis() - startTime);
		return switch (pInstanceReward.getInstanceProgressionType()) {
			case REINFORCE_MEMBER -> 120000 - current;
			case PREPARING -> 60000 - current;
			case START_PROGRESS, END_PROGRESS -> 1200000 - current;
		};
	}

	protected void updateProgress(InstanceProgressionType progressionType) {
		pInstanceReward.setInstanceProgressionType(progressionType);
		startTime = System.currentTimeMillis(); // Reset start time
		sendPacket(
			new SM_INSTANCE_SCORE(instance.getMapId(), new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.UPDATE_INSTANCE_PROGRESS), getTime()));
		sendPacket(new SM_INSTANCE_SCORE(instance.getMapId(),
			new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.UPDATE_ALL_PLAYER_INFO, instance.getPlayersInside()), getTime()));
		sendPacket(new SM_INSTANCE_SCORE(instance.getMapId(),
			new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.UPDATE_INSTANCE_BUFFS_AND_SCORE, instance.getPlayersInside()), getTime()));
	}

	protected void updatePoints(Player player, Race race, String npcL10n, int points) {
		updatePoints(player, race, npcL10n, points, false, false);
	}

	protected void updatePoints(Player player, Race race, String npcL10n, int points, boolean isVictim, boolean shouldRewardPvpKill) {
		if (!pInstanceReward.isStartProgress())
			return;

		pInstanceReward.addPointsByRace(race, points);
		if (player != null) {
			PvpInstancePlayerReward pReward = pInstanceReward.getPlayerReward(player.getObjectId());
			pReward.addPoints(points);
			if (isVictim) {
				sendPacket(SM_SYSTEM_MESSAGE.STR_MSG_LOSE_SCORE_ENEMY(player.getName(), race.getL10n(), points));
			} else {
				if (shouldRewardPvpKill) {
					pInstanceReward.incrementKillsByRace(race);
					pReward.addPvPKillToPlayer();
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GET_SCORE_FOR_ENEMY(points));
				} else if (npcL10n != null) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_GET_SCORE(npcL10n, points));
				}
			}
		}
		sendPacket(
			new SM_INSTANCE_SCORE(instance.getMapId(), new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.UPDATE_FACTION_SCORE, race), getTime()));
		sendPacket(new SM_INSTANCE_SCORE(instance.getMapId(),
			new PvpInstanceScoreInfo(pInstanceReward, InstanceScoreType.UPDATE_ALL_PLAYER_INFO, instance.getPlayersInside()), getTime()));
	}

	protected void revivePlayerOnEnd(Player player) {
		if (player.isDead())
			PlayerReviveService.duelRevive(player);
	}

	protected void sendPacket(AionServerPacket packet) {
		PacketSendUtility.broadcastToMap(instance, packet);
	}

	protected void cancelTasks() {
		for (Future<?> task : tasks)
			if (task != null && !task.isCancelled())
				task.cancel(true);
	}

	@Override
	public InstanceReward<?> getInstanceReward() {
		return pInstanceReward;
	}
}
