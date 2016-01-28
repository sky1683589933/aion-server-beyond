package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;

/**
 * @author Nemiroff Date: 01.12.2009
 * @modified cura
 */
public class CM_TITLE_SET extends AionClientPacket {

	private int titleId;

	public CM_TITLE_SET(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		titleId = readH();
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (titleId != 0xFFFF)
			if (!player.getTitleList().contains(titleId))
				return;

		player.getTitleList().setDisplayTitle(titleId);
	}
}
