package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CandidateProfile
import com.example.data.model.ResumeDocument
import com.example.data.model.ReviewStatus
import com.example.data.model.ScreeningResult
import com.example.export.ReportExporter
import com.example.ui.components.ResponsibleAiBanner
import com.example.ui.components.ScoreBadge
import com.example.ui.components.ScoreBreakdownBars
import com.example.ui.components.SkillChipGroup
import com.example.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CandidateDetailSheet(
    result: ScreeningResult,
    candidate: CandidateProfile?,
    resume: ResumeDocument?,
    onDismiss: () -> Unit,
    onUpdateReview: (ReviewStatus, String) -> Unit,
    onReportMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedStatus by remember(result.id) { mutableStateOf(result.reviewStatusEnum()) }
    var notes by remember(result.id) { mutableStateOf(result.reviewerNotes) }
    var showRawResume by remember { mutableStateOf(false) }

    val exportReportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val reportText = ReportExporter.buildCandidateReport(result, candidate, resume)
                ReportExporter.writeTextToUri(context, uri, reportText)
                    .onSuccess { onReportMessage("Individual candidate report saved.") }
                    .onFailure { e -> onReportMessage(e.localizedMessage ?: "Export failed.") }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = result.candidateName, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Evaluated for ${result.jobTitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                ScoreBadge(score = result.finalScore)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close detail sheet")
                }
            }

            if (candidate != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (candidate.email.isNotBlank()) {
                            InfoRow(Icons.Filled.Email, candidate.email)
                        }
                        if (candidate.phone.isNotBlank()) {
                            InfoRow(Icons.Filled.Phone, candidate.phone)
                        }
                        InfoRow(
                            Icons.Filled.WorkHistory,
                            "${"%.1f".format(Locale.US, candidate.totalExperienceYears)} years extracted experience"
                        )
                        InfoRow(
                            Icons.Filled.School,
                            candidate.highestEducation.ifBlank { "Education not specified" }
                        )
                    }
                }
            }

            ResponsibleAiBanner()

            Text("Score Breakdown", style = MaterialTheme.typography.titleMedium)
            ScoreBreakdownBars(result = result)

            HorizontalDivider()

            Text("Explainable Skill Alignment", style = MaterialTheme.typography.titleMedium)
            if (result.matchedSkills.isEmpty() && result.missingSkills.isEmpty()) {
                Text(
                    text = "No required skills specified on the job posting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SkillChipGroup(
                    matchedSkills = result.matchedSkills,
                    missingSkills = result.missingSkills,
                    maxPerGroup = 25
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("NLP & AI Evidence Summary", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = result.aiExplanation.ifBlank { "No explanation recorded." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (candidate != null) {
                if (candidate.projectsSummary.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Extracted Projects", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = candidate.projectsSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (candidate.certifications.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Certifications", style = MaterialTheme.typography.titleSmall)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            candidate.certifications.forEach { cert ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = cert,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (resume != null) {
                OutlinedButton(
                    onClick = { showRawResume = !showRawResume },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_original_resume_button")
                ) {
                    Text(if (showRawResume) "Hide Original Extracted Resume" else "Review Original Extracted Resume (${resume.fileName})")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (showRawResume) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
                if (showRawResume) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = resume.extractedText,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Manual Recruiter Decision", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReviewStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status.label) },
                        modifier = Modifier.testTag("review_chip_${status.name.lowercase()}")
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Recruiter Review Notes") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reviewer_notes_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val safeName = result.candidateName.replace(Regex("[^A-Za-z0-9_\\-]"), "_")
                        exportReportLauncher.launch("${safeName}_Screening_Report.txt")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_individual_report_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Report")
                }
                Button(
                    onClick = {
                        onUpdateReview(selectedStatus, notes)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_review_decision_button")
                ) {
                    Text("Save Decision", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
