package com.example.nlp

import com.example.data.model.JobPosting
import com.example.data.model.ScoringWeights
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.sqrt

data class SkillTaxonomyEntry(
    val canonical: String,
    val category: String,
    val aliases: List<String>
)

data class ExtractedResumeEntities(
    val fullName: String,
    val email: String,
    val phone: String,
    val skills: List<Pair<String, String>>, // (canonical, category)
    val highestEducation: String,
    val educationTier: Int, // 0=unknown, 1=diploma/assoc, 2=bachelor, 3=master, 4=phd
    val totalExperienceYears: Double,
    val workExperienceSummary: String,
    val projectsSummary: String,
    val certifications: List<String>,
    val wordCount: Int
)

data class DeterministicMatchResult(
    val tfidfCosineScore: Double,
    val skillMatchScore: Double,
    val experienceScore: Double,
    val educationScore: Double,
    val matchedSkills: List<String>,
    val missingSkills: List<String>,
    val localSemanticFallbackScore: Double
)

object NlpPipeline {

    private val stopWords = setOf(
        "a", "an", "the", "and", "or", "but", "if", "then", "else", "when", "at", "by", "for",
        "from", "in", "into", "of", "off", "on", "onto", "out", "over", "to", "under", "up",
        "with", "within", "without", "as", "is", "am", "are", "was", "were", "be", "been",
        "being", "have", "has", "had", "do", "does", "did", "will", "would", "shall", "should",
        "can", "could", "may", "might", "must", "this", "that", "these", "those", "it", "its",
        "they", "them", "their", "we", "our", "you", "your", "he", "she", "his", "her", "i",
        "my", "me", "such", "also", "more", "most", "other", "some", "any", "all", "no", "not",
        "only", "own", "same", "so", "than", "too", "very", "just", "about", "above", "after",
        "again", "against", "because", "before", "below", "between", "both", "during", "each",
        "few", "further", "here", "there", "how", "what", "which", "who", "whom", "why", "where",
        "while", "using", "used", "use", "work", "worked", "working", "team", "experience",
        "responsible", "responsibilities", "including", "across", "strong", "excellent", "good"
    )

    private val skillTaxonomy: List<SkillTaxonomyEntry> = listOf(
        // AI / ML / NLP
        SkillTaxonomyEntry("Python", "Languages", listOf("python", "python3", "py")),
        SkillTaxonomyEntry("NLP", "AI & ML", listOf("nlp", "natural language processing", "text mining")),
        SkillTaxonomyEntry("spaCy", "AI & ML", listOf("spacy")),
        SkillTaxonomyEntry("NLTK", "AI & ML", listOf("nltk")),
        SkillTaxonomyEntry("Scikit-learn", "AI & ML", listOf("scikit-learn", "scikit learn", "sklearn")),
        SkillTaxonomyEntry("PyTorch", "AI & ML", listOf("pytorch", "torch")),
        SkillTaxonomyEntry("TensorFlow", "AI & ML", listOf("tensorflow", "tf2", "keras")),
        SkillTaxonomyEntry("Transformers", "AI & ML", listOf("transformers", "huggingface", "hugging face", "bert", "sentence transformers", "sentence-transformers")),
        SkillTaxonomyEntry("LLMs", "AI & ML", listOf("llm", "llms", "large language models", "generative ai", "genai", "rag", "prompt engineering")),
        SkillTaxonomyEntry("TF-IDF", "AI & ML", listOf("tf-idf", "tfidf", "cosine similarity", "bm25")),
        SkillTaxonomyEntry("NER", "AI & ML", listOf("ner", "named entity recognition")),
        SkillTaxonomyEntry("Machine Learning", "AI & ML", listOf("machine learning", "ml", "supervised learning", "xgboost", "lightgbm")),
        SkillTaxonomyEntry("Deep Learning", "AI & ML", listOf("deep learning", "neural networks", "cnn", "rnn", "lstm")),
        SkillTaxonomyEntry("Computer Vision", "AI & ML", listOf("computer vision", "opencv", "yolo")),
        SkillTaxonomyEntry("Pandas", "Data Science", listOf("pandas")),
        SkillTaxonomyEntry("NumPy", "Data Science", listOf("numpy")),
        SkillTaxonomyEntry("Data Science", "Data Science", listOf("data science", "statistical modeling", "a/b testing")),
        SkillTaxonomyEntry("Matplotlib", "Data Science", listOf("matplotlib", "seaborn", "plotly")),

        // Languages
        SkillTaxonomyEntry("Kotlin", "Languages", listOf("kotlin", "coroutines", "kotlin flow")),
        SkillTaxonomyEntry("Java", "Languages", listOf("java", "jvm", "spring", "spring boot")),
        SkillTaxonomyEntry("JavaScript", "Languages", listOf("javascript", "js", "es6", "ecmascript")),
        SkillTaxonomyEntry("TypeScript", "Languages", listOf("typescript", "ts")),
        SkillTaxonomyEntry("C++", "Languages", listOf("c++", "cpp")),
        SkillTaxonomyEntry("C#", "Languages", listOf("c#", "csharp", ".net", "dotnet")),
        SkillTaxonomyEntry("Go", "Languages", listOf("golang")),
        SkillTaxonomyEntry("Rust", "Languages", listOf("rust")),
        SkillTaxonomyEntry("SQL", "Languages", listOf("sql", "t-sql", "pl/sql")),
        SkillTaxonomyEntry("R", "Languages", listOf("rstudio")),

        // Web / Mobile / Backend
        SkillTaxonomyEntry("React", "Frontend", listOf("react", "react.js", "reactjs", "redux", "next.js", "nextjs")),
        SkillTaxonomyEntry("HTML5", "Frontend", listOf("html5", "html")),
        SkillTaxonomyEntry("CSS3", "Frontend", listOf("css3", "css", "tailwind", "tailwindcss", "bootstrap")),
        SkillTaxonomyEntry("Chart.js", "Frontend", listOf("chart.js", "chartjs", "d3.js", "recharts")),
        SkillTaxonomyEntry("Jetpack Compose", "Mobile", listOf("jetpack compose", "compose")),
        SkillTaxonomyEntry("Android SDK", "Mobile", listOf("android", "android sdk", "material 3")),
        SkillTaxonomyEntry("Flask", "Backend", listOf("flask")),
        SkillTaxonomyEntry("Django", "Backend", listOf("django")),
        SkillTaxonomyEntry("FastAPI", "Backend", listOf("fastapi")),
        SkillTaxonomyEntry("Node.js", "Backend", listOf("node.js", "nodejs", "express", "express.js")),
        SkillTaxonomyEntry("REST API", "Backend", listOf("rest", "rest api", "restful", "openapi", "swagger")),
        SkillTaxonomyEntry("GraphQL", "Backend", listOf("graphql")),
        SkillTaxonomyEntry("Microservices", "Architecture", listOf("microservices", "distributed systems", "event-driven")),

        // Databases
        SkillTaxonomyEntry("PostgreSQL", "Databases", listOf("postgresql", "postgres")),
        SkillTaxonomyEntry("MySQL", "Databases", listOf("mysql")),
        SkillTaxonomyEntry("SQLite", "Databases", listOf("sqlite", "room")),
        SkillTaxonomyEntry("MongoDB", "Databases", listOf("mongodb", "mongo")),
        SkillTaxonomyEntry("Redis", "Databases", listOf("redis")),
        SkillTaxonomyEntry("Firestore", "Databases", listOf("firestore", "firebase")),
        SkillTaxonomyEntry("Elasticsearch", "Databases", listOf("elasticsearch", "opensearch", "vector db", "pinecone", "faiss")),

        // Cloud & DevOps
        SkillTaxonomyEntry("AWS", "Cloud & DevOps", listOf("aws", "amazon web services", "ec2", "s3", "lambda", "sagemaker")),
        SkillTaxonomyEntry("GCP", "Cloud & DevOps", listOf("gcp", "google cloud", "vertex ai", "bigquery")),
        SkillTaxonomyEntry("Azure", "Cloud & DevOps", listOf("azure")),
        SkillTaxonomyEntry("Docker", "Cloud & DevOps", listOf("docker", "containers")),
        SkillTaxonomyEntry("Kubernetes", "Cloud & DevOps", listOf("kubernetes", "k8s", "helm")),
        SkillTaxonomyEntry("CI/CD", "Cloud & DevOps", listOf("ci/cd", "github actions", "jenkins", "gitlab ci")),
        SkillTaxonomyEntry("Git", "Cloud & DevOps", listOf("git", "github", "gitlab")),
        SkillTaxonomyEntry("Linux", "Cloud & DevOps", listOf("linux", "bash", "shell scripting")),
        SkillTaxonomyEntry("Spark", "Data Engineering", listOf("apache spark", "pyspark", "spark")),
        SkillTaxonomyEntry("Kafka", "Data Engineering", listOf("kafka", "apache kafka")),
        SkillTaxonomyEntry("Airflow", "Data Engineering", listOf("airflow", "apache airflow"))
    )

    private val emailRegex = Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
    private val phoneRegex = Regex("(?:\\+?\\d{1,3}[\\s.-]?)?(?:\\(?\\d{3}\\)?[\\s.-]?)\\d{3}[\\s.-]?\\d{4}")
    private val expYearsRegex = Regex("(\\d{1,2}(?:\\.\\d)?)\\s*\\+?\\s*(?:years?|yrs?)\\s*(?:of)?\\s*(?:experience|exp)?", RegexOption.IGNORE_CASE)
    private val yearRangeRegex = Regex("(20\\d{2}|199\\d)\\s*[-–to]+\\s*(20\\d{2}|present|current)", RegexOption.IGNORE_CASE)

    fun preprocessAndLemmatize(text: String): List<String> {
        val cleaned = text.lowercase()
            .replace(Regex("[^a-z0-9+.#/\\-\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleaned.isBlank()) return emptyList()

        return cleaned.split(" ")
            .map { it.trim('.', ',', '-', '/') }
            .filter { it.length >= 2 && it !in stopWords && !it.all { ch -> ch.isDigit() } }
            .map { lemmatizeToken(it) }
    }

    private fun lemmatizeToken(token: String): String {
        return when {
            token.length > 5 && token.endsWith("ies") -> token.dropLast(3) + "y"
            token.length > 5 && token.endsWith("ing") -> {
                val stem = token.dropLast(3)
                if (stem.length >= 3 && stem.last() == stem[stem.length - 2]) stem.dropLast(1) else stem
            }
            token.length > 4 && token.endsWith("ed") -> token.dropLast(2)
            token.length > 5 && token.endsWith("ly") -> token.dropLast(2)
            token.length > 4 && token.endsWith("s") && !token.endsWith("ss") -> token.dropLast(1)
            else -> token
        }
    }

    fun extractEntities(rawText: String, fileName: String = "resume.pdf"): ExtractedResumeEntities {
        val normalizedText = rawText.replace("\r\n", "\n").trim()
        val email = emailRegex.find(normalizedText)?.value.orEmpty()
        val phone = phoneRegex.find(normalizedText)?.value.orEmpty()
        val fullName = extractCandidateName(normalizedText, fileName)
        val skills = extractTechnicalSkills(normalizedText)
        val (educationLabel, educationTier) = extractEducation(normalizedText)
        val experienceYears = extractExperienceYears(normalizedText)
        val workSummary = extractSection(
            normalizedText,
            listOf("experience", "work experience", "employment", "professional experience", "career history"),
            listOf("education", "projects", "skills", "technical skills", "certifications", "publications")
        )
        val projectsSummary = extractSection(
            normalizedText,
            listOf("projects", "key projects", "selected projects", "academic projects"),
            listOf("education", "certifications", "skills", "experience", "awards")
        )
        val certifications = extractCertifications(normalizedText)
        val wordCount = normalizedText.split(Regex("\\s+")).count { it.isNotBlank() }

        return ExtractedResumeEntities(
            fullName = fullName,
            email = email,
            phone = phone,
            skills = skills,
            highestEducation = educationLabel,
            educationTier = educationTier,
            totalExperienceYears = experienceYears,
            workExperienceSummary = workSummary.ifBlank {
                normalizedText.lines().drop(2).take(10).joinToString(" ").take(1200)
            },
            projectsSummary = projectsSummary,
            certifications = certifications,
            wordCount = wordCount
        )
    }

    private fun extractCandidateName(text: String, fileName: String): String {
        val ignoredHeadings = setOf(
            "resume", "curriculum vitae", "cv", "profile", "summary",
            "contact", "objective", "experience", "education", "skills"
        )
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }.take(8)
        for (line in lines) {
            val clean = line.replace(Regex("[^A-Za-z\\s.\\-]"), " ").replace(Regex("\\s+"), " ").trim()
            val lower = clean.lowercase()
            if (clean.length in 4..50 &&
                lower !in ignoredHeadings &&
                !line.contains("@") &&
                !line.any { it.isDigit() }
            ) {
                val words = clean.split(" ").filter { it.isNotBlank() }
                if (words.size in 2..4 && words.all { it.first().isUpperCase() }) {
                    return clean
                }
            }
        }
        return fileName
            .substringBeforeLast(".")
            .replace(Regex("[_\\-]+"), " ")
            .replace(Regex("\\b(resume|cv|final|updated)\\b", RegexOption.IGNORE_CASE), "")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
            .ifBlank { "Candidate Profile" }
    }

    fun extractTechnicalSkills(text: String): List<Pair<String, String>> {
        val lower = " ${text.lowercase()} "
        val found = linkedMapOf<String, String>()
        for (entry in skillTaxonomy) {
            val matched = entry.aliases.any { alias ->
                val escaped = Regex.escape(alias.lowercase())
                val pattern = if (alias.any { !it.isLetterOrDigit() }) {
                    Regex("(?<![a-z0-9])$escaped(?![a-z0-9])")
                } else {
                    Regex("\\b$escaped\\b")
                }
                pattern.containsMatchIn(lower)
            }
            if (matched) {
                found[entry.canonical] = entry.category
            }
        }
        return found.entries.map { it.key to it.value }
    }

    fun normalizeSkillName(skill: String): String {
        val lower = skill.trim().lowercase()
        val match = skillTaxonomy.firstOrNull { entry ->
            entry.canonical.lowercase() == lower || entry.aliases.any { it.lowercase() == lower }
        }
        return match?.canonical ?: skill.trim()
    }

    private fun extractEducation(text: String): Pair<String, Int> {
        val lower = text.lowercase()
        val phdPatterns = listOf("ph.d", "phd", "doctor of philosophy", "doctorate")
        val mastersPatterns = listOf("m.s.", "ms in", "m.sc", "m.tech", "master of science", "master's", "masters", "mba", "m.eng")
        val bachelorsPatterns = listOf("b.s.", "bs in", "b.sc", "b.tech", "b.e.", "bachelor of science", "bachelor of engineering", "bachelor's", "bachelors", "b.a.")
        val associatePatterns = listOf("associate", "diploma", "bootcamp")

        val eduSection = extractSection(
            text,
            listOf("education", "academic background", "qualifications"),
            listOf("experience", "projects", "skills", "certifications")
        )
        val snippet = eduSection.lines().firstOrNull { it.trim().length > 4 }?.trim()?.take(120).orEmpty()

        return when {
            phdPatterns.any { lower.contains(it) } ->
                (snippet.ifBlank { "Ph.D. / Doctorate" }) to 4
            mastersPatterns.any { lower.contains(it) } ->
                (snippet.ifBlank { "Master's Degree" }) to 3
            bachelorsPatterns.any { lower.contains(it) } ->
                (snippet.ifBlank { "Bachelor's Degree" }) to 2
            associatePatterns.any { lower.contains(it) } ->
                (snippet.ifBlank { "Associate / Diploma" }) to 1
            else -> "Not specified" to 0
        }
    }

    private fun extractExperienceYears(text: String): Double {
        val explicitMatches = expYearsRegex.findAll(text)
            .mapNotNull { it.groupValues[1].toDoubleOrNull() }
            .filter { it in 0.5..40.0 }
            .toList()

        val maxExplicit = explicitMatches.maxOrNull() ?: 0.0

        val currentYear = 2026
        val rangeSpans = yearRangeRegex.findAll(text).mapNotNull { m ->
            val start = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val endRaw = m.groupValues[2].lowercase()
            val end = if (endRaw == "present" || endRaw == "current") currentYear else endRaw.toIntOrNull() ?: currentYear
            val diff = (end - start).coerceIn(0, 35)
            diff.toDouble()
        }.toList()

        val combinedSpan = if (rangeSpans.isNotEmpty()) {
            val minStart = yearRangeRegex.findAll(text).mapNotNull { it.groupValues[1].toIntOrNull() }.minOrNull()
            val maxEnd = yearRangeRegex.findAll(text).mapNotNull {
                val raw = it.groupValues[2].lowercase()
                if (raw == "present" || raw == "current") currentYear else raw.toIntOrNull()
            }.maxOrNull()
            if (minStart != null && maxEnd != null && maxEnd >= minStart) {
                (maxEnd - minStart).toDouble().coerceAtMost(35.0)
            } else {
                rangeSpans.sum().coerceAtMost(35.0)
            }
        } else 0.0

        return maxOf(maxExplicit, combinedSpan)
    }

    private fun extractSection(
        text: String,
        startHeaders: List<String>,
        stopHeaders: List<String>
    ): String {
        val lines = text.lines()
        var capturing = false
        val collected = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            val cleanLower = trimmed.lowercase().trimEnd(':').trim()
            if (!capturing) {
                if (startHeaders.any { cleanLower == it || cleanLower.startsWith("$it:") }) {
                    capturing = true
                }
            } else {
                if (trimmed.length <= 45 && stopHeaders.any { cleanLower == it || cleanLower.startsWith("$it:") }) {
                    break
                }
                if (trimmed.isNotBlank()) {
                    collected.add(trimmed)
                }
                if (collected.size >= 18) break
            }
        }
        return collected.joinToString("\n").take(3000)
    }

    private fun extractCertifications(text: String): List<String> {
        val certKeywords = listOf(
            "aws certified", "google cloud", "azure", "tensorflow developer",
            "pmp", "cka", "ckad", "scrum master", "oracle certified", "cisco", "coursera", "nvidia"
        )
        val section = extractSection(
            text,
            listOf("certifications", "certificates", "licenses"),
            listOf("education", "experience", "projects", "skills")
        )
        val fromSection = section.lines()
            .map { it.trim().trimStart('-', '*', '•').trim() }
            .filter { it.length in 4..120 }
            .take(10)

        if (fromSection.isNotEmpty()) return fromSection

        return text.lines()
            .map { it.trim().trimStart('-', '*', '•').trim() }
            .filter { line ->
                line.length in 5..120 && certKeywords.any { kw -> line.lowercase().contains(kw) }
            }
            .distinct()
            .take(8)
    }

    fun evaluateDeterministicMatch(
        job: JobPosting,
        entities: ExtractedResumeEntities,
        rawResumeText: String,
        corpusDocuments: List<String> = emptyList()
    ): DeterministicMatchResult {
        val jobText = "${job.title} ${job.description} ${job.requiredSkills.joinToString(" ")} ${job.qualifications}"
        val jobTokens = preprocessAndLemmatize(jobText)
        val resumeTokens = preprocessAndLemmatize(rawResumeText)
        val extraTokenDocs = corpusDocuments.map { preprocessAndLemmatize(it) }

        val tfidfCosine = computeTfidfCosineSimilarity(jobTokens, resumeTokens, extraTokenDocs) * 100.0

        // Skills matching (case-insensitive + taxonomy alias normalized)
        val candidateCanonicalSkills = entities.skills.map { it.first.lowercase() }.toSet()
        val resumeLower = rawResumeText.lowercase()
        val matched = mutableListOf<String>()
        val missing = mutableListOf<String>()

        for (req in job.requiredSkills) {
            val trimmedReq = req.trim()
            if (trimmedReq.isBlank()) continue
            val normalizedReq = normalizeSkillName(trimmedReq).lowercase()
            val directHit = normalizedReq in candidateCanonicalSkills ||
                trimmedReq.lowercase() in candidateCanonicalSkills ||
                Regex("\\b${Regex.escape(trimmedReq.lowercase())}\\b").containsMatchIn(resumeLower)
            if (directHit) {
                matched.add(trimmedReq)
            } else {
                missing.add(trimmedReq)
            }
        }

        val skillScore = if (job.requiredSkills.isEmpty()) {
            min(100.0, entities.skills.size * 12.0)
        } else {
            (matched.size.toDouble() / job.requiredSkills.size.toDouble()) * 100.0
        }

        // Experience score
        val expScore = when {
            job.minExperienceYears <= 0 -> if (entities.totalExperienceYears > 0) 90.0 else 75.0
            entities.totalExperienceYears >= job.minExperienceYears -> {
                val bonus = min(10.0, (entities.totalExperienceYears - job.minExperienceYears) * 2.5)
                min(100.0, 90.0 + bonus)
            }
            else -> {
                ((entities.totalExperienceYears / job.minExperienceYears.toDouble()) * 85.0)
                    .coerceIn(15.0, 85.0)
            }
        }

        // Education score
        val requiredTier = when {
            job.qualifications.lowercase().contains("ph.d") || job.qualifications.lowercase().contains("phd") -> 4
            job.qualifications.lowercase().contains("master") || job.qualifications.lowercase().contains("m.s") -> 3
            job.qualifications.lowercase().contains("bachelor") || job.qualifications.lowercase().contains("b.s") || job.qualifications.lowercase().contains("b.tech") -> 2
            else -> 1
        }
        val eduScore = when {
            entities.educationTier == 0 -> 50.0
            entities.educationTier >= requiredTier -> 100.0
            entities.educationTier == requiredTier - 1 -> 75.0
            else -> 55.0
        }

        val localSemanticFallback = ((tfidfCosine * 0.55) + (skillScore * 0.45)).coerceIn(0.0, 100.0)

        return DeterministicMatchResult(
            tfidfCosineScore = tfidfCosine.coerceIn(0.0, 100.0),
            skillMatchScore = skillScore.coerceIn(0.0, 100.0),
            experienceScore = expScore.coerceIn(0.0, 100.0),
            educationScore = eduScore.coerceIn(0.0, 100.0),
            matchedSkills = matched,
            missingSkills = missing,
            localSemanticFallbackScore = localSemanticFallback
        )
    }

    fun computeWeightedFinalScore(
        semanticScore: Double,
        skillMatchScore: Double,
        experienceScore: Double,
        educationScore: Double,
        weights: ScoringWeights
    ): Double {
        val w = weights.normalized()
        val raw = (semanticScore * w.semanticWeight) +
            (skillMatchScore * w.skillsWeight) +
            (experienceScore * w.experienceWeight) +
            (educationScore * w.educationWeight)
        return raw.coerceIn(0.0, 100.0)
    }

    fun computeTfidfCosineSimilarity(
        docA: List<String>,
        docB: List<String>,
        backgroundDocs: List<List<String>> = emptyList()
    ): Double {
        if (docA.isEmpty() || docB.isEmpty()) return 0.0
        val allDocs = listOf(docA, docB) + backgroundDocs
        val nDocs = allDocs.size.toDouble()
        val vocab = (docA + docB).toSet()

        val tfA = termFrequencies(docA)
        val tfB = termFrequencies(docB)

        val vecA = DoubleArray(vocab.size)
        val vecB = DoubleArray(vocab.size)

        vocab.forEachIndexed { idx, term ->
            val df = allDocs.count { term in it }.toDouble()
            val idf = ln((nDocs + 1.0) / (df + 1.0)) + 1.0
            vecA[idx] = (tfA[term] ?: 0.0) * idf
            vecB[idx] = (tfB[term] ?: 0.0) * idf
        }

        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in vecA.indices) {
            dot += vecA[i] * vecB[i]
            normA += vecA[i] * vecA[i]
            normB += vecB[i] * vecB[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom <= 0.0) 0.0 else (dot / denom).coerceIn(0.0, 1.0)
    }

    private fun termFrequencies(tokens: List<String>): Map<String, Double> {
        val total = tokens.size.toDouble().coerceAtLeast(1.0)
        val counts = mutableMapOf<String, Int>()
        for (t in tokens) {
            counts[t] = (counts[t] ?: 0) + 1
        }
        return counts.mapValues { (_, c) -> c.toDouble() / total }
    }
}
