package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.JobPosting
import com.example.data.model.RecruiterProfile
import com.example.data.model.ResumeDocument
import com.example.data.model.ScreeningResult
import com.example.data.model.ShortlistedCandidate
import com.example.data.model.UiState
import com.example.ui.components.JobPipelineChart
import com.example.ui.components.ResponsibleAiBanner
import com.example.ui.components.ReviewStatusChip
import com.example.ui.components.ScoreBadge
import com.example.ui.components.ScoreDistributionChart
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.RoyalBluePrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AnalysisProgressState
import java.util.Locale

@Composable
fun DashboardScreen(
    recruiterState: UiState<RecruiterProfile?>,
    jobsState: UiState<List<JobPosting>>,
    resumesState: UiState<List<ResumeDocument>>,
    screeningsState: UiState<List<ScreeningResult>>,
    shortlistState: UiState<List<ShortlistedCandidate>>,
    analysisProgress: AnalysisProgressState,
    onNavigateToScreen: () -> Unit,
    onNavigateToJobs: () -> Unit,
    onLoadSampleBatch: () -> Unit,
    onInspectCandidate: (ScreeningResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val recruiter = (recruiterState as? UiState.Success)?.data
    val jobs = (jobsState as? UiState.Success)?.data.orEmpty()
    val resumes = (resumesState as? UiState.Success)?.data.orEmpty()
    val screenings = (screeningsState as? UiState.Success)?.data.orEmpty()
    val shortlisted = (shortlistState as? UiState.Success)?.data.orEmpty()

    val avgScore = if (screenings.isNotEmpty()) {
        screenings.map { it.finalScore }.average()
    } else 0.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_recruitment),
                        contentDescription = "Recruitment dashboard hero banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp),
                        contentScale = ContentScale.Crop,
                        alpha = 0.28f
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Welcome back, ${recruiter?.displayName?.ifBlank { "Recruiter" } ?: "Recruiter"}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${jobs.size} active role(s) • NLP weights: ${( (recruiter?.semanticWeight ?: 0.4) * 100 ).toInt()}% Semantic / ${((recruiter?.skillsWeight ?: 0.3) * 100).toInt()}% Skills / ${((recruiter?.experienceWeight ?: 0.2) * 100).toInt()}% Exp / ${((recruiter?.educationWeight ?: 0.1) * 100).toInt()}% Edu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Button(
                                onClick = onNavigateToScreen,
                                modifier = Modifier.testTag("dashboard_upload_resumes_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.UploadFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Screen Resumes")
                            }
                            FilledTonalButton(
                                onClick = onLoadSampleBatch,
                                enabled = !analysisProgress.isRunning,
                                modifier = Modifier.testTag("dashboard_load_sample_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load Sample Set")
                            }
                        }
                    }
                }
            }
        }

        if (analysisProgress.isRunning) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Analyzing: ${analysisProgress.currentFile}",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = analysisProgress.stageLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = {
                                if (analysisProgress.totalCount > 0) {
                                    (analysisProgress.processedCount.toFloat() / analysisProgress.totalCount.toFloat()).coerceIn(0f, 1f)
                                } else 0.5f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // KPI Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Uploaded Resumes",
                        value = resumes.size.toString(),
                        icon = Icons.Filled.Description,
                        accent = RoyalBluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Analyzed Candidates",
                        value = screenings.size.toString(),
                        icon = Icons.Filled.Leaderboard,
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Shortlisted",
                        value = shortlisted.size.toString(),
                        icon = Icons.Filled.CheckCircle,
                        accent = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Avg Match Score",
                        value = if (screenings.isEmpty()) "—" else "${"%.1f".format(Locale.US, avgScore)}%",
                        icon = Icons.Filled.Star,
                        accent = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (screeningsState is UiState.Error) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = screeningsState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        if (screenings.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_resumes),
                            contentDescription = "No screened candidates yet",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "Start Your First NLP Screening",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Create a job opening and upload PDF or DOCX resumes—or tap 'Load Sample Set' above to test the complete NLP & Gemini ranking pipeline right away.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(onClick = onNavigateToJobs) {
                                Text("Manage Jobs")
                            }
                            Button(
                                onClick = onLoadSampleBatch,
                                enabled = !analysisProgress.isRunning
                            ) {
                                Text("Run Sample Benchmark")
                            }
                        }
                    }
                }
            }
        } else {
            item {
                ScoreDistributionChart(results = screenings)
            }
            item {
                JobPipelineChart(jobs = jobs, results = screenings)
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
                        Text(
                            text = "Top-Ranked Candidates",
                            style = MaterialTheme.typography.titleMedium
                        )
                        screenings.sortedByDescending { it.finalScore }.take(5).forEachIndexed { idx, res ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onInspectCandidate(res) }
                                    .testTag("top_candidate_card_${idx + 1}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "#${idx + 1}",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontFamily = JetBrainsMono,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = res.candidateName,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${res.jobTitle} • ${res.matchedSkills.size}/${res.matchedSkills.size + res.missingSkills.size} skills",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        ReviewStatusChip(status = res.reviewStatusEnum())
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ScoreBadge(score = res.finalScore)
                                }
                            }
                        }
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
private fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
