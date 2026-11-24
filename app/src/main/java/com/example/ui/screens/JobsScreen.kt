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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.JobPosting
import com.example.data.model.JobStatus
import com.example.data.model.ScreeningResult
import com.example.data.model.UiState
import com.example.ui.theme.SuccessContainerLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningContainerLight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobsScreen(
    jobsState: UiState<List<JobPosting>>,
    screeningsState: UiState<List<ScreeningResult>>,
    onSaveJob: (
        existingId: String?,
        title: String,
        department: String,
        description: String,
        requiredSkillsRaw: String,
        qualifications: String,
        minExp: Int,
        status: JobStatus
    ) -> Unit,
    onDeleteJob: (String) -> Unit,
    onScreenForJob: (JobPosting) -> Unit,
    modifier: Modifier = Modifier
) {
    val jobs = (jobsState as? UiState.Success)?.data.orEmpty()
    val screenings = (screeningsState as? UiState.Success)?.data.orEmpty()
    var editingJob by remember { mutableStateOf<JobPosting?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingJob = null
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_job_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Job Opening")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Job Openings",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Manage job descriptions, required skills & experience benchmarks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (jobs.isEmpty()) {
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Work,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("No job postings yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Tap + to create a job description with required skills and minimum experience.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    editingJob = null
                                    showCreateDialog = true
                                },
                                modifier = Modifier.testTag("empty_create_job_button")
                            ) {
                                Text("Create Job Opening")
                            }
                        }
                    }
                }
            } else {
                items(jobs, key = { it.id }) { job ->
                    val candidateCount = screenings.count { it.jobId == job.id }
                    JobCard(
                        job = job,
                        candidateCount = candidateCount,
                        onEdit = {
                            editingJob = job
                            showCreateDialog = true
                        },
                        onDelete = { onDeleteJob(job.id) },
                        onScreen = { onScreenForJob(job) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showCreateDialog) {
        JobEditorDialog(
            initialJob = editingJob,
            onDismiss = { showCreateDialog = false },
            onSave = { title, dept, desc, skills, quals, exp, status ->
                onSaveJob(editingJob?.id, title, dept, desc, skills, quals, exp, status)
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobCard(
    job: JobPosting,
    candidateCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onScreen: () -> Unit
) {
    val status = job.statusEnum()
    val (bg, fg) = when (status) {
        JobStatus.OPEN -> SuccessContainerLight to SuccessGreen
        JobStatus.PAUSED -> WarningContainerLight to androidx.compose.ui.graphics.Color(0xFFB45309)
        JobStatus.CLOSED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = bg, shape = RoundedCornerShape(50)) {
                            Text(
                                text = status.name,
                                color = fg,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = job.department,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = job.title, style = MaterialTheme.typography.titleLarge)
                }
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_job_${job.id}")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit job")
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_job_${job.id}")
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete job",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = job.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Min Experience: ${job.minExperienceYears} yrs • Qualifications: ${job.qualifications}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (job.requiredSkills.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    job.requiredSkills.take(10).forEach { skill ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = skill,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$candidateCount candidate(s) screened",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onScreen,
                    modifier = Modifier.testTag("screen_job_${job.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Screen Resumes")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobEditorDialog(
    initialJob: JobPosting?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        department: String,
        description: String,
        requiredSkillsRaw: String,
        qualifications: String,
        minExp: Int,
        status: JobStatus
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialJob?.title.orEmpty()) }
    var department by remember { mutableStateOf(initialJob?.department ?: "Engineering") }
    var description by remember { mutableStateOf(initialJob?.description.orEmpty()) }
    var skillsRaw by remember { mutableStateOf(initialJob?.requiredSkills?.joinToString(", ").orEmpty()) }
    var qualifications by remember { mutableStateOf(initialJob?.qualifications ?: "B.S. in Computer Science") }
    var minExpText by remember { mutableStateOf((initialJob?.minExperienceYears ?: 2).toString()) }
    var status by remember { mutableStateOf(initialJob?.statusEnum() ?: JobStatus.OPEN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialJob == null) "New Job Opening" else "Edit Job Opening")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Job Title *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("job_title_input")
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Job Description *") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("job_description_input")
                )
                OutlinedTextField(
                    value = skillsRaw,
                    onValueChange = { skillsRaw = it },
                    label = { Text("Required Skills (comma-separated)") },
                    placeholder = { Text("Python, spaCy, NLP, Docker, SQL") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("job_skills_input")
                )
                OutlinedTextField(
                    value = qualifications,
                    onValueChange = { qualifications = it },
                    label = { Text("Qualifications") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minExpText,
                    onValueChange = { minExpText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Min Experience (Years)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Posting Status", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JobStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val exp = minExpText.toIntOrNull() ?: 0
                    onSave(title, department, description, skillsRaw, qualifications, exp, status)
                },
                modifier = Modifier.testTag("save_job_button")
            ) {
                Text("Save Job")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
