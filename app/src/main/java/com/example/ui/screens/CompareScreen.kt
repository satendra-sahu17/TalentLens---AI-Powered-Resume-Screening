package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CandidateProfile
import com.example.data.model.ScreeningResult
import com.example.data.model.UiState
import com.example.ui.components.ResponsibleAiBanner
import com.example.ui.components.ReviewStatusChip
import com.example.ui.components.ScoreBadge
import com.example.ui.components.SkillChipGroup
import com.example.ui.theme.JetBrainsMono
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompareScreen(
    candidatesState: UiState<List<CandidateProfile>>,
    screeningsState: UiState<List<ScreeningResult>>,
    comparisonIds: List<String>,
    onToggleCompare: (String) -> Unit,
    onInspectCandidate: (ScreeningResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenings = (screeningsState as? UiState.Success)?.data.orEmpty().sortedByDescending { it.finalScore }
    val candidatesById = remember(candidatesState) {
        (candidatesState as? UiState.Success)?.data.orEmpty().associateBy { it.id }
    }

    val selectedResults = remember(screenings, comparisonIds) {
        val explicit = screenings.filter { it.id in comparisonIds }
        if (explicit.size >= 2) explicit else screenings.take(2)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Candidate Comparison", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Select up to 3 candidates to compare skills, education, experience, certifications & NLP match scores side-by-side.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (screenings.isNotEmpty()) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    screenings.forEach { res ->
                        val active = res.id in selectedResults.map { it.id }
                        FilterChip(
                            selected = active,
                            onClick = { onToggleCompare(res.id) },
                            label = {
                                Text(
                                    "${res.candidateName} (${"%.0f".format(res.finalScore)}%)",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("compare_chip_${res.id}")
                        )
                    }
                }
            }
        }

        if (selectedResults.size < 2) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Screen at least 2 candidates to enable side-by-side comparison.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    selectedResults.forEach { res ->
                        val candidate = candidatesById[res.candidateId]
                        CompareColumnCard(
                            result = res,
                            candidate = candidate,
                            onInspect = { onInspectCandidate(res) }
                        )
                    }
                }
            }
        }

        item {
            ResponsibleAiBanner()
        }
    }
}

@Composable
private fun CompareColumnCard(
    result: ScreeningResult,
    candidate: CandidateProfile?,
    onInspect: () -> Unit
) {
    Card(
        onClick = onInspect,
        modifier = Modifier.width(280.dp),
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.candidateName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = result.jobTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                ScoreBadge(score = result.finalScore)
            }

            ReviewStatusChip(status = result.reviewStatusEnum())
            HorizontalDivider()

            CompareField("Semantic Similarity", "${"%.1f".format(Locale.US, result.semanticScore)}%", mono = true)
            CompareField("TF-IDF Cosine", "${"%.1f".format(Locale.US, result.tfidfCosineScore)}%", mono = true)
            CompareField("Technical Skills", "${"%.1f".format(Locale.US, result.skillMatchScore)}%", mono = true)
            CompareField("Experience", "${"%.1f".format(Locale.US, candidate?.totalExperienceYears ?: 0.0)} yrs (${"%.0f".format(result.experienceScore)}%)")
            CompareField("Education", candidate?.highestEducation?.ifBlank { "N/A" } ?: "N/A")
            CompareField(
                "Certifications",
                candidate?.certifications?.joinToString(", ").orEmpty().ifBlank { "None listed" }
            )

            HorizontalDivider()
            Text("Skills Alignment", style = MaterialTheme.typography.labelMedium)
            SkillChipGroup(
                matchedSkills = result.matchedSkills,
                missingSkills = result.missingSkills,
                maxPerGroup = 6
            )

            if (result.aiExplanation.isNotBlank()) {
                HorizontalDivider()
                Text("AI Evidence", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = result.aiExplanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompareField(label: String, value: String, mono: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = if (mono) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            }
        )
    }
}
