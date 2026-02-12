package com.elyefris.khalessleeptracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elyefris.khalessleeptracker.ui.theme.GlassBorder
import com.elyefris.khalessleeptracker.ui.theme.GlassWhite

@Composable
fun LiquidCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Forma redondeada suave
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .clip(shape)
            // 1. Fondo semitransparente (La base del cristal)
            .background(GlassWhite)
            // 2. Borde con gradiente (El reflejo del cristal)
            .border(
                width = 2.dp, // Un poquito más grueso para que se note el borde
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassBorder.copy(alpha = 0.9f), // Arriba muy brillante
                        Color.Transparent,              // Centro invisible
                        GlassBorder.copy(alpha = 0.5f)  // Abajo algo brillante
                    )
                ),
                shape = shape
            )
            .padding(24.dp), // Más espacio interno para que el texto respire
        content = content
    )
}