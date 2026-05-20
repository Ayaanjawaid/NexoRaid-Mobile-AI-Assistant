package pw.mng.nexoraid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MessagePart {
    data class Text(val content: String) : MessagePart()
    data class Code(val language: String, val code: String) : MessagePart()
}

fun parseMessageContent(content: String): List<MessagePart> {
    val parts = mutableListOf<MessagePart>()
    val codeBlockRegex = Regex("```(\\w*)\\n?([\\s\\S]*?)```")
    
    var lastIndex = 0
    codeBlockRegex.findAll(content).forEach { matchResult ->
        // Add text before the code block
        if (matchResult.range.first > lastIndex) {
            parts.add(MessagePart.Text(content.substring(lastIndex, matchResult.range.first)))
        }
        
        // Add the code block
        val language = matchResult.groupValues[1].trim()
        val code = matchResult.groupValues[2].trim() // Remove extra newlines
        parts.add(MessagePart.Code(language, code))
        
        lastIndex = matchResult.range.last + 1
    }
    
    // Add remaining text
    if (lastIndex < content.length) {
        parts.add(MessagePart.Text(content.substring(lastIndex)))
    }
    
    return parts
}

@Composable
fun FormattedMessageText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val parts = remember(text) { parseMessageContent(text) }

    Column(modifier = modifier) {
        parts.forEach { part ->
            when (part) {
                is MessagePart.Text -> {
                    if (part.content.isNotEmpty()) {
                        SelectionContainer {
                            Text(
                                text = parseMarkdownStyles(part.content),
                                color = textColor,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
                is MessagePart.Code -> {
                    CodeBlock(language = part.language, code = part.code)
                }
            }
        }
    }
}

@Composable
fun CodeBlock(language: String, code: String) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "Code" },
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Row(
                    modifier = Modifier
                        .clickable {
                            clipboardManager.setText(AnnotatedString(code))
                            copied = true
                        }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = pw.mng.nexoraid.R.drawable.ic_copy),
                        contentDescription = "Copy",
                        tint = if(copied) Color.Green else Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if(copied) "Copied" else "Copy",
                        color = if(copied) Color.Green else Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Code Content
            SelectionContainer {
                Text(
                    text = code,
                    color = Color(0xFFD4D4D4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Simple Markdown parser for Bold (**text**) and Italic (*text*)
fun parseMarkdownStyles(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("(\\*\\*(.*?)\\*\\*)|(\n)") // Match **bold** or newlines
        
        // This is a naive implementation; improving it effectively is hard without a full library
        // Let's stick to basic bold processing
        
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        
        var currentIndex = 0
        boldRegex.findAll(text).forEach { match ->
            // Append text before match
            append(text.substring(currentIndex, match.range.first))
            
            // Append bold text
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            
            currentIndex = match.range.last + 1
        }
        
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val dots = listOf(
        remember { androidx.compose.animation.core.Animatable(0.2f) },
        remember { androidx.compose.animation.core.Animatable(0.2f) },
        remember { androidx.compose.animation.core.Animatable(0.2f) }
    )

    dots.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            kotlinx.coroutines.delay(index * 100L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(600),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                )
            )
        }
    }

    Row(
        modifier = modifier
            .padding(16.dp)
            .wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { animatable ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = animatable.value),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

