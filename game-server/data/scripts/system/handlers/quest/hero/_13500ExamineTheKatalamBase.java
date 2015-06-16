package quest.hero;

import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;

/**
 * @author Whoop
 */
public class _13500ExamineTheKatalamBase extends QuestHandler {
	
	public static final int questId = 13500;

  public _13500ExamineTheKatalamBase() {
      super(questId);
  }
  
  @Override
  public void register() {
      qe.registerQuestItem(182215270, questId);
      qe.registerQuestNpc(800527).addOnQuestStart(questId); //Tirins.
      qe.registerQuestNpc(801231).addOnTalkEvent(questId); //Berden.
      qe.registerQuestNpc(801233).addOnTalkEvent(questId); //Diena.
      qe.registerQuestNpc(801236).addOnTalkEvent(questId); //Ginos.
  }
  
  @Override
  public boolean onDialogEvent(QuestEnv env) {
  	Player player = env.getPlayer();
  	int targetId = env.getTargetId();
    QuestState qs = player.getQuestStateList().getQuestState(questId);
    DialogAction dialog = env.getDialog();
    
    if (qs == null || qs.getStatus() == QuestStatus.NONE) {
    	if (targetId == 800527) {
    		switch (dialog) {
    			case QUEST_SELECT:
    				return sendQuestDialog(env, 1011);
    			case QUEST_ACCEPT_SIMPLE:
    				return sendQuestDialog(env, 1012);
    			case QUEST_ACCEPT:
    				return sendQuestDialog(env, 1013);
    			case SETPRO1:
    				QuestService.startQuest(env);
    				changeQuestStep(env, 0, 1, false);
    				giveQuestItem(env, 182215270, 1);
    				return sendQuestDialog(env, 1352);    				
    			case SETPRO2:
    				QuestService.startQuest(env);
    				changeQuestStep(env, 0, 2, false);
    				giveQuestItem(env, 182215270, 1);
    				return sendQuestDialog(env, 1693);
    			case SETPRO3:
    				QuestService.startQuest(env);
    				changeQuestStep(env, 0, 3, false);
    				giveQuestItem(env, 182215270, 1);
    				return sendQuestDialog(env, 2034);
    		}
    	}
    } else if (qs.getStatus() == QuestStatus.START) {
    	switch (targetId) {
    		case 801231:
    			switch (dialog) {
    				case QUEST_SELECT:
    					return sendQuestDialog(env, 2375);
    				case SELECT_QUEST_REWARD:
    					removeQuestItem(env, 182215270, 1);
    					return defaultCloseDialog(env, 1, 4, true, true, 0);
    			}
    		case 801233:
    			switch (dialog) {
    				case QUEST_SELECT:
    					return sendQuestDialog(env, 2716);
    				case SELECT_QUEST_REWARD:
    					removeQuestItem(env, 182215270, 1);
    					return defaultCloseDialog(env, 2, 5, true, true, 1);
    			}
    		case 801236:
    			switch (dialog) {
    				case QUEST_SELECT:
    					return sendQuestDialog(env, 3057);
    				case SELECT_QUEST_REWARD:
    					removeQuestItem(env, 182215270, 1);
    					return defaultCloseDialog(env, 3, 6, true, true, 2);
    			}
    	}
    } else if (qs.getStatus() == QuestStatus.REWARD) {
    	switch (targetId) {
    		case 801231:
                return sendQuestEndDialog(env, 0);
            case 801233:
                return sendQuestEndDialog(env, 1);
            case 801236:
                return sendQuestEndDialog(env, 2);
    	} 		
    }
  	return false;
  }
}