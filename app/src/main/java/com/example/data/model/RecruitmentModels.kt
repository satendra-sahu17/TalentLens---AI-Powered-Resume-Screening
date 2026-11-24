package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class JobStatus {
    OPEN, PAUSED, CLOSED;

    companion object {
        fun fromRaw(raw: String?): JobStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

enum class ResumeFileType {
    PDF, DOCX, SAMPLE;

    companion object {
        fun fromRaw(raw: String?): ResumeFileType =
            entries.firstOrNull { it.name == raw } ?: PDF
    }
}

enum class ParseStatus {
    PARSED, FAILED;

    companion object {
        fun fromRaw(raw: String?): ParseStatus =
            entries.firstOrNull { it.name == raw } ?: PARSED
    }
}

enum class ReviewStatus(val label: String) {
    PENDING_REVIEW("Pending Review"),
    SHORTLISTED("Shortlisted"),
    NEEDS_REVIEW("Needs Review"),
    NOT_SELECTED("Not Selected");

    companion object {
        fun fromRaw(raw: String?): ReviewStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING_REVIEW
    }
}

enum class ShortlistStatus(val label: String) {
    SHORTLISTED("Shortlisted"),
    INTERVIEW_SCHEDULED("Interview Scheduled"),
    OFFER_EXTENDED("Offer Extended"),
    ARCHIVED("Archived");

    companion object {
        fun fromRaw(raw: String?): ShortlistStatus =
            entries.firstOrNull { it.name == raw } ?: SHORTLISTED
    }
}

data class ScoringWeights(
    val semanticWeight: Double = 0.40,
    val skillsWeight: Double = 0.30,
    val experienceWeight: Double = 0.20,
    val educationWeight: Double = 0.10
) {
    fun normalized(): ScoringWeights {
        val sum = semanticWeight + skillsWeight + experienceWeight + educationWeight
        if (sum <= 0.0) return ScoringWeights()
        return ScoringWeights(
            semanticWeight = semanticWeight / sum,
            skillsWeight = skillsWeight / sum,
            experienceWeight = experienceWeight / sum,
            educationWeight = educationWeight / sum
        )
    }
}

@Keep
@IgnoreExtraProperties
data class RecruiterProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val organization: String = "",
    val semanticWeight: Double = 0.40,
    val skillsWeight: Double = 0.30,
    val experienceWeight: Double = 0.20,
    val educationWeight: Double = 0.10,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWeights(): ScoringWeights = ScoringWeights(
        semanticWeight = semanticWeight,
        skillsWeight = skillsWeight,
        experienceWeight = experienceWeight,
        educationWeight = educationWeight
    ).normalized()
}

@Keep
@IgnoreExtraProperties
data class JobPosting(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val department: String = "",
    val description: String = "",
    val requiredSkills: List<String> = emptyList(),
    val qualifications: String = "",
    val minExperienceYears: Int = 0,
    val status: String = JobStatus.OPEN.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): JobStatus = JobStatus.fromRaw(status)

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to title.trim().take(160),
        "department" to department.trim().ifBlank { "Engineering" }.take(120),
        "description" to description.trim().take(15000),
        "requiredSkills" to requiredSkills.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(50),
        "qualifications" to qualifications.trim().ifBlank { "Bachelor's Degree or equivalent" }.take(2000),
        "minExperienceYears" to minExperienceYears.coerceIn(0, 50),
        "status" to JobStatus.fromRaw(status).name,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CandidateProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val totalExperienceYears: Double = 0.0,
    val highestEducation: String = "",
    val workExperienceSummary: String = "",
    val projectsSummary: String = "",
    val certifications: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "fullName" to fullName.trim().ifBlank { "Unnamed Candidate" }.take(150),
        "email" to email.trim().take(200),
        "phone" to phone.trim().take(60),
        "totalExperienceYears" to totalExperienceYears.coerceIn(0.0, 60.0),
        "highestEducation" to highestEducation.trim().take(300),
        "workExperienceSummary" to workExperienceSummary.trim().take(6000),
        "projectsSummary" to projectsSummary.trim().take(6000),
        "certifications" to certifications.map { it.trim() }.filter { it.isNotBlank() }.take(30),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ResumeDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val candidateId: String = "",
    val fileName: String = "",
    val fileType: String = ResumeFileType.PDF.name,
    val fileSizeBytes: Long = 0L,
    val extractedText: String = "",
    val wordCount: Int = 0,
    val parseStatus: String = ParseStatus.PARSED.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "candidateId" to candidateId,
        "fileName" to fileName.trim().ifBlank { "resume.pdf" }.take(255),
        "fileType" to ResumeFileType.fromRaw(fileType).name,
        "fileSizeBytes" to fileSizeBytes.coerceIn(0L, 15_728_640L),
        "extractedText" to extractedText.trim().ifBlank { "No text" }.take(45000),
        "wordCount" to wordCount.coerceIn(0, 20000),
        "parseStatus" to ParseStatus.fromRaw(parseStatus).name,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ExtractedSkillItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val candidateId: String = "",
    val resumeId: String = "",
    val skillName: String = "",
    val category: String = "",
    val confidenceScore: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "candidateId" to candidateId,
        "resumeId" to resumeId,
        "skillName" to skillName.trim().take(80),
        "category" to category.trim().ifBlank { "Technical" }.take(80),
        "confidenceScore" to confidenceScore.coerceIn(0.0, 1.0),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ScreeningResult(
    @DocumentId val id: String = "",
    val userId: String = "",
    val jobId: String = "",
    val candidateId: String = "",
    val resumeId: String = "",
    val candidateName: String = "",
    val jobTitle: String = "",
    val finalScore: Double = 0.0,
    val semanticScore: Double = 0.0,
    val tfidfCosineScore: Double = 0.0,
    val skillMatchScore: Double = 0.0,
    val experienceScore: Double = 0.0,
    val educationScore: Double = 0.0,
    val matchedSkills: List<String> = emptyList(),
    val missingSkills: List<String> = emptyList(),
    val aiExplanation: String = "",
    val reviewStatus: String = ReviewStatus.PENDING_REVIEW.name,
    val reviewerNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun reviewStatusEnum(): ReviewStatus = ReviewStatus.fromRaw(reviewStatus)

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "jobId" to jobId,
        "candidateId" to candidateId,
        "resumeId" to resumeId,
        "candidateName" to candidateName.trim().ifBlank { "Candidate" }.take(150),
        "jobTitle" to jobTitle.trim().ifBlank { "Role" }.take(160),
        "finalScore" to finalScore.coerceIn(0.0, 100.0),
        "semanticScore" to semanticScore.coerceIn(0.0, 100.0),
        "tfidfCosineScore" to tfidfCosineScore.coerceIn(0.0, 100.0),
        "skillMatchScore" to skillMatchScore.coerceIn(0.0, 100.0),
        "experienceScore" to experienceScore.coerceIn(0.0, 100.0),
        "educationScore" to educationScore.coerceIn(0.0, 100.0),
        "matchedSkills" to matchedSkills.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(50),
        "missingSkills" to missingSkills.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(50),
        "aiExplanation" to aiExplanation.trim().take(4000),
        "reviewStatus" to ReviewStatus.fromRaw(reviewStatus).name,
        "reviewerNotes" to reviewerNotes.trim().take(2000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ShortlistedCandidate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val jobId: String = "",
    val candidateId: String = "",
    val screeningResultId: String = "",
    val candidateName: String = "",
    val jobTitle: String = "",
    val finalScore: Double = 0.0,
    val priorityNote: String = "",
    val status: String = ShortlistStatus.SHORTLISTED.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): ShortlistStatus = ShortlistStatus.fromRaw(status)

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "jobId" to jobId,
        "candidateId" to candidateId,
        "screeningResultId" to screeningResultId,
        "candidateName" to candidateName.trim().ifBlank { "Candidate" }.take(150),
        "jobTitle" to jobTitle.trim().ifBlank { "Role" }.take(160),
        "finalScore" to finalScore.coerceIn(0.0, 100.0),
        "priorityNote" to priorityNote.trim().take(1000),
        "status" to ShortlistStatus.fromRaw(status).name,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
