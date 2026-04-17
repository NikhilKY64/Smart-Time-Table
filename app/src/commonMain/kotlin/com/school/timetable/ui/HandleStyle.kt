package com.school.timetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.school.timetable.ui.theme.AccentPrimary

enum class HandleStyle(val displayName: String) {
    DEFAULT("Default"),
    WHITE_ROUNDED("White Rounded Bar"),
    GRAY_ROUNDED("Gray Rounded Bar"),
    BLUE_GLOW("Blue Glow Bar"),
    PURPLE_GLOW("Purple Glow Bar"),
    PILL_SHAPE("Pill Shape"),
    DOTTED("Dotted Handle"),
    DOUBLE_LINE("Double Line")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DragHandle(style: HandleStyle, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val handleModifier = Modifier.combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
    )

    when (style) {
        HandleStyle.DEFAULT -> {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(AccentPrimary.copy(alpha = 0.8f))
                    .then(handleModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.width(32.dp).height(4.dp).background(Color.White, RoundedCornerShape(2.dp)))
            }
        }
        HandleStyle.WHITE_ROUNDED -> {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color.Black.copy(alpha = 0.4f)) // distinct background wrapper
                    .then(handleModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.width(48.dp).height(6.dp).background(Color.White, RoundedCornerShape(3.dp)))
            }
        }
        HandleStyle.GRAY_ROUNDED -> {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color.DarkGray.copy(alpha = 0.8f))
                    .then(handleModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.width(48.dp).height(6.dp).background(Color.LightGray, RoundedCornerShape(3.dp)))
            }
        }
        HandleStyle.BLUE_GLOW -> {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(Color(0xFF1976D2).copy(alpha = 0.8f))
                    .then(handleModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).shadow(8.dp, spotColor = Color(0xFF81D4FA)).background(Color(0xFF81D4FA), RoundedCornerShape(2.dp)))
            }
        }
        HandleStyle.PURPLE_GLOW -> {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(Color(0xFF512DA8).copy(alpha = 0.8f))
                    .then(handleModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).shadow(8.dp, spotColor = Color(0xFFD1C4E9)).background(Color(0xFFD1C4E9), RoundedCornerShape(2.dp)))
            }
        }
        HandleStyle.PILL_SHAPE -> {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.3f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .then(handleModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.width(28.dp).height(6.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(3.dp)))
            }
        }
        HandleStyle.DOTTED -> {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(AccentPrimary.copy(alpha = 0.8f))
                    .then(handleModifier),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                }
            }
        }
        HandleStyle.DOUBLE_LINE -> {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(AccentPrimary.copy(alpha = 0.8f))
                    .then(handleModifier),
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(modifier = Modifier.width(36.dp).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(36.dp).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                }
            }
        }
    }
}
