package com.aionemu.gameserver.world;

public enum WorldMapType {
	// Asmodae
	PANDAEMONIUM(120010000),
	MARCHUTAN(120020000),
	ISHALGEN(220010000),
	MORHEIM(220020000),
	ALTGARD(220030000),
	BELUSLAN(220040000),
	BRUSTHONIN(220050000),

	// Elysea
	SANCTUM(110010000),
	KAISINEL(110020000),
	POETA(210010000),
	ELTNEN(210020000),
	VERTERON(210030000),
	HEIRON(210040000),
	THEOBOMOS(210060000),

	// Balaurea
	INGGISON(210050000),
	GELKMAROS(220070000),
	SILENTERA_CANYON(600010000),

	// Prison
	LF_PRISON(510010000), // For ELYOS
	DF_PRISON(520010000), // For ASMODIANS

	RESHANTA(400010000),

	// Instances
	NO_ZONE_NAME(300010000),
	ID_TEST_DUNGEON(300020000),
	NOCHSANA_TRAINING_CAMP(300030000),
	DARK_POETA(300040000),
	ASTERIA_CHAMBER(300050000),
	SULFUR_TREE_NEST(300060000),
	CHAMBER_OF_ROAH(300070000),
	LEFT_WING_CHAMBER(300080000),
	RIGHT_WING_CHAMBER(300090000),
	STEEL_RAKE(300100000),
	DREDGION(300110000),
	KYSIS_CHAMBER(300120000),
	MIREN_CHAMBER(300130000),
	KROTAN_CHAMBER(300140000),
	UDAS_TEMPLE(300150000),
	UDAS_TEMPLE_LOWER(300160000),
	BESHMUNDIR_TEMPLE(300170000),
	TALOCS_HOLLOW(300190000),
	HARAMEL(300200000),
	DREDGION_OF_CHANTRA(300210000),
	ABYSSAL_SPLINTER(300220000),
	KROMEDES_TRIAL(300230000),
	KARAMATIS(310010000),
	KARAMATIS_B(310020000),
	AERDINA(310030000),
	GERANAIA(310040000),
	AETHEROGENETICS_LAB(310050000),
	FRAGMENT_OF_DARKNESS(310060000),
	IDLF1B_STIGMA(310070000),
	SANCTUM_UNDERGROUND_ARENA(310080000),
	TRINIEL_UNDERGROUND_ARENA(320090000),
	INDRATU_FORTRESS(310090000),
	AZOTURAN_FORTRESS(310100000),
	THEOBOMOS_LAB(310110000),
	IDAB_PRO_L3(310120000),
	ATAXIAR(320010000),
	ATAXIAR_B(320020000),
	BREGIRUN(320030000),
	NIDALBER(320040000),
	ARKANIS_TEMPLE(320050000),
	SPACE_OF_OBLIVION(320060000),
	SPACE_OF_DESTINY(320070000),
	DRAUPNIR_CAVE(320080000),
	FIRE_TEMPLE(320100000),
	ALQUIMIA_RESEARCH_CENTER(320110000),
	SHADOW_COURT_DUNGEON(320120000),
	ADMA_STRONGHOLD(320130000),
	IDAB_PRO_D3(320140000),

	// Maps 2.5
	KAISINEL_ACADEMY(110070000),
	MARCHUTAN_PRIORY(120080000),
	ESOTERRACE(300250000),
	EMPYREAN_CRUCIBLE(300300000),

	// Map 2.6
	CRUCIBLE_CHALLENGE(300320000),

	// Maps 2.7
	ARENA_OF_CHAOS(300350000),
	ARENA_OF_DISCIPLINE(300360000),
	CHAOS_TRAINING_GROUNDS(300420000),
	DISCIPLINE_TRAINING_GROUNDS(300430000),
	PADMARASHKA_CAVE(320150000),

	// Test Map
	TEST_BASIC(900020000),
	TEST_SERVER(900030000),
	TEST_GIANTMONSTER(900100000),
	HOUSING_BARRACK(900110000),
	Test_IDArena(900120000),
	IDLDF5RE_test(900130000),
	Test_Kgw(900140000),
	Test_Basic_Mj(900150000),
	test_intro(900170000),
	Test_server_art(900180000),
	Test_TagMatch(900190000),
	test_timeattack(900200000),
	System_Basic(900220000),

	// Maps 3.0

	// Instances
	RAKSANG(300310000),
	RENTUS_BASE(300280000),
	ATURAN_SKY_FORTRESS(300240000),
	STEEL_RAKE_CABIN(300460000),
	TERATH_DREDGION(300440000),

	// Map 3.5
	ARENA_OF_HARMONY(300450000),
	TIAMAT_STRONGHOLD(300510000),
	DRAGON_LORDS_REFUGE(300520000),
	ARENA_OF_GLORY(300550000),
	SHUGO_IMPERIAL_TOMB(300560000),
	IDArena_Team01_T(300570000),
	UNSTABLE_SPLINTER(300600000), // Unstable Abyssal Splinter
	HEXWAY(300700000),

	// Instances 4.3 NA
	SEALED_DANUAR_MYSTICARIUM(300480000),
	ETERNAL_BASTION(300540000),
	OPHIDAN_BRIDGE(300590000),
	INFINITY_SHARD(300800000),
	DANUAR_RELIQUARY(301110000),
	KAMAR_BATTLEFIELD(301120000),
	SAURO_SUPPLY_BASE(301130000),
	SEIZED_DANUAR_SANCTUARY(301140000),
	NIGHTMARE_CIRCUS(301160000),
	THE_NIGHTMARE_CIRCUS(301200000),

	// Maps 4.3 NA
	LIFE_PARTY_CONCERT_HALL(600080000),

	// Maps 4.5 NA + Instances
	ENGULFED_OPHIDAN_BRIDGE(301210000),
	IRON_WALL_WARFRONT(301220000),
	ILLUMINARY_OBELISK(301230000),
	LEGIONS_KYSIS_BARRACKS(301240000),
	LEGIONS_MIREN_BARRACKS(301250000),
	LEGIONS_KROTAN_BARRACKS(301260000),
	LINKGATE_FOUNDRY(301270000),
	KYSIS_BARRACKS(301280000),
	MIREN_BARRACKS(301290000),
	KROTAN_BARRACKS(301300000),
	IDGEL_DOME(301310000),
	LUCKY_OPHIDAN_BRIDGE(301320000),
	LUCKY_DANUAR_RELIQUARY(301330000),

	// Maps 4.7 NA + Instances
	QUEST_LINKGATE_FOUNDRY(301340000),
	INFERNAL_DANUAR_RELIQUARY(301360000),
	INFERNAL_ILLUMINARY_OBELISK(301370000),
	BELUS(400020000),
	TRANSIDIUM_ANNEX(400030000),
	ASPIDA(400040000),
	ATANATOS(400050000),
	DISILLON(400060000),
	KALDOR(600090000),
	LEVINSHOR(600100000),

	// Housing
	ORIEL(700010000),
	PERNON(710010000),

	// Test Maps 4.7
	Test_MRT_IDZone(300290000),

	// Maps 4.7.5.0
	THE_SHUGO_EMPERORS_VAULT(301400000),
	WISPLIGHT_ABBEY(130090000),
	FATEBOUND_ABBEY(140010000),

	// Maps 4.8
	IDIAN_DEPTHS_DARK(220100000),
	CYGNEA(210070000),
	GRIFFOEN(210080000),
	IDIAN_DEPTHS_LIGHT(210090000),
	ENSHAR(220080000),
	HABROK(220090000),

	// Instances 4.8
	RAKSANG_RUINS(300610000),
	OCCUPIED_RENTUS_BASE(300620000),
	ANGUISHED_DRAGON_LORDS_REFUGE(300630000),
	DANUAR_SANCTUARY(301380000),
	DRAKENSPIRE_DEPHTS(301390000),
	STONESPEAR_REACH(301500000),

	// Housing
	HOUSING_LC_LEGION(700020000, true),
	HOUSING_DC_LEGION(710020000, true),
	HOUSING_IDLF_PERSONAL(720010000, true),
	HOUSING_IDDF_PERSONAL(730010000, true);

	private final int worldId;
	private final boolean isPersonal;

	WorldMapType(int worldId) {
		this(worldId, false);
	}

	WorldMapType(int worldId, boolean personal) {
		this.worldId = worldId;
		this.isPersonal = personal;
	}

	public int getId() {
		return worldId;
	}

	public boolean isPersonal() {
		return isPersonal;
	}

	/**
	 * @param id
	 *          of world
	 * @return WorldMapType
	 */
	public static WorldMapType getWorld(int id) {
		for (WorldMapType type : values()) {
			if (type.getId() == id)
				return type;
		}
		return null;
	}

	public static WorldMapType of(String worldName) {
		worldName = worldName.toLowerCase().replace(" ", "_");
		for (WorldMapType type : values())
			if (type.name().toLowerCase().equals(worldName))
				return type;
		return null;
	}

	public static int getMapId(String worldName) {
		WorldMapType worldMapType = of(worldName);
		return worldMapType == null ? 0 : worldMapType.getId();
	}
}
