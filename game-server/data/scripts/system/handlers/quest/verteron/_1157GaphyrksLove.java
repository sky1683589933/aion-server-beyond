package quest.verteron;

import static com.aionemu.gameserver.model.DialogAction.*;

import com.aionemu.gameserver.model.actions.NpcActions;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.AbstractQuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.PositionUtil;

/**
 * @author Rhys2002
 */
public class _1157GaphyrksLove extends AbstractQuestHandler {

	public _1157GaphyrksLove() {
		super(1157);
	}

	@Override
	public void register() {
		qe.registerQuestNpc(798003).addOnQuestStart(questId);
		qe.registerQuestNpc(798003).addOnTalkEvent(questId);
		qe.registerQuestNpc(210319).addOnAttackEvent(questId);
		qe.registerOnMovieEndQuest(17, questId);
	}

	@Override
	public boolean onAttackEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);

		if (qs == null || qs.getStatus() != QuestStatus.START)
			return false;

		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		if (targetId != 210319)
			return false;

		final Npc npc = (Npc) env.getVisibleObject();

		if (PositionUtil.getDistance(892, 2024, 166, npc.getX(), npc.getY(), npc.getZ()) > 13) {
			return false;
		}
		NpcActions.delete(npc, !npc.isDead());
		playQuestMovie(env, 17);
		return true;
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);

		int targetId = 0;
		if (env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();

		if (qs == null || qs.isStartable()) {
			if (targetId == 798003) {
				if (env.getDialogActionId() == QUEST_SELECT)
					return sendQuestDialog(env, 1011);
				else
					return sendQuestStartDialog(env);
			}
		} else if (qs.getStatus() == QuestStatus.REWARD) {
			if (targetId == 798003) {
				if (env.getDialogActionId() == USE_OBJECT)
					return sendQuestDialog(env, 2375);
				else if (env.getDialogActionId() == SELECT_QUEST_REWARD)
					return sendQuestDialog(env, 5);
				else
					return sendQuestEndDialog(env);
			}
			return false;
		}
		return false;
	}

	@Override
	public boolean onMovieEndEvent(QuestEnv env, int movieId) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		if (qs == null || movieId != 17 || qs.getStatus() == QuestStatus.COMPLETE)
			return false;
		qs.setStatus(QuestStatus.REWARD);
		updateQuestStatus(env);
		return true;
	}
}
