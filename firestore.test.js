const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_recruiter_123";
const BOB_UID = "bob_recruiter_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).collection("jobs").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).collection("jobs").doc("job_1").set({
      userId: ALICE_UID,
      title: "ML Engineer",
      department: "AI",
      description: "Build NLP pipelines and transformer models.",
      requiredSkills: ["Python", "NLP"],
      qualifications: "B.S. in CS",
      minExperienceYears: 3,
      status: "OPEN",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Recruiter profile create, partial update, and cross-user isolation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Chen",
      email: "alice@talentlens.ai",
      organization: "TalentLens Labs",
      semanticWeight: 0.4,
      skillsWeight: 0.3,
      experienceWeight: 0.2,
      educationWeight: 0.1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      semanticWeight: 0.45,
      skillsWeight: 0.25,
      updatedAt: pastDate(1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Reject ghost field
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Bob cannot read or write Alice's profile
  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bob.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: pastDate(500),
    })
  );
});

test("Jobs CRUD, validation bounds, and @DocumentId rejection", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("jobs").doc("job_nlp_1").set({
      userId: ALICE_UID,
      title: "Senior NLP Engineer",
      department: "Machine Learning",
      description: "Design resume parsing, TF-IDF, and transformer matching pipelines.",
      requiredSkills: ["Python", "spaCy", "NLP", "PyTorch"],
      qualifications: "M.S. or B.S. in Computer Science",
      minExperienceYears: 4,
      status: "OPEN",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("jobs").doc("job_nlp_1").update({
      status: "PAUSED",
      updatedAt: pastDate(1000),
    })
  );

  // Reject @DocumentId "id" key in payload
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("jobs").doc("job_nlp_2").set({
      id: "job_nlp_2",
      userId: ALICE_UID,
      title: "Backend Engineer",
      department: "Platform",
      description: "Build scalable REST APIs and cloud data services.",
      requiredSkills: ["Kotlin", "SQL"],
      qualifications: "B.S. in CS",
      minExperienceYears: 2,
      status: "OPEN",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List query succeeds for owner, fails for Bob
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("jobs").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("jobs").get());
});

test("Candidates, Resumes, ExtractedSkills, ScreeningResults, and ShortlistedCandidates lifecycle", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("candidates").doc("cand_1").set({
      userId: ALICE_UID,
      fullName: "Priya Nair",
      email: "priya.nair@example.com",
      phone: "+1-415-555-0192",
      totalExperienceYears: 5.5,
      highestEducation: "M.S. Computer Science",
      workExperienceSummary: "5.5 years building NLP and search ranking models.",
      projectsSummary: "Built neural document ranker with 94% precision.",
      certifications: ["AWS Machine Learning Specialty"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("resumes").doc("res_1").set({
      userId: ALICE_UID,
      candidateId: "cand_1",
      fileName: "priya_nair_resume.pdf",
      fileType: "PDF",
      fileSizeBytes: 245000,
      extractedText: "Priya Nair\nM.S. Computer Science\nPython, spaCy, NLP, PyTorch...",
      wordCount: 420,
      parseStatus: "PARSED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("extracted_skills").doc("skill_1").set({
      userId: ALICE_UID,
      candidateId: "cand_1",
      resumeId: "res_1",
      skillName: "NLP",
      category: "AI & ML",
      confidenceScore: 0.96,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("screening_results").doc("scr_1").set({
      userId: ALICE_UID,
      jobId: "job_nlp_1",
      candidateId: "cand_1",
      resumeId: "res_1",
      candidateName: "Priya Nair",
      jobTitle: "Senior NLP Engineer",
      finalScore: 91.5,
      semanticScore: 93.0,
      tfidfCosineScore: 84.2,
      skillMatchScore: 95.0,
      experienceScore: 90.0,
      educationScore: 100.0,
      matchedSkills: ["Python", "spaCy", "NLP", "PyTorch"],
      missingSkills: [],
      aiExplanation: "Strong overlap across transformer NLP, spaCy pipelines, and production ML.",
      reviewStatus: "PENDING_REVIEW",
      reviewerNotes: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject out-of-range score > 100
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("screening_results").doc("scr_bad").set({
      userId: ALICE_UID,
      jobId: "job_nlp_1",
      candidateId: "cand_1",
      resumeId: "res_1",
      candidateName: "Priya Nair",
      jobTitle: "Senior NLP Engineer",
      finalScore: 125.0,
      semanticScore: 90.0,
      tfidfCosineScore: 80.0,
      skillMatchScore: 90.0,
      experienceScore: 90.0,
      educationScore: 100.0,
      matchedSkills: ["Python"],
      missingSkills: [],
      aiExplanation: "Invalid score",
      reviewStatus: "PENDING_REVIEW",
      reviewerNotes: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Manual shortlist update succeeds
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("screening_results").doc("scr_1").update({
      reviewStatus: "SHORTLISTED",
      reviewerNotes: "Strong portfolio; advance to technical interview.",
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("shortlisted_candidates").doc("short_1").set({
      userId: ALICE_UID,
      jobId: "job_nlp_1",
      candidateId: "cand_1",
      screeningResultId: "scr_1",
      candidateName: "Priya Nair",
      jobTitle: "Senior NLP Engineer",
      finalScore: 91.5,
      priorityNote: "Top rank for Senior NLP Engineer",
      status: "SHORTLISTED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user isolation on screening_results and shortlisted_candidates
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("screening_results").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("shortlisted_candidates").get());
});
