package com.aionemu.gameserver.services.teleport;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.hotspot.HotspotTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_TELEPORT;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * @author ViAl
 */
public class BindPointTeleportService {

	private static final int COOLDOWN_IN_SECONDS = 600; // 10 mins
	/**
	 * player id - cooldown
	 */
	private static final Map<Integer, Cooldown> cooldowns = new HashMap<Integer, Cooldown>();

	public static void onLogin(Player player) {
		Cooldown cooldown = getCooldown(player);
		if (cooldown != null && cooldown.getTimeLeft() > 0)
			PacketSendUtility.broadcastPacketAndReceive(player,
				new SM_BIND_POINT_TELEPORT(3, player.getObjectId(), cooldown.getLocId(), cooldown.getTimeLeft()));
	}

	public static void teleport(Player player, int locId, long kinah) {
		HotspotTemplate hotspot = DataManager.HOTSPOT_DATA.getHotspotTemplateById(locId);
		if (hotspot == null) {
			AuditLogger.info(player, "Try to use null Hotspot #" + locId);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE());
			return;
		}
		final long price = computePrice(player, hotspot, kinah);

		if (!checkRequirements(player, hotspot, price))
			return;

		PacketSendUtility.broadcastPacket(player, new SM_BIND_POINT_TELEPORT(1, player.getObjectId(), locId, 0), true);

		player.getController().addTask(TaskId.SKILL_USE, ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				if (!player.getInventory().tryDecreaseKinah(price, ItemPacketService.ItemUpdateType.DEC_KINAH_FLY)) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE());
					return;
				}
				addCooldown(player, locId);
				PacketSendUtility.broadcastPacket(player, new SM_BIND_POINT_TELEPORT(3, player.getObjectId(), locId, COOLDOWN_IN_SECONDS), true);
				ThreadPoolManager.getInstance().schedule(new Runnable() {
					@Override
					public void run() {
						if (player != null && !player.getLifeStats().isAboutToDie() && !player.getLifeStats().isAlreadyDead())
							TeleportService2.teleportTo(player, hotspot.getWorldId(), hotspot.getX(), hotspot.getY(), hotspot.getZ());
					}
				}, 1000);
			}
		}, 10000));
	}

	public static void cancelTeleport(Player player, int locId) {
		if (player.getController().hasTask(TaskId.SKILL_USE)) {
			player.getController().cancelTask(TaskId.SKILL_USE);
			PacketSendUtility.broadcastPacket(player, new SM_BIND_POINT_TELEPORT(2, player.getObjectId(), locId, 0), true);
		}
	}

	private static long computePrice(Player player, HotspotTemplate hotspot, long kinah) {
		long price = kinah > hotspot.getPrice() ? kinah : hotspot.getPrice();
		if (player.getController().isHiPassInEffect() || price < 1)
			price = 1;
		return price;
	}

	private static boolean checkRequirements(Player player, HotspotTemplate hotspot, long price) {
		if (player.getWorldId() != hotspot.getWorldId()) {
			AuditLogger.info(player, "Try to use Hotspot #" + hotspot.getId() + " from not native start world " + player.getWorldId() + ". expected "
				+ hotspot.getWorldId());
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE());
			return false;
		}
		if (!(player.getRace() == Race.PC_ALL) && player.getRace() != hotspot.getRace()) {
			AuditLogger.info(player, "Try to use Hotspot #" + hotspot.getId() + " from race " + player.getRace() + ". expected " + hotspot.getRace());
			return false;
		}
		if (player.getInventory().getKinah() < price) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NOT_ENOUGH_FEE());
			return false;
		}
		Cooldown cooldown = getCooldown(player);
		if (cooldown != null && cooldown.getTimeLeft() > 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_FLYING_TIME_NOT_READY());
			return false;
		}

		return true;
	}

	private static void addCooldown(Player player, int locId) {
		long cooldown = System.currentTimeMillis() + COOLDOWN_IN_SECONDS * 1000;
		cooldowns.put(player.getObjectId(), new Cooldown(locId, cooldown));
	}

	private static Cooldown getCooldown(Player player) {
		return cooldowns.get(player.getObjectId());
	}

	private static class Cooldown {

		int locId;
		long cdEnd;

		protected Cooldown(int locId, long cdEnd) {
			this.locId = locId;
			this.cdEnd = cdEnd;
		}

		protected int getLocId() {
			return locId;
		}

		protected int getTimeLeft() {
			int estimated = (int) ((cdEnd - System.currentTimeMillis()) / 1000);
			if (estimated > 0)
				return estimated;
			else
				return 0;
		}
	}

}
