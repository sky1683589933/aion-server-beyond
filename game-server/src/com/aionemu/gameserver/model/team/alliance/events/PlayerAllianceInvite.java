package com.aionemu.gameserver.model.team.alliance.events;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.team.TeamType;
import com.aionemu.gameserver.model.team.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team.group.PlayerGroup;
import com.aionemu.gameserver.model.team.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.collections.Predicates;
import com.google.common.base.Preconditions;

/**
 * @author ATracer
 */
public class PlayerAllianceInvite extends RequestResponseHandler<Player> {

	public PlayerAllianceInvite(Player inviter) {
		super(inviter);
	}

	@Override
	public void acceptRequest(Player inviter, Player invited) {
		if (RestrictionsManager.canInviteToAlliance(inviter, invited)) {

			PlayerAlliance alliance = inviter.getPlayerAlliance();
			List<Player> playersToAdd = new ArrayList<>();
			collectPlayersToAdd(inviter, invited, playersToAdd, alliance);

			if (alliance == null) {
				alliance = PlayerAllianceService.createAlliance(inviter, invited, TeamType.ALLIANCE);
				playersToAdd.remove(invited);
			}

			for (Player member : playersToAdd) {
				PlayerAllianceService.addPlayer(alliance, member);
			}
		}
	}

	private final void collectPlayersToAdd(Player inviter, Player invited, List<Player> playersToAdd, PlayerAlliance alliance) {
		// Collect requester Group without leader
		if (inviter.isInGroup()) {
			Preconditions.checkState(alliance == null, "If requester is in group - alliance should be null");
			PlayerGroup group = inviter.getPlayerGroup();
			playersToAdd.addAll(group.filterMembers(Predicates.Players.allExcept(inviter)));

			Iterator<Player> pIter = group.getMembers().iterator();
			while (pIter.hasNext()) {
				PlayerGroupService.removePlayer(pIter.next());
			}
		}

		// Collect full Invited Group
		if (invited.isInGroup()) {
			PlayerGroup group = invited.getPlayerGroup();
			playersToAdd.addAll(group.getMembers());
			Iterator<Player> pIter = group.getMembers().iterator();
			while (pIter.hasNext()) {
				PlayerGroupService.removePlayer(pIter.next());
			}
		} else { // or just single player
			playersToAdd.add(invited);
		}
	}

	@Override
	public void denyRequest(Player requester, Player responder) {
		PacketSendUtility.sendPacket(requester, SM_SYSTEM_MESSAGE.STR_PARTY_ALLIANCE_HE_REJECT_INVITATION(responder.getName()));
	}

}
