package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.RecruitmentRepository
import com.example.data.model.CandidateProfile
import com.example.data.model.ExtractedSkillItem
import com.example.data.model.JobPosting
import com.example.data.model.JobStatus
import com.example.data.model.ParseStatus
import com.example.data.model.RecruiterProfile
import com.example.data.model.ResumeDocument
import com.example.data.model.ResumeFileType
import com.example.data.model.ReviewStatus
import com.example.data.model.ScoringWeights
import com.example.data.model.ScreeningResult
import com.example.data.model.ShortlistStatus
import com.example.data.model.ShortlistedCandidate
import com.example.data.model.UiState
import com.example.nlp.DocumentParser
import com.example.nlp.NlpPipeline
import com.example.nlp.SampleDataset
import com.example.nlp.SemanticAiService
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnalysisProgressState(
    val isRunning: Boolean = false,
    val currentFile: String = "",
    val stageLabel: String = "",
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val errorBanner: String? = null
)

class RecruitmentViewModel(
    private val repository: RecruitmentRepository,
    private val semanticAiService: SemanticAiService,
    private val currentUserId: String,
    private val recruiterDisplayName: String,
    private val recruiterEmail: String
) : ViewModel() {

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Stream error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val recruiterState: StateFlow<UiState<RecruiterProfile?>> =
        repository.observeRecruiterProfile(currentUserId).toUiState("recruiter profile")

    val jobsState: StateFlow<UiState<List<JobPosting>>> =
        repository.observeJobs(currentUserId).toUiState("jobs")

    val candidatesState: StateFlow<UiState<List<CandidateProfile>>> =
        repository.observeCandidates(currentUserId).toUiState("candidates")

    val resumesState: StateFlow<UiState<List<ResumeDocument>>> =
        repository.observeResumes(currentUserId).toUiState("resumes")

    val skillsState: StateFlow<UiState<List<ExtractedSkillItem>>> =
        repository.observeExtractedSkills(currentUserId).toUiState("extracted skills")

    val screeningsState: StateFlow<UiState<List<ScreeningResult>>> =
        repository.observeScreeningResults(currentUserId).toUiState("screening results")

    val shortlistState: StateFlow<UiState<List<ShortlistedCandidate>>> =
        repository.observeShortlisted(currentUserId).toUiState("shortlisted candidates")

    private val _selectedJobId = MutableStateFlow<String?>(null)
    val selectedJobId: StateFlow<String?> = _selectedJobId.asStateFlow()

    private val _analysisProgress = MutableStateFlow(AnalysisProgressState())
    val analysisProgress: StateFlow<AnalysisProgressState> = _analysisProgress.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _comparisonIds = MutableStateFlow<List<String>>(emptyList())
    val comparisonIds: StateFlow<List<String>> = _comparisonIds.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureRecruiterProfile(
                uid = currentUserId,
                displayName = recruiterDisplayName,
                email = recruiterEmail
            ).onFailure { e ->
                _snackbarMessage.value = e.localizedMessage ?: "Failed to initialize recruiter profile."
            }
        }
    }

    fun selectJobForScreening(jobId: String?) {
        _selectedJobId.value = jobId
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun clearAnalysisError() {
        _analysisProgress.value = _analysisProgress.value.copy(errorBanner = null)
    }

    fun toggleComparisonCandidate(resultId: String) {
        val current = _comparisonIds.value
        _comparisonIds.value = if (resultId in current) {
            current - resultId
        } else if (current.size < 3) {
            current + resultId
        } else {
            _snackbarMessage.value = "Compare up to 3 candidates at a time."
            current
        }
    }

    fun saveJob(
        existingJobId: String?,
        title: String,
        department: String,
        description: String,
        requiredSkillsRaw: String,
        qualifications: String,
        minExperienceYears: Int,
        status: JobStatus
    ) {
        val skills = requiredSkillsRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(50)

        if (title.trim().length < 2) {
            _snackbarMessage.value = "Job title must be at least 2 characters."
            return
        }
        if (description.trim().length < 10) {
            _snackbarMessage.value = "Job description must be at least 10 characters."
            return
        }

        viewModelScope.launch {
            val posting = JobPosting(
                id = existingJobId.orEmpty(),
                title = title.trim(),
                department = department.trim().ifBlank { "Engineering" },
                description = description.trim(),
                requiredSkills = skills,
                qualifications = qualifications.trim().ifBlank { "Bachelor's Degree or equivalent" },
                minExperienceYears = minExperienceYears.coerceIn(0, 50),
                status = status.name
            )
            val result = if (existingJobId.isNullOrBlank()) {
                repository.createJob(currentUserId, posting)
                    .onSuccess { newId ->
                        if (_selectedJobId.value == null) _selectedJobId.value = newId
                        _snackbarMessage.value = "Job '${posting.title}' created."
                    }
            } else {
                repository.updateJob(currentUserId, posting)
                    .onSuccess {
                        _snackbarMessage.value = "Job '${posting.title}' updated."
                    }
            }
            result.onFailure { e ->
                _snackbarMessage.value = e.localizedMessage ?: "Failed to save job."
            }
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            repository.deleteJob(currentUserId, jobId)
                .onSuccess {
                    if (_selectedJobId.value == jobId) _selectedJobId.value = null
                    _snackbarMessage.value = "Job deleted."
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Failed to delete job."
                }
        }
    }

    fun updateWeightsAndRecalculate(newWeights: ScoringWeights, organization: String? = null) {
        val norm = newWeights.normalized()
        viewModelScope.launch {
            repository.updateScoringWeights(currentUserId, norm, organization)
                .onSuccess {
                    val existingResults = (screeningsState.value as? UiState.Success)?.data.orEmpty()
                    existingResults.forEach { res ->
                        val updatedFinal = NlpPipeline.computeWeightedFinalScore(
                            semanticScore = res.semanticScore,
                            skillMatchScore = res.skillMatchScore,
                            experienceScore = res.experienceScore,
                            educationScore = res.educationScore,
                            weights = norm
                        )
                        repository.updateScreeningScores(
                            currentUserId,
                            res.copy(finalScore = updatedFinal)
                        )
                    }
                    _snackbarMessage.value = "Scoring weights saved and rankings recalculated."
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Failed to update scoring weights."
                }
        }
    }

    fun processUploadedResumes(context: Context, job: JobPosting, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val weights = (recruiterState.value as? UiState.Success)?.data?.toWeights() ?: ScoringWeights()
        val corpusDocs = (resumesState.value as? UiState.Success)?.data?.map { it.extractedText }.orEmpty()

        viewModelScope.launch {
            _analysisProgress.value = AnalysisProgressState(
                isRunning = true,
                currentFile = "Preparing batch...",
                stageLabel = "Validating ${uris.size} resume file(s)",
                processedCount = 0,
                totalCount = uris.size,
                errorBanner = null
            )

            var successCount = 0
            val failures = mutableListOf<String>()
            var aiFailureNote: String? = null

            uris.forEachIndexed { idx, uri ->
                _analysisProgress.value = _analysisProgress.value.copy(
                    currentFile = "File ${idx + 1} of ${uris.size}",
                    stageLabel = "Extracting text (PDF / DOCX)...",
                    processedCount = idx
                )

                val parsed = DocumentParser.parseUri(context, uri)
                if (parsed.isFailure) {
                    failures.add(parsed.exceptionOrNull()?.localizedMessage ?: "Parse error")
                    return@forEachIndexed
                }

                val fileRes = parsed.getOrThrow()
                val resPair = screenSingleResumeText(
                    job = job,
                    fileName = fileRes.fileName,
                    fileType = fileRes.fileType,
                    fileSizeBytes = fileRes.fileSizeBytes,
                    rawText = fileRes.extractedText,
                    weights = weights,
                    corpusDocs = corpusDocs
                )
                if (resPair.first) {
                    successCount++
                } else if (resPair.second != null) {
                    failures.add(resPair.second!!)
                }
                if (resPair.third != null && aiFailureNote == null) {
                    aiFailureNote = resPair.third
                }
            }

            _analysisProgress.value = AnalysisProgressState(
                isRunning = false,
                processedCount = uris.size,
                totalCount = uris.size,
                errorBanner = listOfNotNull(
                    if (failures.isNotEmpty()) failures.joinToString("; ") else null,
                    aiFailureNote
                ).joinToString("\n").ifBlank { null }
            )

            _snackbarMessage.value = when {
                successCount > 0 && failures.isEmpty() ->
                    "Analyzed & ranked $successCount resume(s) for ${job.title}."
                successCount > 0 ->
                    "Analyzed $successCount resume(s); ${failures.size} failed."
                else ->
                    failures.firstOrNull() ?: "No resumes could be processed."
            }
        }
    }

    fun loadSampleJobsAndResumes() {
        viewModelScope.launch {
            _analysisProgress.value = AnalysisProgressState(
                isRunning = true,
                currentFile = "Sample Dataset",
                stageLabel = "Creating sample job openings...",
                processedCount = 0,
                totalCount = SampleDataset.sampleResumes.size,
                errorBanner = null
            )

            val existingJobs = (jobsState.value as? UiState.Success)?.data.orEmpty()
            val primaryJob: JobPosting = existingJobs.firstOrNull() ?: run {
                var firstJob: JobPosting? = null
                for (sampleJob in SampleDataset.sampleJobs) {
                    val createdId = repository.createJob(currentUserId, sampleJob).getOrNull()
                    if (createdId != null && firstJob == null) {
                        firstJob = sampleJob.copy(id = createdId)
                    }
                }
                firstJob ?: run {
                    _analysisProgress.value = AnalysisProgressState(
                        isRunning = false,
                        errorBanner = "Could not create sample job postings."
                    )
                    return@launch
                }
            }

            _selectedJobId.value = primaryJob.id
            val weights = (recruiterState.value as? UiState.Success)?.data?.toWeights() ?: ScoringWeights()
            val corpusDocs = SampleDataset.sampleResumes.map { it.content }

            var successCount = 0
            var aiFailureNote: String? = null

            SampleDataset.sampleResumes.forEachIndexed { idx, spec ->
                _analysisProgress.value = _analysisProgress.value.copy(
                    currentFile = spec.fileName,
                    stageLabel = "NLP NER, TF-IDF & Gemini semantic scoring (${idx + 1}/${SampleDataset.sampleResumes.size})",
                    processedCount = idx
                )
                val fileType = if (spec.fileName.endsWith(".docx")) ResumeFileType.DOCX else ResumeFileType.PDF
                val outcome = screenSingleResumeText(
                    job = primaryJob,
                    fileName = spec.fileName,
                    fileType = fileType,
                    fileSizeBytes = spec.content.length.toLong() * 2L,
                    rawText = spec.content,
                    weights = weights,
                    corpusDocs = corpusDocs
                )
                if (outcome.first) successCount++
                if (outcome.third != null && aiFailureNote == null) aiFailureNote = outcome.third
            }

            _analysisProgress.value = AnalysisProgressState(
                isRunning = false,
                processedCount = SampleDataset.sampleResumes.size,
                totalCount = SampleDataset.sampleResumes.size,
                errorBanner = aiFailureNote
            )
            _snackbarMessage.value = "Loaded & ranked $successCount sample candidates for '${primaryJob.title}'."
        }
    }

    private suspend fun screenSingleResumeText(
        job: JobPosting,
        fileName: String,
        fileType: ResumeFileType,
        fileSizeBytes: Long,
        rawText: String,
        weights: ScoringWeights,
        corpusDocs: List<String>
    ): Triple<Boolean, String?, String?> {
        _analysisProgress.value = _analysisProgress.value.copy(
            currentFile = fileName,
            stageLabel = "Running NLP NER, lemmatization & TF-IDF cosine..."
        )

        val entities = NlpPipeline.extractEntities(rawText, fileName)
        val deterministic = NlpPipeline.evaluateDeterministicMatch(
            job = job,
            entities = entities,
            rawResumeText = rawText,
            corpusDocuments = corpusDocs
        )

        _analysisProgress.value = _analysisProgress.value.copy(
            stageLabel = "Running Gemini semantic similarity..."
        )

        val aiResult = semanticAiService.analyzeSemanticMatch(
            job = job,
            resumeText = rawText,
            deterministic = deterministic
        )

        var aiErrorBanner: String? = null
        val semanticScore: Double
        val explanation: String

        if (aiResult.isSuccess) {
            val ai = aiResult.getOrThrow()
            semanticScore = ai.semanticScore
            explanation = ai.evidenceSummary
        } else {
            val errMsg = aiResult.exceptionOrNull()?.localizedMessage ?: "Semantic AI service unavailable"
            aiErrorBanner = "Firebase AI notice: $errMsg. Deterministic TF-IDF & skill scoring preserved."
            semanticScore = deterministic.localSemanticFallbackScore
            explanation = "Semantic AI unavailable ($errMsg). Deterministic TF-IDF cosine (${"%.1f".format(deterministic.tfidfCosineScore)}%) and required skill overlap (${deterministic.matchedSkills.size}/${job.requiredSkills.size}) applied."
        }

        val finalScore = NlpPipeline.computeWeightedFinalScore(
            semanticScore = semanticScore,
            skillMatchScore = deterministic.skillMatchScore,
            experienceScore = deterministic.experienceScore,
            educationScore = deterministic.educationScore,
            weights = weights
        )

        val candidate = CandidateProfile(
            fullName = entities.fullName,
            email = entities.email,
            phone = entities.phone,
            totalExperienceYears = entities.totalExperienceYears,
            highestEducation = entities.highestEducation,
            workExperienceSummary = entities.workExperienceSummary,
            projectsSummary = entities.projectsSummary,
            certifications = entities.certifications
        )

        val resume = ResumeDocument(
            fileName = fileName,
            fileType = fileType.name,
            fileSizeBytes = fileSizeBytes,
            extractedText = rawText.take(45000),
            wordCount = entities.wordCount,
            parseStatus = ParseStatus.PARSED.name
        )

        val skillItems = entities.skills.map { (skillName, category) ->
            ExtractedSkillItem(
                skillName = skillName,
                category = category,
                confidenceScore = 0.92
            )
        }

        val screening = ScreeningResult(
            jobId = job.id,
            candidateName = entities.fullName,
            jobTitle = job.title,
            finalScore = finalScore,
            semanticScore = semanticScore,
            tfidfCosineScore = deterministic.tfidfCosineScore,
            skillMatchScore = deterministic.skillMatchScore,
            experienceScore = deterministic.experienceScore,
            educationScore = deterministic.educationScore,
            matchedSkills = deterministic.matchedSkills,
            missingSkills = deterministic.missingSkills,
            aiExplanation = explanation,
            reviewStatus = ReviewStatus.PENDING_REVIEW.name,
            reviewerNotes = ""
        )

        val saveRes = repository.saveScreenedCandidateBundle(
            uid = currentUserId,
            candidate = candidate,
            resume = resume,
            extractedSkills = skillItems,
            screeningResult = screening
        )

        return if (saveRes.isSuccess) {
            Triple(true, null, aiErrorBanner)
        } else {
            Triple(false, saveRes.exceptionOrNull()?.localizedMessage ?: "Failed to save $fileName", aiErrorBanner)
        }
    }

    fun updateReviewStatus(result: ScreeningResult, newStatus: ReviewStatus, notes: String) {
        viewModelScope.launch {
            repository.updateScreeningReviewStatus(currentUserId, result, newStatus, notes)
                .onSuccess {
                    _snackbarMessage.value = "${result.candidateName} marked as ${newStatus.label}."
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Failed to update review status."
                }
        }
    }

    fun updateShortlistItem(shortlistId: String, status: ShortlistStatus, note: String) {
        viewModelScope.launch {
            repository.updateShortlistEntry(currentUserId, shortlistId, status, note)
                .onSuccess {
                    _snackbarMessage.value = "Shortlist status updated to ${status.label}."
                }
                .onFailure { e ->
                    _snackbarMessage.value = e.localizedMessage ?: "Failed to update shortlist entry."
                }
        }
    }

    fun deleteScreeningResult(resultId: String) {
        viewModelScope.launch {
            _comparisonIds.value = _comparisonIds.value - resultId
            repository.deleteScreeningResult(currentUserId, resultId)
                .onSuccess { _snackbarMessage.value = "Candidate screening entry removed." }
                .onFailure { e -> _snackbarMessage.value = e.localizedMessage ?: "Failed to remove entry." }
        }
    }

    private companion object {
        const val TAG = "RecruitmentVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
