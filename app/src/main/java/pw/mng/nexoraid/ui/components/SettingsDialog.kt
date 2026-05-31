package pw.mng.nexoraid.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import pw.mng.nexoraid.domain.Provider
import pw.mng.nexoraid.data.SavedApi
import pw.mng.nexoraid.data.Persona
import pw.mng.nexoraid.security.SecureStorage
import pw.mng.nexoraid.ui.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    secureStorage: SecureStorage,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(secureStorage.getDefaultProvider()) }
    var apiKey by remember { mutableStateOf(secureStorage.getApiKey(selectedProvider) ?: "") }
    var model by remember { mutableStateOf(secureStorage.getDefaultModel(selectedProvider)) }
    
    val currentPersona by viewModel.currentPersona.collectAsState()
    val personas = Persona.values().toList()
    
    var configName by remember { mutableStateOf("") }
    
    var apiKeyVisible by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    
    val clipboardManager = LocalClipboardManager.current
    val toastManager = LocalToastManager.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    
                    // Persona Section
                    Column {
                        SettingsSectionTitle("AI Persona")
                        personas.forEach { persona ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setPersona(persona) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (persona == currentPersona),
                                    onClick = { viewModel.setPersona(persona) }
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(persona.displayName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // Active Configuration Section
                    Column {
                        SettingsSectionTitle("Active Configuration")
                        
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedProvider.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Provider") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                Provider.values().forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(provider.name) },
                                        onClick = {
                                            selectedProvider = provider
                                            apiKey = secureStorage.getApiKey(provider) ?: ""
                                            model = secureStorage.getDefaultModel(provider)
                                            secureStorage.setDefaultProvider(provider)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { 
                                apiKey = it
                                secureStorage.saveApiKey(selectedProvider, it)
                            },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                    Icon(
                                        imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (apiKeyVisible) "Hide API Key" else "Show API Key"
                                    )
                                }
                            }
                        )

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = model,
                            onValueChange = { 
                                model = it
                                secureStorage.setDefaultModel(selectedProvider, it)
                            },
                            label = { Text("Model ID (e.g. gpt-4o)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Instruction Image
                        Image(
                            painter = painterResource(id = pw.mng.nexoraid.R.drawable.instruction_model_id),
                            contentDescription = "Model ID Instruction",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Save to configurations
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Save Configuration", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = configName,
                                    onValueChange = { configName = it },
                                    label = { Text("Configuration Name (e.g. Work API)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (saveError != null) {
                                    Text(
                                        text = saveError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                
                                Button(
                                    onClick = {
                                        if (configName.isBlank()) {
                                            saveError = "Configuration Name cannot be empty"
                                        } else if (apiKey.isBlank()) {
                                            saveError = "API Key cannot be empty"
                                        } else {
                                            saveError = null
                                            val newConfig = SavedApi(
                                                name = configName,
                                                provider = selectedProvider,
                                                apiKey = apiKey,
                                                model = model
                                            )
                                            secureStorage.saveApiConfiguration(newConfig)
                                            configName = ""
                                            coroutineScope.launch {
                                                toastManager.showToast("API Configuration Saved", ToastType.SUCCESS)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("SAVE TO LIBRARY", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
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
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
