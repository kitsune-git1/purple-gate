package com.purplegate.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.purplegate.app.R
import com.purplegate.app.game.BeastId
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

/** Optional drawables — missing ids fall back to Canvas shapes. */
data class CampSprites(
    val campBg: ImageBitmap? = null,
    val portal: ImageBitmap? = null,
    val portalSwirl: ImageBitmap? = null,
    val goblinIdle: ImageBitmap? = null,
    val goblinCast: ImageBitmap? = null,
    val grabber: ImageBitmap? = null,
    val coin: ImageBitmap? = null,
    val goldPiles: List<ImageBitmap?> = emptyList(),
    val spellFire: ImageBitmap? = null,
    val spellLightning: ImageBitmap? = null,
    val spellIce: ImageBitmap? = null,
    val beasts: Map<BeastId, ImageBitmap?> = emptyMap(),
)

@Composable
fun rememberCampSprites(): CampSprites {
    val ctx = LocalContext.current
    return remember(ctx) {
        fun load(id: Int): ImageBitmap? = try {
            ImageBitmap.imageResource(ctx.resources, id)
        } catch (_: Exception) {
            null
        }
        CampSprites(
            campBg = load(R.drawable.camp_bg),
            portal = load(R.drawable.portal_purple),
            portalSwirl = load(R.drawable.portal_swirl),
            goblinIdle = load(R.drawable.goblin_idle),
            goblinCast = load(R.drawable.goblin_cast),
            grabber = load(R.drawable.grabber_goblin),
            coin = load(R.drawable.coin_single),
            goldPiles = listOf(
                load(R.drawable.gold_pile_1),
                load(R.drawable.gold_pile_2),
                load(R.drawable.gold_pile_3),
                load(R.drawable.gold_pile_4),
                load(R.drawable.gold_pile_5),
            ),
            spellFire = load(R.drawable.spell_fire),
            spellLightning = load(R.drawable.spell_lightning),
            spellIce = load(R.drawable.spell_ice),
            beasts = mapOf(
                BeastId.DIRE_RAT to load(R.drawable.beast_dire_rat),
                BeastId.WOLF to load(R.drawable.beast_wolf),
                BeastId.WAR_BOAR to load(R.drawable.beast_war_boar),
                BeastId.TROLL to load(R.drawable.beast_troll),
                BeastId.MINOTAUR to load(R.drawable.beast_minotaur),
            ),
        )
    }
}

@Composable
fun CampCanvas(
    state: GameState,
    onCast: () -> Unit,
    modifier: Modifier = Modifier,
    sprites: CampSprites = rememberCampSprites(),
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

        // Backdrop
        val bg = sprites.campBg
        if (bg != null) {
            drawSprite(bg, dst = Offset.Zero, dstSize = Size(w, h))
        } else {
            drawRect(CampDirt)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(MudDark, CampDirt),
                    startY = 0f,
                    endY = h * 0.35f,
                ),
                size = Size(w, h * 0.35f),
            )
        }

        // Gold pile behind portal
        drawGoldPileSprite(pileBase, state.pileStage, state.hottestSchool, w, sprites)

        // Goblin / grabber
        if (state.grabberOwned) {
            val casting = state.flyingSpells.isNotEmpty()
            val goblinBmp = when {
                casting && sprites.goblinCast != null -> sprites.goblinCast
                sprites.grabber != null -> sprites.grabber
                sprites.goblinIdle != null -> sprites.goblinIdle
                else -> null
            }
            val goblinPos = Offset(w * 0.18f, h * 0.78f)
            if (goblinBmp != null) {
                val gh = h * 0.12f
                val scale = gh / goblinBmp.height
                val gw = goblinBmp.width * scale
                drawSprite(
                    goblinBmp,
                    dst = Offset(goblinPos.x - gw / 2f, goblinPos.y - gh),
                    dstSize = Size(gw, gh),
                )
            } else {
                drawGoblin(goblinPos)
            }
        }

        // Parked beast
        val parked = if (state.raid.active) null else state.selectedBeast
        if (parked != null) {
            drawBeastAt(Offset(w * 0.82f, h * 0.76f), parked, sprites, alpha = 1f, h = h)
        }

        // Portal (ornate frame) + animated swirl in the oval
        val portalBmp = sprites.portal
        if (portalBmp != null) {
            val pulse = 1f + 0.04f * sin(state.tick * 0.15f)
            val ph = portalRy * 2.6f * pulse
            val scale = ph / portalBmp.height
            val pw = portalBmp.width * scale
            val portalDst = Offset(portalCenter.x - pw / 2f, portalCenter.y - ph / 2f)
            drawSprite(portalBmp, dst = portalDst, dstSize = Size(pw, ph))

            val swirlBmp = sprites.portalSwirl
            if (swirlBmp != null) {
                val swirlW = pw * 0.52f
                val swirlH = ph * 0.58f
                val angle = (state.tick * 10f) % 360f
                val oval = Path().apply {
                    addOval(
                        Rect(
                            left = portalCenter.x - swirlW / 2f,
                            top = portalCenter.y - swirlH / 2f,
                            right = portalCenter.x + swirlW / 2f,
                            bottom = portalCenter.y + swirlH / 2f,
                        ),
                    )
                }
                clipPath(oval) {
                    rotate(degrees = angle, pivot = portalCenter) {
                        drawSprite(
                            swirlBmp,
                            dst = Offset(portalCenter.x - swirlW / 2f, portalCenter.y - swirlH / 2f),
                            dstSize = Size(swirlW, swirlH),
                            alpha = 0.72f,
                        )
                    }
                }
            }
        } else {
            drawPortal(portalCenter, portalRx, portalRy, state.tick)
        }

        // Beast walking into portal during raid
        if (state.raid.active && state.raid.beast != null) {
            val remaining = (state.raid.endsAtMs - System.currentTimeMillis()).coerceAtLeast(0)
            val total = state.raid.beast.raidSeconds * 1000f
            val walk = 1f - (remaining / total).coerceIn(0f, 1f)
            if (walk < 0.35f) {
                val t = walk / 0.35f
                val x = w * 0.82f + (portalCenter.x - w * 0.82f) * t
                val y = h * 0.76f + (portalCenter.y - h * 0.76f) * t
                drawBeastAt(Offset(x, y), state.raid.beast, sprites, alpha = 1f - t * 0.5f, h = h)
            }
        }

        // Flying spells into portal
        state.flyingSpells.forEach { spell ->
            drawFlyingSpellSprite(spell, portalCenter, w, h, sprites)
        }

        // Flying coins onto pile
        state.flyingCoins.forEach { coin ->
            drawFlyingCoinSprite(coin, portalCenter, pileBase, sprites)
        }

        // Torch accents (extra glow even when bg has torches)
        drawCircle(TorchOrange.copy(alpha = 0.25f), radius = 18.dp.toPx(), center = Offset(w * 0.12f, h * 0.3f))
        drawCircle(TorchOrange.copy(alpha = 0.25f), radius = 18.dp.toPx(), center = Offset(w * 0.88f, h * 0.3f))
    }
}

private fun DrawScope.drawSprite(
    bitmap: ImageBitmap,
    dst: Offset,
    dstSize: Size,
    alpha: Float = 1f,
) {
    drawImage(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset(dst.x.toInt(), dst.y.toInt()),
        dstSize = IntSize(dstSize.width.toInt().coerceAtLeast(1), dstSize.height.toInt().coerceAtLeast(1)),
        alpha = alpha,
        filterQuality = FilterQuality.None, // keep pixels chunky
    )
}

private fun DrawScope.drawGoldPileSprite(
    base: Offset,
    stage: Int,
    hottest: SpellSchool,
    canvasW: Float,
    sprites: CampSprites,
) {
    if (stage <= 0) {
        val coin = sprites.coin
        if (coin != null) {
            val s = 14.dp.toPx()
            drawSprite(coin, Offset(base.x - s / 2, base.y - s / 2), Size(s, s))
        } else {
            drawCircle(Gold, radius = 6.dp.toPx(), center = base)
        }
        return
    }
    val idx = (stage - 1).coerceIn(0, 4)
    val pile = sprites.goldPiles.getOrNull(idx)
    if (pile != null) {
        val pw = canvasW * (0.28f + stage * 0.04f)
        val scale = pw / pile.width
        val ph = pile.height * scale
        drawSprite(
            pile,
            dst = Offset(base.x - pw / 2f, base.y - ph * 0.85f),
            dstSize = Size(pw, ph),
        )
        // school tint wash on crest
        if (stage >= 4) {
            val tint = when (hottest) {
                SpellSchool.FIRE -> EmberTint
                SpellSchool.LIGHTNING -> SparkTint
                SpellSchool.ICE -> FrostTint
                SpellSchool.NECROMANCY -> Color(0xFFECEFF1)
            }
            drawCircle(tint.copy(alpha = 0.35f), radius = 12.dp.toPx(), center = Offset(base.x, base.y - ph * 0.7f))
        }
    } else {
        drawGoldPileFallback(base, stage, hottest, canvasW)
    }
}

private fun DrawScope.drawBeastAt(
    pos: Offset,
    beast: BeastId,
    sprites: CampSprites,
    alpha: Float,
    h: Float,
) {
    val bmp = sprites.beasts[beast]
    if (bmp != null) {
        val bh = h * 0.14f
        val scale = bh / bmp.height
        val bw = bmp.width * scale
        drawSprite(
            bmp,
            dst = Offset(pos.x - bw / 2f, pos.y - bh),
            dstSize = Size(bw, bh),
            alpha = alpha,
        )
    } else {
        drawBeastSilhouette(pos, beast.emoji.hashCode(), alpha)
    }
}

private fun DrawScope.drawFlyingSpellSprite(
    spell: FlyingSpell,
    portal: Offset,
    w: Float,
    h: Float,
    sprites: CampSprites,
) {
    val start = Offset(w * 0.5f, h * 0.88f)
    val t = spell.progress
    val x = start.x + (portal.x - start.x) * t
    val y = start.y + (portal.y - start.y) * t - sin(t * Math.PI).toFloat() * 40f
    val bmp = when (spell.school) {
        SpellSchool.FIRE -> sprites.spellFire
        SpellSchool.LIGHTNING -> sprites.spellLightning
        SpellSchool.ICE -> sprites.spellIce
        SpellSchool.NECROMANCY -> sprites.spellIce
    }
    if (bmp != null) {
        val s = 28.dp.toPx() * (1.2f - t * 0.4f)
        drawSprite(bmp, Offset(x - s / 2, y - s / 2), Size(s, s))
    } else {
        val color = when (spell.school) {
            SpellSchool.FIRE -> TorchOrange
            SpellSchool.LIGHTNING -> SparkTint
            SpellSchool.ICE -> FrostTint
            SpellSchool.NECROMANCY -> Color(0xFFECEFF1)
        }
        drawCircle(color, radius = 10.dp.toPx() * (1.2f - t * 0.4f), center = Offset(x, y))
        drawCircle(color.copy(alpha = 0.4f), radius = 18.dp.toPx() * (1.2f - t * 0.4f), center = Offset(x, y))
    }
}

private fun DrawScope.drawFlyingCoinSprite(
    coin: FlyingCoin,
    portal: Offset,
    pile: Offset,
    sprites: CampSprites,
) {
    val t = coin.progress
    val arc = sin(t * Math.PI).toFloat() * 60f
    val x = portal.x + (pile.x - portal.x) * t + coin.arcOffset * 30f
    val y = portal.y + (pile.y - portal.y) * t - arc
    val bmp = sprites.coin
    if (bmp != null) {
        val s = 16.dp.toPx()
        drawSprite(bmp, Offset(x - s / 2, y - s / 2), Size(s, s))
    } else {
        drawCircle(Gold, radius = 7.dp.toPx(), center = Offset(x, y))
        drawCircle(GoldBright, radius = 3.dp.toPx(), center = Offset(x - 2.dp.toPx(), y - 2.dp.toPx()))
    }
}

private fun DrawScope.drawPortal(
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
    drawOval(
        color = GrapePurpleBright,
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2, ry * 2),
        style = Stroke(width = 4.dp.toPx()),
    )
}

private fun DrawScope.drawGoldPileFallback(
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
    val width = canvasW * (0.15f + stage * 0.06f)
    val height = canvasW * (0.06f + stage * 0.035f)
    val path = Path().apply {
        moveTo(base.x - width, base.y)
        quadraticTo(base.x - width * 0.5f, base.y - height * 1.6f, base.x, base.y - height)
        quadraticTo(base.x + width * 0.5f, base.y - height * 1.6f, base.x + width, base.y)
        close()
    }
    drawPath(path, goldColor)
    if (stage >= 4) {
        drawCircle(GoldBright, radius = 5.dp.toPx(), center = Offset(base.x, base.y - height - 8.dp.toPx()))
        drawCircle(GoldBright.copy(alpha = 0.6f), radius = 10.dp.toPx(), center = Offset(base.x, base.y - height - 8.dp.toPx()))
    }
    if (stage in 1..2) {
        for (i in 0 until stage * 3) {
            val ox = (i % 3 - 1) * 14.dp.toPx()
            val oy = (i / 3) * -10.dp.toPx()
            drawCircle(GoldBright, radius = 5.dp.toPx(), center = Offset(base.x + ox, base.y + oy - 4.dp.toPx()))
        }
    }
}

private fun DrawScope.drawGoblin(pos: Offset) {
    drawRoundRect(
        SlimeGreen,
        topLeft = Offset(pos.x - 14.dp.toPx(), pos.y - 28.dp.toPx()),
        size = Size(28.dp.toPx(), 32.dp.toPx()),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    drawCircle(SlimeGreen, radius = 12.dp.toPx(), center = Offset(pos.x, pos.y - 36.dp.toPx()))
    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(pos.x - 4.dp.toPx(), pos.y - 38.dp.toPx()))
    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(pos.x + 4.dp.toPx(), pos.y - 38.dp.toPx()))
    drawCircle(Color.Black, radius = 1.5.dp.toPx(), center = Offset(pos.x - 4.dp.toPx(), pos.y - 38.dp.toPx()))
    drawCircle(Color.Black, radius = 1.5.dp.toPx(), center = Offset(pos.x + 4.dp.toPx(), pos.y - 38.dp.toPx()))
}

private fun DrawScope.drawBeastSilhouette(
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
    if (seed % 2 == 0) {
        drawCircle(color, radius = 5.dp.toPx(), center = Offset(pos.x - 28.dp.toPx(), pos.y - 40.dp.toPx()))
        drawCircle(color, radius = 5.dp.toPx(), center = Offset(pos.x - 10.dp.toPx(), pos.y - 40.dp.toPx()))
    }
}
