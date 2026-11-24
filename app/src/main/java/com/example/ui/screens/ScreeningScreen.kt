package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CandidateProfile
import com.example.data.model.JobPosting
import com.example.data.model.ReviewStatus
import com.example.data.model.ScreeningResult
import com.example.data.model.UiState
import com.example.export.ReportExporter
import com.example.ui.components.ResponsibleAiBanner
import com.example.ui.components.ReviewStatusChip
import com.example.ui.components.ScoreBadge
import com.example.ui.components.SkillChipGroup
import com.example.ui.theme.JetBrainsMono
import com.example.ui.viewmodel.AnalysisProgressState
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreeningScreen(
    jobsState: UiState<List<JobPosting>>,
    candidatesState: UiState<List<CandidateProfile>>,
    screeningsState: UiState<List<ScreeningResult>>,
    selectedJobId: String?,
    comparisonIds: List<String>,
    analysisProgress: AnalysisProgressState,
    onSelectJob: (String?) -> Unit,
    onUploadResumes: (JobPosting, List<Uri>) -> Unit,
    onLoadSampleBatch: () -> Unit,
    onInspectCandidate: (ScreeningResult) -> Unit,
    onQuickShortlist: (ScreeningResult) -> Unit,
    onToggleCompare: (String) -> Unit,
    onDeleteScreening: (String) -> Unit,
    onClearError: () -> Unit,
    onExportMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val jobs = (jobsState as? UiState.Success)?.data.orEmpty()
    val candidates = (candidatesState as? UiState.Success)?.data.orEmpty()
    val candidatesById = remember(candidates) { candidates.associateBy { it.id } }
    val allResults = (screeningsState as? UiState.Success)?.data.orEmpty()

    val activeJob = remember(jobs, selectedJobId) {
        jobs.firstOrNull { it.id == selectedJobId } ?: jobs.firstOrNull()
    }

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<ReviewStatus?>(null) }
    var minExpFilter by remember { mutableIntStateOf(0) }
    var eduFilter by remember { mutableStateOf("All") }
    var showFilters by remember { mutableStateOf(false) }

    val multiDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty() && activeJob != null) {
            onUploadResumes(activeJob, uris)
        }
    }

    val filteredResults = remember(allResults, selectedJobId, searchQuery, filterStatus, minExpFilter, eduFilter, candidatesById) {
        allResults
            .filter { res ->
                val matchesJob = selectedJobId == null || res.jobId == selectedJobId
                val candidate = candidatesById[res.candidateId]
                val q = searchQuery.trim().lowercase()
                val matchesQuery = q.isBlank() ||
                    res.candidateName.lowercase().contains(q) ||
                    res.matchedSkills.any { it.lowercase().contains(q) } ||
                    res.missingSkills.any { it.lowercase().contains(q) } ||
                    (candidate?.highestEducation?.lowercase()?.contains(q) == true)
                val matchesStatus = filterStatus == null || res.reviewStatusEnum() == filterStatus
                val expYears = candidate?.totalExperienceYears ?: 0.0
                val matchesExp = expYears >= minExpFilter
                val eduText = candidate?.highestEducation?.lowercase().orEmpty()
                val matchesEdu = when (eduFilter) {
                    "Master's+" -> eduText.contains("master") || eduText.contains("m.s") || eduText.contains("ph") || eduText.contains("doctor")
                    "Bachelor's+" -> eduText.contains("bachelor") || eduText.contains("b.s") || eduText.contains("b.tech") || eduText.contains("master") || eduText.contains("m.s") || eduText.contains("ph")
                    else -> true
                }
                matchesJob && matchesQuery && matchesStatus && matchesExp && matchesEdu
            }
            .sortedByDescending { it.finalScore }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val csv = ReportExporter.buildRankingsCsv(filteredResults, candidatesById)
                ReportExporter.writeTextToUri(context, uri, csv)
                    .onSuccess { onExportMessage("Candidate rankings exported to CSV.") }
                    .onFailure { e -> onExportMessage(e.localizedMessage ?: "CSV export failed.") }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Role Selector & Upload Zone
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Resume Screening & Ranking", style = MaterialTheme.typography.titleLarge)

                    if (jobs.isEmpty()) {
                        Text(
                            text = "No job postings found. Create a job in the Jobs tab or load sample jobs & resumes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onLoadSampleBatch,
                            enabled = !analysisProgress.isRunning,
                            modifier = Modifier.testTag("screening_load_sample_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load Sample Jobs & 5 Resumes")
                        }
                    } else {
                        Text(
                            text = "Target Job Opening",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedJobId == null,
                                onClick = { onSelectJob(null) },
                                label = { Text("All Jobs (${allResults.size})") }
                            )
                            jobs.forEach { job ->
                                FilterChip(
                                    selected = selectedJobId == job.id,
                                    onClick = { onSelectJob(job.id) },
                                    label = { Text(job.title) }
                                )
                            }
                        }

                        if (activeJob != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Upload PDF or DOCX Resumes for '${activeJob.title}'",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Select one or multiple resumes simultaneously (max 15 MB each). Text extraction, NER, TF-IDF cosine, and Gemini semantic matching run automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                multiDocPicker.launch(
                                                    arrayOf(
                                                        "application/pdf",
                                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                                    )
                                                )
                                            },
                                            enabled = !analysisProgress.isRunning,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("upload_resumes_button")
                                        ) {
                                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Select Files")
                                        }
                                        FilledTonalButton(
                                            onClick = onLoadSampleBatch,
                                            enabled = !analysisProgress.isRunning,
                                            modifier = Modifier.testTag("screening_sample_batch_button")
                                        ) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Samples")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Analysis Progress
        if (analysisProgress.isRunning) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "NLP Screening: ${analysisProgress.currentFile}",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = analysisProgress.stageLabel,
                            style = MaterialTheme.typography.bodySmall
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

        if (analysisProgress.errorBanner != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = analysisProgress.errorBanner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButtonShim("Dismiss", onClick = onClearError)
                    }
                }
            }
        }

        // Search, Filters & Export Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search candidate or skill...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_candidates_input")
                    )
                    IconButton(
                        onClick = { showFilters = !showFilters },
                        modifier = Modifier.testTag("toggle_filters_button")
                    ) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter candidates")
                    }
                    OutlinedButton(
                        onClick = {
                            if (filteredResults.isEmpty()) {
                                onExportMessage("No ranked candidates to export.")
                            } else {
                                csvExportLauncher.launch("TalentLens_Candidate_Rankings.csv")
                            }
                        },
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV")
                    }
                }

                if (showFilters) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Review Status", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = filterStatus == null,
                                    onClick = { filterStatus = null },
                                    label = { Text("All") }
                                )
                                ReviewStatus.entries.forEach { st ->
                                    FilterChip(
                                        selected = filterStatus == st,
                                        onClick = { filterStatus = st },
                                        label = { Text(st.label) }
                                    )
                                }
                            }
                            Text("Minimum Experience", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0, 2, 4, 6).forEach { yrs ->
                                    FilterChip(
                                        selected = minExpFilter == yrs,
                                        onClick = { minExpFilter = yrs },
                                        label = { Text(if (yrs == 0) "Any" else "${yrs}+ yrs") }
                                    )
                                }
                            }
                            Text("Education Tier", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("All", "Bachelor's+", "Master's+").forEach { tier ->
                                    FilterChip(
                                        selected = eduFilter == tier,
                                        onClick = { eduFilter = tier },
                                        label = { Text(tier) }
                                    )
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

        if (filteredResults.isEmpty()) {
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
                            text = "No matching candidate screenings yet. Upload PDF/DOCX resumes or adjust filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(filteredResults, key = { _, r -> r.id }) { index, res ->
                val candidate = candidatesById[res.candidateId]
                val isComparing = res.id in comparisonIds
                RankedCandidateCard(
                    rank = index + 1,
                    result = res,
                    candidate = candidate,
                    isComparing = isComparing,
                    onClick = { onInspectCandidate(res) },
                    onQuickShortlist = { onQuickShortlist(res) },
                    onToggleCompare = { onToggleCompare(res.id) },
                    onDelete = { onDeleteScreening(res.id) }
                )
            }
        }
    }
}

@Composable
private fun TextButtonShim(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(label)
    }
}

@Composable
private fun RankedCandidateCard(
    rank: Int,
    result: ScreeningResult,
    candidate: CandidateProfile?,
    isComparing: Boolean,
    onClick: () -> Unit,
    onQuickShortlist: () -> Unit,
    onToggleCompare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("ranked_candidate_card_$rank"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isComparing) 2.dp else 1.dp,
            color = if (isComparing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (rank <= 3) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#$rank",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (rank <= 3) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = result.candidateName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${result.jobTitle} • ${"%.1f".format(Locale.US, candidate?.totalExperienceYears ?: 0.0)} yrs • ${candidate?.highestEducation?.ifBlank { "Education N/A" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                ScoreBadge(score = result.finalScore)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReviewStatusChip(status = result.reviewStatusEnum())
                Text(
                    text = "Sem ${"%.0f".format(result.semanticScore)}% • TF-IDF ${"%.0f".format(result.tfidfCosineScore)}% • Skills ${"%.0f".format(result.skillMatchScore)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (result.matchedSkills.isNotEmpty() || result.missingSkills.isNotEmpty()) {
                SkillChipGroup(
                    matchedSkills = result.matchedSkills,
                    missingSkills = result.missingSkills,
                    maxPerGroup = 4
                )
            }

            if (result.aiExplanation.isNotBlank()) {
                Text(
                    text = result.aiExplanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onQuickShortlist,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (result.reviewStatusEnum() == ReviewStatus.SHORTLISTED) Icons.Filled.Check else Icons.Filled.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (result.reviewStatusEnum() == ReviewStatus.SHORTLISTED) "Shortlisted" else "Shortlist"
                    )
                }
                OutlinedButton(
                    onClick = onToggleCompare,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isComparing) "Comparing" else "Compare")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Remove screening result",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
