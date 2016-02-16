package quest.crafting;

import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author Thuatan
 * @modified Pad
 */
public class _19014MasterArmorsmithsPotential extends QuestHandler {

	private final static int questId = 19014;

	public _19014MasterArmorsmithsPotential() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(203790).addOnQuestStart(questId);
		qe.registerQuestNpc(203790).addOnTalkEvent(questId);
		qe.registerQuestNpc(203791).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		DialogAction dialog = env.getDialog();
		int targetId = env.getTargetId();

		if (dialog == DialogAction.QUEST_SELECT && !CraftSkillUpdateService.getInstance().canLearnMoreMasterCraftingSkill(player)) {
			return sendQuestSelectionDialog(env);
		}

		if (qs == null || qs.getStatus() == QuestStatus.NONE) {
			if (targetId == 203790) {
				if (dialog == DialogAction.QUEST_SELECT)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			switch (targetId) {
				case 203791:
					switch (dialog) {
						case QUEST_SELECT:
							return sendQuestDialog(env, 1011);
						case SETPRO10:
							if (!giveQuestItem(env, 152201806, 1))
								return true;
							qs.setQuestVarById(0, 1);
							updateQuestStatus(env);
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
							return true;
						case SETPRO20:
							if (!giveQuestItem(env, 152201807, 1))
								return true;
							qs.setQuestVarById(0, 1);
							updateQuestStatus(env);
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
							return true;
					}
				case 203790:
					switch (dialog) {
						case QUEST_SELECT:
							long itemCount1 = player.getInventory().getItemCountByItemId(182206765);
							if (itemCount1 > 0) {
								removeQuestItem(env, 182206765, 1);
								qs.setStatus(QuestStatus.REWARD);
								updateQuestStatus(env);
								return sendQuestDialog(env, 1352);
							} else
								return sendQuestDialog(env, 2716);
					}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 203790) {
				if (dialog == DialogAction.CHECK_USER_HAS_QUEST_ITEM)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
		}
		return false;
	}
}
