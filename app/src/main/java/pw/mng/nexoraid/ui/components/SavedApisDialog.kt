package pw.mng.nexoraid.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pw.mng.nexoraid.security.SecureStorage
import pw.mng.nexoraid.data.SavedApi
import kotlinx.coroutines.launch

@Composable
fun SavedApisDialog(
    secureStorage: SecureStorage,
    onDismiss: () -> Unit
) {
    var savedApis by remember { mutableStateOf(secureStorage.getSavedApiConfigurations()) }
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
                        text = "Saved API Library",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (savedApis.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No APIs saved yet.\nSave an API in Settings to see it here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(savedApis) { config ->
                            SavedApiCard(
                                config = config,
                                onApply = {
                                    secureStorage.setDefaultProvider(it.provider)
                                    secureStorage.saveApiKey(it.provider, it.apiKey)
                                    secureStorage.setDefaultModel(it.provider, it.model)
                                    
                                    coroutineScope.launch {
                                        toastManager.showToast("Configuration Applied Successfully", ToastType.SUCCESS)
                                    }
                                },
                                onDelete = {
                                    secureStorage.deleteSavedApiConfiguration(it.id)
                                    savedApis = secureStorage.getSavedApiConfigurations()
                                },
                                onCopy = { text ->
                                    clipboardManager.setText(AnnotatedString(text))
                                    coroutineScope.launch {
                                        toastManager.showToast("Copied to clipboard", ToastType.INFO)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedApiCard(
    config: SavedApi,
    onApply: (SavedApi) -> Unit,
    onDelete: (SavedApi) -> Unit,
    onCopy: (String) -> Unit
) {
    var keyVisible by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { onDelete(config) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Provider: ${config.provider.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Model: ${config.model}", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onCopy(config.model) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Model", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Key: " + (if (keyVisible) config.apiKey else "••••••••••••••••"), 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { keyVisible = !keyVisible }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Key Visibility",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onCopy(config.apiKey) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Key", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onApply(config) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("APPLY CONFIGURATION")
            }
        }
    }
}
