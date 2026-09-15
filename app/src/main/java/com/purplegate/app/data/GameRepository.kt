package com.purplegate.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.purplegate.app.game.BeastId
import com.purplegate.app.game.GameState
import com.purplegate.app.game.SchoolProgress
import com.purplegate.app.game.SpellSchool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "purple_gate")

class GameRepository(private val context: Context) {

    private object Keys {
        val GOLD = longPreferencesKey("gold")
        val SELECTED_SCHOOL = stringPreferencesKey("selected_school")
        val GRABBER = booleanPreferencesKey("grabber")
        val OWNED_BEASTS = stringPreferencesKey("owned_beasts")
        val SELECTED_BEAST = stringPreferencesKey("selected_beast")
        val RAID_BEAST = stringPreferencesKey("raid_beast")
        val RAID_ENDS = longPreferencesKey("raid_ends")
        val RAID_ACTIVE = booleanPreferencesKey("raid_active")
        val GRIMOIRE = intPreferencesKey("grimoire")
        val NECRO = booleanPreferencesKey("necromancy")
        val PRESTIGE = intPreferencesKey("prestige")
        val LAST_SEEN = longPreferencesKey("last_seen")
        val TOTAL_EARNED = longPreferencesKey("total_earned")

        fun schoolUnlocked(s: SpellSchool) = booleanPreferencesKey("school_${s.name}_unlocked")
        fun schoolLevel(s: SpellSchool) = intPreferencesKey("school_${s.name}_level")
        fun schoolFamiliar(s: SpellSchool) = booleanPreferencesKey("school_${s.name}_familiar")
    }

    val stateFlow: Flow<PersistedSlice> = context.dataStore.data.map { prefs ->
        val schools = SpellSchool.entries.associateWith { school ->
            val defaultUnlocked = school == SpellSchool.FIRE
            SchoolProgress(
                unlocked = prefs[Keys.schoolUnlocked(school)] ?: defaultUnlocked,
                upgradeLevel = prefs[Keys.schoolLevel(school)] ?: 0,
                familiarOwned = prefs[Keys.schoolFamiliar(school)] ?: false,
            )
        }
        val ownedBeasts = prefs[Keys.OWNED_BEASTS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { runCatching { BeastId.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: emptySet()
        val selectedBeast = prefs[Keys.SELECTED_BEAST]?.let {
            runCatching { BeastId.valueOf(it) }.getOrNull()
        }
        val raidBeast = prefs[Keys.RAID_BEAST]?.let {
            runCatching { BeastId.valueOf(it) }.getOrNull()
        }
        PersistedSlice(
            gold = prefs[Keys.GOLD] ?: 0L,
            selectedSchool = prefs[Keys.SELECTED_SCHOOL]?.let {
                runCatching { SpellSchool.valueOf(it) }.getOrNull()
            } ?: SpellSchool.FIRE,
            schools = schools,
            grabberOwned = prefs[Keys.GRABBER] ?: false,
            ownedBeasts = ownedBeasts,
            selectedBeast = selectedBeast,
            raidBeast = raidBeast,
            raidEndsAtMs = prefs[Keys.RAID_ENDS] ?: 0L,
            raidActive = prefs[Keys.RAID_ACTIVE] ?: false,
            grimoirePages = prefs[Keys.GRIMOIRE] ?: 0,
            necromancyUnlocked = prefs[Keys.NECRO] ?: false,
            prestigeCount = prefs[Keys.PRESTIGE] ?: 0,
            lastSeenMs = prefs[Keys.LAST_SEEN] ?: System.currentTimeMillis(),
            totalGoldEarned = prefs[Keys.TOTAL_EARNED] ?: 0L,
        )
    }

    suspend fun save(state: GameState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOLD] = state.gold
            prefs[Keys.SELECTED_SCHOOL] = state.selectedSchool.name
            prefs[Keys.GRABBER] = state.grabberOwned
            prefs[Keys.OWNED_BEASTS] = state.ownedBeasts.joinToString(",") { it.name }
            if (state.selectedBeast != null) {
                prefs[Keys.SELECTED_BEAST] = state.selectedBeast.name
            } else {
                prefs.remove(Keys.SELECTED_BEAST)
            }
            if (state.raid.beast != null) {
                prefs[Keys.RAID_BEAST] = state.raid.beast.name
            } else {
                prefs.remove(Keys.RAID_BEAST)
            }
            prefs[Keys.RAID_ENDS] = state.raid.endsAtMs
            prefs[Keys.RAID_ACTIVE] = state.raid.active
            prefs[Keys.GRIMOIRE] = state.grimoirePages
            prefs[Keys.NECRO] = state.necromancyUnlocked
            prefs[Keys.PRESTIGE] = state.prestigeCount
            prefs[Keys.LAST_SEEN] = System.currentTimeMillis()
            prefs[Keys.TOTAL_EARNED] = state.totalGoldEarned
            for (school in SpellSchool.entries) {
                val p = state.schools[school] ?: SchoolProgress()
                prefs[Keys.schoolUnlocked(school)] = p.unlocked
                prefs[Keys.schoolLevel(school)] = p.upgradeLevel
                prefs[Keys.schoolFamiliar(school)] = p.familiarOwned
            }
        }
    }
}

data class PersistedSlice(
    val gold: Long,
    val selectedSchool: SpellSchool,
    val schools: Map<SpellSchool, SchoolProgress>,
    val grabberOwned: Boolean,
    val ownedBeasts: Set<BeastId>,
    val selectedBeast: BeastId?,
    val raidBeast: BeastId?,
    val raidEndsAtMs: Long,
    val raidActive: Boolean,
    val grimoirePages: Int,
    val necromancyUnlocked: Boolean,
    val prestigeCount: Int,
    val lastSeenMs: Long,
    val totalGoldEarned: Long,
)
