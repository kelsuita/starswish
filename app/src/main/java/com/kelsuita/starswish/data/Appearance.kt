package com.starsavings.app.data

import kotlinx.serialization.Serializable

/** Theme 0 follows system; banner 0 is light, 1 is dark. */
@Serializable
data class Appearance(
    val themeMode: Int = 0,
    val accentIndex: Int = 0,
    val bannerStyle: Int = 0,
    val featuredGoalId: String = "",
    val compactList: Boolean = false,
    val paletteStyle: Int = 0,
    val colorSpec: Int = 1,
    val blurEnabled: Boolean = false,
    val pureBlackBackground: Boolean = false,
)
