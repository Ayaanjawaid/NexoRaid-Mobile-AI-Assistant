package pw.mng.nexoraid.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pw.mng.nexoraid.data.ChatSession
import pw.mng.nexoraid.data.Message
import pw.mng.nexoraid.ui.components.CyberButton
import pw.mng.nexoraid.ui.components.CyberTextField
import pw.mng.nexoraid.ui.components.FormattedMessageText
import pw.mng.nexoraid.ui.components.TypingIndicator
import pw.mng.nexoraid.ui.components.MessagePart
import pw.mng.nexoraid.ui.components.parseMessageContent
import pw.mng.nexoraid.ui.components.CodeBlock
import androidx.compose.foundation.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.speech.RecognizerIntent
import android.app.Activity
import android.widget.Toast
import pw.mng.nexoraid.ui.theme.*
import pw.mng.nexoraid.security.SecureStorage
import pw.mng.nexoraid.ui.components.SettingsDialog
import pw.mng.nexoraid.ui.components.PrivacyDialog
import pw.mng.nexoraid.ui.components.CustomToastState
import pw.mng.nexoraid.ui.components.CustomToastHost
import pw.mng.nexoraid.ui.components.LocalToastManager
import pw.mng.nexoraid.ui.components.SavedApisDialog
import pw.mng.nexoraid.ui.components.AboutDialog
import pw.mng.nexoraid.ui.components.PrivacyDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(viewModel: ChatViewModel, secureStorage: SecureStorage) {
    var currentScreen by remember { mutableStateOf(if (viewModel.isSetupDone.value) "chat" else "onboarding") }
    val isSetupDone by viewModel.isSetupDone.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentPersona by viewModel.currentPersona.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val toastState = remember { CustomToastState(snackbarHostState) }

    NexoRaidTheme(darkTheme = isDarkMode, persona = currentPersona) {
        CompositionLocalProvider(LocalToastManager provides toastState) {
            Scaffold(
                snackbarHost = { CustomToastHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0.dp)
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    when (currentScreen) {
                        "onboarding" -> OnboardingScreen(secureStorage = secureStorage) { name, age, gender ->
                            viewModel.saveProfile(name, age, gender)
                            currentScreen = "chat"
                        }
                        "chat" -> ChatWithSidebar(viewModel, secureStorage)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWithSidebar(viewModel: ChatViewModel, secureStorage: SecureStorage) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(secureStorage.getApiKey(secureStorage.getDefaultProvider()).isNullOrBlank()) }
    var showSavedApisDialog by remember { mutableStateOf(false) }
    var showTour by remember { mutableStateOf(!secureStorage.hasSeenTour()) }
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            secureStorage = secureStorage,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showSavedApisDialog) {
        SavedApisDialog(secureStorage = secureStorage, onDismiss = { showSavedApisDialog = false })
    }

    var showStorageScreen by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.width(300.dp)
            ) {
                SidebarContent(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    onSessionClick = { id ->
                        viewModel.selectSession(id)
                        scope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        viewModel.createNewChat()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { id -> viewModel.deleteSession(id) },
                    onRenameSession = { id, newName -> viewModel.renameSession(id, newName) },
                    onAboutClick = { showAboutDialog = true },
                    onPrivacyClick = { showPrivacyDialog = true },
                    onSettingsClick = { showSettingsDialog = true },
                    onSavedApisClick = { showSavedApisDialog = true },
                    onStorageClick = {
                        showStorageScreen = true
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showStorageScreen) {
                pw.mng.nexoraid.ui.components.StoragePage(
                    documentManager = pw.mng.nexoraid.data.DocumentManager(androidx.compose.ui.platform.LocalContext.current),
                    onBack = { showStorageScreen = false }
                )
            } else {
                ChatScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            
            if (showTour) {
                TourOverlay(onDismiss = { 
                    secureStorage.setHasSeenTour()
                    showTour = false 
                })
            }
        }
    }
}

@Composable
fun SidebarContent(
    sessions: List<ChatSession>,
    currentSessionId: Long?,
    onSessionClick: (Long) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteSession: (Long) -> Unit,
    onRenameSession: (Long, String) -> Unit,
    onAboutClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSavedApisClick: () -> Unit,
    onStorageClick: () -> Unit
) {
    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var renameText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    if (sessionToRename != null) {
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Rename Mission") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        onRenameSession(sessionToRename!!.id, renameText)
                        sessionToRename = null
                    }
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) { Text("CANCEL") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "NEXORAID MISSION LOG",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = onNewChatClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.background)
            Spacer(Modifier.width(8.dp))
            Text("NEW SESSION", color = MaterialTheme.colorScheme.background)
        }

        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Missions...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        val filteredSessions = sessions.filter { it.title.contains(searchQuery, ignoreCase = true) }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredSessions) { session ->
                val isSelected = session.id == currentSessionId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { onSessionClick(session.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List, null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        session.title,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        IconButton(onClick = { 
                            renameText = session.title
                            sessionToRename = session
                        }) {
                            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onDeleteSession(session.id) }) {
                            Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

        NavigationDrawerItem(
            label = { Text("Storage") },
            selected = false,
            onClick = onStorageClick,
            icon = { Icon(Icons.Default.Storage, null) },
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
        NavigationDrawerItem(
            label = { Text("Settings") },
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, null) },
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
        NavigationDrawerItem(
            label = { Text("Saved APIs") },
            selected = false,
            onClick = onSavedApisClick,
            icon = { Icon(Icons.Default.Bookmarks, null) },
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
        NavigationDrawerItem(
            label = { Text("About Creator") },
            selected = false,
            onClick = onAboutClick,
            icon = { Icon(Icons.Default.Info, null) },
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
        NavigationDrawerItem(
            label = { Text("Privacy & Security") },
            selected = false,
            onClick = onPrivacyClick,
            icon = { Icon(Icons.Default.Security, null) },
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
    }
}



@Composable
fun OnboardingScreen(secureStorage: SecureStorage, onComplete: (String, String, String) -> Unit) {
    var step by remember { mutableStateOf(1) }
    
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        AnimatedContent(targetState = step, label = "onboarding") { targetStep ->
            when (targetStep) {
                1 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = pw.mng.nexoraid.R.drawable.ic_nexoraid_vector),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(Modifier.height(32.dp))
                        Text("WELCOME TO NEXORAID", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Your premium, privacy-first AI workspace. All data stays strictly on your device. Let's set up your profile.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(48.dp))
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("NEXT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("PROFILE & ACCESS", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Define your profile for optimal interface sync", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                        
                        Spacer(Modifier.height(32.dp))
                        
                        OutlinedTextField(
                            value = name, onValueChange = { name = it },
                            label = { Text("Agent Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = age, onValueChange = { age = it },
                                label = { Text("Age") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = gender, onValueChange = { gender = it },
                                label = { Text("Gender") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "⚠️ IMPORTANT: An API Key is required to use this app.",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = apiKey, onValueChange = { apiKey = it },
                                    label = { Text("API Key (Optional right now)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        
                        Button(
                            onClick = { 
                                if (apiKey.isNotBlank()) {
                                    val defaultProvider = secureStorage.getDefaultProvider()
                                    secureStorage.saveApiKey(defaultProvider, apiKey)
                                }
                                onComplete(name, age, gender) 
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("BOOT SEQUENCE START", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onMenuClick: () -> Unit) {
    var textState by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val documentManager = remember { pw.mng.nexoraid.data.DocumentManager(context) }
    
    val documentLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val file = documentManager.saveDocumentFromUri(it)
            if (file != null) {
                val text = pw.mng.nexoraid.utils.DocumentParser.extractText(file)
                textState += "\n[Attached Document: ${file.name}]\n$text\n"
            }
        }
    }

    LaunchedEffect(messages.size, isTyping) {
        val totalItems = messages.size + if (isTyping) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NEXORAID", fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    val isMuted by viewModel.isMuted.collectAsState()
                    IconButton(onClick = { viewModel.toggleMute() }) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            ChatInputArea(
                text = textState,
                onTextChange = { textState = it },
                onSend = {
                    if (textState.isNotBlank()) {
                        viewModel.sendMessage(textState)
                        textState = ""
                    }
                },
                onAttach = {
                    documentLauncher.launch("*/*")
                },
                suggestions = viewModel.suggestions.collectAsState().value,
                onSuggestionClick = { suggestion ->
                    viewModel.sendMessage(suggestion)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message -> MessageItem(message) }
            if (isTyping) {
                item {
                    val streamingResponse by viewModel.streamingResponse.collectAsState()
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                        Surface(
                            modifier = Modifier.widthIn(max = 340.dp).padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(
                                topStart = 20.dp, topEnd = 20.dp,
                                bottomEnd = 20.dp, bottomStart = 4.dp
                            ),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                            tonalElevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (streamingResponse.isNotBlank()) {
                                    val parts = parseMessageContent(streamingResponse)
                                    parts.forEach { part ->
                                        when (part) {
                                            is MessagePart.Text -> FormattedMessageText(
                                                text = part.content,
                                                textColor = MaterialTheme.colorScheme.onSurface
                                            )
                                            is MessagePart.Code -> CodeBlock(
                                                language = part.language,
                                                code = part.code
                                            )
                                        }
                                    }
                                } else {
                                    TypingIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputArea(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0)
            if (spokenText != null) {
                onTextChange(text + (if (text.isNotEmpty()) " " else "") + spokenText)
            }
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Column {
            if (suggestions.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions) { suggestion ->
                        SuggestionChip(
                            onClick = { onSuggestionClick(suggestion) },
                            label = { Text(suggestion) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }
            val combinedInsets = WindowInsets.ime.union(WindowInsets.navigationBars)
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp, top = 8.dp)
                    .windowInsetsPadding(combinedInsets),
                verticalAlignment = Alignment.CenterVertically
            ) {
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
            }
            TextField(
                value = text, onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message Nexoraid...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak to Nexoraid...")
                    }
                    try {
                        speechLauncher.launch(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Voice input not supported", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Spacer(Modifier.width(8.dp))
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onSend()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null)
            }
            }
        }
    }
}

@Composable
fun MessageItem(message: Message) {
    val isBot = !message.isUser
    val alignment = if (isBot) Alignment.Start else Alignment.End
    
    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isBot) 4.dp else 20.dp,
        bottomEnd = if (isBot) 20.dp else 4.dp
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalAlignment = alignment
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 340.dp),
                shape = bubbleShape,
                color = if (isBot) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isBot) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                tonalElevation = if (isBot) 2.dp else 0.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val parts = parseMessageContent(message.content)
                    parts.forEach { part ->
                        when (part) {
                            is MessagePart.Text -> FormattedMessageText(
                                text = part.content,
                                textColor = MaterialTheme.colorScheme.onSurface
                            )
                            is MessagePart.Code -> CodeBlock(
                                language = part.language,
                                code = part.code
                            )
                        }
                    }
                }
            }
            
            // Optional: Timestamp or Status could go here
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun TourOverlay(onDismiss: () -> Unit) {
    var currentStep by remember { mutableStateOf(1) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable { 
                if (currentStep < 2) currentStep++ else onDismiss()
            }
    ) {
        if (currentStep == 1) {
            Column(
                modifier = Modifier.padding(top = 90.dp, start = 32.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "Welcome! Tap the menu icon\nto access Settings & Saved APIs.",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Text("Tap anywhere to continue", color = Color.Gray, fontSize = 14.sp)
            }
        } else if (currentStep == 2) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Tap anywhere to finish", color = Color.Gray, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Type your message here\nand hit Send to start chatting!",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            }
        }
    }
}
