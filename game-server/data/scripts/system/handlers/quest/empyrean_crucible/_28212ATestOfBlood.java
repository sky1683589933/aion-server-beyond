package quest.empyrean_crucible;

import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;


/**
 * @author Cheatkiller
 *
 */
public class _28212ATestOfBlood extends QuestHandler {

	private final static int questId = 28212;
	
	public _28212ATestOfBlood() {
		super(questId);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(205986).addOnQuestStart(questId);
		qe.registerQuestNpc(205986).addOnTalkEvent(questId);
		qe.registerOnKillInWorld(300350000, questId);
		qe.registerOnKillInWorld(300360000, questId);
	}
	
	@Override
	public boolean onKillInWorldEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			giveQuestItem(env, 182212221, 1);
		return defaultOnKillRankedEvent(env, 0, 1, true); 
		}
		return false;
	}
	
	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (env.getTargetId() == 205986) {
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				if (env.getDialog() == DialogAction.QUEST_SELECT)
					return sendQuestDialog(env, 4762);
				else
					return sendQuestStartDialog(env);
			}
			else if (qs != null && qs.getStatus() == QuestStatus.REWARD) {
				if (env.getDialog() == DialogAction.USE_OBJECT)
					return sendQuestDialog(env, 10002);
				else {
					removeQuestItem(env, 182212221, 1);
					return sendQuestEndDialog(env);
				}
			}
		}
		return false;
	}

}

