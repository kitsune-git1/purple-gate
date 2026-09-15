package com.purplegate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purplegate.app.game.GameState
import com.purplegate.app.game.SpellSchool
import com.purplegate.app.game.formatGold
import com.purplegate.app.ui.theme.GrapePurpleDeep
import com.purplegate.app.ui.theme.HudBg
import com.purplegate.app.ui.theme.MudBrown
import com.purplegate.app.ui.theme.TorchOrange

@Composable
fun PrestigeScreen(
    state: GameState,
    onSlam: () -> Unit,
) {
    val fireOk = state.schools[SpellSchool.FIRE]?.familiarOwned == true
    val lightOk = state.schools[SpellSchool.LIGHTNING]?.familiarOwned == true
    val iceOk = state.schools[SpellSchool.ICE]?.familiarOwned == true
    val pileOk = state.gold >= GameState.PRESTIGE_GOLD_THRESHOLD

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Slam the Gate", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "When Fire, Lightning, and Ice familiars are online and the gold pile is huge, slam the gate. Keep Grimoire pages. Unlock Necromancy. New city starts with Fire.",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(MudBrown, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CheckLine("🔥 Fire familiar", fireOk)
            CheckLine("⚡ Lightning familiar", lightOk)
            CheckLine("❄️ Ice familiar", iceOk)
            CheckLine(
                "💰 Huge pile (${formatGold(GameState.PRESTIGE_GOLD_THRESHOLD)}g+)",
                pileOk,
            )
        }
        Text(
            "Grimoire pages: ${state.grimoirePages}  ·  Prestiges: ${state.prestigeCount}",
            color = TorchOrange,
            fontWeight = FontWeight.Bold,
        )
        if (state.necromancyUnlocked) {
            Text(
                "💀 Necromancy unlocked (stub school on Spells tab). Bone spark awaits.",
                color = Color(0xFFECEFF1),
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSlam,
            enabled = state.canSlamGate,
            colors = ButtonDefaults.buttonColors(
                containerColor = GrapePurpleDeep,
                disabledContainerColor = Color.DarkGray,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.canSlamGate) "SLAM THE GATE" else "Requirements not met")
        }
        Text(
            "Resets camp, gold, schools, beasts. Keeps pages + Necromancy unlock.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun CheckLine(label: String, ok: Boolean) {
    Text(
        "${if (ok) "✅" else "⬜"} $label",
        color = if (ok) Color(0xFF7CFC00) else Color.White.copy(alpha = 0.6f),
        fontSize = 14.sp,
    )
}
