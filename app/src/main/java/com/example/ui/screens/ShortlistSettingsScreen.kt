package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.RecruiterProfile
import com.example.data.model.ScoringWeights
import com.example.data.model.ShortlistStatus
import com.example.data.model.ShortlistedCandidate
import com.example.data.model.UiState
import com.example.ui.components.ResponsibleAiBanner
import com.example.ui.components.ScoreBadge
import com.example.ui.theme.JetBrainsMono
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShortlistSettingsScreen(
    recruiterState: UiState<RecruiterProfile?>,
    shortlistState: UiState<List<ShortlistedCandidate>>,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onSaveWeights: (ScoringWeights) -> Unit,
    onUpdateShortlistStatus: (String, ShortlistStatus, String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val shortlisted = (shortlistState as? UiState.Success)?.data.orEmpty()
    val recruiter = (recruiterState as? UiState.Success)?.data

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Shortlist (${shortlisted.size})") },
                icon = { Icon(Icons.Filled.BookmarkAdded, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Weights & Account") },
                icon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (shortlisted.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.BookmarkAdded,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text("No Shortlisted Candidates Yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Shortlist top-scoring candidates from the Screen tab after manual review.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(shortlisted, key = { it.id }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.candidateName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            item.jobTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    ScoreBadge(score = item.finalScore)
                                }
                                if (item.priorityNote.isNotBlank()) {
                                    Text(
                                        text = item.priorityNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ShortlistStatus.entries.forEach { st ->
                                        FilterChip(
                                            selected = item.statusEnum() == st,
                                            onClick = { onUpdateShortlistStatus(item.id, st, item.priorityNote) },
                                            label = { Text(st.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val baseWeights = recruiter?.toWeights() ?: ScoringWeights()
            var semW by remember(recruiter) { mutableFloatStateOf((baseWeights.semanticWeight * 100).toFloat()) }
            var skiW by remember(recruiter) { mutableFloatStateOf((baseWeights.skillsWeight * 100).toFloat()) }
            var expW by remember(recruiter) { mutableFloatStateOf((baseWeights.experienceWeight * 100).toFloat()) }
            var eduW by remember(recruiter) { mutableFloatStateOf((baseWeights.educationWeight * 100).toFloat()) }

            val total = (semW + skiW + expW + eduW).coerceAtLeast(1f)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Configurable NLP Scoring Weights", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Weights are automatically normalized to 100% when saved and recalculate candidate rankings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            WeightSliderRow(
                                label = "Semantic Similarity (Normalized ${(semW / total * 100).roundToInt()}%)",
                                value = semW,
                                onValueChange = { semW = it }
                            )
                            WeightSliderRow(
                                label = "Technical Skills Match (Normalized ${(skiW / total * 100).roundToInt()}%)",
                                value = skiW,
                                onValueChange = { skiW = it }
                            )
                            WeightSliderRow(
                                label = "Experience Relevance (Normalized ${(expW / total * 100).roundToInt()}%)",
                                value = expW,
                                onValueChange = { expW = it }
                            )
                            WeightSliderRow(
                                label = "Education Relevance (Normalized ${(eduW / total * 100).roundToInt()}%)",
                                value = eduW,
                                onValueChange = { eduW = it }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = {
                                        semW = 40f
                                        skiW = 30f
                                        expW = 20f
                                        eduW = 10f
                                    }
                                ) {
                                    Text("Reset 40/30/20/10")
                                }
                                Button(
                                    onClick = {
                                        onSaveWeights(
                                            ScoringWeights(
                                                semanticWeight = semW.toDouble(),
                                                skillsWeight = skiW.toDouble(),
                                                experienceWeight = expW.toDouble(),
                                                educationWeight = eduW.toDouble()
                                            ).normalized()
                                        )
                                    },
                                    modifier = Modifier.testTag("save_weights_button")
                                ) {
                                    Text("Save & Recalculate")
                                }
                            }
                        }
                    }
                }

                item {
                    ResponsibleAiBanner()
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Appearance & Recruiter Account", style = MaterialTheme.typography.titleMedium)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Dark Mode", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(
                                    checked = isDarkTheme,
                                    onCheckedChange = onToggleTheme,
                                    modifier = Modifier.testTag("theme_toggle_switch")
                                )
                            }

                            HorizontalDivider()

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = recruiter?.displayName?.ifBlank { "Authenticated Recruiter" } ?: "Authenticated Recruiter",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = recruiter?.email.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(
                text = value.roundToInt().toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f
        )
    }
}
