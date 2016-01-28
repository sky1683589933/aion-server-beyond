package admincommands;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.siegespawns.SiegeSpawnTemplate;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * @author Luno, modified Bobobear
 */
public class Delete extends AdminCommand {

	public Delete() {
		super("delete", "Removes a spawn from world.");
	}

	@Override
	public void execute(Player admin, String... params) {
		VisibleObject target = admin.getTarget();
		if (!(target instanceof Npc) && !(target instanceof Gatherable)) {
			sendInfo(admin, "You need to target an Npc or Gatherable type.");
			return;
		}

		SpawnTemplate spawn = target.getSpawn();
		if (spawn.hasPool()) {
			sendInfo(admin, "Can't delete pooled spawn template");
			return;
		}
		if (spawn instanceof SiegeSpawnTemplate) {
			sendInfo(admin, "Can't delete siege spawn template");
			return;
		}

		if (target instanceof Npc) {
			((Npc) target).getController().cancelTask(TaskId.RESPAWN);
			target.getController().onDelete();
		} else if (target instanceof Gatherable) {
			World.getInstance().despawn(target); // onDelete would trigger a respawn task
		}
		sendInfo(admin, "Spawn removed.");

		if (!DataManager.SPAWNS_DATA2.saveSpawn(target, true)) {
			sendInfo(admin, "Could not save deleted spawn.");
			return;
		}
	}
}
