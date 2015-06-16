package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Iterator;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * @author -Nemesiss-
 */
public class SM_L2AUTH_LOGIN_CHECK extends AionServerPacket {

	/**
	 * True if client is authed.
	 */
	private final boolean ok;
	private final String accountName;
	private static byte[] standardData;
	@SuppressWarnings("unused")
	private static byte[] fastTrackData;

	static {
		standardData = hex2Byte(
			  "0000000000000001010102020203030304040405050506060607070708080809"
			+ "09090A0A0A0B0B0B0C0C0C0D0D0D0E0E0E0F0F0F101010111111121212131313"
			+ "1414141515151616161717171818181919191A1A1A1B1B1B1C1C1C1D1D1D1E1E"
			+ "1E1F1F1F20202021212122222223232324242425252526262627272728282829"
			+ "29292A2A2A2B2B2B2C2C2C2D2D2D2E2E2E2F2F2F303030313131323232000000"
			+ "0000000000000000000000000000000000000000000000000000000000000000"
			+ "00000000000000000000423D3D00000000000000000000000000000000000000"
			+ "0000000000000000000000000000000000000000000000000000000000000000"
			+ "0000000000000000000000000000000000000000000000000000000000000000"
			+ "0000000000000000000000000000000000000000000000000000000000000000"
			+ "0000000000000000000000000000000000000000000000000000000000000000"
			+ "0000000000000000000000000000000000000000000000000000000000000000"
			+ "0000000000000001010102020203030304040405050506060607070708080809"
			+ "09090A0A0A0B0B0B0C0C0C0D0D0D0E0E0E0F0F0F101010111111121212131313"
			+ "1414141515151616161717171818181919191A1A1A1B1B1B1C1C1C1D1D1D1E1E"
			+ "1E1F1F1F20202021212122222223232324242425252526262627272728282829"
			+ "29292A2A2A2B2B2B2C2C2C2D2D2D2E2E2E2F2F2F303030313131323232000000"
			+ "000000000000000000000000000000000000000000000000000000423D3D0000"
			+ "00000000");
		
		fastTrackData = hex2Byte(
			  "00010101000000010101020202030303040404050505060606070707080808090"
			+ "9090A0A0A0B0B0B0C0C0C0D0D0D0E0E0E0F0F0F101010111111121212131313"
			+ "1414141515151616161717171818181919191A1A1A1B1B1B1C1C1C1D1D1D1E1"
			+ "E1E1F1F1F202020212121222222232323242424252525262626272727282828"
			+ "2929292A2A2A2B2B2B2C2C2C2D2D2D2E2E2E2F2F2F303030313131323232000"
			+ "000000000000000000000000000000000000000000000000000000000000000"
			+ "000000000000000000000000423D3D000000000000000000000000000000000"
			+ "000000000000000000000000000000000000000000000000000000000000000"
			+ "000000000000000000000000000000000000000000000000000000000000000"
			+ "000000000000000000000000000000000000000000000000000000000000000"
			+ "000000000000000000000000000000000000000000000000000000000000000"
			+ "000000000000000000000000000000000000000000000000000000000000000"
			+ "000000000000000000000000010101020202030303040404050505060606070"
			+ "7070808080909090A0A0A0B0B0B0C0C0C0D0D0D0E0E0E0F0F0F101010111111"
			+ "1212121313131414141515151616161717171818181919191A1A1A1B1B1B1C1"
			+ "C1C1D1D1D1E1E1E1F1F1F202020212121222222232323242424252525262626"
			+ "2727272828282929292A2A2A2B2B2B2C2C2C2D2D2D2E2E2E2F2F2F303030313"
			+ "131323232000000000000000000000000000000000000000000000000000000"
			+ "000000423D3D000000000000");
	}

	/**
	 * Constructs new <tt>SM_L2AUTH_LOGIN_CHECK </tt> packet
	 * 
	 * @param ok
	 */
	public SM_L2AUTH_LOGIN_CHECK(boolean ok, String accountName) {
		this.ok = ok;
		this.accountName = accountName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(ok ? 0x00 : 0x01);
		writeB(standardData);
		writeH(DataManager.WORLD_MAPS_DATA.size());
		Iterator<WorldMapTemplate> iter = DataManager.WORLD_MAPS_DATA.iterator();
		while (iter.hasNext()) {
			WorldMapTemplate template = iter.next();
			writeD(template.getMapId());
			if (template.isInstance())
				writeH(0);
			else
				writeH(template.getTwinCount()); // for FastTrack it is getBeginnerTwinCount()
		}
		writeS(accountName);
	}

	private static byte[] hex2Byte(String str) {
		byte[] bytes = new byte[str.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16);
		}
		return bytes;
	}
}
