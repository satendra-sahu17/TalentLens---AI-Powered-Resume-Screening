package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.CandidateProfile
import com.example.data.model.ExtractedSkillItem
import com.example.data.model.JobPosting
import com.example.data.model.JobStatus
import com.example.data.model.RecruiterProfile
import com.example.data.model.ResumeDocument
import com.example.data.model.ReviewStatus
import com.example.data.model.ScoringWeights
import com.example.data.model.ScreeningResult
import com.example.data.model.ShortlistStatus
import com.example.data.model.ShortlistedCandidate
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RecruitmentRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Recruiter must sign in with Google before accessing Firestore.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Active recruiter session changed; refusing cross-user operation." }
        }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun jobsCol(uid: String) = userDoc(uid).collection("jobs")
    private fun candidatesCol(uid: String) = userDoc(uid).collection("candidates")
    private fun resumesCol(uid: String) = userDoc(uid).collection("resumes")
    private fun skillsCol(uid: String) = userDoc(uid).collection("extracted_skills")
    private fun screeningsCol(uid: String) = userDoc(uid).collection("screening_results")
    private fun shortlistCol(uid: String) = userDoc(uid).collection("shortlisted_candidates")

    fun observeRecruiterProfile(uid: String): Flow<RecruiterProfile?> = flow {
        requireUserId(uid)
        val path = "users/$uid"
        emitAll(
            userDoc(uid).snapshots()
                .map { snap ->
                    snap.toObject(
                        RecruiterProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeJobs(uid: String): Flow<List<JobPosting>> = flow {
        requireUserId(uid)
        val path = "users/$uid/jobs"
        emitAll(
            jobsCol(uid).snapshots()
                .map { snap ->
                    snap.toObjects(
                        JobPosting::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeCandidates(uid: String): Flow<List<CandidateProfile>> = flow {
        requireUserId(uid)
        val path = "users/$uid/candidates"
        emitAll(
            candidatesCol(uid).snapshots()
                .map { snap ->
                    snap.toObjects(
                        CandidateProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeResumes(uid: String): Flow<List<ResumeDocument>> = flow {
        requireUserId(uid)
        val path = "users/$uid/resumes"
        emitAll(
            resumesCol(uid).snapshots()
                .map { snap ->
                    snap.toObjects(
                        ResumeDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeExtractedSkills(uid: String): Flow<List<ExtractedSkillItem>> = flow {
        requireUserId(uid)
        val path = "users/$uid/extracted_skills"
        emitAll(
            skillsCol(uid).snapshots()
                .map { snap ->
                    snap.toObjects(
                        ExtractedSkillItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeScreeningResults(uid: String): Flow<List<ScreeningResult>> = flow {
        requireUserId(uid)
        val path = "users/$uid/screening_results"
        emitAll(
            screeningsCol(uid).snapshots()
                .map { snap ->
                    snap.toObjects(
                        ScreeningResult::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.finalScore }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeShortlisted(uid: String): Flow<List<ShortlistedCandidate>> = flow {
        requireUserId(uid)
        val path = "users/$uid/shortlisted_candidates"
        emitAll(
            shortlistCol(uid).snapshots()
                .map { snap ->
                    snap.toObjects(
                        ShortlistedCandidate::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.finalScore }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        op: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun ensureRecruiterProfile(
        uid: String,
        displayName: String,
        email: String
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            requireUserId(uid)
            val snap = userDoc(uid).get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "Recruiter" }.take(120)
                val safeEmail = email.trim().ifBlank { "recruiter@talentlens.ai" }.take(200)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "organization" to "Talent Acquisition",
                    "semanticWeight" to 0.40,
                    "skillsWeight" to 0.30,
                    "experienceWeight" to 0.20,
                    "educationWeight" to 0.10,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDoc(uid).set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateScoringWeights(
        uid: String,
        weights: ScoringWeights,
        organization: String? = null
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            requireUserId(uid)
            val norm = weights.normalized()
            val updateMap = mutableMapOf<String, Any>(
                "semanticWeight" to norm.semanticWeight.coerceIn(0.0, 1.0),
                "skillsWeight" to norm.skillsWeight.coerceIn(0.0, 1.0),
                "experienceWeight" to norm.experienceWeight.coerceIn(0.0, 1.0),
                "educationWeight" to norm.educationWeight.coerceIn(0.0, 1.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (organization != null) {
                updateMap["organization"] = organization.trim().take(150)
            }
            userDoc(uid).update(updateMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createJob(uid: String, job: JobPosting): Result<String> {
        var path = "users/$uid/jobs"
        return try {
            requireUserId(uid)
            require(job.title.trim().length >= 2) { "Job title must be at least 2 characters." }
            require(job.description.trim().length >= 10) { "Job description must be at least 10 characters." }
            val ref = jobsCol(uid).document()
            path = ref.path
            ref.set(job.toCreateMap(uid)).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateJob(uid: String, job: JobPosting): Result<Unit> {
        val path = "users/$uid/jobs/${job.id}"
        return try {
            requireUserId(uid)
            require(job.id.isNotBlank()) { "Job ID is required." }
            require(job.title.trim().length >= 2) { "Job title must be at least 2 characters." }
            require(job.description.trim().length >= 10) { "Job description must be at least 10 characters." }
            jobsCol(uid).document(job.id).update(
                mapOf(
                    "title" to job.title.trim().take(160),
                    "department" to job.department.trim().ifBlank { "Engineering" }.take(120),
                    "description" to job.description.trim().take(15000),
                    "requiredSkills" to job.requiredSkills.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(50),
                    "qualifications" to job.qualifications.trim().ifBlank { "Bachelor's Degree" }.take(2000),
                    "minExperienceYears" to job.minExperienceYears.coerceIn(0, 50),
                    "status" to JobStatus.fromRaw(job.status).name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteJob(uid: String, jobId: String): Result<Unit> {
        val path = "users/$uid/jobs/$jobId"
        return try {
            requireUserId(uid)
            require(jobId.isNotBlank()) { "Job ID is required." }
            jobsCol(uid).document(jobId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveScreenedCandidateBundle(
        uid: String,
        candidate: CandidateProfile,
        resume: ResumeDocument,
        extractedSkills: List<ExtractedSkillItem>,
        screeningResult: ScreeningResult
    ): Result<String> {
        val path = "users/$uid/screening_results"
        return try {
            requireUserId(uid)
            val batch = db.batch()
            val candidateRef = candidatesCol(uid).document()
            val resumeRef = resumesCol(uid).document()
            val screeningRef = screeningsCol(uid).document()

            batch.set(candidateRef, candidate.toCreateMap(uid))
            batch.set(resumeRef, resume.copy(candidateId = candidateRef.id).toCreateMap(uid))

            extractedSkills.take(25).forEach { skill ->
                val skillRef = skillsCol(uid).document()
                batch.set(
                    skillRef,
                    skill.copy(candidateId = candidateRef.id, resumeId = resumeRef.id).toCreateMap(uid)
                )
            }

            batch.set(
                screeningRef,
                screeningResult.copy(
                    candidateId = candidateRef.id,
                    resumeId = resumeRef.id,
                    reviewStatus = ReviewStatus.PENDING_REVIEW.name
                ).toCreateMap(uid)
            )

            batch.commit().await()
            Result.success(screeningRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateScreeningReviewStatus(
        uid: String,
        result: ScreeningResult,
        newStatus: ReviewStatus,
        reviewerNotes: String
    ): Result<Unit> {
        val path = "users/$uid/screening_results/${result.id}"
        return try {
            requireUserId(uid)
            require(result.id.isNotBlank()) { "Screening result ID is required." }
            val batch = db.batch()
            val screeningRef = screeningsCol(uid).document(result.id)
            batch.update(
                screeningRef,
                mapOf(
                    "reviewStatus" to newStatus.name,
                    "reviewerNotes" to reviewerNotes.trim().take(2000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val shortlistId = "short_${result.id}"
            val shortlistRef = shortlistCol(uid).document(shortlistId)
            if (newStatus == ReviewStatus.SHORTLISTED) {
                val shortlistModel = ShortlistedCandidate(
                    jobId = result.jobId,
                    candidateId = result.candidateId,
                    screeningResultId = result.id,
                    candidateName = result.candidateName,
                    jobTitle = result.jobTitle,
                    finalScore = result.finalScore,
                    priorityNote = reviewerNotes.trim().ifBlank { "Shortlisted by recruiter" }.take(1000),
                    status = ShortlistStatus.SHORTLISTED.name
                )
                batch.set(shortlistRef, shortlistModel.toCreateMap(uid))
            } else {
                batch.delete(shortlistRef)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateScreeningScores(
        uid: String,
        result: ScreeningResult
    ): Result<Unit> {
        val path = "users/$uid/screening_results/${result.id}"
        return try {
            requireUserId(uid)
            require(result.id.isNotBlank()) { "Screening result ID is required." }
            screeningsCol(uid).document(result.id).update(
                mapOf(
                    "finalScore" to result.finalScore.coerceIn(0.0, 100.0),
                    "semanticScore" to result.semanticScore.coerceIn(0.0, 100.0),
                    "tfidfCosineScore" to result.tfidfCosineScore.coerceIn(0.0, 100.0),
                    "skillMatchScore" to result.skillMatchScore.coerceIn(0.0, 100.0),
                    "experienceScore" to result.experienceScore.coerceIn(0.0, 100.0),
                    "educationScore" to result.educationScore.coerceIn(0.0, 100.0),
                    "matchedSkills" to result.matchedSkills.take(50),
                    "missingSkills" to result.missingSkills.take(50),
                    "aiExplanation" to result.aiExplanation.take(4000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateShortlistEntry(
        uid: String,
        shortlistId: String,
        status: ShortlistStatus,
        priorityNote: String
    ): Result<Unit> {
        val path = "users/$uid/shortlisted_candidates/$shortlistId"
        return try {
            requireUserId(uid)
            require(shortlistId.isNotBlank()) { "Shortlist ID is required." }
            shortlistCol(uid).document(shortlistId).update(
                mapOf(
                    "status" to status.name,
                    "priorityNote" to priorityNote.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteScreeningResult(uid: String, resultId: String): Result<Unit> {
        val path = "users/$uid/screening_results/$resultId"
        return try {
            requireUserId(uid)
            require(resultId.isNotBlank()) { "Screening result ID is required." }
            val batch = db.batch()
            batch.delete(screeningsCol(uid).document(resultId))
            batch.delete(shortlistCol(uid).document("short_$resultId"))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
