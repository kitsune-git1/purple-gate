package com.purplegate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purplegate.app.game.GameState
import com.purplegate.app.game.SpellSchool
import com.purplegate.app.game.formatGold
import com.purplegate.app.ui.components.CampCanvas
import com.purplegate.app.ui.theme.GrapePurple
import com.purplegate.app.ui.theme.HudBg
import com.purplegate.app.ui.theme.SlimeGreen
import com.purplegate.app.ui.theme.TorchOrange

@Composable
fun CampScreen(
    state: GameState,
    onCast: () -> Unit,
    onSelectSchool: (SpellSchool) -> Unit,
    onBuyGrabber: () -> Unit,
    onDismissOffline: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        CampCanvas(
            state = state,
            onCast = onCast,
            modifier = Modifier.fillMaxSize(),
        )

        // School ring
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(HudBg)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val schools = buildList {
                add(SpellSchool.FIRE)
                add(SpellSchool.LIGHTNING)
                add(SpellSchool.ICE)
                if (state.necromancyUnlocked) add(SpellSchool.NECROMANCY)
            }
            schools.forEach { school ->
                val unlocked = state.schools[school]?.unlocked == true
                val selected = state.selectedSchool == school
                Button(
                    onClick = { onSelectSchool(school) },
                    enabled = unlocked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) TorchOrange else GrapePurple,
                        disabledContainerColor = Color.DarkGray,
                    ),
                ) {
                    Text(school.emoji, fontSize = 18.sp)
                }
            }
        }

        // Cast hint + grabber hire
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Tap the portal — ${state.selectedSchool.emoji} ${state.selectedSchool.displayName}",
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            if (!state.grabberOwned) {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onBuyGrabber,
                    enabled = state.gold >= 40,
                    colors = ButtonDefaults.buttonColors(containerColor = SlimeGreen),
                ) {
                    Text("Hire Grabber goblin — 40g", color = Color.Black, fontSize = 12.sp)
                }
            }
        }

        if (state.showOfflineBanner && state.offlineGranted > 0) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .background(HudBg, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "While you were away: +${formatGold(state.offlineGranted)}g",
                    color = TorchOrange,
                    fontWeight = FontWeight.Bold,
                )
                Button(onClick = onDismissOffline) { Text("Collect") }
            }
        }
    }
}
