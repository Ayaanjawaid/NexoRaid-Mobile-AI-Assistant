# NexoRaid - AI Chat Companion App

A sophisticated Android chat application powered by OpenRouter AI that offers personalized conversations with multiple AI personas, voice-to-speech capabilities, and persistent chat history management.

---

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [Features](#features)
3. [Architecture](#architecture)
4. [Technology Stack](#technology-stack)
5. [LLM Integration](#llm-integration)
6. [Project Structure](#project-structure)
7. [Setup Instructions](#setup-instructions)
8. [Usage Guide](#usage-guide)

---

## 🆕 Recent Updates
- **RAG (Retrieval‑Augmented Generation)**: Integrated real‑time web search using Google Custom Search API. The app now provides up‑to‑date answers for date, weather, news, and general queries.


## 🎯 Project Overview

**NexoRaid** is a modern Android application that enables users to engage in intelligent conversations with an AI companion. The app features multiple AI personas, each with distinct personalities and communication styles, along with text-to-speech capabilities for an immersive conversational experience.

- **Package Name**: `pw.mng.nexoraid`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Language**: Kotlin + Jetpack Compose
- **Version**: 1.0

---

## ✨ Features

### 1. **Multiple AI Personas**
The app includes 5 customizable AI personas, each with unique system prompts and visual themes:

- **Nexoraid (Default)**: A warm, futuristic, human-like companion (Cyan & Blue theme)
- **Strict Coder**: Senior staff engineer focused on high-quality code and technical explanations (Matrix Green theme)
- **Creative Writer**: Visionary novelist using vivid imagery and metaphors (Purple & Pink theme)
- **Empathetic Friend**: Supportive, non-judgmental companion focused on emotional intelligence (Orange & Gold theme)
- **Elite Commander**: Tactical commander speaking with authority and strategic focus (Red theme)

### 2. **Chat Management**
- Create and manage multiple chat sessions
- Persistent chat history stored locally via Room database
- Rename chat sessions for better organization
- Delete sessions individually
- Session sorting by creation time (most recent first)

### 3. **Personalization**
- User profile setup (name, age, gender)
- Profile data integrated into AI system prompts
- Persona selection and switching
- Dark mode toggle

### 4. **Text-to-Speech (TTS)**
- Automatic audio playback of AI responses
- Mute/unmute toggle
- US English language support
- Graceful fallback if language pack unavailable

### 5. **Smart Suggestions**
- Context-aware follow-up suggestions based on AI response
- Code-specific suggestions (Explain Code, Copy Code) when code blocks detected
- Dynamic suggestion generation for long responses

### 6. **Modern UI**
- Jetpack Compose-based modern interface
- Material Design 3 components
- Extended Material Icons
- Edge-to-edge layout
- Responsive design for various screen sizes

---

## 🏗️ Architecture

The app follows the **MVVM (Model-View-ViewModel)** architectural pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                      MainActivity                            │
│                   (Activity + UI Setup)                      │
└──────────────────┬──────────────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
    ┌───▼────────┐      ┌────▼─────────────┐
    │ ChatScreen │      │  ChatViewModel   │
    │   (UI)     │      │ (Business Logic) │
    └────────────┘      └────┬─────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
   ┌────▼──────────┐ ┌──────▼──────┐ ┌──────────▼──┐
   │ChatRepository │ │ UserManager  │ │ TTSManager  │
   │(Data Access)  │ │(Preferences) │ │ (Audio)     │
   └────┬──────────┘ └──────────────┘ └─────────────┘
        │
        │
   ┌────▼─────────────────┐
   │   Data Sources       │
   │ ┌──────────────────┐ │
   │ │ Room Database    │ │  ← Local storage
   │ │ (ChatDatabase)   │ │
   │ └──────────────────┘ │
   │ ┌──────────────────┐ │
   │ │ OpenRouter API   │ │  ← Remote AI
   │ │ (Retrofit)       │ │
   │ └──────────────────┘ │
   └──────────────────────┘
```

### Design Patterns Used:
- **Repository Pattern**: ChatRepository abstracts data sources (local DB + remote API)
- **Factory Pattern**: ChatViewModelFactory for safe ViewModel instantiation
- **Observer Pattern**: StateFlow for reactive UI updates
- **Dependency Injection**: Constructor-based injection of dependencies

---

## 🛠️ Technology Stack

### Core Framework
- **Android SDK**: API 24-35
- **Language**: Kotlin 2.0.21
- **Build System**: Gradle 8.13.2

### UI Framework
- **Jetpack Compose**: 2024.04.01 BOM
- **Material Design 3**: Material3 components
- **Material Icons**: Extended icon set
- **Activity Compose**: Activity integration

### Architecture & State Management
- **Lifecycle**: ViewModel, LiveData, StateFlow
- **Coroutines**: kotlinx-coroutines-android 1.8.1
- **Flow**: StateFlow for reactive data streams

### Data Persistence
- **Room Database**: 2.6.1
  - DAO pattern for database access
  - Automatic migrations with foreign keys
  - Query optimization with indices

### Networking
- **Retrofit**: 2.9.0 (HTTP client)
- **GSON Converter**: For JSON serialization
- **OkHttp**: 4.11.0 (HTTP interceptor & logging)

### Audio
- **TextToSpeech**: Native Android TTS engine
- **Locale Support**: US English

---

## 🤖 LLM Integration

### OpenRouter AI Integration

**NexoRaid** uses **OpenRouter.ai** as the LLM provider, which acts as a unified API gateway to multiple AI models.

#### Service Details

**API Endpoint**: `https://openrouter.ai/api/v1/chat/completions`

**Service Implementation**: [OpenRouterService.kt](app/src/main/java/pw/mng/nexoraid/api/OpenRouterService.kt)

```kotlin
interface OpenRouterService {
    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
```

#### Default Model

**Model**: `nvidia/nemotron-3-nano-30b-a3b:free`
- Lightweight LLM optimized for mobile/resource-constrained environments
- Free tier access via OpenRouter
- 30B parameters providing good balance between capability and speed
- Supports role-based messaging (system, user, assistant)

#### Request Format

```kotlin
data class ChatRequest(
    val model: String,
    val messages: List<ChatChoiceMessage>
)

data class ChatChoiceMessage(
    val role: String,
    val content: String
)
```

#### Response Format

```kotlin
data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatChoiceMessage
)
```

#### Message Flow

1. **System Prompt**: Each request includes a persona-specific system prompt injected with user details (name, age)
   ```kotlin
   ChatChoiceMessage(role = "system", content = systemPrompt)
   ```

2. **User Message**: User input is sent as the user role
   ```kotlin
   ChatChoiceMessage(role = "user", content = userInput)
   ```

3. **AI Response**: The model responds, and the response is stored locally and potentially read aloud via TTS

#### Authentication

- **API Key**: Bearer token-based authentication via OpenRouter
- **Headers**: 
  - `Authorization`: Bearer token
  - `HTTP-Referer`: GitHub reference
  - `X-Title`: Application identifier

#### Error Handling

- Network failures: "Connection lost" error message
- API failures: "Protocol failure" error message
- Graceful fallback with local error storage

---

## 📁 Project Structure

### Root Configuration
```
build.gradle.kts          ← Root build configuration
settings.gradle.kts       ← Project settings
gradle.properties         ← Gradle properties
gradlew / gradlew.bat     ← Gradle wrapper scripts
local.properties          ← Local Android SDK path
```

### Gradle Configuration
```
gradle/
  libs.versions.toml      ← Dependency versions catalog
  wrapper/                ← Gradle wrapper
```

### App Module
```
app/
  build.gradle.kts        ← App-specific build config
  proguard-rules.pro      ← ProGuard optimization rules
  
  src/main/
    AndroidManifest.xml   ← App manifest with permissions
    
    java/pw/mng/nexoraid/
      ├── MainActivity.kt           ← Entry point & initialization
      │
      ├── api/
      │   ├── OpenRouterService.kt  ← Retrofit API interface
      │   └── OpenRouterModels.kt   ← API request/response models
      │
      ├── data/
      │   ├── ChatDatabase.kt       ← Room database setup
      │   ├── ChatModels.kt         ← Entity models (ChatSession, Message)
      │   ├── ChatRepository.kt     ← Data repository layer
      │   ├── ChatDao.kt            ← Database access object
      │   ├── Personas.kt           ← Persona enum definitions
      │   ├── UserManager.kt        ← SharedPreferences wrapper
      │   └── UserProfile.kt        ← User data model
      │
      ├── ui/
      │   ├── ChatScreen.kt         ← Main composable UI
      │   ├── ChatViewModel.kt      ← ViewModel with business logic
      │   ├── ChatViewModelFactory.kt ← ViewModel factory
      │   ├── components/           ← Reusable Compose components
      │   └── theme/                ← Theming configuration
      │
      └── utils/
          └── TTSManager.kt         ← Text-to-Speech utility
    
    res/
      ├── values/                   ← String, color, dimension resources
      ├── drawable/                 ← Vector drawables & images
      └── mipmap/                   ← App launcher icons
  
  build/                  ← Build outputs (generated, intermediates, outputs)
```

### Key Files

| File | Purpose |
|------|---------|
| [MainActivity.kt](app/src/main/java/pw/mng/nexoraid/MainActivity.kt) | Application entry point; initializes services, Retrofit, Room, ViewModel |
| [ChatViewModel.kt](app/src/main/java/pw/mng/nexoraid/ui/ChatViewModel.kt) | Core business logic; handles user interactions, message sending, state management |
| [ChatRepository.kt](app/src/main/java/pw/mng/nexoraid/data/ChatRepository.kt) | Data access layer; abstracts Room DB and OpenRouter API |
| [ChatDatabase.kt](app/src/main/java/pw/mng/nexoraid/data/ChatDatabase.kt) | Room database configuration; defines entities and DAOs |
| [OpenRouterService.kt](app/src/main/java/pw/mng/nexoraid/api/OpenRouterService.kt) | Retrofit interface for OpenRouter API calls |
| [Personas.kt](app/src/main/java/pw/mng/nexoraid/data/Personas.kt) | Enum defining 5 AI personas with system prompts and themes |
| [UserManager.kt](app/src/main/java/pw/mng/nexoraid/data/UserManager.kt) | Manages user preferences and profile via SharedPreferences |
| [TTSManager.kt](app/src/main/java/pw/mng/nexoraid/utils/TTSManager.kt) | Handles text-to-speech synthesis |

---

## 🚀 Setup Instructions

### Prerequisites
- Android Studio Flamingo or later
- JDK 11+
- Android SDK 35 (API Level 35)
- Gradle 8.13.2

### Installation Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/NexoRaid.git
   cd NexoRaid
   ```

2. **Open in Android Studio**
   - File → Open → Select NexoRaid folder
   - Android Studio will sync Gradle files automatically

3. **Configure API Keys**
   - **OpenRouter (LLM)**: Get a free key from [OpenRouter](https://openrouter.ai)
   - **Google Custom Search (RAG)**: Get a free Search API key from Google Cloud, and create a Programmable Search Engine to get a CX ID.
   - Create or open the `local.properties` file in the root of the project
   - Add your keys like this:
     ```properties
     OPENROUTER_API_KEY=your-openrouter-api-key-here
     SEARCH_API_KEY=your-google-search-api-key
     SEARCH_ENGINE_ID=your-search-engine-cx-id
     ```

4. **Build the Project**
   ```bash
   ./gradlew build
   ```

5. **Run on Device/Emulator**
   - Android Studio: Run → Run 'app'
   - Or connect device and press Shift+F10

### Environment Configuration
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 35 (Android 15)
- **Compile SDK**: API 35
- **Java/Kotlin Compatibility**: JVM 11

---

## 📱 Usage Guide

### Initial Setup
1. **Launch the App**: On first run, you'll see the setup screen
2. **Enter Profile**: Provide your name, age, and gender
3. **Start Chatting**: A default chat session is created automatically

### Chat Features

#### Sending Messages
- Type your message in the input field
- Press Send or tap the send button
- Wait for the AI response
- Response automatically plays audio (if not muted)

#### Persona Selection
- Tap the persona selector menu
- Choose from 5 distinct personalities
- Each persona responds differently to the same query
- Persona choice persists across sessions

#### Managing Sessions
- **New Chat**: Create a new conversation
- **Switch Session**: Tap a session from the sidebar to open it
- **Rename**: Long-press session title to rename
- **Delete**: Swipe or tap delete icon to remove session

#### Suggestions
- After each AI response, contextual suggestions appear
- Tap suggestions for quick follow-up prompts
- Suggestions vary based on response content

#### Audio Control
- **Mute Button**: Toggle to silence TTS
- **Audio Playback**: Responses read aloud automatically (unless muted)

#### Appearance
- **Dark Mode Toggle**: Settings → Dark Mode
- **Theme**: Each persona has its own color scheme

### Example Interactions

**Example 1: Strict Coder Persona**
```
User: "How do I reverse a list in Kotlin?"
Coder: "Use the reversed() extension function: list.reversed() returns a new list.
        For in-place reversal: list.reverse(). Performance: O(n) time, O(n) space for reversed()."
```

**Example 2: Empathetic Friend Persona**
```
User: "I'm feeling stressed about my project."
Friend: "That sounds really tough, and it's completely valid to feel stressed.
        I'm here for you. Want to talk about what's bothering you most?"
```

**Example 3: Creative Writer Persona**
```
User: "Write a haiku about technology."
Writer: "Silicon dreams breathe,
        Circuits weave through morning mist—
        Light finds no pathway."
```

---

## 🔐 Security & Privacy

- **Local Storage**: Chat history stored locally; never synced to cloud without consent
- **API Keys**: Stored in source (development). Consider using encrypted SharedPreferences for production
- **Permissions**: Only requires `INTERNET` permission for API communication
- **TTS Data**: All TTS processing done on-device

---

## 🔄 Data Flow

### Message Sending Flow
```
User Input
    ↓
ChatViewModel.sendMessage()
    ↓
ChatRepository.sendMessage()
    ├→ Save user message to Room DB
    ├→ Build ChatRequest with system prompt + user message
    ├→ Call OpenRouterService.getChatCompletion()
    │
    └→ OpenRouter API
        ↓
        AI Model (nvidia/nemotron-3-nano-30b-a3b:free)
        ↓
        Response
    ↓
Save AI response to Room DB
    ↓
TTSManager.speak(response)  [if not muted]
    ↓
UI updates with new message
```

---

## 🐛 Known Limitations

1. **Single API Key**: API key hardcoded in source (development-only)
2. **No Real-time Typing**: Indicator doesn't show character-by-character streaming
3. **No Image Support**: Text-only chat interface
4. **TTS Language**: Only US English supported
5. **No Offline Mode**: Requires internet for API calls

---

## 🚀 Future Enhancements

- [ ] Secure API key storage via encrypted SharedPreferences
- [ ] Model selection UI for different OpenRouter models
- [ ] Image upload and analysis
- [ ] Chat export to PDF/text
- [ ] Cloud sync with user authentication
- [ ] Multi-language TTS support
- [ ] Message search functionality
- [ ] Custom persona creation
- [ ] Response streaming/tokens display
- [ ] Device-local LLM fallback option

---

## 📝 License

[Add your license information here]

---

## 👤 Author

**Project**: NexoRaid  
**Package**: pw.mng.nexoraid  
**Created**: 2024

---

## 🤝 Contributing

[Add contribution guidelines here]

---

## 📞 Support

For issues, questions, or feature requests, please [open an issue](https://github.com/yourusername/NexoRaid/issues).

