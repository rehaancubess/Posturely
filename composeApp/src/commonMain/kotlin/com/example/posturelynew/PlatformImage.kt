package com.mobil80.posturely

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
expect fun PlatformImage(
    model: Any,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) 