package com.purplegate.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.purplegate.app.game.FlyingCoin
import com.purplegate.app.game.FlyingSpell
import com.purplegate.app.game.GameState
import com.purplegate.app.game.SpellSchool
import com.purplegate.app.ui.theme.CampDirt
import com.purplegate.app.ui.theme.EmberTint
import com.purplegate.app.ui.theme.FrostTint
import com.purplegate.app.ui.theme.Gold
import com.purplegate.app.ui.theme.GoldBright
import com.purplegate.app.ui.theme.GrapePurple
import com.purplegate.app.ui.theme.GrapePurpleBright
import com.purplegate.app.ui.theme.GrapePurpleDeep
import com.purplegate.app.ui.theme.MudBrown
import com.purplegate.app.ui.theme.MudDark
import com.purplegate.app.ui.theme.SlimeGreen
import com.purplegate.app.ui.theme.SparkTint
import com.purplegate.app.ui.theme.TorchOrange
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CampCanvas(
    state: GameState,
    onCast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onCast() }
            }
    ) {
        val w = size.width
        val h = size.height
        val portalCenter = Offset(w * 0.5f, h * 0.42f)
        val portalRx = w * 0.18f
        val portalRy = h * 0.12f
        val pileBase = Offset(w * 0.5f, h * 0.72f)

        // Dirt ground
        drawRect(CampDirt)
        // Sky-ish night mud gradient top
        drawRect(
            brush = Brush.verticalGradient(
                listOf(MudDark, CampDirt),
                startY = 0f,
                endY = h * 0.35f,
            ),
            size = Size(w, h * 0.35f),
        )

        // Gold pile behind portal
        drawGoldPile(pileBase, state.pileStage, state.hottestSchool, w)

        // Goblin grabber
        if (state.grabberOwned) {
            drawGoblin(Offset(w * 0.18f, h * 0.78f))
        }

        // Parked beast
        val parked = if (state.raid.active) null else state.selectedBeast
        if (parked != null) {
            drawBeastSilhouette(Offset(w * 0.82f, h * 0.76f), parked.emoji.hashCode())
        }

        // Portal swirl (purple oval)
        drawPortal(portalCenter, portalRx, portalRy, state.tick)

        // Beast walking into portal during raid
        if (state.raid.active && state.raid.beast != null) {
            val remaining = (state.raid.endsAtMs - System.currentTimeMillis()).coerceAtLeast(0)
            val total = state.raid.beast.raidSeconds * 1000f
            val walk = 1f - (remaining / total).coerceIn(0f, 1f)
            // First half: walk in; while inside stay hidden; near end emerge for return feel on complete
            if (walk < 0.35f) {
                val t = walk / 0.35f
                val x = w * 0.82f + (portalCenter.x - w * 0.82f) * t
                val y = h * 0.76f + (portalCenter.y - h * 0.76f) * t
                drawBeastSilhouette(Offset(x, y), state.raid.beast.emoji.hashCode(), alpha = 1f - t * 0.5f)
            }
        }

        // Flying spells into portal
        state.flyingSpells.forEach { spell ->
            drawFlyingSpell(spell, portalCenter, w, h)
        }

        // Flying coins onto pile
        state.flyingCoins.forEach { coin ->
            drawFlyingCoin(coin, portalCenter, pileBase)
        }

        // Torch accents
        drawCircle(TorchOrange.copy(alpha = 0.35f), radius = 18.dp.toPx(), center = Offset(w * 0.12f, h * 0.3f))
        drawCircle(TorchOrange.copy(alpha = 0.35f), radius = 18.dp.toPx(), center = Offset(w * 0.88f, h * 0.3f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPortal(
    center: Offset,
    rx: Float,
    ry: Float,
    tick: Long,
) {
    val pulse = 1f + 0.04f * sin(tick * 0.15f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(GrapePurpleBright, GrapePurple, GrapePurpleDeep, Color.Black),
            center = center,
            radius = rx * pulse,
        ),
        topLeft = Offset(center.x - rx * pulse, center.y - ry * pulse),
        size = Size(rx * 2 * pulse, ry * 2 * pulse),
    )
    // Swirl rings
    for (i in 1..3) {
        val a = (tick * 0.08f + i) % 6.28f
        drawArc(
            color = GrapePurpleBright.copy(alpha = 0.45f),
            startAngle = Math.toDegrees(a.toDouble()).toFloat(),
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(center.x - rx * (0.5f + i * 0.15f), center.y - ry * (0.5f + i * 0.15f)),
            size = Size(rx * 2 * (0.5f + i * 0.15f), ry * 2 * (0.5f + i * 0.15f)),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
    // Rim
    drawOval(
        color = GrapePurpleBright,
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2, ry * 2),
        style = Stroke(width = 4.dp.toPx()),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGoldPile(
    base: Offset,
    stage: Int,
    hottest: SpellSchool,
    canvasW: Float,
) {
    val tint = when (hottest) {
        SpellSchool.FIRE -> EmberTint
        SpellSchool.LIGHTNING -> SparkTint
        SpellSchool.ICE -> FrostTint
        SpellSchool.NECROMANCY -> Color(0xFFECEFF1)
    }
    val goldColor = Color(
        red = (Gold.red * 0.7f + tint.red * 0.3f).coerceIn(0f, 1f),
        green = (Gold.green * 0.7f + tint.green * 0.3f).coerceIn(0f, 1f),
        blue = (Gold.blue * 0.7f + tint.blue * 0.3f).coerceIn(0f, 1f),
        alpha = 1f,
    )
    if (stage <= 0) {
        // starter coin
        drawCircle(goldColor, radius = 6.dp.toPx(), center = base)
        return
    }
    val width = canvasW * (0.15f + stage * 0.06f)
    val height = canvasW * (0.06f + stage * 0.035f)
    val path = Path().apply {
        moveTo(base.x - width, base.y)
        quadraticTo(base.x - width * 0.5f, base.y - height * 1.6f, base.x, base.y - height)
        quadraticTo(base.x + width * 0.5f, base.y - height * 1.6f, base.x + width, base.y)
        close()
    }
    drawPath(path, goldColor)
    // Sparkle crest on huge pile
    if (stage >= 4) {
        drawCircle(GoldBright, radius = 5.dp.toPx(), center = Offset(base.x, base.y - height - 8.dp.toPx()))
        drawCircle(GoldBright.copy(alpha = 0.6f), radius = 10.dp.toPx(), center = Offset(base.x, base.y - height - 8.dp.toPx()))
    }
    // Coin dots for early stages
    if (stage in 1..2) {
        for (i in 0 until stage * 3) {
            val ox = (i % 3 - 1) * 14.dp.toPx()
            val oy = (i / 3) * -10.dp.toPx()
            drawCircle(GoldBright, radius = 5.dp.toPx(), center = Offset(base.x + ox, base.y + oy - 4.dp.toPx()))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGoblin(pos: Offset) {
    // Chunky pixel goblin: slime green body
    drawRoundRect(
        SlimeGreen,
        topLeft = Offset(pos.x - 14.dp.toPx(), pos.y - 28.dp.toPx()),
        size = Size(28.dp.toPx(), 32.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    drawCircle(SlimeGreen, radius = 12.dp.toPx(), center = Offset(pos.x, pos.y - 36.dp.toPx()))
    // Eyes
    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(pos.x - 4.dp.toPx(), pos.y - 38.dp.toPx()))
    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(pos.x + 4.dp.toPx(), pos.y - 38.dp.toPx()))
    drawCircle(Color.Black, radius = 1.5.dp.toPx(), center = Offset(pos.x - 4.dp.toPx(), pos.y - 38.dp.toPx()))
    drawCircle(Color.Black, radius = 1.5.dp.toPx(), center = Offset(pos.x + 4.dp.toPx(), pos.y - 38.dp.toPx()))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBeastSilhouette(
    pos: Offset,
    seed: Int,
    alpha: Float = 1f,
) {
    val color = MudBrown.copy(alpha = alpha)
    drawRoundRect(
        color,
        topLeft = Offset(pos.x - 20.dp.toPx(), pos.y - 24.dp.toPx()),
        size = Size(40.dp.toPx(), 28.dp.toPx()),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    drawCircle(color, radius = 14.dp.toPx(), center = Offset(pos.x - 18.dp.toPx(), pos.y - 28.dp.toPx()))
    // Ears / horns vary by seed
    if (seed % 2 == 0) {
        drawCircle(color, radius = 5.dp.toPx(), center = Offset(pos.x - 28.dp.toPx(), pos.y - 40.dp.toPx()))
        drawCircle(color, radius = 5.dp.toPx(), center = Offset(pos.x - 10.dp.toPx(), pos.y - 40.dp.toPx()))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFlyingSpell(
    spell: FlyingSpell,
    portal: Offset,
    w: Float,
    h: Float,
) {
    val start = Offset(w * 0.5f, h * 0.88f)
    val t = spell.progress
    val x = start.x + (portal.x - start.x) * t
    val y = start.y + (portal.y - start.y) * t - sin(t * Math.PI).toFloat() * 40f
    val color = when (spell.school) {
        SpellSchool.FIRE -> TorchOrange
        SpellSchool.LIGHTNING -> SparkTint
        SpellSchool.ICE -> FrostTint
        SpellSchool.NECROMANCY -> Color(0xFFECEFF1)
    }
    drawCircle(color, radius = 10.dp.toPx() * (1.2f - t * 0.4f), center = Offset(x, y))
    drawCircle(color.copy(alpha = 0.4f), radius = 18.dp.toPx() * (1.2f - t * 0.4f), center = Offset(x, y))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFlyingCoin(
    coin: FlyingCoin,
    portal: Offset,
    pile: Offset,
) {
    val t = coin.progress
    val arc = sin(t * Math.PI).toFloat() * 60f
    val x = portal.x + (pile.x - portal.x) * t + coin.arcOffset * 30f
    val y = portal.y + (pile.y - portal.y) * t - arc
    drawCircle(Gold, radius = 7.dp.toPx(), center = Offset(x, y))
    drawCircle(GoldBright, radius = 3.dp.toPx(), center = Offset(x - 2.dp.toPx(), y - 2.dp.toPx()))
}
