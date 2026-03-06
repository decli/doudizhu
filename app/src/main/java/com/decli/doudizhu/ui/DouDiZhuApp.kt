package com.decli.doudizhu.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.decli.doudizhu.engine.CardFormatter
import com.decli.doudizhu.engine.RuleEngine
import com.decli.doudizhu.model.Card
import com.decli.doudizhu.model.GamePhase
import com.decli.doudizhu.model.GameSession
import com.decli.doudizhu.model.PlayerRole
import com.decli.doudizhu.model.Seat
import com.decli.doudizhu.model.player
import com.decli.doudizhu.viewmodel.GameUiState
import com.decli.doudizhu.viewmodel.GameViewModel

@Composable
fun DouDiZhuApp(
    viewModel: GameViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session = state.session ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF05231E), Color(0xFF0B4339), Color(0xFF031816)),
                ),
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderBar(state, session)
            TableArea(state, session, Modifier.weight(1f))
            BottomArea(state, session, viewModel)
        }
    }
}

@Composable
private fun HeaderBar(state: GameUiState, session: GameSession) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0x551B3D35),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("长青斗地主", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFFFF6E8))
                Text(
                    "Android 15 平板适老版",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFDCE6DD),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                InfoChip(Icons.Rounded.Save, if (state.restoredFromSave) "已恢复存档" else "实时存档")
                InfoChip(Icons.Rounded.Campaign, "AI 出牌播报")
                InfoChip(Icons.Rounded.Psychology, "高性能算法")
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    AssistChip(
        onClick = {},
        label = { Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        leadingIcon = { Icon(icon, contentDescription = null) },
    )
}

@Composable
private fun TableArea(state: GameUiState, session: GameSession, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SidePlayerPanel(session, Seat.West, Modifier.weight(0.22f).fillMaxHeight())
        CenterBoard(state, session, Modifier.weight(0.56f).fillMaxHeight())
        SidePlayerPanel(session, Seat.East, Modifier.weight(0.22f).fillMaxHeight())
    }
}

@Composable
private fun SidePlayerPanel(session: GameSession, seat: Seat, modifier: Modifier = Modifier) {
    val player = session.player(seat)
    val isCurrent = session.currentTurn == seat
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0x55173931)),
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) Color(0xFFE0BA67) else Color(0x334E7D73)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (seat == Seat.West) "西" else "东",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) Color(0xFF18322C) else Color(0xFFFFF7EA),
                    )
                }
                Text(player.name, style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFF6E8))
                RoleBadge(player.role)
                Text("剩余 ${player.hand.size} 张", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFDCE6DD))
                HandBackStrip(count = player.hand.size)
            }

            session.lastPlayedTurn?.takeIf { it.seat == seat }?.let { turn ->
                ComboBillboard(title = "上一手", text = turn.description)
            }
        }
    }
}

@Composable
private fun CenterBoard(state: GameUiState, session: GameSession, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0x331B3D35)),
        shape = RoundedCornerShape(36.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("第 ${session.roundIndex} 局", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(
                        "叫分 ${session.calledScore} 倍数 x${session.multiplier}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFE0D0A8),
                    )
                }
                BottomCardsPanel(session)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ComboBillboard(title = "桌面播报", text = session.message)
                AnimatedVisibility(visible = session.lastPlayedTurn != null) {
                    session.lastPlayedTurn?.let { turn ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "${labelOf(turn.seat)}刚刚出牌",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFDCE6DD),
                            )
                            FaceUpCards(turn.cards, compact = false, selectedIds = emptySet(), highlightedIds = emptySet(), onTap = {})
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StatusStrip(session)
                if (session.phase == GamePhase.Bidding && session.currentTurn == Seat.South) {
                    BidButtons(session = session)
                } else {
                    Text(
                        text = when (session.phase) {
                            GamePhase.Bidding -> "${labelOf(session.currentTurn)}思考叫分中"
                            GamePhase.Playing -> "${labelOf(session.currentTurn)}正在出牌"
                            GamePhase.Finished -> "${labelOf(session.winner ?: Seat.South)}获胜，点击新局继续"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFFFF6E8),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomCardsPanel(session: GameSession) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        session.bottomCards.forEach { card ->
            val showFace = session.phase != GamePhase.Bidding
            if (showFace) {
                MiniCard(card)
            } else {
                CardBackSmall()
            }
        }
    }
}

@Composable
private fun StatusStrip(session: GameSession) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoleBadge(session.player(Seat.South).role)
        RoleBadge(session.player(Seat.West).role)
        RoleBadge(session.player(Seat.East).role)
    }
}

@Composable
private fun BidButtons(session: GameSession) {
    val highest = session.bidState?.highestScore ?: 0
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("轮到您叫分", color = Color(0xFFFFF6E8), style = MaterialTheme.typography.bodyLarge)
        Text("当前最高 $highest 分", color = Color(0xFFE0D0A8), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BottomArea(state: GameUiState, session: GameSession, viewModel: GameViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x5A18332D)),
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("您的手牌", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(
                        "点击牌面即可选中，再用大按钮操作",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFDCE6DD),
                    )
                }
                ActionBar(state, session, viewModel)
            }
            FaceUpCards(
                cards = session.player(Seat.South).hand,
                compact = false,
                selectedIds = state.selectedCardIds,
                highlightedIds = state.highlightedCardIds,
                onTap = { viewModel.toggleCardSelection(it.id) },
            )
        }
    }
}

@Composable
private fun ActionBar(state: GameUiState, session: GameSession, viewModel: GameViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            session.phase == GamePhase.Bidding && session.currentTurn == Seat.South -> {
                listOf(0, 1, 2, 3).forEach { score ->
                    LargeButton(
                        text = if (score == 0) "不叫" else "叫${score}分",
                        onClick = { viewModel.bid(score) },
                        enabled = score == 0 || score > (session.bidState?.highestScore ?: 0),
                    )
                }
            }

            session.phase == GamePhase.Playing && session.currentTurn == Seat.South -> {
                LargeButton("提示", { viewModel.hint() }, icon = Icons.Rounded.AutoAwesome)
                LargeButton(
                    "不出",
                    { viewModel.pass() },
                    enabled = session.lastPlayedTurn != null && session.lastPlayedTurn.seat != Seat.South,
                )
                LargeButton(
                    "出牌",
                    { viewModel.playSelected() },
                    enabled = state.selectedCardIds.isNotEmpty(),
                    icon = Icons.Rounded.Campaign,
                )
            }

            else -> {
                Text(
                    "请稍候，${labelOf(session.currentTurn)}操作中",
                    color = Color(0xFFE0D0A8),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        LargeButton("新局", { viewModel.newGame() }, icon = Icons.Rounded.Refresh)
    }
}

@Composable
private fun LargeButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(66.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Color(0xFFE0BA67) else Color(0x556A6459),
            contentColor = if (enabled) Color(0xFF132620) else Color(0xFFDDD4C4),
        ),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, contentDescription = null) }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RoleBadge(role: PlayerRole?) {
    val text = when (role) {
        PlayerRole.Landlord -> "地主"
        PlayerRole.Farmer -> "农民"
        null -> "未定"
    }
    val background = when (role) {
        PlayerRole.Landlord -> Color(0xFFE0BA67)
        PlayerRole.Farmer -> Color(0x334E7D73)
        null -> Color(0x33273E39)
    }
    val content = if (role == PlayerRole.Landlord) Color(0xFF18312B) else Color(0xFFFFF6E8)
    Surface(shape = RoundedCornerShape(999.dp), color = background) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ComboBillboard(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x5515231F))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = Color(0xFFE0BA67), style = MaterialTheme.typography.bodyMedium)
        Text(text, color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FaceUpCards(
    cards: List<Card>,
    compact: Boolean,
    selectedIds: Set<Int>,
    highlightedIds: Set<Int>,
    onTap: (Card) -> Unit,
) {
    val scrollState = rememberScrollState()
    val containerModifier = if (compact) Modifier else Modifier.horizontalScroll(scrollState)
    FlowRow(
        modifier = containerModifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        cards.forEach { card ->
            val selected = card.id in selectedIds
            val highlighted = card.id in highlightedIds
            val lift by animateDpAsState(
                targetValue = if (selected) 14.dp else 0.dp,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "cardLift",
            )
            PlayingCard(
                card = card,
                modifier = Modifier.padding(top = lift),
                highlighted = highlighted,
                selected = selected,
                onClick = { onTap(card) },
            )
        }
    }
}

@Composable
private fun PlayingCard(
    card: Card,
    modifier: Modifier = Modifier,
    highlighted: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val isRed = card.suit.name == "Heart" || card.suit.name == "Diamond"
    val borderColor = when {
        highlighted -> Color(0xFFE0BA67)
        selected -> Color(0xFF81B3A8)
        else -> Color(0xFFD8CCB3)
    }
    Column(
        modifier = modifier
            .width(86.dp)
            .height(126.dp)
            .shadow(if (selected) 14.dp else 8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFFAEE))
            .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            CardFormatter.rankLabel(card.rank),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRed) Color(0xFFC64A38) else Color(0xFF1D1D1D),
        )
        Text(
            suitSymbol(card),
            modifier = Modifier.fillMaxWidth(),
            fontSize = 30.sp,
            textAlign = TextAlign.Center,
            color = if (isRed) Color(0xFFC64A38) else Color(0xFF1D1D1D),
        )
    }
}

@Composable
private fun MiniCard(card: Card) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFFAEE))
            .border(1.5.dp, Color(0xFFD9CCB0), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${CardFormatter.rankLabel(card.rank)}${suitSymbol(card)}",
            color = if (card.suit.name == "Heart" || card.suit.name == "Diamond") Color(0xFFC64A38) else Color(0xFF1D1D1D),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CardBackSmall() {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFE0BA67), Color(0xFF0A4A3F))))
            .border(1.5.dp, Color(0xFFF3D999), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("牌", color = Color(0xFF12312A), fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
private fun HandBackStrip(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy((-22).dp)) {
        repeat(minOf(count.coerceAtLeast(3), 8)) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFE0BA67), Color(0xFF0B3E35))))
                    .border(1.dp, Color(0xFFF0D99D), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("牌", color = Color(0xFF12312A), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun labelOf(seat: Seat): String = when (seat) {
    Seat.West -> "西侧电脑"
    Seat.South -> "您"
    Seat.East -> "东侧电脑"
}

private fun suitSymbol(card: Card): String = when (card.suit.name) {
    "Spade" -> "♠"
    "Heart" -> "♥"
    "Club" -> "♣"
    "Diamond" -> "♦"
    else -> if (card.rank == 16) "J" else "W"
}
