package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

@Composable
fun UserAvatar(
    avatarUrl: String?,
    fullName: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null
) {
    val initials = remember(fullName) {
        if (fullName.isNullOrBlank()) ""
        else {
            val parts = fullName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            when {
                parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                parts.isNotEmpty() -> parts[0].take(2).uppercase()
                else -> ""
            }
        }
    }

    val boxModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    if (avatarUrl.isNullOrBlank()) {
        DefaultAvatarBox(initials = initials, size = size, modifier = boxModifier)
    } else {
        SubcomposeAsyncImage(
            model = avatarUrl,
            contentDescription = "Photo de profil",
            contentScale = ContentScale.Crop,
            modifier = boxModifier,
            loading = {
                DefaultAvatarBox(initials = initials, size = size)
            },
            error = {
                DefaultAvatarBox(initials = initials, size = size)
            }
        )
    }
}

@Composable
private fun DefaultAvatarBox(
    initials: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotBlank()) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (size.value * 0.36f).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
