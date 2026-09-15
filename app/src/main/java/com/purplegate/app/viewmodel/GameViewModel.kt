package com.purplegate.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purplegate.app.data.GameRepository
import com.purplegate.app.data.PersistedSlice
import com.purplegate.app.game.BeastId
import com.purplegate.app.game.FlyingCoin
import com.purplegate.app.game.FlyingSpell
import com.purplegate.app.game.GameState
import com.purplegate.app.game.RaidState
import com.purplegate.app.game.SchoolProgress
import com.purplegate.app.game.SpellSchool
import com.purplegate.app.game.Tab
import com.purplegate.app.game.familiarCost
import com.purplegate.app.game.familiarGps
import com.purplegate.app.game.schoolUnlockCost
import com.purplegate.app.game.tapGold
import com.purplegate.app.game.upgradeCost
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GameRepository(app)
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var nextAnimId = 1L
    private var loaded = false
    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            val slice = repo.stateFlow.first()
            applyPersisted(slice)
            loaded = true
            startTicker()
        }
    }

    private fun applyPersisted(slice: PersistedSlice) {
        val now = System.currentTimeMillis()
        var gold = slice.gold
        var offlineGranted = 0L
        var showBanner = false

        // Reconstruct interim gps from slice
        var gps = 0.0
        for (school in SpellSchool.entries) {
            val p = slice.schools[school] ?: continue
            if (p.familiarOwned) gps += familiarGps(school, p.upgradeLevel)
        }
        if (slice.grabberOwned) gps += 0.5

        val elapsedMs = (now - slice.lastSeenMs).coerceAtLeast(0L)
        val elapsedSec = elapsedMs / 1000.0
        if (elapsedSec > 3 && gps > 0) {
            val capped = min(elapsedSec, 8 * 60 * 60.0) // 8h cap
            offlineGranted = (gps * capped).toLong()
            gold += offlineGranted
            showBanner = offlineGranted > 0
        }

        var raid = RaidState(
            beast = slice.raidBeast,
            endsAtMs = slice.raidEndsAtMs,
            active = slice.raidActive,
        )
        // If raid finished while away, grant reward now
        if (raid.active && raid.beast != null && now >= raid.endsAtMs) {
            val reward = raidReward(raid.beast, slice.schools)
            gold += reward
            offlineGranted += reward
            showBanner = true
            raid = RaidState()
        }

        _state.value = GameState(
            gold = gold,
            selectedSchool = slice.selectedSchool,
            schools = slice.schools,
            grabberOwned = slice.grabberOwned,
            ownedBeasts = slice.ownedBeasts,
            selectedBeast = slice.selectedBeast,
            raid = raid,
            grimoirePages = slice.grimoirePages,
            necromancyUnlocked = slice.necromancyUnlocked,
            prestigeCount = slice.prestigeCount,
            lastSeenMs = now,
            offlineGranted = offlineGranted,
            showOfflineBanner = showBanner,
            totalGoldEarned = slice.totalGoldEarned + offlineGranted,
        )
        persist()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(100L)
                tick()
            }
        }
    }

    private fun tick() {
        _state.update { s ->
            var gold = s.gold
            val earned = (s.goldPerSec * 0.1).let { if (it >= 1) it.toLong() else 0L }
            // Fractional gps: accumulate via random for sub-1
            val fracEarn = if (s.goldPerSec > 0 && earned == 0L) {
                if (Random.nextDouble() < s.goldPerSec * 0.1) 1L else 0L
            } else earned
            gold += fracEarn

            // Animate spells toward portal
            val spells = s.flyingSpells.mapNotNull { sp ->
                val p = sp.progress + 0.12f
                if (p >= 1f) null else sp.copy(progress = p)
            }
            // Coins arc onto pile
            val coins = s.flyingCoins.mapNotNull { c ->
                val p = c.progress + 0.08f
                if (p >= 1f) null else c.copy(progress = p)
            }

            // Raid complete?
            var raid = s.raid
            var pileBurst = false
            if (raid.active && raid.beast != null && System.currentTimeMillis() >= raid.endsAtMs) {
                val reward = raidReward(raid.beast, s.schools)
                gold += reward
                raid = RaidState()
                pileBurst = true
                // Burst coins
            }

            val newCoins = if (pileBurst) {
                coins + List(5) {
                    FlyingCoin(nextAnimId++, 0f, Random.nextFloat() * 2f - 1f)
                }
            } else coins

            s.copy(
                gold = gold,
                flyingSpells = spells,
                flyingCoins = newCoins,
                raid = raid,
                totalGoldEarned = s.totalGoldEarned + fracEarn + if (pileBurst && s.raid.beast != null) {
                    raidReward(s.raid.beast!!, s.schools)
                } else 0L,
                tick = s.tick + 1,
            )
        }
        if (_state.value.tick % 20L == 0L) persist()
    }

    private fun raidReward(beast: BeastId, schools: Map<SpellSchool, SchoolProgress>): Long {
        var reward = beast.baseReward.toDouble()
        if (schools[SpellSchool.FIRE]?.familiarOwned == true) reward *= 1.35
        if (schools[SpellSchool.LIGHTNING]?.familiarOwned == true) {
            // already faster return handled at launch; small bonus
            reward *= 1.1
        }
        return reward.toLong()
    }

    fun dismissOfflineBanner() {
        _state.update { it.copy(showOfflineBanner = false, offlineGranted = 0) }
    }

    fun selectTab(tab: Tab) {
        _state.update { it.copy(currentTab = tab) }
    }

    fun selectSchool(school: SpellSchool) {
        val s = _state.value
        if (s.schools[school]?.unlocked != true) return
        _state.update { it.copy(selectedSchool = school) }
        persist()
    }

    fun castSpell() {
        val s = _state.value
        val school = s.selectedSchool
        val progress = s.schools[school] ?: return
        if (!progress.unlocked) return
        val gain = tapGold(school, progress.upgradeLevel)
        val spellId = nextAnimId++
        val coinId = nextAnimId++
        _state.update {
            it.copy(
                gold = it.gold + gain,
                totalGoldEarned = it.totalGoldEarned + gain,
                flyingSpells = it.flyingSpells + FlyingSpell(spellId, school),
                flyingCoins = it.flyingCoins + FlyingCoin(
                    coinId,
                    0f,
                    Random.nextFloat() * 2f - 1f,
                ),
            )
        }
    }

    fun buyUpgrade(school: SpellSchool) {
        _state.update { s ->
            val p = s.schools[school] ?: return@update s
            if (!p.unlocked || p.upgradeLevel >= 3) return@update s // levels 0..3 then familiar
            val cost = upgradeCost(school, p.upgradeLevel)
            if (s.gold < cost) return@update s
            val newSchools = s.schools.toMutableMap()
            newSchools[school] = p.copy(upgradeLevel = p.upgradeLevel + 1)
            s.copy(gold = s.gold - cost, schools = newSchools)
        }
        persist()
    }

    fun buyFamiliar(school: SpellSchool) {
        _state.update { s ->
            val p = s.schools[school] ?: return@update s
            if (!p.unlocked || p.familiarOwned || p.upgradeLevel < 3) return@update s
            val cost = familiarCost(school)
            if (s.gold < cost) return@update s
            val newSchools = s.schools.toMutableMap()
            newSchools[school] = p.copy(familiarOwned = true, upgradeLevel = 4)
            s.copy(gold = s.gold - cost, schools = newSchools)
        }
        persist()
    }

    fun unlockSchool(school: SpellSchool) {
        if (school == SpellSchool.FIRE || school == SpellSchool.NECROMANCY) return
        _state.update { s ->
            val p = s.schools[school] ?: return@update s
            if (p.unlocked) return@update s
            // Require previous school unlocked
            val prev = when (school) {
                SpellSchool.LIGHTNING -> SpellSchool.FIRE
                SpellSchool.ICE -> SpellSchool.LIGHTNING
                else -> return@update s
            }
            if (s.schools[prev]?.unlocked != true) return@update s
            val cost = schoolUnlockCost(school)
            if (s.gold < cost) return@update s
            val newSchools = s.schools.toMutableMap()
            newSchools[school] = p.copy(unlocked = true)
            s.copy(gold = s.gold - cost, schools = newSchools, selectedSchool = school)
        }
        persist()
    }

    fun buyGrabber() {
        _state.update { s ->
            if (s.grabberOwned || s.gold < 40L) return@update s
            s.copy(gold = s.gold - 40L, grabberOwned = true)
        }
        persist()
    }

    fun buyBeast(beast: BeastId) {
        _state.update { s ->
            if (beast in s.ownedBeasts || s.gold < beast.cost) return@update s
            // Sequential unlock
            val idx = BeastId.entries.indexOf(beast)
            if (idx > 0 && BeastId.entries[idx - 1] !in s.ownedBeasts) return@update s
            s.copy(
                gold = s.gold - beast.cost,
                ownedBeasts = s.ownedBeasts + beast,
                selectedBeast = beast,
            )
        }
        persist()
    }

    fun selectBeast(beast: BeastId) {
        _state.update { s ->
            if (beast !in s.ownedBeasts) return@update s
            s.copy(selectedBeast = beast)
        }
    }

    fun startRaid() {
        _state.update { s ->
            if (s.raid.active) return@update s
            val beast = s.selectedBeast ?: return@update s
            if (beast !in s.ownedBeasts) return@update s
            var seconds = beast.raidSeconds
            if (s.schools[SpellSchool.LIGHTNING]?.familiarOwned == true) {
                seconds = (seconds * 0.7).toInt().coerceAtLeast(3)
            }
            s.copy(
                raid = RaidState(
                    beast = beast,
                    endsAtMs = System.currentTimeMillis() + seconds * 1000L,
                    active = true,
                )
            )
        }
        persist()
    }

    fun slamGate() {
        _state.update { s ->
            if (!s.canSlamGate) return@update s
            val pages = 1 + (s.gold / 50_000L).toInt().coerceAtMost(5)
            val schools = mapOf(
                SpellSchool.FIRE to SchoolProgress(unlocked = true, upgradeLevel = 0),
                SpellSchool.LIGHTNING to SchoolProgress(),
                SpellSchool.ICE to SchoolProgress(),
                SpellSchool.NECROMANCY to SchoolProgress(
                    unlocked = true, // sketch unlock
                    upgradeLevel = 0,
                ),
            )
            s.copy(
                gold = 1L, // starter coin
                selectedSchool = SpellSchool.FIRE,
                schools = schools,
                grabberOwned = false,
                ownedBeasts = emptySet(),
                selectedBeast = null,
                raid = RaidState(),
                grimoirePages = s.grimoirePages + pages,
                necromancyUnlocked = true,
                prestigeCount = s.prestigeCount + 1,
                flyingSpells = emptyList(),
                flyingCoins = emptyList(),
                currentTab = Tab.PRESTIGE,
            )
        }
        persist()
    }

    private fun persist() {
        if (!loaded) return
        viewModelScope.launch {
            repo.save(_state.value)
        }
    }

    override fun onCleared() {
        persist()
        super.onCleared()
    }
}
