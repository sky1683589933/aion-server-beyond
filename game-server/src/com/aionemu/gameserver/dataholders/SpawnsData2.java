package com.aionemu.gameserver.dataholders;

import static ch.lambdaj.Lambda.extractIterator;
import static ch.lambdaj.Lambda.flatten;
import static ch.lambdaj.Lambda.on;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import javolution.util.FastMap;
import javolution.util.FastTable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnMap;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.assaults.AssaultSpawn;
import com.aionemu.gameserver.model.templates.spawns.assaults.AssaultWave;
import com.aionemu.gameserver.model.templates.spawns.basespawns.BaseSpawn;
import com.aionemu.gameserver.model.templates.spawns.mercenaries.MercenaryRace;
import com.aionemu.gameserver.model.templates.spawns.mercenaries.MercenarySpawn;
import com.aionemu.gameserver.model.templates.spawns.mercenaries.MercenaryZone;
import com.aionemu.gameserver.model.templates.spawns.panesterra.AhserionsFlightSpawn;
import com.aionemu.gameserver.model.templates.spawns.riftspawns.RiftSpawn;
import com.aionemu.gameserver.model.templates.spawns.siegespawns.SiegeSpawn;
import com.aionemu.gameserver.model.templates.spawns.vortexspawns.VortexSpawn;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.spawnengine.SpawnHandlerType;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;

/**
 * @author xTz
 * @modified Rolandas
 */
@XmlRootElement(name = "spawns")
@XmlType(namespace = "", name = "SpawnsData2")
@XmlAccessorType(XmlAccessType.NONE)
public class SpawnsData2 {

	private static final Logger log = LoggerFactory.getLogger(SpawnsData2.class);
	@XmlElement(name = "spawn_map", type = SpawnMap.class)
	protected List<SpawnMap> templates;
	private TIntObjectHashMap<FastMap<Integer, SimpleEntry<SpawnGroup2, Spawn>>> allSpawnMaps = new TIntObjectHashMap<FastMap<Integer, SimpleEntry<SpawnGroup2, Spawn>>>();
	private TIntObjectHashMap<List<SpawnGroup2>> baseSpawnMaps = new TIntObjectHashMap<List<SpawnGroup2>>();
	private TIntObjectHashMap<List<SpawnGroup2>> riftSpawnMaps = new TIntObjectHashMap<List<SpawnGroup2>>();
	private TIntObjectHashMap<List<SpawnGroup2>> siegeSpawnMaps = new TIntObjectHashMap<List<SpawnGroup2>>();
	private TIntObjectHashMap<List<SpawnGroup2>> vortexSpawnMaps = new TIntObjectHashMap<List<SpawnGroup2>>();
	private TIntObjectHashMap<MercenarySpawn> mercenarySpawns = new TIntObjectHashMap<MercenarySpawn>();
	private TIntObjectHashMap<AssaultSpawn> assaultSpawns = new TIntObjectHashMap<AssaultSpawn>();
	private TIntObjectHashMap<Spawn> customs = new TIntObjectHashMap<Spawn>();
	private TIntObjectHashMap<List<SpawnGroup2>> ahserionSpawnMaps = new TIntObjectHashMap<List<SpawnGroup2>>(); //ahserions flight

	/**
	 * @param u
	 * @param parent
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void afterUnmarshal(Unmarshaller u, Object parent) {
		if (templates != null) {
			for (SpawnMap spawnMap : templates) {
				int mapId = spawnMap.getMapId();

				if (!allSpawnMaps.containsKey(mapId)) {
					allSpawnMaps.put(mapId, new FastMap<Integer, SimpleEntry<SpawnGroup2, Spawn>>());
				}

				for (Spawn spawn : spawnMap.getSpawns()) {
					if (spawn.isCustom()) {
						if (allSpawnMaps.get(mapId).containsKey(spawn.getNpcId()))
							allSpawnMaps.get(mapId).remove(spawn.getNpcId());
						customs.put(spawn.getNpcId(), spawn);
					} else if (customs.containsKey(spawn.getNpcId()))
						continue;
					allSpawnMaps.get(mapId).put(spawn.getNpcId(), new SimpleEntry(new SpawnGroup2(mapId, spawn), spawn));
				}

				for (BaseSpawn BaseSpawn : spawnMap.getBaseSpawns()) {
					int baseId = BaseSpawn.getId();
					if (!baseSpawnMaps.containsKey(baseId)) {
						baseSpawnMaps.put(baseId, new FastTable<SpawnGroup2>());
					}
					for (BaseSpawn.SimpleRaceTemplate simpleRace : BaseSpawn.getBaseRaceTemplates()) {
						for (Spawn spawn : simpleRace.getSpawns()) {
							if (spawn.isCustom()) {
								if (allSpawnMaps.get(mapId).containsKey(spawn.getNpcId()))
									allSpawnMaps.get(mapId).remove(spawn.getNpcId());
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId()))
								continue;
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, baseId, simpleRace.getBaseRace());
							baseSpawnMaps.get(baseId).add(spawnGroup);
						}
					}
				}

				for (RiftSpawn rift : spawnMap.getRiftSpawns()) {
					int id = rift.getId();
					if (!riftSpawnMaps.containsKey(id)) {
						riftSpawnMaps.put(id, new FastTable<SpawnGroup2>());
					}
					for (Spawn spawn : rift.getSpawns()) {
						if (spawn.isCustom()) {
							if (allSpawnMaps.get(mapId).containsKey(spawn.getNpcId())) {
								allSpawnMaps.get(mapId).remove(spawn.getNpcId());
							}
							customs.put(spawn.getNpcId(), spawn);
						} else if (customs.containsKey(spawn.getNpcId()))
							continue;
						SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id);
						riftSpawnMaps.get(id).add(spawnGroup);
					}
				}

				for (SiegeSpawn SiegeSpawn : spawnMap.getSiegeSpawns()) {
					int siegeId = SiegeSpawn.getSiegeId();
					if (!siegeSpawnMaps.containsKey(siegeId)) {
						siegeSpawnMaps.put(siegeId, new FastTable<SpawnGroup2>());
					}
					for (SiegeSpawn.SiegeRaceTemplate race : SiegeSpawn.getSiegeRaceTemplates()) {
						for (SiegeSpawn.SiegeRaceTemplate.SiegeModTemplate mod : race.getSiegeModTemplates()) {
							if (mod == null || mod.getSpawns() == null) {
								continue;
							}
							for (Spawn spawn : mod.getSpawns()) {
								if (spawn.isCustom()) {
									if (allSpawnMaps.get(mapId).containsKey(spawn.getNpcId()))
										allSpawnMaps.get(mapId).remove(spawn.getNpcId());
									customs.put(spawn.getNpcId(), spawn);
								} else if (customs.containsKey(spawn.getNpcId()))
									continue;
								SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, siegeId, race.getSiegeRace(), mod.getSiegeModType());
								siegeSpawnMaps.get(siegeId).add(spawnGroup);
							}
						}
					}
				}

				for (VortexSpawn VortexSpawn : spawnMap.getVortexSpawns()) {
					int id = VortexSpawn.getId();
					if (!vortexSpawnMaps.containsKey(id)) {
						vortexSpawnMaps.put(id, new FastTable<SpawnGroup2>());
					}
					for (VortexSpawn.VortexStateTemplate type : VortexSpawn.getSiegeModTemplates()) {
						if (type == null || type.getSpawns() == null) {
							continue;
						}
						for (Spawn spawn : type.getSpawns()) {
							if (spawn.isCustom()) {
								if (allSpawnMaps.get(mapId).containsKey(spawn.getNpcId())) {
									allSpawnMaps.get(mapId).remove(spawn.getNpcId());
								}
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.containsKey(spawn.getNpcId()))
								continue;
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, id, type.getStateType());
							vortexSpawnMaps.get(id).add(spawnGroup);
						}
					}
				}

				for (MercenarySpawn mercenarySpawn : spawnMap.getMercenarySpawns()) {
					int id = mercenarySpawn.getSiegeId();
					mercenarySpawns.put(id, mercenarySpawn);
					for (MercenaryRace mrace : mercenarySpawn.getMercenaryRaces()) {
						for (MercenaryZone mzone : mrace.getMercenaryZones()) {
							mzone.setWorldId(spawnMap.getMapId());
							mzone.setSiegeId(mercenarySpawn.getSiegeId());
						}

					}
				}

				for (AssaultSpawn assaultSpawn : spawnMap.getAssaultSpawns()) {
					int id = assaultSpawn.getSiegeId();
					assaultSpawns.put(id, assaultSpawn);
					for (AssaultWave awave : assaultSpawn.getAssaultWaves()) {
						awave.setWorldId(spawnMap.getMapId());
						awave.setSiegeId(assaultSpawn.getSiegeId());
					}
				}
				
				for (AhserionsFlightSpawn ahserionSpawn : spawnMap.getAhserionSpawns()) {
					int teamId = ahserionSpawn.getTeam().getId();
					if (!ahserionSpawnMaps.containsKey(teamId)) {
						ahserionSpawnMaps.put(teamId, new FastTable<SpawnGroup2>());
					}
					
					for (AhserionsFlightSpawn.AhserionStageSpawnTemplate stageTemplate : ahserionSpawn.getStageSpawnTemplate()) {
						if (stageTemplate == null || stageTemplate.getSpawns() == null) {
							continue;
						}
						
						for (Spawn spawn : stageTemplate.getSpawns()) {
							if (spawn.isCustom())  {
								if (allSpawnMaps.get(mapId).containsKey(spawn.getNpcId()))
									allSpawnMaps.get(mapId).remove(spawn.getNpcId());
								customs.put(spawn.getNpcId(), spawn);
							} else if (customs.contains(spawn.getNpcId())) {
								continue;	
							}
							SpawnGroup2 spawnGroup = new SpawnGroup2(mapId, spawn, stageTemplate.getStage(), ahserionSpawn.getTeam());
							ahserionSpawnMaps.get(teamId).add(spawnGroup);
						}
					}
				}
				
				customs.clear();
			}
		}
	}

	public void clearTemplates() {
		if (templates != null) {
			templates.clear();
			templates = null;
		}
	}

	public List<SpawnGroup2> getSpawnsByWorldId(int worldId) {
		if (!allSpawnMaps.containsKey(worldId))
			return Collections.emptyList();
		return flatten(extractIterator(allSpawnMaps.get(worldId).values(), on(SimpleEntry.class).getKey()));
	}

	public Spawn getSpawnsForNpc(int worldId, int npcId) {
		if (!allSpawnMaps.containsKey(worldId) || !allSpawnMaps.get(worldId).containsKey(npcId))
			return null;
		return allSpawnMaps.get(worldId).get(npcId).getValue();
	}

	public List<SpawnGroup2> getBaseSpawnsByLocId(int id) {
		return baseSpawnMaps.get(id);
	}

	public List<SpawnGroup2> getRiftSpawnsByLocId(int id) {
		return riftSpawnMaps.get(id);
	}

	public List<SpawnGroup2> getSiegeSpawnsByLocId(int siegeId) {
		return siegeSpawnMaps.get(siegeId);
	}

	public List<SpawnGroup2> getVortexSpawnsByLocId(int id) {
		return vortexSpawnMaps.get(id);
	}

	public MercenarySpawn getMercenarySpawnBySiegeId(int id) {
		return mercenarySpawns.get(id);
	}

	public AssaultSpawn getAssaultSpawnBySiegeId(int id) {
		return assaultSpawns.get(id);
	}

	public synchronized boolean saveSpawn(VisibleObject visibleObject, boolean delete) {
		SpawnTemplate spawn = visibleObject.getSpawn();
		Spawn oldGroup = DataManager.SPAWNS_DATA2.getSpawnsForNpc(visibleObject.getWorldId(), spawn.getNpcId());

		File xml = new File("./data/static_data/spawns/" + getRelativePath(visibleObject));
		SpawnsData2 data = null;
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;
		JAXBContext jc = null;
		boolean addGroup = false;

		try {
			schema = sf.newSchema(new File("./data/static_data/spawns/spawns.xsd"));
			jc = JAXBContext.newInstance(SpawnsData2.class);
		} catch (Exception e) {
			// ignore, if schemas are wrong then we even could not call the command;
		}

		if (xml.exists()) {
			try (FileInputStream fin = new FileInputStream(xml)) {
				Unmarshaller unmarshaller = jc.createUnmarshaller();
				unmarshaller.setSchema(schema);
				data = (SpawnsData2) unmarshaller.unmarshal(fin);
			} catch (Exception e) {
				log.error("Could not load old XML file!", e);
				return false;
			}
		}

		if (oldGroup == null || oldGroup.isCustom()) {
			if (data == null)
				data = new SpawnsData2();

			oldGroup = data.getSpawnsForNpc(visibleObject.getWorldId(), spawn.getNpcId());
			if (oldGroup == null) {
				oldGroup = new Spawn(spawn.getNpcId(), spawn.getRespawnTime(), spawn.getHandlerType());
				addGroup = true;
			}
		} else {
			if (data == null)
				data = DataManager.SPAWNS_DATA2;
			// only remove from memory, will be added back later
			allSpawnMaps.get(visibleObject.getWorldId()).remove(spawn.getNpcId());
			addGroup = true;
		}

		SpawnSpotTemplate spot = new SpawnSpotTemplate(visibleObject.getX(), visibleObject.getY(), visibleObject.getZ(), visibleObject.getHeading(),
			visibleObject.getSpawn().getRandomWalk(), visibleObject.getSpawn().getWalkerId(), visibleObject.getSpawn().getWalkerIndex());
		boolean changeX = visibleObject.getX() != spawn.getX();
		boolean changeY = visibleObject.getY() != spawn.getY();
		boolean changeZ = visibleObject.getZ() != spawn.getZ();
		boolean changeH = visibleObject.getHeading() != spawn.getHeading();
		if (changeH && visibleObject instanceof Npc) {
			if (visibleObject.getHeading() > 120) // xsd validation fails on negative numbers or if greater than 120 (=360 degrees)
				visibleObject.getPosition().setH((byte) (visibleObject.getHeading() - 120));
			else if (visibleObject.getHeading() < 0)
				visibleObject.getPosition().setH((byte) (visibleObject.getHeading() + 120));
		}

		SpawnSpotTemplate oldSpot = null;
		for (SpawnSpotTemplate s : oldGroup.getSpawnSpotTemplates()) {
			if (s.getX() == spot.getX() && s.getY() == spot.getY() && s.getZ() == spot.getZ() && s.getHeading() == spot.getHeading()) {
				if (delete || !StringUtils.equals(s.getWalkerId(), spot.getWalkerId())) {
					oldSpot = s;
					break;
				} else
					return false; // nothing to change
			} else if (changeX && s.getY() == spot.getY() && s.getZ() == spot.getZ() && s.getHeading() == spot.getHeading() || changeY
				&& s.getX() == spot.getX() && s.getZ() == spot.getZ() && s.getHeading() == spot.getHeading() || changeZ && s.getX() == spot.getX()
				&& s.getY() == spot.getY() && s.getHeading() == spot.getHeading() || changeH && s.getX() == spot.getX() && s.getY() == spot.getY()
				&& s.getZ() == spot.getZ()) {
				oldSpot = s;
				break;
			}
		}

		if (oldSpot != null)
			oldGroup.getSpawnSpotTemplates().remove(oldSpot);
		if (!delete)
			oldGroup.addSpawnSpot(spot);
		oldGroup.setCustom(true);

		SpawnMap map = null;
		if (data.templates == null) {
			data.templates = new FastTable<SpawnMap>();
			map = new SpawnMap(spawn.getWorldId());
			data.templates.add(map);
		} else {
			map = data.templates.get(0);
		}

		if (addGroup)
			map.addSpawns(oldGroup);

		xml.getParentFile().mkdir();
		try (FileOutputStream fos = new FileOutputStream(xml)) {
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setSchema(schema);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(data, fos);
			DataManager.SPAWNS_DATA2.templates = data.templates;
			DataManager.SPAWNS_DATA2.afterUnmarshal(null, null);
			DataManager.SPAWNS_DATA2.clearTemplates();
			data.clearTemplates();
		} catch (Exception e) {
			log.error("Could not save XML file!", e);
			return false;
		}
		return true;
	}

	private static String getRelativePath(VisibleObject visibleObject) {
		String path;
		WorldMap map = World.getInstance().getWorldMap(visibleObject.getWorldId());
		if (visibleObject.getSpawn().getHandlerType() == SpawnHandlerType.RIFT)
			path = "Rifts";
		else if (visibleObject instanceof Gatherable)
			path = "Gather";
		else if (map.isInstanceType())
			path = "Instances";
		else
			path = "Npcs";
		return path + "/New/" + visibleObject.getWorldId() + "_" + map.getName().replace(' ', '_') + ".xml";
	}

	public int size() {
		return allSpawnMaps.size();
	}

	/**
	 * @param worldId
	 *          Optional. If provided, searches in this world first
	 * @param npcId
	 * @return template for the spot
	 */
	public SpawnSearchResult getFirstSpawnByNpcId(int worldId, int npcId) {
		Spawn spawns = getSpawnsForNpc(worldId, npcId);

		if (spawns == null) {
			for (WorldMapTemplate template : DataManager.WORLD_MAPS_DATA) {
				if (template.getMapId() == worldId)
					continue;
				spawns = getSpawnsForNpc(template.getMapId(), npcId);
				if (spawns != null) {
					worldId = template.getMapId();
					break;
				}
			}
			if (spawns == null)
				return null;
		}
		return new SpawnSearchResult(worldId, spawns.getSpawnSpotTemplates().get(0));
	}

	/**
	 * Used by Event Service to add additional spawns
	 * 
	 * @param spawnMap
	 *          templates to add
	 */
	public void addNewSpawnMap(SpawnMap spawnMap) {
		if (templates == null)
			templates = new FastTable<SpawnMap>();
		templates.add(spawnMap);
	}

	public void removeEventSpawnObjects(List<VisibleObject> objects) {
		for (VisibleObject visObj : objects) {
			if (!allSpawnMaps.contains(visObj.getWorldId()))
				continue;
			SimpleEntry<SpawnGroup2, Spawn> entry = allSpawnMaps.get(visObj.getWorldId()).get(visObj.getObjectTemplate().getTemplateId());
			if (!entry.getValue().isEventSpawn())
				continue;
			if (entry.getValue().getEventTemplate().equals(visObj.getSpawn().getEventTemplate()))
				allSpawnMaps.get(visObj.getWorldId()).remove(entry);
		}
	}

	public List<SpawnMap> getTemplates() {
		return templates;
	}

	public List<SpawnGroup2> getAhserionSpawnByTeamId(int id) {
		return ahserionSpawnMaps.get(id);
	}

}
