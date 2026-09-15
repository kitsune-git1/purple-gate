package com.purplegate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purplegate.app.game.GameState
import com.purplegate.app.game.Tab
import com.purplegate.app.game.formatGold
import com.purplegate.app.game.formatGps
import com.purplegate.app.ui.theme.Gold
import com.purplegate.app.ui.theme.GrapePurple
import com.purplegate.app.ui.theme.HudBg
import com.purplegate.app.ui.theme.MudBrown
import com.purplegate.app.ui.theme.TorchOrange
import com.purplegate.app.viewmodel.GameViewModel

@Composable
fun MainGameScreen(vm: GameViewModel, state: GameState) {
    Column(Modifier.fillMaxSize().background(MudBrown)) {
        HudBar(state)
        Column(Modifier.weight(1f).fillMaxWidth()) {
            when (state.currentTab) {
                Tab.CAMP -> CampScreen(
                    state = state,
                    onCast = vm::castSpell,
                    onSelectSchool = vm::selectSchool,
                    onBuyGrabber = vm::buyGrabber,
                    onDismissOffline = vm::dismissOfflineBanner,
                )
                Tab.SPELLS -> SpellsScreen(
                    state = state,
                    onSelect = vm::selectSchool,
                    onUpgrade = vm::buyUpgrade,
                    onFamiliar = vm::buyFamiliar,
                    onUnlock = vm::unlockSchool,
                )
                Tab.BEASTS -> BeastsScreen(
                    state = state,
                    onBuy = vm::buyBeast,
                    onSelect = vm::selectBeast,
                    onRaid = vm::startRaid,
                )
                Tab.PRESTIGE -> PrestigeScreen(
                    state = state,
                    onSlam = vm::slamGate,
                )
            }
        }
        NavigationBar(containerColor = HudBg) {
            val items = listOf(
                Tab.CAMP to "Camp",
                Tab.SPELLS to "Spells",
                Tab.BEASTS to "Beasts",
                Tab.PRESTIGE to "Prestige",
            )
            items.forEach { (tab, label) ->
                NavigationBarItem(
                    selected = state.currentTab == tab,
                    onClick = { vm.selectTab(tab) },
                    icon = {
                        Text(
                            when (tab) {
                                Tab.CAMP -> "🏕️"
                                Tab.SPELLS -> "✨"
                                Tab.BEASTS -> "🐾"
                                Tab.PRESTIGE -> "📕"
                            },
                            fontSize = 18.sp,
                        )
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TorchOrange,
                        selectedTextColor = TorchOrange,
                        indicatorColor = GrapePurple.copy(alpha = 0.5f),
                        unselectedIconColor = Color.White.copy(alpha = 0.7f),
                        unselectedTextColor = Color.White.copy(alpha = 0.7f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun HudBar(state: GameState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HudBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "💰 ${formatGold(state.gold)}",
            color = Gold,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            formatGps(state.goldPerSec),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
        )
        val raidText = if (state.raid.active && state.raid.beast != null) {
            val left = ((state.raid.endsAtMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0)
            "⚔️ ${left}s"
        } else {
            "⚔️ —"
        }
        Text(raidText, color = TorchOrange, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
