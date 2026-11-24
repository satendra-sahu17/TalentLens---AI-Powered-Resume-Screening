package com.example.nlp

import android.util.Log
import com.example.data.model.JobPosting
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class SemanticAnalysisResult(
    val semanticScore: Double,
    val evidenceSummary: String,
    val semanticMatchedConcepts: List<String>
)

class SemanticAiService {

    private val semanticSchema = Schema.obj(
        properties = mapOf(
            "semanticScore" to Schema.double(
                description = "Semantic similarity score between 0 and 100 based strictly on job-relevant skills, projects, and experience."
            ),
            "semanticMatchedConcepts" to Schema.array(
                items = Schema.string(),
                description = "Up to 8 technical concepts or domain strengths found in the resume relevant to the job."
            ),
            "evidenceSummary" to Schema.string(
                description = "2-3 sentence objective, explainable decision-support summary citing technical evidence and missing qualifications. Never mention age, gender, race, religion, or protected attributes."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = semanticSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun analyzeSemanticMatch(
        job: JobPosting,
        resumeText: String,
        deterministic: DeterministicMatchResult
    ): Result<SemanticAnalysisResult> {
        return try {
            val prompt = buildString {
                appendLine("You are an impartial NLP recruitment decision-support evaluator.")
                appendLine("Evaluate ONLY technical qualifications, projects, domain alignment, and work experience.")
                appendLine("STRICT FAIRNESS RULE: Ignore and never reference age, gender, ethnicity, religion, caste, marital status, or personal demographics.")
                appendLine()
                appendLine("JOB TITLE: ${job.title}")
                appendLine("REQUIRED SKILLS: ${job.requiredSkills.joinToString(", ")}")
                appendLine("QUALIFICATIONS: ${job.qualifications}")
                appendLine("MIN EXPERIENCE YEARS: ${job.minExperienceYears}")
                appendLine("JOB DESCRIPTION:")
                appendLine(job.description.take(2500))
                appendLine()
                appendLine("DETERMINISTIC SIGNALS:")
                appendLine("- TF-IDF Cosine Similarity: ${"%.1f".format(deterministic.tfidfCosineScore)}%")
                appendLine("- Matched Required Skills: ${deterministic.matchedSkills.joinToString(", ").ifBlank { "None" }}")
                appendLine("- Missing Required Skills: ${deterministic.missingSkills.joinToString(", ").ifBlank { "None" }}")
                appendLine()
                appendLine("CANDIDATE RESUME TEXT:")
                appendLine(resumeText.take(4500))
            }

            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }

            val rawJson = response.text?.trim()
                ?: return Result.failure(IllegalStateException("Gemini returned an empty semantic evaluation."))

            val json = JSONObject(rawJson)
            val score = json.optDouble("semanticScore", deterministic.localSemanticFallbackScore)
                .coerceIn(0.0, 100.0)
            val summary = json.optString("evidenceSummary", "")
                .ifBlank { "Semantic evaluation completed based on technical alignment." }
            val conceptsArr = json.optJSONArray("semanticMatchedConcepts")
            val concepts = buildList {
                if (conceptsArr != null) {
                    for (i in 0 until conceptsArr.length()) {
                        val item = conceptsArr.optString(i).trim()
                        if (item.isNotBlank()) add(item)
                    }
                }
            }

            Result.success(
                SemanticAnalysisResult(
                    semanticScore = score,
                    evidenceSummary = summary,
                    semanticMatchedConcepts = concepts
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Semantic AI analysis timed out", e)
            Result.failure(IllegalStateException("Semantic AI analysis timed out after 45s.", e))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Semantic AI analysis failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "SemanticAiService"
    }
}
