package pw.mng.nexoraid.ui

import androidx.lifecycle.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pw.mng.nexoraid.data.*
import pw.mng.nexoraid.utils.TTSManager

class ChatViewModel(
    private val repository: ChatRepository,
    private val userManager: UserManager,
    private val ttsManager: TTSManager
) : ViewModel() {

    val sessions = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = _currentSessionId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getMessagesForSession(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isDarkMode = userManager.isDarkMode

    private val _userProfile = MutableStateFlow<UserProfile?>(userManager.getProfile())
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _isSetupDone = MutableStateFlow(userManager.isSetupDone())
    val isSetupDone: StateFlow<Boolean> = _isSetupDone

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    private val _streamingResponse = MutableStateFlow("")
    val streamingResponse: StateFlow<String> = _streamingResponse.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _currentPersona = MutableStateFlow(Persona.fromId(userManager.getPersonaId()))
    val currentPersona: StateFlow<Persona> = _currentPersona.asStateFlow()

    init {
        viewModelScope.launch {
            val firstSession = sessions.filter { it.isNotEmpty() }.firstOrNull()?.firstOrNull()
            if (firstSession != null && _currentSessionId.value == null) {
                _currentSessionId.value = firstSession.id
            } else if (firstSession == null && isSetupDone.value) {
                createNewChat()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    fun setPersona(persona: Persona) {
        userManager.savePersona(persona.id)
        _currentPersona.value = persona
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        if (_isMuted.value) ttsManager.stop()
    }

    fun toggleDarkMode(enabled: Boolean) {
        userManager.setDarkMode(enabled)
    }

    fun selectSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun createNewChat() {
        viewModelScope.launch {
            val id = repository.createNewSession()
            _currentSessionId.value = id
            _suggestions.value = emptyList()
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = sessions.value.firstOrNull { it.id != sessionId }?.id
                _suggestions.value = emptyList()
            }
        }
    }

    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, newTitle)
        }
    }

    fun saveProfile(name: String, age: String, gender: String) {
        val profile = UserProfile(name, age, gender)
        userManager.saveProfile(profile)
        _userProfile.value = profile
        _isSetupDone.value = true
        if (_currentSessionId.value == null) createNewChat()
    }

    fun sendMessage(content: String) {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            _isTyping.value = true
            _suggestions.value = emptyList() // Clear previous suggestions
            
            // Prepare Prompt
            val profile = _userProfile.value
            val rawPrompt = _currentPersona.value.systemPrompt
            
            val currentDateTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy - h:mm a"))
            val finalPrompt = rawPrompt
                .replace("%NAME%", profile?.name ?: "User")
                .replace("%AGE%", profile?.age ?: "Unknown") + 
                "\n\nSystem Info: The current date and time is $currentDateTime."

            try {
                _streamingResponse.value = ""
                repository.streamMessage(sessionId, content, profile, finalPrompt).collect { token ->
                    _streamingResponse.value += token
                }
                
                // Stream finished
                val finalResponse = _streamingResponse.value
                if (finalResponse.isNotBlank()) {
                    // Save to DB
                    repository.saveBotMessage(sessionId, finalResponse)
                    if (!_isMuted.value) {
                        ttsManager.speak(finalResponse)
                    }
                    updateSuggestions(finalResponse)
                }
                _streamingResponse.value = ""
            } finally {
                _isTyping.value = false
            }
        }
    }

    private fun updateSuggestions(botResponse: String) {
        val newSuggestions = mutableListOf<String>()
        if (botResponse.contains("```")) {
            newSuggestions.add("Explain Code")
            newSuggestions.add("Copy Code")
        }
        if (botResponse.length > 200) {
            newSuggestions.add("Summarize")
        }
        newSuggestions.add("Tell me more")
        newSuggestions.add("Thanks!")
        _suggestions.value = newSuggestions
    }
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val userManager: UserManager,
    private val ttsManager: TTSManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, userManager, ttsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
