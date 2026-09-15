package com.purplegate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purplegate.app.game.BeastId
import com.purplegate.app.game.GameState
import com.purplegate.app.game.formatGold
import com.purplegate.app.ui.theme.GrapePurple
import com.purplegate.app.ui.theme.HudBg
import com.purplegate.app.ui.theme.MudBrown
import com.purplegate.app.ui.theme.TorchOrange

@Composable
fun BeastsScreen(
    state: GameState,
    onBuy: (BeastId) -> Unit,
    onSelect: (BeastId) -> Unit,
    onRaid: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Beasts & Raids", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Send a beast through the purple gate. Timer only — no aiming. Return = gold gulp.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
        if (state.raid.active && state.raid.beast != null) {
            val left = ((state.raid.endsAtMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0)
            Text(
                "⚔️ ${state.raid.beast.displayName} raiding… ${left}s",
                color = TorchOrange,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Button(
                onClick = onRaid,
                enabled = state.selectedBeast != null && state.selectedBeast in state.ownedBeasts,
                colors = ButtonDefaults.buttonColors(containerColor = GrapePurple),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start Raid${state.selectedBeast?.let { " — ${it.displayName}" } ?: ""}")
            }
        }
        Spacer(Modifier.height(4.dp))
        for (beast in BeastId.entries) {
            val owned = beast in state.ownedBeasts
            val selected = state.selectedBeast == beast
            val idx = BeastId.entries.indexOf(beast)
            val prevOwned = idx == 0 || BeastId.entries[idx - 1] in state.ownedBeasts
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MudBrown, RoundedCornerShape(10.dp))
                    .then(
                        if (selected) Modifier.border(2.dp, TorchOrange, RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clickable(enabled = owned) { onSelect(beast) }
                    .padding(12.dp),
            ) {
                Text(
                    "${beast.emoji} ${beast.displayName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Raid ${beast.raidSeconds}s · Reward ~${formatGold(beast.baseReward)}g",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(6.dp))
                when {
                    owned -> Text(
                        if (selected) "Parked at the gate" else "Tap to select",
                        color = Color(0xFF7CFC00),
                        fontSize = 12.sp,
                    )
                    !prevOwned -> Text("Unlock previous beast first", color = Color.Gray, fontSize = 12.sp)
                    else -> Button(
                        onClick = { onBuy(beast) },
                        enabled = state.gold >= beast.cost,
                        colors = ButtonDefaults.buttonColors(containerColor = GrapePurple),
                    ) {
                        Text("Buy — ${formatGold(beast.cost)}g")
                    }
                }
            }
        }
    }
}
