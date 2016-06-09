package com.aionemu.gameserver.services.siege;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javolution.util.FastTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.LoggingConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.assemblednpc.AssembledNpc;
import com.aionemu.gameserver.model.assemblednpc.AssembledNpcPart;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.siege.ArtifactLocation;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.templates.assemblednpc.AssembledNpcTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_NPC_ASSEMBLER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;

/**
 * @author synchro2
 * @reworked Luzien
 * @modified Whoop TODO: Send Peace Dredgion without assault
 */
public class BalaurAssaultService {

	private static final BalaurAssaultService instance = new BalaurAssaultService();
	private Logger log = LoggerFactory.getLogger("SIEGE_LOG");
	private final Map<Integer, FortressAssault> fortressAssaults = new ConcurrentHashMap<>();

	private final Map<Integer, ArtifactAssault> artifactAssaults = new ConcurrentHashMap<>();

	public static BalaurAssaultService getInstance() {
		return instance;
	}

	public void onSiegeStart(final Siege<?> siege) {
		if (siege instanceof FortressSiege) {
			if (!calculateFortressAssault(((FortressSiege) siege).getSiegeLocation()))
				return;
			newAssault(siege, Rnd.get(60, 900)); // between 1 and 15 minutes
		} else if (siege instanceof ArtifactSiege) {
			if (!calculateArtifactAssault(((ArtifactSiege) siege).getSiegeLocation()))
				return;
			newAssault(siege, Rnd.get(180, 2880)); // between 3 and 48 hours
		} else
			return;
		if (LoggingConfig.LOG_SIEGE)
			log.info("[SIEGE] Balaur Assault scheduled on Siege ID: " + siege.getSiegeLocationId() + "!");
	}

	public void onSiegeFinish(Siege<?> siege) {
		int locId = siege.getSiegeLocationId();
		if (fortressAssaults.containsKey(locId)) {
			Boolean bossIsKilled = siege.isBossKilled();
			fortressAssaults.get(locId).finishAssault(bossIsKilled);
			if (bossIsKilled && siege.getSiegeLocation().getRace().equals(SiegeRace.BALAUR))
				log.info("[SIEGE] > [FORTRESS:" + siege.getSiegeLocationId() + "] has been captured by Balaur Assault!");
			else
				log.info("[SIEGE] > [FORTRESS:" + siege.getSiegeLocationId() + "] Balaur Assault finished without capture!");
			fortressAssaults.remove(locId);
		} else if (artifactAssaults.containsKey(locId)) {
			Boolean bossIsKilled = siege.isBossKilled();
			artifactAssaults.get(locId).finishAssault(bossIsKilled);
			if (bossIsKilled && siege.getSiegeLocation().getRace().equals(SiegeRace.BALAUR))
				log.info("[SIEGE] > [ARTIFACT:" + siege.getSiegeLocationId() + "] has been captured by Balaur Assault!");
			else {
				log.info("[SIEGE] > [ARTIFACT:" + siege.getSiegeLocationId() + "] Balaur Assault finished without capture!");
			}
			artifactAssaults.remove(locId);
		}
	}

	private boolean calculateFortressAssault(FortressLocation fortress) {
		if (fortress.getRace() == SiegeRace.BALAUR || !fortress.isVulnerable())
			return false;

		boolean isBalaurea = fortress.getWorldId() == 210050000 || fortress.getWorldId() == 220070000;

		if (fortressAssaults.containsKey(fortress.getLocationId()))
			return false;

		int count = 0; // Allow only 2 Balaur attacks per map, 1 per Balaurea map
		for (FortressAssault fa : fortressAssaults.values()) {
			if (fa.getWorldId() == fortress.getWorldId())
				count++;
		}
		if (count >= (isBalaurea ? 1 : 2))
			return false;

		float influence = fortress.getRace() == SiegeRace.ASMODIANS ? Influence.getInstance().getGlobalAsmodiansInfluence() : Influence.getInstance()
			.getGlobalElyosInfluence();

		if (Rnd.get() >= influence * SiegeConfig.BALAUR_ASSAULT_RATE)
			return false;

		return true;
	}

	private boolean calculateArtifactAssault(ArtifactLocation artifact) {
		int locationId = artifact.getLocationId();

		if (artifactAssaults.containsKey(locationId) || artifact.getRace() == SiegeRace.BALAUR)
			return false;

		return true;
	}

	public void startAssault(Player player, int location, int delay) {
		if (fortressAssaults.containsKey(location) || artifactAssaults.containsKey(location)) {
			PacketSendUtility.sendMessage(player, "Assault on " + location + " was already started");
			return;
		}

		newAssault(SiegeService.getInstance().getSiege(location), delay);
	}

	private void newAssault(Siege<?> siege, int delay) {
		if (siege instanceof FortressSiege) {
			FortressAssault assault = new FortressAssault((FortressSiege) siege);
			assault.startAssault(delay);
			fortressAssaults.put(siege.getSiegeLocationId(), assault);
		} else if (siege instanceof ArtifactSiege) {
			ArtifactAssault assault = new ArtifactAssault((ArtifactSiege) siege);
			assault.startAssault(delay);
			artifactAssaults.put(siege.getSiegeLocationId(), assault);
		}
	}

	public void spawnDredgion(int spawnId) {
		AssembledNpcTemplate template = DataManager.ASSEMBLED_NPC_DATA.getAssembledNpcTemplate(spawnId);
		FastTable<AssembledNpcPart> assembledPatrs = new FastTable<AssembledNpcPart>();
		for (AssembledNpcTemplate.AssembledNpcPartTemplate npcPart : template.getAssembledNpcPartTemplates()) {
			assembledPatrs.add(new AssembledNpcPart(IDFactory.getInstance().nextId(), npcPart));
		}

		AssembledNpc npc = new AssembledNpc(template.getRouteId(), template.getMapId(), template.getLiveTime(), assembledPatrs);
		Iterator<Player> iter = World.getInstance().getPlayersIterator();
		Player findedPlayer;
		while (iter.hasNext()) {
			findedPlayer = iter.next();
			PacketSendUtility.sendPacket(findedPlayer, new SM_NPC_ASSEMBLER(npc));
			PacketSendUtility.sendPacket(findedPlayer, SM_SYSTEM_MESSAGE.STR_ABYSS_CARRIER_SPAWN());
		}
	}
}