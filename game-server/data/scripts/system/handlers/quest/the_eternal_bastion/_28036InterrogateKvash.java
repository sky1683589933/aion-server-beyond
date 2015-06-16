package quest.the_eternal_bastion;

import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

public class _28036InterrogateKvash extends QuestHandler {

    private final static int questId = 28036;

    public _28036InterrogateKvash() {
        super(questId);
    }

    @Override
    public void register() {
        qe.registerQuestNpc(801047).addOnQuestStart(questId);
        qe.registerQuestNpc(801047).addOnTalkEvent(questId);
        qe.registerQuestNpc(802015).addOnTalkEvent(questId);
    }

    @Override
    public boolean onDialogEvent(QuestEnv env) {
        Player player = env.getPlayer();
        int targetId = env.getTargetId();
        QuestState qs = player.getQuestStateList().getQuestState(questId);
        DialogAction dialog = env.getDialog();

        if (qs == null || qs.getStatus() == QuestStatus.NONE) {
            if (targetId == 801047) {
                if (dialog == DialogAction.QUEST_SELECT) {
                    return sendQuestDialog(env, 4762);
                }
                else {
                    return sendQuestStartDialog(env);
                }
            }
        }
        else if (qs.getStatus() == QuestStatus.START) {
            if (targetId == 802015) {
                if (dialog == DialogAction.QUEST_SELECT)
                       return sendQuestDialog(env, 1011);

                if (dialog == DialogAction.SETPRO1)
                {
                            changeQuestStep(env, 0, 1, true);
                            updateQuestStatus(env);
                         return closeDialogWindow(env);
                   }
                }
        }
        else if (qs.getStatus() == QuestStatus.REWARD) {
            if (targetId == 801047) {
                return sendQuestEndDialog(env);
            }
        }
        return false;
    }
}
