package quest.beluslan;

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
public class _80226ATestinDarkPoeta extends QuestHandler {

    private final static int questId = 80226;

    int[] mobs = { 214904 };

    public _80226ATestinDarkPoeta() {
        super(questId);
    }

    @Override
    public void register() {
        qe.registerQuestNpc(831027).addOnQuestStart(questId);
        qe.registerQuestNpc(831027).addOnTalkEvent(questId);
        for (int mob : mobs)
            qe.registerQuestNpc(mob).addOnKillEvent(questId);
    }

  	@Override
  	public boolean onDialogEvent(QuestEnv env) {
  		Player player = env.getPlayer();
  		QuestState qs = player.getQuestStateList().getQuestState(questId);
  		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
  			if (env.getTargetId() == 831027) {
  				if (env.getDialog() == DialogAction.QUEST_SELECT) {
  					return sendQuestDialog(env, 1011);
  				}
  				else {
  					return sendQuestStartDialog(env);
  				}
  			}
  		}
  		else if (qs.getStatus() == QuestStatus.REWARD) {
  			if (env.getTargetId() == 831027) {
  				if (env.getDialog() == DialogAction.USE_OBJECT) {
  					return sendQuestDialog(env, 1352);
  				}
  				else {
  					return sendQuestEndDialog(env);
  				}
  			}
  		}
  		return false;
  	}
    
  	@Override
  	public boolean onKillEvent(QuestEnv env) {
  		Player player = env.getPlayer();
  		QuestState qs = player.getQuestStateList().getQuestState(questId);
  		if (qs != null && qs.getStatus() == QuestStatus.START) {
  			int var = qs.getQuestVarById(0);
  			if (var == 0) {
  				return defaultOnKillEvent(env, mobs, 0, true);
  			}
  		}
  		return false;
  	}
}
