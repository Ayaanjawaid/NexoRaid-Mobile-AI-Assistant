package pw.mng.nexoraid.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ToastType { SUCCESS, ERROR, INFO }

class CustomToastState(val snackbarHostState: SnackbarHostState) {
    suspend fun showToast(message: String, type: ToastType = ToastType.INFO) {
        // We encode the type in the actionLabel temporarily to pass it to the visual
        snackbarHostState.showSnackbar(
            message = message,
            actionLabel = type.name,
            duration = SnackbarDuration.Short
        )
    }
}

val LocalToastManager = staticCompositionLocalOf<CustomToastState> {
    error("No ToastManager provided")
}

@Composable
fun CustomToastHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        val type = try { ToastType.valueOf(data.visuals.actionLabel ?: "INFO") } catch(e: Exception) { ToastType.INFO }
        val (containerColor, icon) = when (type) {
            ToastType.SUCCESS -> Color(0xFF10B981) to Icons.Default.CheckCircle // Emerald 500
            ToastType.ERROR -> Color(0xFFEF4444) to Icons.Default.Error // Red 500
            ToastType.INFO -> Color(0xFF3B82F6) to Icons.Default.Info // Blue 500
        }

        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = data.visuals.message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
