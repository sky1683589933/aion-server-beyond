package com.aionemu.gameserver.model.gameobjects.player;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.HouseScriptsDAO;
import com.aionemu.gameserver.model.gameobjects.Persistable.PersistentState;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.PlayerScript;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.utils.xml.CompressUtil;

/**
 * @author Rolandas
 * @reworked Neon
 */
public class PlayerScripts {

	private static final Logger log = LoggerFactory.getLogger(PlayerScripts.class);
	private static final byte SCRIPT_LIMIT = 8; // max number of active scripts a player can have

	private final int houseObjId;
	private final LinkedHashMap<Integer, PlayerScript> scripts;

	public PlayerScripts(int houseId) {
		this.houseObjId = houseId;
		this.scripts = new LinkedHashMap<>();
	}

	public boolean set(int id, byte[] compressedXML, int uncompressedSize) {
		return set(id, compressedXML, uncompressedSize, true);
	}

	public boolean set(int id, byte[] compressedXML, int uncompressedSize, boolean doStore) {
		if (count() >= getMaxCount() && !scripts.containsKey(id))
			return false;

		String scriptXML = decompressAndValidate(compressedXML, uncompressedSize);

		if (scriptXML == null)
			return false;

		if (doStore) {
			House house = HousingService.getInstance().findHouseOrStudio(houseObjId);
			if (house.getPersistentState() == PersistentState.NEW)
				house.save(); // new houses must be inserted first, due to foreign key constraints
			DAOManager.getDAO(HouseScriptsDAO.class).storeScript(houseObjId, id, scriptXML);
		}

		scripts.put(id, new PlayerScript(compressedXML, uncompressedSize));
		return true;
	}

	public boolean remove(int id) {
		if (!scripts.containsKey(id))
			return false;

		DAOManager.getDAO(HouseScriptsDAO.class).deleteScript(houseObjId, id);

		scripts.remove(id);
		return true;
	}

	public void removeAll() {
		new ArrayList<>(getIds()).forEach(this::remove);
	}

	public PlayerScript get(int id) {
		return scripts.get(id);
	}

	public Set<Integer> getIds() {
		return scripts.keySet();
	}

	public int count() {
		return scripts.size();
	}

	public static int getMaxCount() {
		return SCRIPT_LIMIT;
	}

	private String decompressAndValidate(byte[] compressedXML, int uncompressedSize) {
		String scriptXML = "";

		if (compressedXML != null && compressedXML.length > 0) {
			try {
				scriptXML = CompressUtil.decompress(compressedXML);
			} catch (Exception ex) {
				log.error("New housing script data could not be decompressed", ex);
				return null;
			}
			byte[] bytes = scriptXML.getBytes(StandardCharsets.UTF_16LE);
			if (bytes.length != uncompressedSize) {
				log.error("New housing script data had unexpected file size after decompression: Expected " + uncompressedSize + " bytes, got "
					+ bytes.length + " bytes:\n" + scriptXML);
				return null;
			}
		}

		return scriptXML;
	}
}
