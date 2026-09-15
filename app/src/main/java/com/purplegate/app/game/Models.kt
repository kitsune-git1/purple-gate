package com.purplegate.app.game

enum class SpellSchool(val displayName: String, val emoji: String) {
    FIRE("Fire", "🔥"),
    LIGHTNING("Lightning", "⚡"),
    ICE("Ice", "❄️"),
    NECROMANCY("Necromancy", "💀");
}

enum class Tab { CAMP, SPELLS, BEASTS, PRESTIGE }

data class SchoolProgress(
    val unlocked: Boolean = false,
    val upgradeLevel: Int = 0, // 0..4 (Spark..Familiar for Fire)
    val familiarOwned: Boolean = false,
)

data class FlyingSpell(
    val id: Long,
    val school: SpellSchool,
    val progress: Float = 0f, // 0 = start near finger, 1 = into portal
)

data class FlyingCoin(
    val id: Long,
    val progress: Float = 0f,
    val arcOffset: Float = 0f,
)

enum class BeastId(
    val displayName: String,
    val emoji: String,
    val cost: Long,
    val raidSeconds: Int,
    val baseReward: Long,
) {
    DIRE_RAT("Dire Rat", "🐀", 50, 8, 80),
    WOLF("Wolf", "🐺", 250, 14, 350),
    WAR_BOAR("War Boar", "🐗", 1_200, 22, 1_800),
    TROLL("Troll", "🧌", 6_000, 35, 9_000),
    MINOTAUR("Minotaur", "🐂", 30_000, 50, 50_000),
}

data class RaidState(
    val beast: BeastId? = null,
    val endsAtMs: Long = 0L,
    val active: Boolean = false,
)

data class GameState(
    val gold: Long = 0L,
    val selectedSchool: SpellSchool = SpellSchool.FIRE,
    val schools: Map<SpellSchool, SchoolProgress> = mapOf(
        SpellSchool.FIRE to SchoolProgress(unlocked = true, upgradeLevel = 0),
        SpellSchool.LIGHTNING to SchoolProgress(),
        SpellSchool.ICE to SchoolProgress(),
        SpellSchool.NECROMANCY to SchoolProgress(),
    ),
    val grabberOwned: Boolean = false,
    val ownedBeasts: Set<BeastId> = emptySet(),
    val selectedBeast: BeastId? = null,
    val raid: RaidState = RaidState(),
    val grimoirePages: Int = 0,
    val necromancyUnlocked: Boolean = false,
    val prestigeCount: Int = 0,
    val lastSeenMs: Long = System.currentTimeMillis(),
    val offlineGranted: Long = 0L,
    val showOfflineBanner: Boolean = false,
    val flyingSpells: List<FlyingSpell> = emptyList(),
    val flyingCoins: List<FlyingCoin> = emptyList(),
    val currentTab: Tab = Tab.CAMP,
    val totalGoldEarned: Long = 0L,
    val tick: Long = 0L,
) {
    val hottestSchool: SpellSchool
        get() {
            val order = listOf(
                SpellSchool.NECROMANCY,
                SpellSchool.ICE,
                SpellSchool.LIGHTNING,
                SpellSchool.FIRE,
            )
            return order.firstOrNull { schools[it]?.familiarOwned == true }
                ?: order.firstOrNull { (schools[it]?.upgradeLevel ?: 0) > 0 && schools[it]?.unlocked == true }
                ?: SpellSchool.FIRE
        }

    val goldPerSec: Double
        get() {
            var gps = 0.0
            for (school in SpellSchool.entries) {
                val p = schools[school] ?: continue
                if (p.familiarOwned) {
                    gps += familiarGps(school, p.upgradeLevel)
                }
            }
            if (grabberOwned) gps += 0.5
            // Ice drip while raid is out
            if (raid.active && schools[SpellSchool.ICE]?.familiarOwned == true) {
                gps += 2.0 * (1 + (schools[SpellSchool.ICE]?.upgradeLevel ?: 0))
            }
            return gps
        }

    /** 0 = almost empty; 1..12 = granular pile sprites up to background mountains. */
    val pileStage: Int
        get() = when {
            gold < 5 -> 0
            gold < 15 -> 1
            gold < 35 -> 2
            gold < 75 -> 3
            gold < 150 -> 4
            gold < 300 -> 5
            gold < 600 -> 6
            gold < 1_200 -> 7
            gold < 2_500 -> 8
            gold < 5_000 -> 9
            gold < 10_000 -> 10
            gold < 20_000 -> 11
            else -> 12
        }

    val canSlamGate: Boolean
        get() {
            val fire = schools[SpellSchool.FIRE]?.familiarOwned == true
            val light = schools[SpellSchool.LIGHTNING]?.familiarOwned == true
            val ice = schools[SpellSchool.ICE]?.familiarOwned == true
            return fire && light && ice && gold >= PRESTIGE_GOLD_THRESHOLD
        }

    companion object {
        const val PRESTIGE_GOLD_THRESHOLD = 25_000L
        const val MAX_PILE_STAGE = 12
    }
}

fun tapGold(school: SpellSchool, level: Int): Long {
    val base = when (school) {
        SpellSchool.FIRE -> 1L + level * 2L
        SpellSchool.LIGHTNING -> 1L + level
        SpellSchool.ICE -> 2L + level * 3L
        SpellSchool.NECROMANCY -> 3L + level * 2L
    }
    return base
}

fun upgradeCost(school: SpellSchool, currentLevel: Int): Long {
    val base = when (school) {
        SpellSchool.FIRE -> 15L
        SpellSchool.LIGHTNING -> 40L
        SpellSchool.ICE -> 120L
        SpellSchool.NECROMANCY -> 200L
    }
    return (base * Math.pow(2.2, currentLevel.toDouble())).toLong().coerceAtLeast(base)
}

fun familiarCost(school: SpellSchool): Long = when (school) {
    SpellSchool.FIRE -> 100L
    SpellSchool.LIGHTNING -> 400L
    SpellSchool.ICE -> 1_500L
    SpellSchool.NECROMANCY -> 3_000L
}

fun schoolUnlockCost(school: SpellSchool): Long = when (school) {
    SpellSchool.FIRE -> 0L
    SpellSchool.LIGHTNING -> 80L
    SpellSchool.ICE -> 500L
    SpellSchool.NECROMANCY -> 0L // prestige only
}

fun familiarGps(school: SpellSchool, level: Int): Double = when (school) {
    SpellSchool.FIRE -> 1.0 + level * 0.5
    SpellSchool.LIGHTNING -> 2.0 + level * 0.8
    SpellSchool.ICE -> 0.8 + level * 1.2
    SpellSchool.NECROMANCY -> 1.5 + level
}

fun upgradeNames(school: SpellSchool): List<String> = when (school) {
    SpellSchool.FIRE -> listOf("Fire Spark", "Bigger Ember", "Fireball", "Meteor Spit", "Campfire Familiar")
    SpellSchool.LIGHTNING -> listOf("Spark Chain", "Fork", "Storm Rune", "Bottle Imp", "Storm Familiar")
    SpellSchool.ICE -> listOf("Shard", "Hail Gate", "Glacier Plug", "Snow-goblin", "Frost Familiar")
    SpellSchool.NECROMANCY -> listOf("Bone Spark", "Rattle", "Grave Whisper", "Skeleton Goblin", "Bone Familiar")
}

fun formatGold(value: Long): String = when {
    value < 1_000 -> value.toString()
    value < 1_000_000 -> String.format("%.1fK", value / 1_000.0)
    value < 1_000_000_000 -> String.format("%.1fM", value / 1_000_000.0)
    else -> String.format("%.1fB", value / 1_000_000_000.0)
}

fun formatGps(value: Double): String = when {
    value < 10 -> String.format("%.1f/s", value)
    value < 1_000 -> String.format("%.0f/s", value)
    else -> formatGold(value.toLong()) + "/s"
}
