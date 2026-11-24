package com.example.export

import android.content.Context
import android.net.Uri
import com.example.data.model.CandidateProfile
import com.example.data.model.ResumeDocument
import com.example.data.model.ScreeningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    fun buildRankingsCsv(
        results: List<ScreeningResult>,
        candidatesById: Map<String, CandidateProfile>
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM for clean Excel compatibility
        sb.append('\uFEFF')
        sb.appendLine(
            "Rank,Candidate Name,Email,Phone,Job Title,Final Score (%),Semantic (%),TF-IDF Cosine (%),Skills Match (%),Experience (%),Education (%),Years Experience,Education Level,Matched Skills,Missing Skills,Review Status,Reviewer Notes"
        )
        results.sortedByDescending { it.finalScore }.forEachIndexed { index, r ->
            val candidate = candidatesById[r.candidateId]
            val row = listOf(
                (index + 1).toString(),
                r.candidateName,
                candidate?.email.orEmpty(),
                candidate?.phone.orEmpty(),
                r.jobTitle,
                "%.1f".format(Locale.US, r.finalScore),
                "%.1f".format(Locale.US, r.semanticScore),
                "%.1f".format(Locale.US, r.tfidfCosineScore),
                "%.1f".format(Locale.US, r.skillMatchScore),
                "%.1f".format(Locale.US, r.experienceScore),
                "%.1f".format(Locale.US, r.educationScore),
                "%.1f".format(Locale.US, candidate?.totalExperienceYears ?: 0.0),
                candidate?.highestEducation.orEmpty(),
                r.matchedSkills.joinToString("; "),
                r.missingSkills.joinToString("; "),
                r.reviewStatusEnum().label,
                r.reviewerNotes
            ).joinToString(",") { escapeCsv(it) }
            sb.appendLine(row)
        }
        return sb.toString()
    }

    fun buildCandidateReport(
        result: ScreeningResult,
        candidate: CandidateProfile?,
        resume: ResumeDocument?
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        return buildString {
            appendLine("======================================================================")
            appendLine("TALENTLENS AI - INDIVIDUAL CANDIDATE SCREENING REPORT")
            appendLine("Generated: $dateStr")
            appendLine("======================================================================")
            appendLine()
            appendLine("RESPONSIBLE AI & DECISION-SUPPORT NOTICE:")
            appendLine("NLP matching scores are configurable decision-support signals only, NOT")
            appendLine("automated hiring decisions. Protected attributes (age, gender, religion,")
            appendLine("ethnicity, photo) are strictly excluded from scoring.")
            appendLine()
            appendLine("CANDIDATE SUMMARY")
            appendLine("-----------------")
            appendLine("Name:              ${result.candidateName}")
            appendLine("Email:             ${candidate?.email.orEmpty().ifBlank { "N/A" }}")
            appendLine("Phone:             ${candidate?.phone.orEmpty().ifBlank { "N/A" }}")
            appendLine("Experience:        ${"%.1f".format(Locale.US, candidate?.totalExperienceYears ?: 0.0)} years")
            appendLine("Education:         ${candidate?.highestEducation.orEmpty().ifBlank { "N/A" }}")
            appendLine("Certifications:    ${candidate?.certifications?.joinToString(", ").orEmpty().ifBlank { "None extracted" }}")
            appendLine()
            appendLine("JOB ALIGNMENT & NLP SCORE BREAKDOWN")
            appendLine("-----------------------------------")
            appendLine("Target Role:       ${result.jobTitle}")
            appendLine("Final Match Score: ${"%.1f".format(Locale.US, result.finalScore)}%")
            appendLine("Semantic Score:    ${"%.1f".format(Locale.US, result.semanticScore)}%")
            appendLine("TF-IDF Cosine:     ${"%.1f".format(Locale.US, result.tfidfCosineScore)}%")
            appendLine("Technical Skills:  ${"%.1f".format(Locale.US, result.skillMatchScore)}%")
            appendLine("Experience Score:  ${"%.1f".format(Locale.US, result.experienceScore)}%")
            appendLine("Education Score:   ${"%.1f".format(Locale.US, result.educationScore)}%")
            appendLine("Review Status:     ${result.reviewStatusEnum().label}")
            if (result.reviewerNotes.isNotBlank()) {
                appendLine("Recruiter Notes:   ${result.reviewerNotes}")
            }
            appendLine()
            appendLine("EXPLAINABLE SKILL EVIDENCE")
            appendLine("--------------------------")
            appendLine("Matched Skills:    ${result.matchedSkills.joinToString(", ").ifBlank { "None" }}")
            appendLine("Missing Skills:    ${result.missingSkills.joinToString(", ").ifBlank { "None" }}")
            appendLine()
            appendLine("AI EVIDENCE SUMMARY")
            appendLine("-------------------")
            appendLine(result.aiExplanation.ifBlank { "N/A" })
            appendLine()
            if (resume != null) {
                appendLine("ORIGINAL EXTRACTED RESUME (${resume.fileName})")
                appendLine("------------------------------------------------------------")
                appendLine(resume.extractedText)
            }
        }
    }

    suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: return@withContext Result.failure(IllegalStateException("Unable to open output stream."))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r", " ").replace("\n", " ")
        return if (clean.contains(",") || clean.contains("\"") || clean.contains(";")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }
}
