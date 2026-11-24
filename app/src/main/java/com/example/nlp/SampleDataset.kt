package com.example.nlp

import com.example.data.model.JobPosting
import com.example.data.model.JobStatus

data class SampleResumeSpec(
    val fileName: String,
    val content: String
)

object SampleDataset {
    val sampleJobs: List<JobPosting> = listOf(
        JobPosting(
            title = "Senior NLP & ML Engineer",
            department = "Applied AI",
            description = "Build production resume parsing, named entity recognition (NER), TF-IDF indexing, and Sentence Transformer semantic ranking systems. Deploy scalable Flask/FastAPI microservices on AWS with Docker and Kubernetes, backed by PostgreSQL.",
            requiredSkills = listOf(
                "Python", "NLP", "spaCy", "Scikit-learn", "Transformers",
                "PyTorch", "TF-IDF", "Docker", "PostgreSQL", "REST API"
            ),
            qualifications = "M.S. or B.S. in Computer Science, Machine Learning, or related field",
            minExperienceYears = 4,
            status = JobStatus.OPEN.name
        ),
        JobPosting(
            title = "Full-Stack Kotlin & Cloud Engineer",
            department = "Platform Engineering",
            description = "Architect modern Android Jetpack Compose client applications and cloud-native REST APIs. Collaborate across Firestore, SQL, CI/CD pipelines, and distributed backend systems.",
            requiredSkills = listOf(
                "Kotlin", "Jetpack Compose", "Android SDK", "REST API",
                "Firestore", "SQL", "Git", "CI/CD"
            ),
            qualifications = "B.S. in Software Engineering or Computer Science",
            minExperienceYears = 3,
            status = JobStatus.OPEN.name
        )
    )

    val sampleResumes: List<SampleResumeSpec> = listOf(
        SampleResumeSpec(
            fileName = "Priya_Nair_NLP_Engineer.pdf",
            content = """
                Priya Nair
                priya.nair@talentlens-sample.org | +1-415-555-0192

                Summary
                Senior Machine Learning Engineer with 6+ years of experience designing production NLP, Named Entity Recognition (NER), and semantic search pipelines.

                Technical Skills
                Python, NLP, spaCy, NLTK, Scikit-learn, PyTorch, Transformers, Sentence-Transformers, TF-IDF, Docker, Kubernetes, PostgreSQL, Flask, FastAPI, REST API, AWS, Git

                Work Experience
                Lead NLP Engineer | Apex Search Labs (2022 - Present)
                - Engineered hybrid TF-IDF and Sentence Transformer candidate ranking services in Python and PyTorch, reducing screening latency by 62%.
                - Built custom spaCy NER pipelines for document extraction and deployed containerized REST APIs using Docker and PostgreSQL on AWS.
                ML Engineer | Horizon Analytics (2020 - 2022)
                - Trained Scikit-learn text classification and cosine similarity models over 2M+ documents.

                Projects
                Explainable Resume Ranker: Open-source Flask + spaCy + HuggingFace pipeline with skill gap visualization.

                Education
                M.S. in Computer Science (AI Specialization) - Stanford University (2018 - 2020)

                Certifications
                AWS Certified Machine Learning - Specialty
                DeepLearning.AI Natural Language Processing Specialization
            """.trimIndent()
        ),
        SampleResumeSpec(
            fileName = "Marcus_Vance_ML_Backend.docx",
            content = """
                Marcus Vance
                marcus.vance@talentlens-sample.org | +1-312-555-0148

                Summary
                Backend and Machine Learning Engineer with 4.5 years of experience in Python, NLP text processing, and cloud microservices.

                Technical Skills
                Python, NLP, spaCy, Scikit-learn, TF-IDF, Docker, PostgreSQL, FastAPI, REST API, Linux, Git, Redis, AWS

                Work Experience
                Senior ML Platform Engineer | Nimbus Cloud (2023 - Present)
                - Developed FastAPI microservices and PostgreSQL data stores for real-time text scoring with TF-IDF and Scikit-learn.
                - Containerized inference workers with Docker and automated CI/CD pipelines.
                Software Engineer | ByteGrid (2021 - 2023)
                - Built spaCy entity extraction workflows for legal and HR documents.

                Projects
                Document Similarity Engine: Implemented lemmatization, stop-word filtering, and cosine similarity scoring in Python.

                Education
                B.S. in Computer Science - University of Michigan (2017 - 2021)

                Certifications
                AWS Certified Solutions Architect - Associate
            """.trimIndent()
        ),
        SampleResumeSpec(
            fileName = "Elena_Rostova_Data_Scientist.pdf",
            content = """
                Elena Rostova
                elena.rostova@talentlens-sample.org | +1-646-555-0175

                Summary
                Applied Data Scientist with 3 years of experience in statistical modeling, Python data pipelines, and NLP prototyping.

                Technical Skills
                Python, Pandas, NumPy, Scikit-learn, NLTK, Matplotlib, SQL, MySQL, TensorFlow, Git

                Work Experience
                Data Scientist | MetricFlow Analytics (2023 - Present)
                - Built customer churn prediction and topic modeling workflows using NLTK and Scikit-learn.
                - Queried MySQL warehouses and created executive dashboards in Matplotlib and Plotly.

                Projects
                Job Market Skill Clustering: K-Means clustering on 50,000 job descriptions using NLTK and TF-IDF.

                Education
                M.S. in Data Science - Columbia University (2021 - 2023)

                Certifications
                TensorFlow Developer Certificate
            """.trimIndent()
        ),
        SampleResumeSpec(
            fileName = "David_Okafor_Mobile_Cloud.pdf",
            content = """
                David Okafor
                david.okafor@talentlens-sample.org | +1-206-555-0133

                Summary
                Android and Cloud Platform Engineer with 5 years of experience building Jetpack Compose apps, Kotlin coroutines architectures, and Firebase/REST backends.

                Technical Skills
                Kotlin, Jetpack Compose, Android SDK, Java, REST API, Firestore, SQLite, SQL, Git, CI/CD, Docker, GCP

                Work Experience
                Senior Android Engineer | PulseHealth Systems (2022 - Present)
                - Led migration to Jetpack Compose with Material 3 adaptive layouts, Kotlin Flow, and Firestore real-time sync.
                - Integrated REST APIs and automated GitHub Actions CI/CD pipelines.
                Mobile Developer | FinTrack (2020 - 2022)
                - Built offline-first Kotlin Android client with encrypted SQLite caching.

                Projects
                Smart Document Scanner: Android Compose app using PDF parsing and cloud synchronization.

                Education
                B.S. in Software Engineering - University of Washington (2016 - 2020)

                Certifications
                Google Associate Android Developer
            """.trimIndent()
        ),
        SampleResumeSpec(
            fileName = "Sophia_Lindqvist_FullStack_Junior.docx",
            content = """
                Sophia Lindqvist
                sophia.lindqvist@talentlens-sample.org | +1-512-555-0181

                Summary
                Full-Stack Developer with 2 years of experience in React, JavaScript, HTML5, CSS3, and Flask REST APIs.

                Technical Skills
                React, JavaScript, TypeScript, HTML5, CSS3, Chart.js, Flask, Python, SQLite, Git, REST API

                Work Experience
                Full-Stack Developer | BrightPath Web (2024 - Present)
                - Built interactive React and Chart.js analytics dashboards connected to Python Flask REST APIs.
                - Designed responsive CSS3 and Tailwind interfaces with authentication workflows.

                Projects
                Recruitment Analytics UI: React dashboard displaying candidate charts and CSV exports.

                Education
                B.S. in Computer Science - University of Texas at Austin (2020 - 2024)

                Certifications
                Meta Front-End Developer Professional Certificate
            """.trimIndent()
        )
    )
}
