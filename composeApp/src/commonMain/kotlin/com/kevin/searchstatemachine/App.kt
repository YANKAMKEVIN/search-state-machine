package com.kevin.searchstatemachine

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue,
            secondary = Green,
            background = Paper,
            surface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Paper
        ) {
            SearchStateMachine()
        }
    }
}

private enum class SearchState {
    Idle,
    Suggest,
    Loading,
    Results,
    Empty,
    Error
}

private data class SearchCopy(
    val state: SearchState,
    val tab: String,
    val query: String,
    val title: String,
    val helper: String,
    val rule: String,
    val accent: Color
)

private val Paper = Color(0xFFF6F1E8)
private val Ink = Color(0xFF101615)
private val Muted = Color(0xFF66706A)
private val Line = Color(0xFFE0D7CA)
private val Blue = Color(0xFF245FA8)
private val Green = Color(0xFF0B6B57)
private val Coral = Color(0xFFC8523A)
private val Gold = Color(0xFFB86F12)
private val Violet = Color(0xFF6B57B8)
private val Red = Color(0xFFD34242)

private val states = listOf(
    SearchCopy(
        state = SearchState.Idle,
        tab = "Idle",
        query = "",
        title = "Start with memory",
        helper = "Recent searches reduce the cost of starting over.",
        rule = "The empty search field should still be useful.",
        accent = Green
    ),
    SearchCopy(
        state = SearchState.Suggest,
        tab = "Suggest",
        query = "bud",
        title = "Suggest before results",
        helper = "Partial intent deserves lightweight guidance, not a blank pause.",
        rule = "Suggestions are navigation, correction and discovery at the same time.",
        accent = Blue
    ),
    SearchCopy(
        state = SearchState.Loading,
        tab = "Loading",
        query = "budget",
        title = "Keep the query visible",
        helper = "Loading should confirm what is being searched and preserve control.",
        rule = "Search loading belongs near the result area, not as a full-screen interruption.",
        accent = Gold
    ),
    SearchCopy(
        state = SearchState.Results,
        tab = "Results",
        query = "budget",
        title = "Rank with context",
        helper = "Results are easier to trust when the match reason is visible.",
        rule = "A result list should explain why the top answer is top.",
        accent = Coral
    ),
    SearchCopy(
        state = SearchState.Empty,
        tab = "Empty",
        query = "crypto taxes",
        title = "Recover from no results",
        helper = "Offer a narrower path, a broader path or a saved alert.",
        rule = "No result is a product moment, not a dead end.",
        accent = Violet
    ),
    SearchCopy(
        state = SearchState.Error,
        tab = "Error",
        query = "portfolio",
        title = "Make retry specific",
        helper = "A search error should keep the query and describe what can be retried.",
        rule = "Do not punish the user by making them rebuild their intent.",
        accent = Red
    )
)

@Composable
private fun SearchStateMachine() {
    var selectedIndex by rememberSaveable { mutableStateOf(3) }
    val selected = states[selectedIndex]

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Header(Modifier.padding(horizontal = 20.dp)) }
        item {
            StateTabs(
                selectedIndex = selectedIndex,
                onSelected = { selectedIndex = it },
                modifier = Modifier.padding(start = 20.dp)
            )
        }
        item {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "search-state"
            ) { copy ->
                SearchPanel(
                    copy = copy,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        item {
            RuleCard(
                copy = selected,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        item { Spacer(modifier = Modifier.height(2.dp)) }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Search State Machine",
            color = Ink,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "A Compose lab for designing search as a flow: intent, feedback, result quality and recovery.",
            color = Muted,
            lineHeight = 23.sp,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun StateTabs(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        states.forEachIndexed { index, state ->
            val selected = selectedIndex == index
            Surface(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelected(index) }),
                shape = RoundedCornerShape(50),
                color = if (selected) state.accent else Color.White,
                border = BorderStroke(1.dp, if (selected) state.accent else Line)
            ) {
                Text(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    text = state.tab,
                    color = if (selected) Color.White else Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    lineHeight = 18.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
    }
}

@Composable
private fun SearchPanel(copy: SearchCopy, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SearchField(copy)
            SearchBody(copy)
            Text(
                text = copy.title,
                color = Ink,
                fontWeight = FontWeight.Black,
                fontSize = 25.sp,
                lineHeight = 30.sp
            )
            Text(
                text = copy.helper,
                color = Muted,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun SearchField(copy: SearchCopy) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Paper,
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(copy.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (copy.query.isBlank()) "Search anything" else copy.query,
                    color = if (copy.query.isBlank()) Muted else Ink,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(
                    text = copy.tab,
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (copy.query.isNotBlank()) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        text = "Clear",
                        color = Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBody(copy: SearchCopy) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp),
        shape = RoundedCornerShape(22.dp),
        color = Paper,
        border = BorderStroke(1.dp, Line)
    ) {
        when (copy.state) {
            SearchState.Idle -> IdleContent(copy.accent)
            SearchState.Suggest -> SuggestContent(copy.accent)
            SearchState.Loading -> LoadingContent(copy.accent)
            SearchState.Results -> ResultsContent(copy.accent)
            SearchState.Empty -> EmptyContent(copy.accent)
            SearchState.Error -> ErrorContent(copy.accent)
        }
    }
}

@Composable
private fun IdleContent(accent: Color) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionLabel("Recent")
        repeat(4) { index ->
            SuggestionRow(
                label = listOf("budget split", "compose snackbar", "offline sync", "kmp storage")[index],
                accent = if (index == 0) accent else Line
            )
        }
    }
}

@Composable
private fun SuggestContent(accent: Color) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionLabel("Suggestions")
        listOf("budget app", "budget alert", "budget categories", "budget forecast").forEachIndexed { index, label ->
            SuggestionRow(
                label = label,
                accent = if (index == 0) accent else Line
            )
        }
    }
}

@Composable
private fun LoadingContent(accent: Color) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionLabel("Searching budget")
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = accent,
            trackColor = Color.White
        )
        repeat(4) {
            SkeletonRow()
        }
    }
}

@Composable
private fun ResultsContent(accent: Color) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionLabel("12 results")
        listOf("Budget forecast", "Monthly rules", "Savings plan").forEachIndexed { index, label ->
            ResultRow(
                label = label,
                meta = if (index == 0) "Top match: title + recent edit" else "Matched body copy",
                accent = if (index == 0) accent else Line
            )
        }
    }
}

@Composable
private fun EmptyContent(accent: Color) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text("0", color = accent, fontWeight = FontWeight.Black, fontSize = 24.sp)
        }
        Text(
            text = "No exact match",
            color = Ink,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp
        )
        RecoveryAction("Broaden query", accent)
        RecoveryAction("Create saved alert", Line)
    }
}

@Composable
private fun ErrorContent(accent: Color) {
    Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = accent.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Search failed",
                    color = accent,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                Text(
                    text = "Your query is safe. Retry only the request.",
                    color = Ink,
                    lineHeight = 20.sp
                )
            }
        }
        RecoveryAction("Retry portfolio", accent)
        RecoveryAction("Search offline items", Line)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Muted,
        fontWeight = FontWeight.Black,
        fontSize = 13.sp
    )
}

@Composable
private fun SuggestionRow(label: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Text(
            text = label,
            color = Ink,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ResultRow(label: String, meta: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = Ink, fontWeight = FontWeight.Black)
            Text(meta, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun SkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Line)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Line)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Line.copy(alpha = 0.65f))
            )
        }
    }
}

@Composable
private fun RecoveryAction(label: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = if (color == Line) Color.White else color,
        border = if (color == Line) BorderStroke(1.dp, Line) else null
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            text = label,
            color = if (color == Line) Ink else Color.White,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun RuleCard(copy: SearchCopy, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Ink
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(copy.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "State rule",
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = copy.rule,
                color = Color(0xFFECE7DC),
                lineHeight = 22.sp
            )
        }
    }
}
