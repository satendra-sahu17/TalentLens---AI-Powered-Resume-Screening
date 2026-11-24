package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.JobPosting
import com.example.data.model.ReviewStatus
import com.example.data.model.ScreeningResult
import com.example.ui.theme.DangerContainerLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.RoyalBlueContainer
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.SuccessContainerLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainerLight
import java.util.Locale

@Composable
fun ScoreBadge(score: Double, modifier: Modifier = Modifier) {
    val (bg, fg) = when {
        score >= 80.0 -> SuccessContainerLight to SuccessGreen
        score >= 60.0 -> RoyalBlueContainer to RoyalBluePrimary
        score >= 40.0 -> WarningContainerLight to Color(0xFFB45309)
        else -> DangerContainerLight to DangerRed
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.25f))
    ) {
        Text(
            text = "${"%.1f".format(Locale.US, score)}%",
            color = fg,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ReviewStatusChip(status: ReviewStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        ReviewStatus.SHORTLISTED -> SuccessContainerLight to SuccessGreen
        ReviewStatus.PENDING_REVIEW -> RoyalBlueContainer to RoyalBluePrimary
        ReviewStatus.NEEDS_REVIEW -> WarningContainerLight to Color(0xFFB45309)
        ReviewStatus.NOT_SELECTED -> DangerContainerLight to DangerRed
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = status.label,
            color = fg,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SkillPill(label: String, matched: Boolean, modifier: Modifier = Modifier) {
    val bg = if (matched) SuccessContainerLight else DangerContainerLight
    val fg = if (matched) SuccessGreen else DangerRed
    val icon = if (matched) Icons.Filled.CheckCircle else Icons.Filled.Close

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (matched) "Matched skill" else "Missing skill",
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = fg,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillChipGroup(
    matchedSkills: List<String>,
    missingSkills: List<String>,
    maxPerGroup: Int = 6
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        matchedSkills.take(maxPerGroup).forEach { skill ->
            SkillPill(label = skill, matched = true)
        }
        missingSkills.take(maxPerGroup).forEach { skill ->
            SkillPill(label = skill, matched = false)
        }
    }
}

@Composable
fun ScoreMetricRow(label: String, score: Double, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = (score / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "metric_progress"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${"%.1f".format(Locale.US, score)}%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun ScoreBreakdownBars(result: ScreeningResult) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScoreMetricRow("Semantic Similarity (40% default)", result.semanticScore, RoyalBluePrimary)
        ScoreMetricRow("Technical Skills Match (30% default)", result.skillMatchScore, SuccessGreen)
        ScoreMetricRow("TF-IDF Cosine Similarity", result.tfidfCosineScore, MaterialTheme.colorScheme.tertiary)
        ScoreMetricRow("Experience Relevance (20% default)", result.experienceScore, WarningAmber)
        ScoreMetricRow("Education Relevance (10% default)", result.educationScore, Color(0xFF6366F1))
    }
}

@Composable
fun ResponsibleAiBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = "Responsible AI notice",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Responsible AI Signal: NLP scores are configurable decision-support indicators only—never automated hiring decisions. Protected attributes (age, gender, religion, ethnicity, photo) are strictly excluded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ScoreDistributionChart(
    results: List<ScreeningResult>,
    modifier: Modifier = Modifier
) {
    val buckets = listOf(
        "90-100%" to results.count { it.finalScore >= 90.0 },
        "75-89%" to results.count { it.finalScore in 75.0..89.99 },
        "60-74%" to results.count { it.finalScore in 60.0..74.99 },
        "<60%" to results.count { it.finalScore < 60.0 }
    )
    val colors = listOf(SuccessGreen, RoyalBluePrimary, WarningAmber, DangerRed)
    val maxCount = (buckets.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Match Score Distribution",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Candidate breakdown across relevance bands",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val barCount = buckets.size
                val slotWidth = size.width / barCount
                val barWidth = (slotWidth * 0.48f).coerceAtMost(56.dp.toPx())
                buckets.forEachIndexed { index, (_, count) ->
                    val x = index * slotWidth + (slotWidth - barWidth) / 2f
                    // Track
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    val ratio = count.toFloat() / maxCount.toFloat()
                    val barHeight = (size.height * ratio).coerceAtLeast(if (count > 0) 12.dp.toPx() else 0f)
                    if (barHeight > 0f) {
                        drawRoundRect(
                            color = colors[index],
                            topLeft = Offset(x, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                buckets.forEachIndexed { index, (label, count) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors[index])
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono)
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobPipelineChart(
    jobs: List<JobPosting>,
    results: List<ScreeningResult>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Job-Wise Candidate Statistics",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Screened vs. shortlisted candidates per opening",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (jobs.isEmpty()) {
                Text(
                    text = "Create a job opening to track job-wise recruitment statistics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxTotal = jobs.maxOf { j ->
                    results.count { it.jobId == j.id }
                }.coerceAtLeast(1)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    jobs.take(5).forEach { job ->
                        val jobResults = results.filter { it.jobId == job.id }
                        val total = jobResults.size
                        val shortlisted = jobResults.count {
                            it.reviewStatusEnum() == ReviewStatus.SHORTLISTED
                        }
                        val avg = if (jobResults.isNotEmpty()) jobResults.map { it.finalScore }.average() else 0.0

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = job.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$total screened • $shortlisted short • avg ${"%.0f".format(avg)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                val totalRatio = (total.toFloat() / maxTotal.toFloat()).coerceIn(0f, 1f)
                                val shortRatio = if (total > 0) (shortlisted.toFloat() / maxTotal.toFloat()).coerceIn(0f, 1f) else 0f
                                if (totalRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(totalRatio)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(RoyalBluePrimary.copy(alpha = 0.35f))
                                    )
                                }
                                if (shortRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(shortRatio)
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(SuccessGreen)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
