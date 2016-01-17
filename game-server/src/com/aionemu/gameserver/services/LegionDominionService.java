package com.aionemu.gameserver.services;

import java.util.Collection;
import java.util.Map;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.LegionDominionDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.legionDominion.LegionDominionLocation;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * @author Yeats
 *
 */
public class LegionDominionService {

	private static final LegionDominionService instance = new LegionDominionService();
	
	private Map<Integer, LegionDominionLocation> legionDominionLocations;
	
	public static LegionDominionService getInstance() {
		return instance;
	}
	
	public void initLocations() {
		legionDominionLocations = DataManager.LEGION_DOMINION_DATA.getLegionDominionLocations();
		DAOManager.getDAO(LegionDominionDAO.class).loadLegionDominionLocations(legionDominionLocations);
		for (LegionDominionLocation loc : legionDominionLocations.values()) {
			loc.setParticipantInfo(DAOManager.getDAO(LegionDominionDAO.class).loadParticipants(loc));
			System.out.println("Loaded participant info for: " + loc.getLocationId());
		}
	}
	
	public Collection<LegionDominionLocation> getLegionDominions() {
		return legionDominionLocations.values();
	}
	
	public LegionDominionLocation getLegionDominionLoc(int locId) {
		return legionDominionLocations.get(locId);
	}

	/**
	 * @param legion
	 * @param locId
	 * @return
	 */
	public boolean join(int legionId, int locId) {
		return legionDominionLocations.get(locId).join(legionId);
	}

	/**
	 * @param locId
	 * @return
	 */
	public DescriptionId getNameDesc(int locId) {
		return new DescriptionId( 2 * legionDominionLocations.get(locId).getNameId() + 2);
	}

	public LegionDominionLocation getLegionDominionByZone(String zoneName) {
		for (LegionDominionLocation loc : legionDominionLocations.values()) {
			if (loc.getZoneNameAsString().equalsIgnoreCase(zoneName)) {
				return loc;
			}
		}
		return null;
	}
}
