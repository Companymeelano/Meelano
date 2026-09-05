package com.example.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design tokens for layout rhythm.
 *
 * Screens previously mixed 6/8/10/12/14/18/20dp gaps, which reads as visual
 * noise. Everything now snaps to this 4dp-based scale so related elements group
 * tightly and unrelated sections separate clearly.
 */
object Spacing {
    /** Between tightly coupled elements (a label and its value). */
    val Tight = 4.dp

    /** Inside a component (icon to text, row to row within one card). */
    val Small = 8.dp

    /** Default gap between sibling components. */
    val Medium = 12.dp

    /** Between distinct sections of a screen. */
    val Large = 20.dp

    /** Around the outer edge of a screen, and before the footer. */
    val Screen = 16.dp

    /** Corner radii, kept consistent so nothing looks accidentally different. */
    val CornerSmall = 12.dp
    val CornerMedium = 16.dp
    val CornerLarge = 22.dp
}
