package com.kelsuita.starswish.data

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
    val floatingNavBar: Boolean = false,
    /** 0 is the Miuix floating bar, 1 is the iOS-like floating bar. */
    val floatingNavBarStyle: Int = 0,
    /** Use the platform predictive-back animation for the tab pager. */
    val predictiveBackEnabled: Boolean = true,
)
