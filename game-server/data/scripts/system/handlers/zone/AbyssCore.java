package zone;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.controllers.observer.CollisionDieActor;
import com.aionemu.gameserver.geoEngine.GeoWorldLoader;
import com.aionemu.gameserver.geoEngine.math.Matrix3f;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.Node;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.handler.ZoneHandler;
import com.aionemu.gameserver.world.zone.handler.ZoneNameAnnotation;

/**
 * @author MrPoke
 * @modified Neon
 */
@ZoneNameAnnotation("CORE_400010000")
public class AbyssCore implements ZoneHandler {

	private Map<Integer, CollisionDieActor> observed = new HashMap<>();
	private Node geometry;

	public AbyssCore() throws IOException {
		geometry = GeoWorldLoader.loadMeshes("data/geo/models/na_ab_lmark_col_01a.mesh")
			.get("levels/common/abyss/abground/landmark/ground_a/na_ab_lmark_col_01a.cgf");
		geometry.setTransform(new Matrix3f(1.15f, 0, 0, 0, 1.15f, 0, 0, 0, 1.15f), new Vector3f(2140.104f, 1925.5823f, 2303.919f), 1f);
		geometry.updateModelBound();
	}

	@Override
	public void onEnterZone(Creature creature, ZoneInstance zone) {
		Creature acting = creature.getActingCreature();
		if (acting instanceof Player && !((Player) acting).isStaff()) {

			CollisionDieActor observer = new CollisionDieActor(creature, geometry);
			creature.getObserveController().addObserver(observer);
			observed.put(creature.getObjectId(), observer);
		}
	}

	@Override
	public void onLeaveZone(Creature creature, ZoneInstance zone) {
		Creature acting = creature.getActingCreature();
		if (acting instanceof Player && !((Player) acting).isStaff()) {
			CollisionDieActor observer = observed.get(creature.getObjectId());
			if (observer != null) {
				creature.getObserveController().removeObserver(observer);
				observed.remove(creature.getObjectId());
			}
		}
	}

}
