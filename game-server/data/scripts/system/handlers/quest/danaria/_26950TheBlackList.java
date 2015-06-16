package quest.danaria;

import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;


/**
 * @author Ritsu
 *
 */
public class _26950TheBlackList extends QuestHandler {

	private static final int questId = 26950;

	public _26950TheBlackList()
	{
		super(questId);
	}

	@Override
	public void register()
	{
		qe.registerQuestNpc(801290).addOnQuestStart(questId);
		qe.registerQuestNpc(804488).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env)
	{
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		DialogAction dialog = env.getDialog();
		int targetId = env.getTargetId();

		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat())
		{
			if(targetId == 801290)
			{
				switch (dialog)
				{
					case QUEST_SELECT:
						return sendQuestDialog(env, 1011);
					case QUEST_ACCEPT_SIMPLE:
						return sendQuestStartDialog(env);
				}
			}
		}

		if (qs == null)
			return false;

		if(qs.getStatus() == QuestStatus.START)
		{
			int var = qs.getQuestVarById(0);
			if(targetId == 804488)
			{
				switch (dialog)
				{
					case QUEST_SELECT:
						if(var == 0)
							return sendQuestDialog(env, 1352);
						if(var == 1)
							return sendQuestDialog(env, 2375);
					case SETPRO1:
						changeQuestStep(env, 0, 1, false);
						return closeDialogWindow(env);
					case CHECK_USER_HAS_QUEST_ITEM_SIMPLE:
						return checkQuestItems(env, 1, 1, true, 5, 0); 
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD)
		{
			if(targetId == 804488)
				return sendQuestEndDialog(env);
		}

		return false;
	}

}
