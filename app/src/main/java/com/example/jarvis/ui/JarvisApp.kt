package com.example.jarvis.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.jarvis.auth.AuthState
import com.example.jarvis.model.JarvisState
import com.example.jarvis.ui.components.BottomNav
import com.example.jarvis.ui.components.ChatGPTDrawer
import com.example.jarvis.ui.components.NavTab
import com.example.jarvis.ui.components.SafetyConfirmationDialog
import com.example.jarvis.ui.components.TopBar
import com.example.jarvis.ui.screens.AboutScreen
import com.example.jarvis.ui.screens.AccountProfileScreen
import com.example.jarvis.ui.screens.ActivityScreen
import com.example.jarvis.ui.screens.AndroidBridgeScreen
import com.example.jarvis.ui.screens.ChatScreen
import com.example.jarvis.ui.screens.DevicesScreen
import com.example.jarvis.ui.screens.ForgotPasswordScreen
import com.example.jarvis.ui.screens.HomeScreen
import com.example.jarvis.ui.screens.LoginScreen
import com.example.jarvis.ui.screens.MemoryScreen
import com.example.jarvis.ui.screens.PrivacyScreen
import com.example.jarvis.ui.screens.SettingsScreen
import com.example.jarvis.ui.screens.SignUpScreen
import com.example.jarvis.ui.screens.SkillsScreen
import com.example.jarvis.ui.screens.TasksScreen
import com.example.jarvis.ui.screens.ToolsScreen
import com.example.jarvis.ui.screens.VisionScreen
import com.example.jarvis.ui.screens.VoiceSelectionScreen
import com.example.jarvis.ui.screens.VoiceSetupScreen
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.launch

private enum class AuthNavState {
    LOGIN,
    SIGN_UP,
    FORGOT_PASSWORD
}

@Composable
fun JarvisApp(
    viewModel: JarvisViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    var authNavState by remember { mutableStateOf(AuthNavState.LOGIN) }

    val jarvisState by viewModel.jarvisState.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val activeSubScreen by viewModel.activeSubScreen.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val timers by viewModel.timers.collectAsState()
    val logs by viewModel.activityLogs.collectAsState()
    val scans by viewModel.visionScans.collectAsState()
    val activeVisionResult by viewModel.activeVisionResult.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val lastResponse by viewModel.lastResponse.collectAsState()
    val speechSupported by viewModel.speechSupported.collectAsState()
    val safetyRequest by viewModel.safetyRequest.collectAsState()
    val isContinuousConversationActive by viewModel.isContinuousConversationActive.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isSpeakerEnabled by viewModel.isSpeakerEnabled.collectAsState()
    val expenses by viewModel.expenses.collectAsState(initial = emptyList())
    val habits by viewModel.habits.collectAsState(initial = emptyList())
    val deviceTilt by viewModel.deviceTilt.collectAsState()
    val voiceRmsDb by viewModel.voiceRmsDb.collectAsState()

    // ChatGPT Theme & Accent states
    var isDarkTheme by remember { mutableStateOf(false) }
    var selectedAccentColor by remember { mutableStateOf(Color(0xFF2563EB)) } // Default ChatGPT Blue

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Request audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        }
    }

    when (val state = authState) {
        is AuthState.Initializing -> {
            // Futuristic Boot Splash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) Color(0xFF171717) else Color(0xFFFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(selectedAccentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "IM",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "JARVIS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                        color = JarvisCyanBright
                    )
                    Text(
                        text = "Connecting to Neural Core...",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        is AuthState.Unauthenticated, is AuthState.AuthError -> {
            when (authNavState) {
                AuthNavState.LOGIN -> {
                    LoginScreen(
                        authManager = viewModel.authManager,
                        onNavigateToSignUp = { authNavState = AuthNavState.SIGN_UP },
                        onNavigateToForgotPassword = { authNavState = AuthNavState.FORGOT_PASSWORD },
                        onLoginSuccess = {}
                    )
                }
                AuthNavState.SIGN_UP -> {
                    SignUpScreen(
                        authManager = viewModel.authManager,
                        onNavigateBackToLogin = { authNavState = AuthNavState.LOGIN },
                        onSignUpSuccess = {}
                    )
                }
                AuthNavState.FORGOT_PASSWORD -> {
                    ForgotPasswordScreen(
                        authManager = viewModel.authManager,
                        onNavigateBack = { authNavState = AuthNavState.LOGIN }
                    )
                }
            }
        }

        is AuthState.Authenticated -> {
            // Handle back button if drawer is open or in a SubScreen
            BackHandler(enabled = drawerState.isOpen || activeSubScreen != null) {
                if (drawerState.isOpen) {
                    coroutineScope.launch { drawerState.close() }
                } else if (activeSubScreen != null) {
                    viewModel.closeSubScreen()
                }
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = activeSubScreen == null,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = Color(0xFF070F1E),
                        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    ) {
                        ChatGPTDrawer(
                            isDarkTheme = isDarkTheme,
                            accentColor = selectedAccentColor,
                            onImagesClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.openSubScreen(SubScreen.VISION)
                            },
                            onLibraryClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.openSubScreen(SubScreen.MEMORY)
                            },
                            onProjectsClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.openSubScreen(SubScreen.CODE_STUDIO)
                            },
                            onScheduledClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.openSubScreen(SubScreen.TIMER_STOPWATCH)
                            },
                            onPluginsClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.openSubScreen(SubScreen.PLUGINS)
                            },
                            onSearchClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.openSubScreen(SubScreen.SEARCH)
                            },
                            onSelectConversation = { title ->
                                coroutineScope.launch { drawerState.close() }
                                viewModel.setTab(NavTab.CONVERSATION)
                                viewModel.sendUserMessage("Let's talk about: $title")
                            },
                            onNewChatClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.repository.clearMessages()
                                viewModel.setTab(NavTab.CONVERSATION)
                            },
                            onProfileClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.setTab(NavTab.SETTINGS)
                            },
                            onVoiceModeClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.setTab(NavTab.HOME)
                            }
                        )
                    }
                }
            ) {
                val appBg = if (isDarkTheme) Color(0xFF171717) else Color(0xFFF9F9FB)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = JarvisBackground,
                    bottomBar = {
                        if (activeSubScreen == null) {
                            BottomNav(
                                currentTab = currentTab,
                                onTabSelected = { viewModel.setTab(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        val currentSubScreen = activeSubScreen
                        if (currentSubScreen != null) {
                            // SubScreen Container with Header
                            Column(modifier = Modifier.fillMaxSize()) {
                                SubScreenHeader(
                                    title = currentSubScreen.name,
                                    isDarkTheme = true,
                                    accentColor = JarvisCyan,
                                    onBack = { viewModel.closeSubScreen() }
                                )

                                when (currentSubScreen) {
                                    SubScreen.ACCOUNT -> AccountProfileScreen(
                                        user = state.user,
                                        authManager = viewModel.authManager,
                                        onSignOut = {
                                            viewModel.authManager.signOut()
                                            viewModel.closeSubScreen()
                                        }
                                    )
                                    SubScreen.TOOLS -> ToolsScreen(
                                        tools = viewModel.brain.registry.getAllTools(),
                                        toolContext = viewModel.toolContext,
                                        onOpenSubScreen = { viewModel.openSubScreen(it) },
                                        onOpenTab = { viewModel.setTab(it) }
                                    )
                                    SubScreen.FILES -> com.example.jarvis.ui.screens.FilesScreen(
                                        onGenerateFilePrompt = { prompt ->
                                            viewModel.sendUserMessage(prompt)
                                            viewModel.closeSubScreen()
                                            viewModel.setTab(NavTab.CONVERSATION)
                                        },
                                        onCopyToClipboard = { text -> viewModel.copyToClipboard(text) }
                                    )
                                    SubScreen.AUTOMATION -> com.example.jarvis.ui.screens.AutomationScreen(
                                        orchestrator = viewModel.brain.automationOrchestrator
                                    )
                                    SubScreen.TASKS -> TasksScreen(
                                        tasks = tasks,
                                        timers = timers,
                                        onToggleTask = { viewModel.repository.toggleTask(it) },
                                        onDeleteTask = { viewModel.repository.deleteTask(it) },
                                        onAddTask = { title, notes, priority ->
                                            viewModel.repository.addTask(title, notes, priority)
                                        },
                                        onAddTimer = { label, seconds ->
                                            viewModel.repository.addTimer(label, seconds)
                                        },
                                        onDeleteTimer = { viewModel.repository.deleteTimer(it) },
                                        onUpdateTimer = { id, remaining, running ->
                                            viewModel.repository.updateTimer(id, remaining, running)
                                        }
                                    )
                                    SubScreen.MEMORY -> MemoryScreen(
                                        memories = memories,
                                        onAddMemory = { title, content, cat ->
                                            viewModel.repository.addMemory(title, content, cat)
                                        },
                                        onDeleteMemory = { viewModel.repository.deleteMemory(it) },
                                        onClearAllMemories = { viewModel.repository.clearAllMemories() }
                                    )
                                    SubScreen.ACTIVITY -> ActivityScreen(
                                        logs = logs,
                                        onClearLogs = { viewModel.repository.clearActivityLogs() }
                                    )
                                    SubScreen.VISION -> VisionScreen(
                                        scans = scans,
                                        aiProvider = viewModel.aiProvider,
                                        onAddScan = { viewModel.repository.addVisionScan(it) },
                                        visionEngine = viewModel.visionEngine,
                                        activeResult = activeVisionResult,
                                        onSetActiveResult = { res, uri -> viewModel.setActiveVisionContext(res, uri) },
                                        onExecuteAction = { action -> viewModel.executeVisionDerivedAction(action) },
                                        onAskJarvis = { prompt -> viewModel.askJarvisAboutVision(prompt) },
                                        onCopyToClipboard = { text -> viewModel.copyToClipboard(text) }
                                    )
                                    SubScreen.PRIVACY -> PrivacyScreen(
                                        auditor = viewModel.privacyAuditor,
                                        onOpenSettings = { viewModel.openAndroidSettings(it) }
                                    )
                                    SubScreen.BRIDGE -> AndroidBridgeScreen(
                                        telemetry = telemetry,
                                        onRefreshTelemetry = { viewModel.bridge.refreshTelemetry() },
                                        onToggleFlashlight = { viewModel.toggleFlashlight(it) },
                                        onOpenSystemSettings = { viewModel.openAndroidSettings() }
                                    )
                                    SubScreen.VOICE_SETUP -> VoiceSetupScreen(
                                        currentSettings = settings,
                                        onUpdateSettings = { viewModel.repository.updateSettings(it) },
                                        onTestWakeTrigger = { viewModel.triggerWakeSession() }
                                    )
                                    SubScreen.VOICE_SELECTION -> VoiceSelectionScreen(
                                        currentSettings = settings,
                                        onUpdateSettings = { viewModel.repository.updateSettings(it) },
                                        onTestSpeak = { text, rate, pitch ->
                                            viewModel.speakText(text, rate, pitch)
                                        }
                                    )
                                    SubScreen.ABOUT -> AboutScreen()
                                    SubScreen.DIAGNOSTICS -> com.example.jarvis.ui.screens.DiagnosticsScreen(
                                        repository = viewModel.repository,
                                        bridge = viewModel.bridge
                                    )
                                    SubScreen.NOTIFICATIONS -> com.example.jarvis.ui.screens.NotificationIntelligenceScreen(
                                        repository = viewModel.repository,
                                        onOpenNotificationSettings = {
                                            viewModel.bridge.getApplicationContext().startActivity(
                                                com.example.jarvis.notification.JarvisNotificationListenerService.getNotificationSettingsIntent()
                                            )
                                        }
                                    )
                                    SubScreen.SEARCH -> com.example.jarvis.ui.screens.UniversalSearchScreen(
                                        repository = viewModel.repository,
                                        bridge = viewModel.bridge,
                                        onOpenSubScreen = { subScreen ->
                                            viewModel.openSubScreen(subScreen)
                                        }
                                    )
                                    SubScreen.PLUGINS -> com.example.jarvis.ui.screens.PluginsScreen(
                                        pluginManager = viewModel.pluginManager,
                                        onBack = { viewModel.closeSubScreen() }
                                    )
                                    SubScreen.ARMOR_THEMES -> com.example.jarvis.ui.screens.ArmorThemeScreen(
                                        onBack = { viewModel.closeSubScreen() }
                                    )
                                    SubScreen.EXPENSES -> com.example.jarvis.ui.screens.ExpenseTrackerScreen(
                                        repository = viewModel.repository,
                                        onBack = { viewModel.closeSubScreen() }
                                    )
                                    SubScreen.HABITS -> com.example.jarvis.ui.screens.HabitTrackerScreen(
                                        repository = viewModel.repository,
                                        onBack = { viewModel.closeSubScreen() }
                                    )
                                    SubScreen.CODE_STUDIO -> com.example.jarvis.ui.screens.CodeStudioScreen(
                                        onBack = { viewModel.closeSubScreen() }
                                    )
                                    SubScreen.VOICE_NOTES -> com.example.jarvis.ui.screens.VoiceNotesScreen(
                                        onBack = { viewModel.closeSubScreen() }
                                    )
                                    SubScreen.TIMER_STOPWATCH -> com.example.jarvis.ui.screens.TimerStopwatchScreen(
                                        onBack = { viewModel.closeSubScreen() }
                                    )
                                    SubScreen.BACKUP_EXPORT -> com.example.jarvis.ui.screens.DataBackupScreen(
                                        repository = viewModel.repository,
                                        onBack = { viewModel.closeSubScreen() }
                                    )
                                }
                            }
                        } else {
                            // Main Screen Router
                            when (currentTab) {
                                NavTab.HOME -> {
                                    HomeScreen(
                                        jarvisState = jarvisState,
                                        telemetry = telemetry,
                                        isListening = isListening,
                                        isSpeaking = isSpeaking,
                                        liveTranscript = liveTranscript,
                                        lastResponse = lastResponse,
                                        messages = messages,
                                        tasks = tasks,
                                        memories = memories,
                                        logs = logs,
                                        settings = settings,
                                        expenses = expenses,
                                        habits = habits,
                                        tiltX = deviceTilt.first,
                                        tiltY = deviceTilt.second,
                                        rmsDb = voiceRmsDb,
                                        onVoiceClick = {
                                            if (isListening) {
                                                viewModel.stopListening()
                                            } else {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        onChatClick = { viewModel.setTab(NavTab.CONVERSATION) },
                                        onToolsClick = { viewModel.setTab(NavTab.SKILLS) },
                                        onMemoryClick = { viewModel.openSubScreen(SubScreen.MEMORY) },
                                        onActivityClick = { viewModel.openSubScreen(SubScreen.ACTIVITY) },
                                        onVisionClick = { viewModel.openSubScreen(SubScreen.VISION) },
                                        onPrivacyClick = { viewModel.openSubScreen(SubScreen.PRIVACY) },
                                        onBridgeClick = { viewModel.setTab(NavTab.DEVICES) },
                                        onSearchClick = { viewModel.openSubScreen(SubScreen.SEARCH) },
                                        onTasksClick = { viewModel.openSubScreen(SubScreen.TASKS) },
                                        onSettingsClick = { viewModel.setTab(NavTab.SETTINGS) },
                                        onArmorClick = { viewModel.openSubScreen(SubScreen.ARMOR_THEMES) },
                                        onExpenseClick = { viewModel.openSubScreen(SubScreen.EXPENSES) },
                                        onHabitClick = { viewModel.openSubScreen(SubScreen.HABITS) },
                                        onCodeStudioClick = { viewModel.openSubScreen(SubScreen.CODE_STUDIO) },
                                        onTimerClick = { viewModel.openSubScreen(SubScreen.TIMER_STOPWATCH) },
                                        onVoiceNotesClick = { viewModel.openSubScreen(SubScreen.VOICE_NOTES) },
                                        onBackupClick = { viewModel.openSubScreen(SubScreen.BACKUP_EXPORT) },
                                        onDiagnosticsClick = { viewModel.openSubScreen(SubScreen.DIAGNOSTICS) },
                                        onQuickCommand = { viewModel.sendUserMessage(it) }
                                    )
                                }

                                NavTab.CONVERSATION -> {
                                    val isOffline = settings.customApiKey.isBlank() &&
                                            (com.example.BuildConfig.GEMINI_API_KEY.isBlank() || com.example.BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY")
                                    ChatScreen(
                                        messages = messages,
                                        jarvisState = jarvisState,
                                        isDarkTheme = true,
                                        accentColor = JarvisCyan,
                                        onSendMessage = { viewModel.sendUserMessage(it) },
                                        onCopyMessage = { viewModel.copyToClipboard(it) },
                                        onSpeakMessage = { viewModel.speakText(it) },
                                        onRetryMessage = { viewModel.retryLastMessage() },
                                        onClearChat = { viewModel.repository.clearMessages() },
                                        onOpenDrawer = {
                                            coroutineScope.launch { drawerState.open() }
                                        },
                                        onVoiceClick = {
                                            if (isListening) {
                                                viewModel.stopListening()
                                            } else {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        onVisionClick = { viewModel.openSubScreen(SubScreen.VISION) },
                                        onCodeStudioClick = { viewModel.openSubScreen(SubScreen.CODE_STUDIO) },
                                        onGetPlusClick = { viewModel.setTab(NavTab.SETTINGS) },
                                        isOfflineBrain = isOffline,
                                        onNavigateSettings = { viewModel.setTab(NavTab.SETTINGS) }
                                    )
                                }

                                NavTab.DEVICES -> {
                                    DevicesScreen(
                                        telemetry = telemetry,
                                        deviceTilt = deviceTilt,
                                        onRefreshTelemetry = { viewModel.bridge.refreshTelemetry() },
                                        onToggleFlashlight = { viewModel.toggleFlashlight(it) },
                                        onOpenSystemSettings = { viewModel.openAndroidSettings(it ?: "general") },
                                        onLaunchApp = { pkg ->
                                            if (pkg == "camera") {
                                                viewModel.bridge.openCameraApp()
                                            } else if (pkg == "maps") {
                                                viewModel.bridge.openMapsApp("Current Location")
                                            } else {
                                                viewModel.bridge.launchApp(pkg)
                                            }
                                        }
                                    )
                                }

                                NavTab.SKILLS -> {
                                    SkillsScreen(
                                        onOpenSearch = { viewModel.openSubScreen(SubScreen.SEARCH) },
                                        onOpenVision = { viewModel.openSubScreen(SubScreen.VISION) },
                                        onOpenCodeStudio = { viewModel.openSubScreen(SubScreen.CODE_STUDIO) },
                                        onOpenVoiceNotes = { viewModel.openSubScreen(SubScreen.VOICE_NOTES) },
                                        onOpenTimers = { viewModel.openSubScreen(SubScreen.TIMER_STOPWATCH) },
                                        onOpenTasks = { viewModel.openSubScreen(SubScreen.TASKS) },
                                        onOpenExpenses = { viewModel.openSubScreen(SubScreen.EXPENSES) },
                                        onOpenHabits = { viewModel.openSubScreen(SubScreen.HABITS) },
                                        onOpenArmorThemes = { viewModel.openSubScreen(SubScreen.ARMOR_THEMES) },
                                        onOpenPrivacy = { viewModel.openSubScreen(SubScreen.PRIVACY) },
                                        onOpenAutomation = { viewModel.openSubScreen(SubScreen.AUTOMATION) },
                                        onOpenMemory = { viewModel.openSubScreen(SubScreen.MEMORY) },
                                        onVoiceCommand = { viewModel.sendUserMessage(it) }
                                    )
                                }

                                NavTab.SETTINGS -> {
                                    SettingsScreen(
                                        currentSettings = settings,
                                        onSaveSettings = { viewModel.repository.updateSettings(it) },
                                        isDarkTheme = true,
                                        selectedAccentColor = JarvisCyan,
                                        onToggleDarkTheme = { },
                                        onSelectAccentColor = { },
                                        onNavigatePrivacy = { viewModel.openSubScreen(SubScreen.PRIVACY) },
                                        onNavigateMemory = { viewModel.openSubScreen(SubScreen.MEMORY) },
                                        onNavigateBridge = { viewModel.setTab(NavTab.DEVICES) },
                                        onNavigateActivity = { viewModel.openSubScreen(SubScreen.ACTIVITY) },
                                        onNavigateVision = { viewModel.openSubScreen(SubScreen.VISION) },
                                        onNavigateVoiceSetup = { viewModel.openSubScreen(SubScreen.VOICE_SETUP) },
                                        onNavigateVoiceProfiles = { viewModel.openSubScreen(SubScreen.VOICE_SELECTION) },
                                        onNavigateAbout = { viewModel.openSubScreen(SubScreen.ABOUT) },
                                        onNavigateDiagnostics = { viewModel.openSubScreen(SubScreen.DIAGNOSTICS) },
                                        onNavigateNotifications = { viewModel.openSubScreen(SubScreen.NOTIFICATIONS) },
                                        onNavigateSearch = { viewModel.openSubScreen(SubScreen.SEARCH) },
                                        onNavigatePlugins = { viewModel.openSubScreen(SubScreen.PLUGINS) },
                                        onNavigateBackup = { viewModel.openSubScreen(SubScreen.BACKUP_EXPORT) },
                                        onLogout = {
                                            viewModel.authManager.signOut()
                                        },
                                        onNavigateBack = {
                                            viewModel.setTab(NavTab.HOME)
                                        }
                                    )
                                }
                            }
                        }

                        // Safety Confirmation Dialog
                        safetyRequest?.let { request ->
                            SafetyConfirmationDialog(
                                request = request,
                                onDismiss = { viewModel.dismissSafetyDialog() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubScreenHeader(
    title: String,
    isDarkTheme: Boolean = false,
    accentColor: Color = Color(0xFF2563EB),
    onBack: () -> Unit
) {
    val bg = if (isDarkTheme) Color(0xFF202020) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkTheme) Color(0xFFECECF1) else Color(0xFF0D0D0D)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary
        )
    }
}
