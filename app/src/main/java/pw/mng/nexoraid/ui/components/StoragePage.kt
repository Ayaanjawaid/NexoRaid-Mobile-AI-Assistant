package pw.mng.nexoraid.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pw.mng.nexoraid.data.DocumentManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoragePage(documentManager: DocumentManager, onBack: () -> Unit) {
    val context = LocalContext.current
    var documents by remember { mutableStateOf(documentManager.listDocuments()) }
    
    val refreshDocs = {
        documents = documentManager.listDocuments()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Document Storage", color = MaterialTheme.colorScheme.onSurface) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Description, // Placeholder back icon if needed, but let's use text or just close
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                if (documents.isNotEmpty()) {
                    TextButton(onClick = {
                        documentManager.clearAllDocuments()
                        refreshDocs()
                    }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        if (documents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No documents stored.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(documents) { file ->
                    DocumentItem(
                        file = file,
                        onDelete = {
                            documentManager.deleteDocument(file.name)
                            refreshDocs()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentItem(file: File, onDelete: () -> Unit) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val lastModified = sdf.format(Date(file.lastModified()))
    val fileSize = Formatter.formatShortFileSize(context, file.length())
    val isPdf = file.name.lowercase().endsWith(".pdf")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Description,
                contentDescription = "File Type",
                tint = if (isPdf) Color(0xFFF44336) else Color(0xFF2196F3),
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$fileSize • $lastModified",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete File",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
