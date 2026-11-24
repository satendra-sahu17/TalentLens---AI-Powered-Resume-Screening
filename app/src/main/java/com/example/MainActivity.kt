package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.RecruitmentRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.signOutRecruiter
import com.example.data.model.ReviewStatus
import com.example.data.model.ScreeningResult
import com.example.data.model.UiState
import com.example.nlp.SemanticAiService
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CandidateDetailSheet
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.JobsScreen
import com.example.ui.screens.ScreeningScreen
import com.example.ui.screens.ShortlistSettingsScreen
import com.example.ui.theme.TalentLensTheme
import com.example.ui.viewmodel.RecruitmentViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_JOBS = "jobs"
const val ROUTE_SCREENING = "screening"
const val ROUTE_COMPARE = "compare"
const val ROUTE_SHORTLIST = "shortlist"

data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navDestinations = listOf(
    NavDestinationItem(ROUTE_DASHBOARD, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    NavDestinationItem(ROUTE_JOBS, "Jobs", Icons.Filled.Work, Icons.Outlined.WorkOutline),
    NavDestinationItem(ROUTE_SCREENING, "Screen", Icons.Filled.Description, Icons.Outlined.Description),
    NavDestinationItem(ROUTE_COMPARE, "Compare", Icons.AutoMirrored.Filled.CompareArrows, Icons.AutoMirrored.Outlined.CompareArrows),
    NavDestinationItem(ROUTE_SHORTLIST, "Shortlist", Icons.Filled.BookmarkAdded, Icons.Outlined.BookmarkBorder)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModel, or Firebase call
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable { mutableStateOf(systemDark) }

            TalentLensTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = it }
                )
            }
        }
    }
}

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: RecruitmentViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        RecruitmentViewModel(
                            repository = RecruitmentRepository(db, auth),
                            semanticAiService = SemanticAiService(),
                            currentUserId = user.uid,
                            recruiterDisplayName = user.displayName ?: "Recruiter",
                            recruiterEmail = user.email ?: ""
                        )
                    }
                }
            )
            MainAppShell(
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(
    viewModel: RecruitmentViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }

    val recruiterState by viewModel.recruiterState.collectAsStateWithLifecycle()
    val jobsState by viewModel.jobsState.collectAsStateWithLifecycle()
    val candidatesState by viewModel.candidatesState.collectAsStateWithLifecycle()
    val resumesState by viewModel.resumesState.collectAsStateWithLifecycle()
    val screeningsState by viewModel.screeningsState.collectAsStateWithLifecycle()
    val shortlistState by viewModel.shortlistState.collectAsStateWithLifecycle()
    val selectedJobId by viewModel.selectedJobId.collectAsStateWithLifecycle()
    val comparisonIds by viewModel.comparisonIds.collectAsStateWithLifecycle()
    val analysisProgress by viewModel.analysisProgress.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var inspectedResult by remember { mutableStateOf<ScreeningResult?>(null) }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "TalentLens AI",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { onToggleTheme(!isDarkTheme) },
                            modifier = Modifier.testTag("topbar_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Toggle light/dark mode"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(
                            recruiterState = recruiterState,
                            jobsState = jobsState,
                            resumesState = resumesState,
                            screeningsState = screeningsState,
                            shortlistState = shortlistState,
                            analysisProgress = analysisProgress,
                            onNavigateToScreen = { navigateTo(ROUTE_SCREENING) },
                            onNavigateToJobs = { navigateTo(ROUTE_JOBS) },
                            onLoadSampleBatch = { viewModel.loadSampleJobsAndResumes() },
                            onInspectCandidate = { inspectedResult = it }
                        )
                    }
                    composable(ROUTE_JOBS) {
                        JobsScreen(
                            jobsState = jobsState,
                            screeningsState = screeningsState,
                            onSaveJob = { id, title, dept, desc, skills, quals, exp, status ->
                                viewModel.saveJob(id, title, dept, desc, skills, quals, exp, status)
                            },
                            onDeleteJob = { viewModel.deleteJob(it) },
                            onScreenForJob = { job ->
                                viewModel.selectJobForScreening(job.id)
                                navigateTo(ROUTE_SCREENING)
                            }
                        )
                    }
                    composable(ROUTE_SCREENING) {
                        ScreeningScreen(
                            jobsState = jobsState,
                            candidatesState = candidatesState,
                            screeningsState = screeningsState,
                            selectedJobId = selectedJobId,
                            comparisonIds = comparisonIds,
                            analysisProgress = analysisProgress,
                            onSelectJob = { viewModel.selectJobForScreening(it) },
                            onUploadResumes = { job, uris ->
                                viewModel.processUploadedResumes(context, job, uris)
                            },
                            onLoadSampleBatch = { viewModel.loadSampleJobsAndResumes() },
                            onInspectCandidate = { inspectedResult = it },
                            onQuickShortlist = { res ->
                                val nextStatus = if (res.reviewStatusEnum() == ReviewStatus.SHORTLISTED) {
                                    ReviewStatus.PENDING_REVIEW
                                } else {
                                    ReviewStatus.SHORTLISTED
                                }
                                viewModel.updateReviewStatus(res, nextStatus, res.reviewerNotes)
                            },
                            onToggleCompare = { viewModel.toggleComparisonCandidate(it) },
                            onDeleteScreening = { viewModel.deleteScreeningResult(it) },
                            onClearError = { viewModel.clearAnalysisError() },
                            onExportMessage = { viewModel.showMessage(it) }
                        )
                    }
                    composable(ROUTE_COMPARE) {
                        CompareScreen(
                            candidatesState = candidatesState,
                            screeningsState = screeningsState,
                            comparisonIds = comparisonIds,
                            onToggleCompare = { viewModel.toggleComparisonCandidate(it) },
                            onInspectCandidate = { inspectedResult = it }
                        )
                    }
                    composable(ROUTE_SHORTLIST) {
                        ShortlistSettingsScreen(
                            recruiterState = recruiterState,
                            shortlistState = shortlistState,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            onSaveWeights = { viewModel.updateWeightsAndRecalculate(it) },
                            onUpdateShortlistStatus = { id, status, note ->
                                viewModel.updateShortlistItem(id, status, note)
                            },
                            onSignOut = {
                                signOutRecruiter(credentialManager, scope)
                            }
                        )
                    }
                }
            }
        }
    }

    val currentInspected = inspectedResult
    if (currentInspected != null) {
        val candidates = (candidatesState as? UiState.Success)?.data.orEmpty()
        val resumes = (resumesState as? UiState.Success)?.data.orEmpty()
        val candidate = candidates.firstOrNull { it.id == currentInspected.candidateId }
        val resume = resumes.firstOrNull { it.id == currentInspected.resumeId }

        CandidateDetailSheet(
            result = currentInspected,
            candidate = candidate,
            resume = resume,
            onDismiss = { inspectedResult = null },
            onUpdateReview = { status, notes ->
                viewModel.updateReviewStatus(currentInspected, status, notes)
            },
            onReportMessage = { viewModel.showMessage(it) }
        )
    }
}
