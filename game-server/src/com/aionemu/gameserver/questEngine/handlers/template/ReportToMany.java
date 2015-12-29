package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastMap;

import com.aionemu.gameserver.model.DialogAction;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.questEngine.handlers.HandlerResult;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.handlers.models.NpcInfos;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;

/**
 * @author Hilgert @modified vlog, Pad
 */
public class ReportToMany extends QuestHandler {

	private static final Logger log = LoggerFactory.getLogger(ReportToMany.class);

	private final int startItem;
	private final Set<Integer> startNpcs = new HashSet<Integer>();
	private final Set<Integer> endNpcs = new HashSet<Integer>();
	private final int startDialog;
	private final int endDialog;
	private final int maxVar;
	private final FastMap<Integer, NpcInfos> npcInfos;
	private boolean mission;
	private QuestItems workItem;

	/**
	 * @param questId
	 * @param startItem
	 * @param endNpc
	 * @param startDialog
	 * @param endDialog
	 * @param maxVar
	 */
	public ReportToMany(int questId, int startItem, List<Integer> startNpcIds, List<Integer> endNpcIds, FastMap<Integer, NpcInfos> npcInfos,
		int startDialog, int endDialog, int maxVar, boolean mission) {
		super(questId);
		this.startItem = startItem;
		if (startNpcIds != null) {
			startNpcs.addAll(startNpcIds);
			startNpcs.remove(0);
		}
		if (endNpcIds != null) {
			endNpcs.addAll(endNpcIds);
			endNpcs.remove(0);
		}
		this.npcInfos = npcInfos;
		this.startDialog = startDialog;
		this.endDialog = endDialog;
		this.maxVar = maxVar;
		this.mission = mission;
	}

	@Override
	protected void onWorkItemsLoaded() {
		if (workItems == null) {
			return;
		}
		/*
		 * if (workItems.size() > 1) {
		 * log.warn("Q{} (ReportToMany) has more than 1 work item.", questId);
		 * } commented out until distribution and removal of quest work items depend on quest step
		 */
		workItem = workItems.get(0);
	}

	@Override
	public void register() {
		if (mission) {
			qe.registerOnLevelUp(getQuestId());
		}
		if (startItem != 0)
			qe.registerQuestItem(startItem, getQuestId());
		else {
			Iterator<Integer> iterator = startNpcs.iterator();
			while (iterator.hasNext()) {
				int startNpc = iterator.next();
				qe.registerQuestNpc(startNpc).addOnQuestStart(getQuestId());
				qe.registerQuestNpc(startNpc).addOnTalkEvent(getQuestId());
			}
		}
		for (int npcId : npcInfos.keySet()) {
			qe.registerQuestNpc(npcId).addOnTalkEvent(getQuestId());
		}
		Iterator<Integer> iterator = endNpcs.iterator();
		while (iterator.hasNext()) {
			int endNpc = iterator.next();
			qe.registerQuestNpc(endNpc).addOnTalkEvent(getQuestId());
		}
	}

	@Override
	public boolean onDialogEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(getQuestId());
		DialogAction dialog = env.getDialog();
		int targetId = env.getTargetId();

		if (qs == null || qs.getStatus() == QuestStatus.NONE || qs.canRepeat()) {
			if (startItem != 0) {
				if (dialog == DialogAction.QUEST_ACCEPT_1) {
					if (QuestService.startQuest(env)) {
						if (workItem != null) {
							// some quest work items come from other quests, so we don't add them again
							long count = workItem.getCount();
							count -= player.getInventory().getItemCountByItemId(workItem.getItemId());
							if (count != 0) {
								giveQuestItem(env, workItem.getItemId(), count, ItemAddType.QUEST_WORK_ITEM);
							}
						}
					}
					return closeDialogWindow(env);
				}
			}
			if (startNpcs.isEmpty() || startNpcs.contains(targetId)) {
				if (dialog == DialogAction.QUEST_SELECT) {
					if (startDialog != 0)
						return sendQuestDialog(env, startDialog);
					else
						return sendQuestDialog(env, 1011);
				} else if (dialog == DialogAction.QUEST_ACCEPT || dialog == DialogAction.QUEST_ACCEPT_1 || dialog == DialogAction.QUEST_ACCEPT_SIMPLE) {
					if (workItem != null) {
						// some quest work items come from other quests, so we don't add them again
						long count = workItem.getCount();
						count -= player.getInventory().getItemCountByItemId(workItem.getItemId());
						if (count == 0 || giveQuestItem(env, workItem.getItemId(), count, ItemAddType.QUEST_WORK_ITEM)) {
							return sendQuestStartDialog(env);
						}
						return false;
					} else {
						return sendQuestStartDialog(env);
					}
				} else {
					return sendQuestStartDialog(env);
				}
			}
		} else if (qs.getStatus() == QuestStatus.START) {
			int var = qs.getQuestVarById(0);
			NpcInfos targetNpcInfo = npcInfos.get(targetId);
			if (var <= maxVar) {
				if (targetNpcInfo != null && var == targetNpcInfo.getVar()) {
					int closeDialog;
					if (targetNpcInfo.getCloseDialog() == 0) {
						closeDialog = 10000 + targetNpcInfo.getVar();
					} else {
						closeDialog = targetNpcInfo.getCloseDialog();
					}

					if (dialog == DialogAction.QUEST_SELECT) {
						return sendQuestDialog(env, targetNpcInfo.getQuestDialog());
					} else if (dialog.id() == targetNpcInfo.getQuestDialog() + 1 && targetNpcInfo.getMovie() != 0) {
						sendQuestDialog(env, targetNpcInfo.getQuestDialog() + 1);
						return playQuestMovie(env, targetNpcInfo.getMovie());
					} else if (dialog == DialogAction.SET_SUCCEED) {
						qs.setQuestVar(maxVar);
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return closeDialogWindow(env);
					} else if (dialog.id() == closeDialog) {
						if ((dialog != DialogAction.CHECK_USER_HAS_QUEST_ITEM && dialog != DialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE)
							|| QuestService.collectItemCheck(env, true)) {
							if (var == maxVar) {
								if (closeDialog == 1009 || closeDialog == 20002 || closeDialog == 34) {
									qs.setStatus(QuestStatus.REWARD);
									updateQuestStatus(env);
									return sendQuestDialog(env, 5);
								}
							} else {
								qs.setQuestVarById(0, var + 1);
							}
							updateQuestStatus(env);
						}
						return sendQuestSelectionDialog(env);
					}
				}
			} else if (var > maxVar) {
				if (endNpcs.contains(targetId)) {
					if (dialog == DialogAction.QUEST_SELECT) {
						return sendQuestDialog(env, endDialog);
					} else if (env.getDialog() == DialogAction.SELECT_QUEST_REWARD) {
						if (startItem != 0) {
							if (!removeQuestItem(env, startItem, 1)) {
								return false;
							}
						}
						if (workItem != null) {
							long count = player.getInventory().getItemCountByItemId(workItem.getItemId());
							if (count < workItem.getCount()) {
								return sendQuestSelectionDialog(env);
							}
							removeQuestItem(env, workItem.getItemId(), count, QuestStatus.COMPLETE);
						}
						qs.setStatus(QuestStatus.REWARD);
						updateQuestStatus(env);
						return sendQuestEndDialog(env);
					}
				}
			}
		} else if (qs.getStatus() == QuestStatus.REWARD && endNpcs.contains(targetId)) {
			int var = qs.getQuestVarById(0);
			NpcInfos targetNpcInfo = npcInfos.get(targetId);
			if (var >= maxVar && targetNpcInfo != null) {
				int closeDialog;
				if (targetNpcInfo.getCloseDialog() == 0) {
					closeDialog = 10000 + targetNpcInfo.getVar();
				} else {
					closeDialog = targetNpcInfo.getCloseDialog();
				}
				if (dialog == DialogAction.USE_OBJECT) {
					if (closeDialog == 1009 || closeDialog == 20002)
						return sendQuestEndDialog(env);
					if (targetNpcInfo.getQuestDialog() != 0)
						return sendQuestDialog(env, targetNpcInfo.getQuestDialog());
				}
				return sendQuestEndDialog(env);
			}
		}
		return false;
	}

	@Override
	public HandlerResult onItemUseEvent(final QuestEnv env, Item item) {
		if (startItem != 0) {
			Player player = env.getPlayer();
			QuestState qs = player.getQuestStateList().getQuestState(getQuestId());
			if (qs == null || qs.getStatus() == QuestStatus.NONE) {
				return HandlerResult.fromBoolean(sendQuestDialog(env, 4));
			}
		}
		return HandlerResult.UNKNOWN;
	}

	@Override
	public boolean onLvlUpEvent(QuestEnv questEnv) {
		return defaultOnLvlUpEvent(questEnv);
	}

	@Override
	public HashSet<Integer> getNpcIds() {
		if (constantSpawns == null) {
			constantSpawns = new HashSet<>();
			if (startNpcs != null)
				constantSpawns.addAll(startNpcs);
			if (endNpcs != null)
				constantSpawns.addAll(endNpcs);
			for (int npcId : npcInfos.keySet())
				constantSpawns.add(npcId);
		}
		return constantSpawns;
	}
}
