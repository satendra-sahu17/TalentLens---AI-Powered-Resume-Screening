package com.example

import com.example.data.model.JobPosting
import com.example.data.model.ScoringWeights
import com.example.nlp.NlpPipeline
import com.example.nlp.SampleDataset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NlpPipelineTest {

    @Test
    fun `nlp pipeline extracts entities and ranks strong candidate above weaker match`() {
        val job = SampleDataset.sampleJobs.first()
        val strongResume = SampleDataset.sampleResumes.first().content
        val weakerResume = SampleDataset.sampleResumes.last().content

        val strongEntities = NlpPipeline.extractEntities(strongResume)
        val weakEntities = NlpPipeline.extractEntities(weakerResume)

        assertEquals("Priya Nair", strongEntities.fullName)
        assertEquals("priya.nair@talentlens-sample.org", strongEntities.email)
        assertTrue(strongEntities.totalExperienceYears >= 6.0)
        assertTrue(strongEntities.skills.any { it.first == "Python" })
        assertTrue(strongEntities.skills.any { it.first == "spaCy" })

        val corpus = SampleDataset.sampleResumes.map { it.content }
        val strongMatch = NlpPipeline.evaluateDeterministicMatch(job, strongEntities, strongResume, corpus)
        val weakMatch = NlpPipeline.evaluateDeterministicMatch(job, weakEntities, weakerResume, corpus)

        val weights = ScoringWeights()
        val strongFinal = NlpPipeline.computeWeightedFinalScore(
            semanticScore = strongMatch.localSemanticFallbackScore,
            skillMatchScore = strongMatch.skillMatchScore,
            experienceScore = strongMatch.experienceScore,
            educationScore = strongMatch.educationScore,
            weights = weights
        )
        val weakFinal = NlpPipeline.computeWeightedFinalScore(
            semanticScore = weakMatch.localSemanticFallbackScore,
            skillMatchScore = weakMatch.skillMatchScore,
            experienceScore = weakMatch.experienceScore,
            educationScore = weakMatch.educationScore,
            weights = weights
        )

        assertTrue("Strong match ($strongFinal) should exceed weak match ($weakFinal)", strongFinal > weakFinal)
        assertTrue(strongMatch.matchedSkills.size > weakMatch.matchedSkills.size)
        assertTrue(strongMatch.tfidfCosineScore > weakMatch.tfidfCosineScore)
    }

    @Test
    fun `tfidf cosine similarity returns 1 for identical docs and 0 for disjoint docs`() {
        val docA = NlpPipeline.preprocessAndLemmatize("Python spaCy NLP TF-IDF Transformers")
        val docB = NlpPipeline.preprocessAndLemmatize("Kotlin Jetpack Compose Android Firestore")

        val selfSim = NlpPipeline.computeTfidfCosineSimilarity(docA, docA)
        val disjointSim = NlpPipeline.computeTfidfCosineSimilarity(docA, docB)

        assertEquals(1.0, selfSim, 0.001)
        assertEquals(0.0, disjointSim, 0.001)
    }

    @Test
    fun `weighted score stays bounded within 0 to 100`() {
        val score = NlpPipeline.computeWeightedFinalScore(
            semanticScore = 95.0,
            skillMatchScore = 90.0,
            experienceScore = 100.0,
            educationScore = 100.0,
            weights = ScoringWeights(0.4, 0.3, 0.2, 0.1)
        )
        assertEquals(95.0, score, 0.01)
    }
}
