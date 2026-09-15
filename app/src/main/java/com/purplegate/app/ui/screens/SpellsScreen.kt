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
import com.purplegate.app.game.GameState
import com.purplegate.app.game.SpellSchool
import com.purplegate.app.game.familiarCost
import com.purplegate.app.game.formatGold
import com.purplegate.app.game.schoolUnlockCost
import com.purplegate.app.game.upgradeCost
import com.purplegate.app.game.upgradeNames
import com.purplegate.app.ui.theme.GrapePurple
import com.purplegate.app.ui.theme.HudBg
import com.purplegate.app.ui.theme.MudBrown
import com.purplegate.app.ui.theme.TorchOrange

@Composable
fun SpellsScreen(
    state: GameState,
    onSelect: (SpellSchool) -> Unit,
    onUpgrade: (SpellSchool) -> Unit,
    onFamiliar: (SpellSchool) -> Unit,
    onUnlock: (SpellSchool) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Spell Schools", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Tap a school to equip it for casting. Unlock Fire → Lightning → Ice one by one.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
        for (school in listOf(SpellSchool.FIRE, SpellSchool.LIGHTNING, SpellSchool.ICE)) {
            SchoolCard(state, school, onSelect, onUpgrade, onFamiliar, onUnlock)
        }
        if (state.necromancyUnlocked) {
            SchoolCard(state, SpellSchool.NECROMANCY, onSelect, onUpgrade, onFamiliar, onUnlock)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Familiars auto-cast while idle. Grabber goblin lives on Camp.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SchoolCard(
    state: GameState,
    school: SpellSchool,
    onSelect: (SpellSchool) -> Unit,
    onUpgrade: (SpellSchool) -> Unit,
    onFamiliar: (SpellSchool) -> Unit,
    onUnlock: (SpellSchool) -> Unit,
) {
    val p = state.schools[school] ?: return
    val selected = state.selectedSchool == school
    val names = upgradeNames(school)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MudBrown, RoundedCornerShape(10.dp))
            .then(
                if (selected) Modifier.border(2.dp, TorchOrange, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(enabled = p.unlocked) { onSelect(school) }
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${school.emoji} ${school.displayName}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            if (selected) Text("EQUIPPED", color = TorchOrange, fontSize = 11.sp)
        }
        if (!p.unlocked) {
            val cost = schoolUnlockCost(school)
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { onUnlock(school) },
                colors = ButtonDefaults.buttonColors(containerColor = GrapePurple),
                enabled = state.gold >= cost,
            ) {
                Text("Unlock — ${formatGold(cost)}g")
            }
            return
        }
        Text(
            "Current: ${names.getOrElse(p.upgradeLevel.coerceAtMost(names.lastIndex)) { names.last() }}",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(6.dp))
        if (!p.familiarOwned && p.upgradeLevel < 3) {
            val cost = upgradeCost(school, p.upgradeLevel)
            val nextName = names.getOrElse(p.upgradeLevel + 1) { "Upgrade" }
            Button(
                onClick = { onUpgrade(school) },
                colors = ButtonDefaults.buttonColors(containerColor = GrapePurple),
                enabled = state.gold >= cost,
            ) {
                Text("Upgrade → $nextName (${formatGold(cost)}g)")
            }
        } else if (!p.familiarOwned) {
            val cost = familiarCost(school)
            val famName = names.last()
            Button(
                onClick = { onFamiliar(school) },
                colors = ButtonDefaults.buttonColors(containerColor = TorchOrange),
                enabled = state.gold >= cost,
            ) {
                Text("Hire $famName (${formatGold(cost)}g)")
            }
        } else {
            Text("Familiar online — idle auto-cast!", color = Color(0xFF7CFC00), fontSize = 13.sp)
        }
    }
}
