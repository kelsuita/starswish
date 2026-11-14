package com.kelsuita.starswish

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.os.Bundle
import android.provider.Settings
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.pagerGestureOverride
import top.yukonga.miuix.kmp.utils.springAnimateToPage
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import com.kelsuita.starswish.data.*
import com.kelsuita.starswish.data.fetchHitokoto
import com.kelsuita.starswish.sync.XiaoxingAccessibilityService
import com.kelsuita.starswish.sync.WebDavClient
import com.kelsuita.starswish.sync.WebDavSettings
import com.kelsuita.starswish.sync.SnapshotBalanceReader
import com.kelsuita.starswish.sync.XiaoxingBackupDecryptor
import com.kelsuita.starswish.sync.SyncError
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

private val Blue = Color(0xFF3975F6)
private val AccentColors = listOf(Blue, Color(0xFF2EAA8C), Color(0xFFB6573B), Color(0xFF8068C8))
// Miuix only applies the palette style and colour spec when Monet is given a key colour; without
// one it returns the platform palette as-is. Read the wallpaper seed the platform palette is built
// from so those two settings work for Monet too. Null means the device has no wallpaper palette
// (Android 8-11), where Monet falls back to the stock Miuix colours.
@SuppressLint("InlinedApi")
private fun monetSeedColor(context: Context): Color? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) runCatching {
        val json = Settings.Secure.getString(context.contentResolver, "theme_customization_overlay_packages")
        val hex = json?.let { JSONObject(it).optString("android.theme.customization.system_palette") }.orEmpty()
        if (hex.isNotBlank()) {
            return Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
        }
    }
    return Color(context.resources.getColor(android.R.color.system_accent1_500, context.theme))
}
@Serializable
private data class AppRoute(val page: Int) : NavKey
/** One confirmed-but-not-yet-applied balance change shown in the import confirmation sheet. */
private data class WebDavBalanceChange(val accountId: String, val name: String, val before: Long, val after: Long)
private val NavIcons = listOf(MiuixIcons.Home, MiuixIcons.BankCards, MiuixIcons.Settings)
/** All transient notices share one short, consistent Snackbar duration. */
private val NoticeDuration = SnackbarDuration.Custom(durationMillis = 2000)
// A full backup can approach the Bundle transaction limit. Persist the pending import across
// recreation only when it is comfortably small; otherwise the user can pick the file again.
private const val PENDING_BACKUP_SAVE_LIMIT = 256 * 1024
private val PendingBackupSaver = Saver<String?, String>(
    save = { json -> json?.takeIf { it.length <= PENDING_BACKUP_SAVE_LIMIT } },
    restore = { it },
)

class MainActivity : ComponentActivity() {
    private var revision by mutableIntStateOf(0)
    private var confirmBalanceRequested by mutableStateOf(false)
    override fun onResume() { super.onResume(); revision++ }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        confirmBalanceRequested = intent.action == ACTION_CONFIRM_BALANCE
        revision++
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        confirmBalanceRequested = intent.action == ACTION_CONFIRM_BALANCE
        enableEdgeToEdge()
        val store = SavingsStore(this)
        // The language lives outside the appearance snapshot: every screen reads it through
        // Languages.tr while composing, so updating it here repaints without a restart.
        Languages.set(AppLanguage.entries[store.language()])
        setContent {
            val appearance = remember(revision) { store.appearance() }
            val dark = when (appearance.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            // Fixed accents derive their palette from the chosen seed. Monet derives it from the
            // wallpaper seed so the palette style and colour spec selections apply there as well;
            // without a seed Miuix would hand back the platform palette and ignore both settings.
            val monetSeed = remember { monetSeedColor(this@MainActivity) }
            val seed = if (appearance.accentIndex == 1) monetSeed else AccentColors.getOrNull(appearance.accentIndex - 2)
            val paletteStyle = ThemePaletteStyle.entries[appearance.paletteStyle]
            val colorSpec = ThemeColorSpec.entries[appearance.colorSpec]
            val controller = remember(seed, dark, paletteStyle, colorSpec, appearance.accentIndex) {
                ThemeController(
                    colorSchemeMode = when (appearance.accentIndex) {
                        0 -> if (dark) ColorSchemeMode.Dark else ColorSchemeMode.Light
                        1 -> ColorSchemeMode.MonetSystem
                        else -> if (dark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
                    },
                    keyColor = seed,
                    paletteStyle = paletteStyle,
                    colorSpec = colorSpec,
                    isDark = dark,
                )
            }
            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            MiuixTheme(controller = controller) {
                CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onBackground) {
                    SavingsApp(store, revision, { revision++ }, appearance, dark, monetSeed, ::isReaderEnabled, ::openReaderSettings, ::openXiaoxing,
                        confirmBalanceRequested, { confirmBalanceRequested = false; intent.action = null })
                }
            }
        }
    }
    private fun isReaderEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val component = ComponentName(this, XiaoxingAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == component }
    }
    private fun openReaderSettings() { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    private fun openXiaoxing(): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(XiaoxingAccessibilityService.XIAOXING_PACKAGE)
        return if (intent != null) {
            runCatching { startActivity(intent); true }.getOrDefault(false)
        } else false
    }
    companion object {
        const val ACTION_CONFIRM_BALANCE = "com.kelsuita.starswish.action.CONFIRM_BALANCE"
    }
}

@SuppressLint("NewApi")
@Composable
private fun SavingsApp(
    store: SavingsStore, revision: Int, refresh: () -> Unit,
    appearance: Appearance,
    dark: Boolean,
    monetSeed: Color?,
    readerEnabled: () -> Boolean, openReaderSettings: () -> Unit, openXiaoxing: () -> Boolean,
    confirmBalanceRequested: Boolean, onConfirmationOpened: () -> Unit,
) {
    val context = LocalContext.current
    val backStack = rememberNavBackStack<AppRoute>(AppRoute(0))
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    // Sort commits are serialized and written off the UI thread; SharedPreferences commit()
    // fsyncs and would otherwise drop a frame right after the drop animation.
    val sortCommitLock = remember { Mutex() }
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    val selectedTab = pagerState.targetPage
    // Compact/medium/expanded breakpoints follow the Miuix adaptive shell: phones keep the
    // bottom bar, landscape phones and tablets switch to a side rail, and the widest windows
    // expand the rail. Content stays centred instead of stretching across a large window.
    val configuration = LocalConfiguration.current
    val wideLayout = configuration.screenWidthDp >= 600
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    val useFloatingNav = appearance.floatingNavBar
    val expandedNavigation = configuration.screenWidthDp >= 1000
    // Use the expandable rail overload so Miuix supplies its own toggle button. The width only
    // supplies the initial state; afterwards the user owns it and it survives rotation.
    val railState = rememberNavigationRailState(
        initialValue = if (expandedNavigation) NavigationRailValue.Expanded else NavigationRailValue.Collapsed,
    )
    val goalGrid = configuration.screenWidthDp >= 840
    // Landscape phones stay below the tablet breakpoint but are still wide enough for two
    // account columns, so the account grid also opens up when the window is short and wide.
    val accountGrid = goalGrid || (landscape && configuration.screenWidthDp >= 640)
    val layoutDirection = LocalLayoutDirection.current
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    var hitokoto by remember { mutableStateOf(store.hitokoto()) }
    // The quote is a Chinese-only extra. A null choice follows the language default (on for
    // Chinese); once the user toggles it, that stored choice is kept across language switches.
    // Switching away from Chinese hides and effectively disables the quote without overwriting
    // the user's choice, so returning to Chinese can restore it unless it was manually disabled.
    var quoteChoice by remember { mutableStateOf(store.quoteEnabled()) }
    val quoteEnabled = quoteChoice ?: Languages.current.isChinese
    val quoteShown = Languages.current.isChinese && quoteEnabled
    var quoteLoading by remember { mutableStateOf(false) }
    // Fetches only when there is nothing cached; a manual tap refreshes regardless. Language
    // switches and the switch itself must not replace content the user is already reading.
    suspend fun loadQuote() {
        quoteLoading = true
        try {
            val fetched = fetchHitokoto()
            if (fetched != null) {
                store.saveHitokoto(fetched)
                hitokoto = fetched
            } else if (hitokoto == null) {
                // Keep the failure in memory only: the next launch should retry instead of
                // treating an error message as saved content (and exporting it in backups).
                hitokoto = tr("settings.quote_unavailable")
            }
        } finally {
            quoteLoading = false
        }
    }
    LaunchedEffect(quoteShown) {
        if (!quoteShown || hitokoto != null) return@LaunchedEffect
        loadQuote()
    }
    fun navigate(page: Int) {
        if (page >= 3) {
            if (backStack.lastOrNull() != AppRoute(page)) backStack.add(AppRoute(page))
        }
        else {
            while (backStack.size > 1) backStack.removeLastOrNull()
            navigationJob?.cancel()
            navigationJob = scope.launch { pagerState.springAnimateToPage(page) }
        }
    }
    fun popRoute() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }
    var editingGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var goalEditor by rememberSaveable { mutableStateOf(false) }
    var editingAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var accountEditor by rememberSaveable { mutableStateOf(false) }
    var disclosure by rememberSaveable { mutableStateOf(false) }
    var webDavSheet by rememberSaveable { mutableStateOf(false) }
    var webDavBusy by remember { mutableStateOf(false) }
    var webDavUrl by rememberSaveable { mutableStateOf("") }
    var webDavAccount by rememberSaveable { mutableStateOf("") }
    var webDavPassword by rememberSaveable { mutableStateOf("") }
    // True once the user edits the password field. Used to keep the stored "connected" flag
    // only when the credentials really changed, and to avoid overwriting typing during a
    // background decrypt.
    var webDavPasswordEdited by remember { mutableStateOf(false) }
    var webDavAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var webDavChanges by remember { mutableStateOf<List<WebDavBalanceChange>>(emptyList()) }
    var webDavChangesVisible by remember { mutableStateOf(false) }
    // Kept aside until the credential sheet has finished hiding: stacking two bottom sheets
    // leaves the outgoing one visible behind the confirmation and both disappear on drag.
    var pendingWebDavChanges by remember { mutableStateOf<List<WebDavBalanceChange>?>(null) }
    // The goal sheet can request the account sheet; open it only after the goal sheet is gone.
    var openAccountAfterGoal by remember { mutableStateOf(false) }
    var aboutIconTaps by rememberSaveable { mutableIntStateOf(0) }
    // Delete targets are intentionally not saved across recreation: the confirmation carries
    // no user input, so restoring it is not worth serializing a whole model object. Visibility
    // is separate from the target so the dialog keeps its content while it hides.
    var deletingGoal by remember { mutableStateOf<SavingGoal?>(null) }
    var deletingAccount by remember { mutableStateOf<BankAccount?>(null) }
    var deletingGoalVisible by remember { mutableStateOf(false) }
    var deletingAccountVisible by remember { mutableStateOf(false) }
    // The pending import is kept as JSON so the confirmation survives recreation.
    var pendingBackupJson by rememberSaveable(stateSaver = PendingBackupSaver) { mutableStateOf<String?>(null) }
    var importVisible by rememberSaveable { mutableStateOf(false) }
    // The banner decimal count is edited in a sheet, so the draft only hits storage on confirm.
    var bannerDecimalsSheet by remember { mutableStateOf(false) }
    var bannerDecimalsDraft by remember { mutableIntStateOf(0) }
    val hapticFeedback = LocalHapticFeedback.current
    val pendingBackup = remember(pendingBackupJson) {
        pendingBackupJson?.let { runCatching { BackupData.fromJson(it) }.getOrNull() }
    }
    // A backup too large for saved state is intentionally dropped on recreation; close the
    // confirmation instead of leaving an invisible dialog blocking back navigation.
    LaunchedEffect(importVisible, pendingBackup) {
        if (importVisible && pendingBackup == null) importVisible = false
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(confirmBalanceRequested) {
        if (confirmBalanceRequested) {
            navigate(1)
            goalEditor = false
            accountEditor = false
            disclosure = false
            deletingGoalVisible = false
            deletingAccountVisible = false
            importVisible = false
            refresh()
            onConfirmationOpened()
        }
    }
    val accounts = remember(revision) { store.accounts() }
    val goals = remember(revision) { store.goals() }
    val developerMode = remember(revision) { store.developerModeEnabled() }
    val webDavMode = remember(revision) { store.webDavBalanceEnabled() }
    val webDavConnected = remember(revision) { store.webDavConnected() }
    val webDavActive = developerMode && webDavMode
    val editingGoal = goals.firstOrNull { it.id == editingGoalId }
    val editingAccount = accounts.firstOrNull { it.id == editingAccountId }
    val candidate = remember(revision) { store.candidate() }
    val request = remember(revision) { store.request() }
    val enabled = remember(revision) { readerEnabled() }
    // Drop a pending read when its two-minute window closes; otherwise the "waiting to read"
    // panel would stay on screen until the next unrelated recomposition.
    LaunchedEffect(request?.expiresAt) {
        val expiresAt = request?.expiresAt ?: return@LaunchedEffect
        val remaining = expiresAt - System.currentTimeMillis()
        if (remaining > 0) delay(remaining)
        val stillExpired = store.request()?.let { it.expiresAt <= System.currentTimeMillis() } == true
        if (stillExpired) {
            runCatching { store.cancelCapture() }
            refresh()
        }
    }
    fun notify(text: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = text,
                withDismissAction = true,
                duration = NoticeDuration,
            )
        }
    }
    fun mutate(block: () -> Unit): Boolean = runCatching { block(); refresh(); true }
        .getOrElse { notify(it.message ?: tr("common.operation_failed")); false }
    // Reordering is visually complete before this runs. Persist it on a background thread so
    // the synchronous SharedPreferences commit cannot stall the frame after the card settles.
    fun commitSortOrder(previous: List<String>, write: () -> Unit, restore: (List<String>) -> Unit) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { sortCommitLock.withLock { write() } }
            }
            if (result.isSuccess) refresh() else {
                restore(previous)
                notify(result.exceptionOrNull()?.message ?: tr("common.operation_failed"))
            }
        }
    }
    var backupBusy by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            backupBusy = true
            try {
                val text = withContext(Dispatchers.IO) { store.exportBackup().toJson() }
                withContext(Dispatchers.IO) {
                    checkNotNull(context.contentResolver.openOutputStream(uri, "wt")) { tr("backup.error_write_selected") }
                        .bufferedWriter(Charsets.UTF_8).use { it.write(text) }
                }
                notify(tr("backup.exported"))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                notify(tr("backup.export_failed", error.message ?: tr("backup.error_write")))
            } finally { backupBusy = false }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            backupBusy = true
            try {
                pendingBackupJson = withContext(Dispatchers.IO) {
                    val bytes = checkNotNull(context.contentResolver.openInputStream(uri)) { tr("backup.error_read_selected") }
                        .use { input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= BackupData.MAX_BYTES) { tr("backup.error_over_1mb") }
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                    require(bytes.size <= BackupData.MAX_BYTES) { tr("backup.error_over_1mb") }
                    BackupData.fromJson(bytes.toString(Charsets.UTF_8)).toJson()
                }
                importVisible = true
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                notify(tr("backup.import_failed", error.message ?: tr("backup.error_read")))
            } finally { backupBusy = false }
        }
    }
    fun capture(account: BankAccount) {
        if (!readerEnabled()) { disclosure = true; return }
        if (mutate { store.startCapture(account.id) }) {
            if (!openXiaoxing()) {
                mutate { store.cancelCapture() }
                notify(tr("accessibility.error_xiaoxing_missing"))
            } else notify(tr("accessibility.open_account_page", account.name))
        }
    }
    fun beginWebDavRead(accountId: String? = null) {
        if (!store.developerModeEnabled() || !store.webDavBalanceEnabled()) return
        pendingWebDavChanges = null
        webDavAccountId = accountId
        webDavUrl = store.webDavUrl()
        webDavAccount = store.webDavAccount()
        webDavPasswordEdited = false
        webDavPassword = ""
        webDavSheet = true
        // Decrypting touches the keystore, so keep it off the UI thread. The sheet opens with an
        // empty password and fills in as soon as the stored secret is available.
        scope.launch {
            val stored = withContext(Dispatchers.IO) {
                runCatching { WebDavSettings.decryptPassword(store.encryptedWebDavPassword()) }
            }
            stored.exceptionOrNull()?.let { notify(it.message ?: tr("common.operation_failed")) }
            if (!webDavPasswordEdited) webDavPassword = stored.getOrDefault("")
        }
    }
    fun readWebDav(accountId: String? = null) {
        if (webDavBusy || webDavChangesVisible || !store.developerModeEnabled() || !store.webDavBalanceEnabled()) return
        val saveCredentials = webDavSheet
        val requestedAccountId = if (saveCredentials) webDavAccountId else accountId
        // Only a real credential edit should invalidate the stored "connected" state.
        val credentialsEdited = saveCredentials && (webDavPasswordEdited ||
            webDavUrl.trim() != store.webDavUrl() || webDavAccount.trim() != store.webDavAccount())
        webDavBusy = true
        scope.launch {
            val temp = File(context.cacheDir, "webdav-${UUID.randomUUID()}").apply { mkdirs() }
            var client: WebDavClient? = null
            try {
                val url = if (saveCredentials) webDavUrl else store.webDavUrl()
                val username = if (saveCredentials) webDavAccount else store.webDavAccount()
                val password = if (saveCredentials) webDavPassword
                    else withContext(Dispatchers.IO) { WebDavSettings.decryptPassword(store.encryptedWebDavPassword()) }
                if (username.isBlank() || password.isBlank()) throw SyncError("webdav.error_credentials")
                val webdav = WebDavClient(url, username, password)
                client = webdav
                val backup = webdav.latestBackup()
                val archive = File(temp, "backup.lch")
                val database = File(temp, "snapshot.mdb")
                withContext(Dispatchers.IO) { webdav.download(backup, archive) }
                withContext(Dispatchers.IO) { XiaoxingBackupDecryptor.decrypt(archive, database) }
                val balances = SnapshotBalanceReader.read(database)
                if (saveCredentials) withContext(Dispatchers.IO) {
                    store.saveWebDavSettings(url, username, WebDavSettings.encryptPassword(password), credentialsEdited)
                }
                val current = store.accounts().let { accounts ->
                    if (requestedAccountId == null) accounts else accounts.filter { it.id == requestedAccountId }
                }
                var matched = 0
                val changes = current.mapNotNull { account ->
                    val matches = balances.filter { it.name == account.name }
                    // Accounts this backup does not contain keep their current balance.
                    if (matches.isEmpty()) {
                        if (requestedAccountId != null) throw SyncError("webdav.error_empty_accounts")
                        return@mapNotNull null
                    }
                    if (matches.size != 1) throw SyncError("webdav.error_match", account.name)
                    val match = matches.single()
                    if (match.currency != 0) throw SyncError("webdav.error_currency", account.name)
                    matched++
                    if (match.cents == account.balance) null
                    else WebDavBalanceChange(account.id, account.name, account.balance, match.cents)
                }
                if (matched == 0) throw SyncError("webdav.error_empty_accounts")
                // Balance writes and the first successful sync require explicit confirmation.
                refresh()
                val fromCredentialsSheet = webDavSheet
                webDavSheet = false
                webDavAccountId = null
                if (fromCredentialsSheet) {
                    // Wait for the credential sheet's hide animation, then reveal the confirmation.
                    pendingWebDavChanges = changes
                } else {
                    webDavChanges = changes
                    webDavChangesVisible = true
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.w("StarwishWebDav", "WebDAV balance import failed", error)
                if (error is SyncError && error.key == "webdav.error_auth") {
                    // Rejected credentials must not keep showing as a working connection.
                    runCatching { store.markWebDavDisconnected() }
                    refresh()
                }
                val message = if (error is SyncError) error.arg?.let { tr(error.key, it) } ?: tr(error.key)
                    else tr("webdav.error_read")
                notify(message)
            } finally {
                client?.close()
                temp.deleteRecursively()
                webDavBusy = false
            }
        }
    }
    fun confirmWebDavChanges() {
        if (!webDavChangesVisible) return
        val changes = webDavChanges
        if (!mutate {
            if (changes.isNotEmpty()) {
                store.saveAccountBalances(changes.associate { it.accountId to it.after }, System.currentTimeMillis())
            }
            store.markWebDavConnected()
        }) return
        webDavChangesVisible = false
        notify(tr("webdav.success", changes.size))
    }
    val accent = MiuixTheme.colorScheme.primary
    // Grouped cards use surfaceContainer; surface keeps their boundary visible in
    // the stock palette too (background and surfaceContainer are both white).
    val pageBackground = if (dark && appearance.pureBlackBackground) Color.Black else MiuixTheme.colorScheme.surface
    // Pager destinations share a single root route. Register their back handler above NavDisplay
    // so the entry's own navigation-event bridge cannot take priority over it.
    val pagerBackState = rememberNavigationEventState(NavigationEventInfo.None)
    val pagerBackEnabled = backStack.size == 1 && selectedTab != 0 &&
        !goalEditor && !accountEditor && !disclosure &&
        !deletingGoalVisible && !deletingAccountVisible && !importVisible && !bannerDecimalsSheet
    if (appearance.predictiveBackEnabled) {
        NavigationBackHandler(
            state = pagerBackState,
            isBackEnabled = pagerBackEnabled,
            onBackCompleted = { navigate(0) },
        )
    } else {
        // Keep normal back navigation while opting out of the predictive transition.
        BackHandler(enabled = pagerBackEnabled) { navigate(0) }
    }
    val pagerBackInProgress = pagerBackState.transitionState as? NavigationEventTransitionState.InProgress
    val backProgress = if (appearance.predictiveBackEnabled && pagerBackEnabled) {
        pagerBackInProgress?.latestEvent?.progress ?: 0f
    } else 0f

    // NavDisplay has its own predictive handler for pushed routes, and the activity can animate
    // to home as well. When the preference is off, take over back with a plain high-priority
    // callback so no predictive gesture starts anywhere in the app. Dialogs keep their own
    // handlers; the pager root uses BackHandler below on older APIs.
    val blockPredictive = !appearance.predictiveBackEnabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !goalEditor && !accountEditor && !disclosure &&
        !deletingGoalVisible && !deletingAccountVisible && !importVisible && !bannerDecimalsSheet
    if (blockPredictive) {
        val activity = context as? ComponentActivity
        DisposableEffect(activity, backStack.size, selectedTab, goalEditor, accountEditor, disclosure,
            deletingGoalVisible, deletingAccountVisible, importVisible) {
            val callback = OnBackInvokedCallback {
                when {
                    backStack.size > 1 -> popRoute()
                    selectedTab != 0 -> navigate(0)
                    else -> activity?.finish()
                }
            }
            activity?.onBackInvokedDispatcher?.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_OVERLAY, callback,
            )
            onDispose {
                activity?.onBackInvokedDispatcher?.unregisterOnBackInvokedCallback(callback)
            }
        }
    }
    // Miuix owns the cross-cutting effect layer: the device screen corner clips the sliding page on
    // a phone, while a wide-shell pane is not a screen edge and keeps square corners. The backdrop
    // fills whatever the scaled-down layers reveal so it reads as the page background.
    val navCornerRadius = rememberNavSystemCornerRadius()
    val navEffects = remember(wideLayout, navCornerRadius, pageBackground) {
        NavDisplayEffects(
            cornerClipRadius = if (wideLayout) 0.dp else navCornerRadius,
            backdropColor = pageBackground,
        )
    }
    // Secondary pages keep the rail visible and map back to the tab that owns them, so the
    // window-size change does not introduce a second navigation state. Tab destinations all
    // live in the pager under AppRoute(0), so only a pushed secondary route overrides the tab.
    val topRoute = (backStack.lastOrNull() as? AppRoute)?.page ?: 0
    val railPage = if (backStack.size > 1 && topRoute >= 3) when (topRoute) {
        6 -> 0
        7 -> 1
        else -> 2
    } else selectedTab
    Row(Modifier.fillMaxSize()) {
    if (wideLayout && !useFloatingNav) {
        NavigationRail(
            state = railState,
            color = pageBackground,
            showDivider = true,
            expandContentDescription = tr("nav.expand"),
            collapseContentDescription = tr("nav.collapse"),
        ) {
            listOf(tr("nav.home") to NavIcons[0], tr("account.title") to NavIcons[1], tr("settings.title") to NavIcons[2]).forEachIndexed { index, item ->
                NavigationRailItem(
                    selected = railPage == index,
                    onClick = { navigate(index) },
                    icon = item.second,
                    label = item.first,
                )
            }
        }
    }
    BoxWithConstraints(
        // Route transitions translate/scale the outgoing entry. In the wide shell the rail is a
        // sibling before NavDisplay, so an unclipped outgoing entry would paint over the rail and
        // leave a ghost there until the next full redraw.
        modifier = Modifier.weight(1f).fillMaxHeight()
            // Nothing else paints the pane, so give it the page colour; the top bar and the
            // scrolling content apply their own display-cutout insets.
            .background(pageBackground)
            .clipToBounds(),
    ) {
    // The pane width is measured (rather than derived from the rail constants) so the title can
    // line up with the centred content column even when system insets widen the rail.
    val paneWidth = maxWidth
    // Export the complete pane once, then draw the glass bar as an overlay sibling. Keeping the
    // bar outside the exported layer is what lets it sample the page underneath without recursion.
    Box(
        Modifier.fillMaxSize(),
    ) {
    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        onBack = { popRoute() },
        transition = NavTransitions.MiuixDefault,
        effects = navEffects,
    ) {
      // NavDisplay owns both system predictive back and in-content swipe dismissal, including
      // cancellation and settling. Physical swipe directions must be selected explicitly for RTL.
      entry<AppRoute>(
          swipeDismiss = when {
              !appearance.predictiveBackEnabled -> NavSwipeDirection.None
              layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
              else -> NavSwipeDirection.LeftToRight
          },
      ) { route ->
        val page = if (route.page >= 3) route.page else selectedTab
        // Wide layouts keep the content in a centred 880dp column. Give the bar title the same
        // gutter so the large title starts on the column instead of floating at the rail edge.
        val barTitlePadding = if (wideLayout)
            ((paneWidth - 880.dp) * 0.5f).coerceAtLeast(0.dp) + 32.dp
        else TopAppBarDefaults.TitlePadding
        val blurActive = appearance.blurEnabled && android.os.Build.VERSION.SDK_INT >= 33 && isRuntimeShaderSupported()
        // A LayerBackdrop has one recording owner. Pager neighbours must not overwrite it.
        val backdrops = List(if (route.page < 3) 3 else 1) {
            if (blurActive) rememberLayerBackdrop {
                drawRect(pageBackground)
                drawContent()
            } else null
        }
        // The bottom bar samples the pager's stable parent, not an individual page.
        // A page's layer can be detached during tab animation or a rapid second tap.
        val bottomBackdrop = if (blurActive && (route.page < 3 || (wideLayout && useFloatingNav))) rememberLayerBackdrop {
            drawRect(pageBackground)
            drawContent()
        } else null
        val barColor = if (blurActive) Color.Transparent else pageBackground
        // The stock palette's primary is Miuix blue. In the default theme the selected tab stays
        // neutral instead, so the bottom bar matches the rest of the chrome (this reads as black in
        // the light theme and white in the dark one). Other accents keep colouring the selection.
        val navSelectedColor = if (appearance.accentIndex == 0) MiuixTheme.colorScheme.onBackground else accent
        Scaffold(
          bottomBar = {
            // Phones show the bar on the three tab pages only. A wide window normally uses the
            // rail, but the floating bar replaces it and therefore stays visible on secondary
            // pages as well, mapping the selection back to the owning tab like the rail does.
            val showBottomBar = if (wideLayout) useFloatingNav else page < 3
            if (showBottomBar) {
                val navItems = listOf(tr("nav.home") to NavIcons[0], tr("account.title") to NavIcons[1],
                    tr("settings.title") to NavIcons[2])
                val navIndex = if (wideLayout) railPage else page
                when {
                    !useFloatingNav -> Box(Modifier.fillMaxWidth()
                        .then(if (bottomBackdrop != null) Modifier.textureBlur(bottomBackdrop, RectangleShape, BlurDefaults.BlurRadius,
                            colors = BlurDefaults.blurColors(blendColors = listOf(BlendColorEntry(pageBackground.copy(alpha = .8f))))) else Modifier)
                        .background(barColor)) {
                        NavigationBar(color = barColor) {
                            navItems.forEachIndexed { index, item ->
                                // NavigationBarItem already applies weight(1f) inside its RowScope.
                                NavigationBarItem(selected = navIndex == index, onClick = { navigate(index) },
                                    icon = item.second, label = item.first,
                                    colors = NavigationBarDefaults.navigationBarItemColors(selectedContentColor = navSelectedColor))
                            }
                        }
                    }
                    appearance.floatingNavBarStyle == 0 -> FloatingNavigationBar {
                        navItems.forEachIndexed { index, item ->
                            FloatingNavigationBarItem(selected = navIndex == index, onClick = { navigate(index) },
                                icon = item.second, label = item.first,
                                colors = NavigationBarDefaults.navigationBarItemColors(selectedContentColor = navSelectedColor))
                        }
                    }
                    // The iOS-like bar lives in the entry's bottomBar, just like the Miuix
                    // floating bar, so it participates in predictive-back scene transitions.
                    else -> IosLiquidGlassNavigationBar(
                        items = navItems,
                        selectedIndex = navIndex,
                        onItemClick = { navigate(it) },
                        backdrop = bottomBackdrop,
                        isBlurActive = blurActive,
                        isDark = dark,
                    )
                }
            }
          },
          snackbarHost = { SnackbarHost(snackbarHostState) },
          containerColor = pageBackground,
          // Each page's TopAppBar owns the status-bar inset. Do not offset the pager.
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            val pageContent: @Composable (Int) -> Unit = { displayedPage ->
        val backdrop = backdrops[if (route.page < 3) displayedPage else 0]
        val contentScrollBehavior = MiuixScrollBehavior()
        val pageScrollState = key(displayedPage) { rememberScrollState() }
        var viewportTop by remember { mutableFloatStateOf(0f) }
        var viewportHeight by remember { mutableFloatStateOf(0f) }
        Scaffold(topBar = {
            Box(Modifier.fillMaxWidth()
                .then(if (backdrop != null) Modifier.textureBlur(backdrop, RectangleShape, BlurDefaults.BlurRadius,
                    colors = BlurDefaults.blurColors(blendColors = listOf(BlendColorEntry(pageBackground.copy(alpha = .8f))))) else Modifier)
                .background(if (backdrop != null) Color.Transparent else pageBackground)) {
                TopAppBar(
                    title = when (displayedPage) { 0 -> tr("app.name"); 1 -> tr("account.title"); 2 -> tr("settings.title"); 4 -> tr("theme.title"); 5 -> tr("settings.import_export"); 6 -> tr("sort.goals_title"); 7 -> tr("sort.accounts_title"); 8 -> tr("settings.developer_options"); else -> tr("about.title") },
                    subtitle = "",
                    color = barColor,
                    scrollBehavior = contentScrollBehavior,
                    titlePadding = barTitlePadding,
                    bottomContent = {
                        if (displayedPage == 1 || displayedPage == 2) {
                            // Unlike TopAppBar.subtitle, this caption does not reappear below the small title.
                            // On wide windows the bar spans the whole pane, so centre the caption on the
                            // same 880dp column as the cards to keep its left edge aligned with them.
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                                Box(Modifier.then(if (wideLayout) Modifier.widthIn(max = 880.dp) else Modifier)
                                    .fillMaxWidth()) {
                                    Box(Modifier.layout { measurable, constraints ->
                                        val placeable = measurable.measure(constraints)
                                        val visibleHeight = (placeable.height *
                                            (1f - contentScrollBehavior.state.collapsedFraction).coerceIn(0f, 1f)).toInt()
                                        layout(placeable.width, visibleHeight) { placeable.placeRelative(0, 0) }
                                    }.clipToBounds()) {
                                        val side = if (wideLayout) 32.dp else TopAppBarDefaults.TitlePadding
                                        val caption = if (displayedPage == 2 && quoteShown) {
                                            if (quoteLoading) tr("settings.quote_loading") else hitokoto ?: tr("settings.quote_loading")
                                        } else null
                                        if (caption != null) Text(caption,
                                            modifier = Modifier.fillMaxWidth()
                                                .then(if (displayedPage == 2) Modifier.clickable(enabled = !quoteLoading) {
                                                    scope.launch { loadQuote() }
                                                } else Modifier)
                                                .padding(start = side, end = side, bottom = 8.dp),
                                            fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                    }
                                }
                            }
                        }
                    },
                actions = {
                    if (displayedPage == 6 || displayedPage == 7) {
                        val sortingGoals = displayedPage == 6
                        var sortCriterion by rememberSaveable(displayedPage) { mutableIntStateOf(-1) }
                        var descending by rememberSaveable(displayedPage) { mutableStateOf(false) }
                        fun applySort(criterion: Int, isDescending: Boolean) {
                            if (sortingGoals) {
                                val sorted = when (criterion) {
                                    0 -> goals.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                                    1 -> goals.sortedBy { it.createdAt }
                                    else -> goals.sortedBy { it.target }
                                }
                                mutate { store.reorderGoals((if (isDescending) sorted.reversed() else sorted).map { it.id }) }
                            } else {
                                val sorted = when (criterion) {
                                    0 -> accounts.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                                    1 -> accounts.sortedBy { it.createdAt }
                                    else -> accounts.sortedBy { it.balance }
                                }
                                mutate { store.reorderAccounts((if (isDescending) sorted.reversed() else sorted).map { it.id }) }
                            }
                        }
                        OverlayIconDropdownMenu(entries = listOf(
                            DropdownEntry(listOf(
                                DropdownItem(tr("sort.name"), selected = sortCriterion == 0, onClick = { sortCriterion = 0; applySort(0, descending) }),
                                DropdownItem(tr("sort.time"), selected = sortCriterion == 1, onClick = { sortCriterion = 1; applySort(1, descending) }),
                                DropdownItem(if (sortingGoals) tr("sort.price") else tr("sort.balance"), selected = sortCriterion == 2,
                                    onClick = { sortCriterion = 2; applySort(2, descending) }),
                            )),
                            DropdownEntry(listOf(DropdownItem(tr("sort.descending"), selected = descending, onClick = {
                                if (sortCriterion >= 0) applySort(sortCriterion, !descending)
                                else if (sortingGoals) mutate { store.reorderGoals(goals.map { it.id }.reversed()) }
                                else mutate { store.reorderAccounts(accounts.map { it.id }.reversed()) }
                                descending = !descending
                            }))),
                        ), collapseOnSelection = false) { Icon(MiuixIcons.More, contentDescription = tr("sort.menu"), modifier = Modifier.size(24.dp)) }
                    }
                },
                navigationIcon = {
                    if (displayedPage >= 3) IconButton(onClick = { popRoute() }) {
                        Icon(MiuixIcons.Back, contentDescription = if (displayedPage == 6) tr("common.back_to_goals")
                            else if (displayedPage == 7) tr("common.back_to_accounts") else tr("common.back_to_settings"))
                    }
                })
            }
        }, containerColor = pageBackground) { pagePadding ->
        Box(Modifier.fillMaxSize().onGloballyPositioned {
            viewportTop = it.positionInWindow().y
            viewportHeight = it.size.height.toFloat()
        }
            .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)) {
        Column(Modifier.fillMaxHeight()
            .then(if (wideLayout) Modifier.widthIn(max = 880.dp) else Modifier)
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .background(pageBackground)
            .overScrollVertical()
            .nestedScroll(contentScrollBehavior.nestedScrollConnection)
            .verticalScroll(pageScrollState)
            .padding(start = if (wideLayout) 32.dp else maxOf(16.dp, pagePadding.calculateStartPadding(layoutDirection)),
                end = maxOf(if (wideLayout) 32.dp else 16.dp, pagePadding.calculateEndPadding(layoutDirection)),
                top = pagePadding.calculateTopPadding(),
                bottom = maxOf(padding.calculateBottomPadding(), pagePadding.calculateBottomPadding())),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(2.dp))
            when (displayedPage) {
                0 -> {
                    Overview(goals, accounts, appearance, accent)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        SmallTitle(tr("home.goal_list"), insideMargin = PaddingValues(start = 12.dp, top = 4.dp, bottom = 4.dp))
                        OverlayIconDropdownMenu(entry = DropdownEntry(listOf(
                            DropdownItem(tr("goal.new_goal"), onClick = { editingGoalId = null; goalEditor = true }),
                            DropdownItem(tr("sort.title"), onClick = { navigate(6) }),
                        ))) { Icon(MiuixIcons.More, contentDescription = tr("goal.menu"), modifier = Modifier.size(24.dp)) }
                    }
                    if (goals.isEmpty()) {
                        EmptyCard("star", tr("home.first_wish_here"), tr("goal.create_intro"), tr("goal.create_my_goal")) {
                            editingGoalId = null; goalEditor = true
                        }
                    }
                    val goalCard: @Composable (SavingGoal) -> Unit = { goal ->
                        GoalCard(goal, accounts,
                            onEdit = { editingGoalId = goal.id; goalEditor = true },
                            onDelete = { deletingGoal = goal; deletingGoalVisible = true },
                            onSync = { navigate(1) }, compact = appearance.compactList,
                            decimals = appearance.bannerDecimals)
                    }
                    if (goalGrid) {
                        // Wider windows use two card columns; the sort page remains a single
                        // column because its drag maths is slot based.
                        goals.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                pair.forEach { goal -> Box(Modifier.weight(1f)) { goalCard(goal) } }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        goals.forEach { goalCard(it) }
                    }
                }
                1 -> {
                    if (candidate != null) {
                        val account = accounts.firstOrNull { it.id == candidate.accountId }
                        Panel {
                            Badge(tr("accessibility.waiting_confirm"), accent)
                            Text(account?.name ?: tr("account.error_already_deleted"), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
                            Text("¥ ${Money.format(candidate.balance)}", style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.Bold)
                            Note(tr("accessibility.recognized_at", timeLabel(candidate.capturedAt)))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { mutate { store.clearCandidate() } }, modifier = Modifier.weight(1f)) { Text(tr("common.ignore")) }
                                Button(onClick = { mutate { store.acceptCandidate() } }, enabled = account != null,
                                    modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()) { Text(tr("accessibility.confirm_sync")) }
                            }
                        }
                    }
                    if (!webDavActive && request != null && request.expiresAt > System.currentTimeMillis()) {
                        Panel {
                            Text(tr("accessibility.waiting_page"), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
                            Note(tr("accessibility.read_steps_detail"))
                            Button(onClick = { mutate { store.cancelCapture() } }) { Text(tr("accessibility.cancel_read")) }
                        }
                    }
                    Panel {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconTile("link", accent)
                            Column(Modifier.weight(1f)) {
                                Text(tr("app.xiaoxing"), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
                                Text(when {
                                    webDavActive -> tr("account.webdav_read_summary")
                                    enabled -> tr("accessibility.read_enabled")
                                    else -> tr("accessibility.enable_read_summary")
                                }, style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                            Badge(when {
                                webDavActive -> if (webDavConnected) tr("account.webdav_connected")
                                    else tr("account.webdav_not_connected")
                                enabled -> tr("accessibility.granted")
                                else -> tr("accessibility.not_granted")
                            }, accent)
                        }
                        if (!webDavActive) {
                            Note(tr("accessibility.no_public_api_note"))
                            Button(onClick = {
                                if (enabled) {
                                    openReaderSettings()
                                    notify(tr("accessibility.disable_in_settings"))
                                } else {
                                    disclosure = true
                                }
                            }, modifier = Modifier.fillMaxWidth(),
                                colors = if (enabled) ButtonDefaults.buttonColorsPrimary()
                                else ButtonDefaults.buttonColors()) {
                                Text(if (enabled) tr("accessibility.disable_service") else tr("accessibility.set_up_permission"))
                            }
                        } else {
                            Button(onClick = { beginWebDavRead() }, modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColorsPrimary(), enabled = !webDavBusy) {
                                Text(tr("webdav.button"))
                            }
                            if (webDavConnected) {
                                Button(onClick = { readWebDav() }, modifier = Modifier.fillMaxWidth(), enabled = !webDavBusy) {
                                    Text(tr("webdav.reread"))
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        SmallTitle(tr("account.my_accounts"), insideMargin = PaddingValues(start = 12.dp, top = 4.dp, bottom = 4.dp))
                        OverlayIconDropdownMenu(entry = DropdownEntry(listOf(
                            DropdownItem(tr("account.add"), onClick = { editingAccountId = null; accountEditor = true }),
                            DropdownItem(tr("sort.title"), onClick = { navigate(7) }),
                        ))) { Icon(MiuixIcons.More, contentDescription = tr("account.menu"), modifier = Modifier.size(24.dp)) }
                    }
                    if (accounts.isEmpty()) EmptyCard("bank", tr("goal.connect_first"), tr("account.empty_intro"), tr("account.add_first_account")) {
                        editingAccountId = null; accountEditor = true
                    }
                    val accountCard: @Composable (BankAccount) -> Unit = { account ->
                        Panel {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconTile("bank", accent)
                                Column(Modifier.weight(1f)) {
                                    Text(account.name, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(tr(account.source), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                }
                            }
                            Text("¥ ${Money.format(account.balance)}", style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.Bold)
                            Text(tr("account.updated_at", timeLabel(account.updatedAt)), style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    if (webDavActive) {
                                        if (webDavConnected) {
                                            notify(tr("webdav.reading_toast"))
                                            readWebDav(account.id)
                                        } else beginWebDavRead(account.id)
                                    } else capture(account)
                                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()) {
                                    Text(tr("accessibility.read_balance"))
                                }
                                Button(onClick = { editingAccountId = account.id; accountEditor = true }) { Text(tr("common.edit")) }
                                Button(onClick = { deletingAccount = account; deletingAccountVisible = true }) { Text(tr("common.delete")) }
                            }
                        }
                    }
                    if (accountGrid) {
                        accounts.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                pair.forEach { account -> Box(Modifier.weight(1f)) { accountCard(account) } }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        accounts.forEach { accountCard(it) }
                    }
                }
                6 -> {
                    val goalOrder = remember(goals) { mutableStateOf(goals.map { it.id }) }
                    val goalDrag = remember { SortDragState() }
                    goalOrder.value.forEach { id -> key(id) {
                        val goal = goals.firstOrNull { it.id == id }
                        if (goal != null) {
                            SortCard(goal.title, Money.format(goal.target), goal.id, goalOrder,
                                pageScrollState, viewportTop, viewportHeight, goalDrag,
                                onCommit = { ids ->
                                    commitSortOrder(goals.map { it.id }, { store.reorderGoals(ids) }) {
                                        goalOrder.value = it
                                    }
                                })
                        }
                    } }
                }
                7 -> {
                    val accountOrder = remember(accounts) { mutableStateOf(accounts.map { it.id }) }
                    val accountDrag = remember { SortDragState() }
                    accountOrder.value.forEach { id -> key(id) {
                        val account = accounts.firstOrNull { it.id == id }
                        if (account != null) {
                            SortCard(account.name, Money.format(account.balance), account.id, accountOrder,
                                pageScrollState, viewportTop, viewportHeight, accountDrag,
                                onCommit = { ids ->
                                    commitSortOrder(accounts.map { it.id }, { store.reorderAccounts(ids) }) {
                                        accountOrder.value = it
                                    }
                                })
                        }
                    } }
                }
                2 -> {
                    SmallTitle(text = tr("settings.language"), textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        OverlayDropdownPreference(title = tr("settings.language"),
                            summary = if (Languages.choice == AppLanguage.FollowSystem)
                                tr("settings.follow_system_language", Languages.current.nativeName)
                            else tr("settings.choose_language"),
                            items = AppLanguage.entries.map {
                                if (it == AppLanguage.FollowSystem) tr("settings.follow_system") else it.nativeName
                            },
                            selectedIndex = Languages.choice.ordinal,
                            onSelectedIndexChange = { value -> mutate {
                                store.saveLanguage(value)
                                Languages.set(AppLanguage.entries[value])
                            } })
                    }
                    SmallTitle(text = tr("settings.appearance"), textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        ArrowPreference(title = tr("theme.title"), summary = tr("settings.theme_summary"), onClick = { navigate(4) })
                        if (Languages.current.isChinese) {
                            SwitchPreference(title = tr("settings.quote_enable"), summary = tr("settings.quote_summary"),
                                checked = quoteEnabled,
                                onCheckedChange = { value -> mutate {
                                    store.saveQuoteEnabled(value)
                                    quoteChoice = value
                                } })
                        }
                    }
                    SmallTitle(text = tr("settings.backup"), textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        ArrowPreference(title = tr("settings.import_export"), summary = tr("settings.import_export_summary"), onClick = { navigate(5) })
                    }
                    SmallTitle(text = tr("settings.app"), textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        ArrowPreference(title = tr("about.title"), summary = tr("about.help_and_privacy"), onClick = { navigate(3) })
                        if (developerMode) {
                            ArrowPreference(title = tr("settings.developer_options"),
                                summary = tr("settings.developer_options_summary"), onClick = { navigate(8) })
                        }
                    }
                }
                4 -> {
                    SmallTitle(text = tr("settings.theme"), textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        OverlayDropdownPreference(title = tr("theme.mode"), summary = tr("theme.choose_mode"),
                            items = listOf(tr("settings.follow_system"), tr("theme.light"), tr("theme.dark")), selectedIndex = appearance.themeMode,
                            onSelectedIndexChange = { value -> mutate { store.saveAppearance(store.appearance().copy(themeMode = value)) } })
                        SwitchPreference(title = tr("theme.pure_black"), summary = tr("theme.pure_black_summary"),
                            checked = appearance.pureBlackBackground,
                            onCheckedChange = { value -> mutate { store.saveAppearance(store.appearance().copy(pureBlackBackground = value)) } })
                        SwitchPreference(title = tr("theme.blur"), summary = if (android.os.Build.VERSION.SDK_INT >= 33) tr("settings.blur_summary") else tr("theme.needs_android13"),
                            enabled = android.os.Build.VERSION.SDK_INT >= 33,
                            checked = appearance.blurEnabled,
                            onCheckedChange = { value -> mutate { store.saveAppearance(store.appearance().copy(blurEnabled = value)) } })
                        OverlayDropdownPreference(title = tr("theme.accent"), summary = tr("theme.choose_accent"),
                            items = listOf(tr("theme.palette_default"), tr("theme.monet"), tr("theme.color_blue"), tr("theme.color_green"), tr("theme.color_orange"), tr("theme.color_purple")),
                            selectedIndex = appearance.accentIndex,
                            onSelectedIndexChange = { value -> mutate { store.saveAppearance(store.appearance().copy(accentIndex = value)) } })
                        AnimatedVisibility(
                            visible = appearance.accentIndex != 0,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                        ) {
                            Column {
                                OverlayDropdownPreference(title = tr("theme.palette_style"), summary = tr("theme.monet_summary"),
                                    enabled = appearance.accentIndex >= 2 || (appearance.accentIndex == 1 && monetSeed != null),
                                    items = ThemePaletteStyle.entries.map { it.name }, selectedIndex = appearance.paletteStyle,
                                    onSelectedIndexChange = { value -> mutate { store.saveAppearance(store.appearance().copy(paletteStyle = value)) } })
                                OverlayDropdownPreference(title = tr("theme.color_spec"), summary = tr("theme.material3_palette"),
                                    enabled = appearance.accentIndex >= 2 || (appearance.accentIndex == 1 && monetSeed != null),
                                    items = listOf("2021", "2025"), selectedIndex = appearance.colorSpec,
                                    onSelectedIndexChange = { value -> mutate { store.saveAppearance(store.appearance().copy(colorSpec = value)) } })
                            }
                        }
                        SwitchPreference(title = tr("settings.floating_bar_enabled"), summary = tr("settings.floating_bar_summary"),
                            checked = appearance.floatingNavBar,
                            onCheckedChange = { value -> mutate { store.saveAppearance(store.appearance().copy(floatingNavBar = value)) } })
                        AnimatedVisibility(visible = appearance.floatingNavBar) {
                            Column {
                                OverlayDropdownPreference(title = tr("settings.floating_bar_style"),
                                    items = listOf("miuix", "iOS-like"), selectedIndex = appearance.floatingNavBarStyle,
                                    onSelectedIndexChange = { value -> mutate { store.saveAppearance(store.appearance().copy(floatingNavBarStyle = value)) } })
                            }
                        }
                    }
                    SmallTitle(text = tr("nav.home"), textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        OverlayDropdownPreference(title = tr("home.banner"), items = listOf(tr("theme.light"), tr("theme.dark")),
                            selectedIndex = appearance.bannerStyle,
                            onSelectedIndexChange = { value -> mutate { store.saveAppearance(store.appearance().copy(bannerStyle = value)) } })
                        ArrowPreference(title = tr("settings.banner_decimals"),
                            summary = tr("settings.banner_decimals_summary", appearance.bannerDecimals),
                            onClick = {
                                bannerDecimalsDraft = appearance.bannerDecimals
                                bannerDecimalsSheet = true
                            })
                        if (goals.isNotEmpty()) OverlayDropdownPreference(title = tr("home.banner_goal"),
                            items = listOf(tr("theme.auto")) + goals.map { it.title },
                            selectedIndex = goals.indexOfFirst { it.id == appearance.featuredGoalId }.let { if (it < 0) 0 else it + 1 },
                            onSelectedIndexChange = { value -> mutate { store.saveAppearance(store.appearance().copy(featuredGoalId = if (value == 0) "" else goals[value - 1].id)) } })
                        SwitchPreference(title = tr("settings.compact_cards"), summary = tr("settings.compact_cards_summary"),
                            checked = appearance.compactList,
                            onCheckedChange = { value -> mutate { store.saveAppearance(store.appearance().copy(compactList = value)) } })
                        SwitchPreference(title = tr("settings.predictive_back"), summary = tr("settings.predictive_back_summary"),
                            checked = appearance.predictiveBackEnabled,
                            onCheckedChange = { value -> mutate { store.saveAppearance(store.appearance().copy(predictiveBackEnabled = value)) } })
                    }
                    OverlayBottomSheet(
                        show = bannerDecimalsSheet,
                        title = tr("settings.banner_decimals"),
                        startAction = {
                            IconButton(onClick = { bannerDecimalsSheet = false }) {
                                Icon(MiuixIcons.Close, contentDescription = tr("common.cancel"),
                                    tint = MiuixTheme.colorScheme.onBackground)
                            }
                        },
                        endAction = {
                            IconButton(onClick = {
                                mutate { store.saveAppearance(store.appearance().copy(bannerDecimals = bannerDecimalsDraft)) }
                                bannerDecimalsSheet = false
                            }) {
                                Icon(MiuixIcons.Ok, contentDescription = tr("common.confirm"),
                                    tint = MiuixTheme.colorScheme.onBackground)
                            }
                        },
                        onDismissRequest = { bannerDecimalsSheet = false },
                        insideMargin = DpSize(24.dp, 48.dp),
                    ) {
                        // The sheet paints itself with colorScheme.background. The stock palette sets
                        // surfaceContainer to that same color, so use [sheetCardColors] here too.
                        Card(Modifier.fillMaxWidth(), colors = sheetCardColors()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(tr("settings.banner_decimals_hint"), modifier = Modifier.weight(1f),
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                    Text(tr("settings.banner_decimals_value", bannerDecimalsDraft),
                                        style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = bannerDecimalsDraft.toFloat(),
                                    onValueChange = { raw ->
                                        val next = raw.roundToInt().coerceIn(0, 5)
                                        if (next != bannerDecimalsDraft) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                            bannerDecimalsDraft = next
                                        }
                                    },
                                    modifier = Modifier.pointerInput(Unit) {
                                        val sliderSize = size
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                            waitForUpOrCancellation(pass = PointerEventPass.Initial) ?: return@awaitEachGesture
                                            val thumbRadius = sliderSize.height / 2f
                                            val available = (sliderSize.width - 2f * thumbRadius).coerceAtLeast(1f)
                                            val fraction = ((down.position.x - thumbRadius) / available).coerceIn(0f, 1f)
                                            val next = (fraction * 5f).roundToInt().coerceIn(0, 5)
                                            if (next != bannerDecimalsDraft) {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                                bannerDecimalsDraft = next
                                            }
                                        }
                                    },
                                    valueRange = 0f..5f,
                                    steps = 4,
                                    showKeyPoints = true,
                                    keyPoints = listOf(0f, 1f, 2f, 3f, 4f, 5f),
                                    hapticEffect = SliderDefaults.SliderHapticEffect.None,
                                )
                            }
                        }
                    }
                }
                5 -> {
                    Card(Modifier.fillMaxWidth()) {
                        ArrowPreference(title = tr("backup.export"), summary = tr("backup.export_summary"),
                            enabled = !backupBusy, onClick = {
                                exportLauncher.launch("starswish-backup-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())}.json")
                            })
                        ArrowPreference(title = tr("backup.import"), summary = tr("settings.import_export_summary_short"),
                            enabled = !backupBusy, onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) })
                    }
                    if (backupBusy) Note(tr("backup.processing"))
                    Panel {
                        Text(tr("common.notice"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Note(tr("backup.plain_json_note"))
                        Note(tr("backup.excludes_note"))
                    }
                }
                3 -> {
                    Panel {
                        Box(Modifier.squircleClip(cornerRadius = 15.dp).clickable {
                            aboutIconTaps++
                            if (aboutIconTaps >= 10 && !store.developerModeEnabled()) {
                                aboutIconTaps = 0
                                if (mutate { store.setDeveloperMode(true) }) notify(tr("webdav.developer_enabled"))
                            }
                        }) { IconTile("star", accent) }
                        Text(tr("app.name_full"), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("$versionName  /  ${tr("about.made_by_kelsuita")}", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    Card(Modifier.fillMaxWidth()) {
                        ArrowPreference(title = tr("about.github_link"), summary = tr("about.project_note"), onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/kelsuita/starswish")))
                        })
                        ArrowPreference(title = tr("about.donate"), summary = tr("about.donate_summary"), onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://afdian.com/a/kelsuita")))
                        })
                    }
                    Panel {
                        Text(tr("accessibility.steps"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        HelpStep("01", tr("account.add"), tr("account.name_must_match"))
                        HelpStep("02", tr("goal.create_wish"), tr("goal.form_intro"))
                        HelpStep("03", tr("accessibility.read_and_confirm"), tr("accessibility.read_help"))
                    }
                    Panel {
                        Text(tr("about.data_stays_local"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Note(tr("about.privacy_note"))
                        Note(tr("accessibility.read_privacy_note"))
                        Button(onClick = { disclosure = true }, modifier = Modifier.fillMaxWidth()) { Text(tr("accessibility.manage_permission")) }
                    }
                    Panel {
                        Text(tr("goal.how_progress_works"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Note(tr("goal.calc_help_balance"))
                        Note(tr("goal.edit_help"))
                    }
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://cxincx.com/support/scheme.html")))
                    }, modifier = Modifier.fillMaxWidth()) { Text(tr("about.xiaoxing_docs")) }
                }
                8 -> {
                    Card(Modifier.fillMaxWidth()) {
                        SwitchPreference(title = tr("settings.developer_options"),
                            summary = tr("settings.developer_options_summary"),
                            checked = developerMode,
                            onCheckedChange = { value -> mutate { store.setDeveloperMode(value) } })
                    }
                    if (developerMode) {
                        SmallTitle(text = tr("settings.developer_accounts"), textColor = accent,
                            insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                        Card(Modifier.fillMaxWidth()) {
                            SwitchPreference(title = tr("settings.webdav_balance"),
                                summary = tr("settings.webdav_balance_summary"),
                                checked = webDavMode,
                                onCheckedChange = { value -> mutate { store.setWebDavBalanceEnabled(value) } })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        }
        }
        }
        Box(Modifier.fillMaxSize()
            .then(if (route.page < 3 && selectedTab != 0) Modifier.graphicsLayer {
                translationX = size.width * 0.06f * backProgress
                scaleX = 1f - 0.04f * backProgress
                scaleY = 1f - 0.04f * backProgress
            } else Modifier)) {
        Box(Modifier.fillMaxSize()
            .then(if (bottomBackdrop != null) Modifier.layerBackdrop(bottomBackdrop) else Modifier)) {
                if (route.page >= 3) pageContent(route.page)
                else HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().pagerGestureOverride(pagerState),
                    userScrollEnabled = false,
                    pageNestedScrollConnection = PagerGestureNestedScrollConnection,
                    flingBehavior = PagerDefaults.flingBehavior(pagerState, snapAnimationSpec = PagerNavigationSpringSpec),
                    verticalAlignment = Alignment.Top,
                    beyondViewportPageCount = 0,
                ) { displayedPage -> pageContent(displayedPage) }
            }
        }
        OverlayBottomSheet(
            show = webDavSheet,
            title = tr("webdav.title"),
            startAction = { IconButton(onClick = { webDavSheet = false }) { Icon(MiuixIcons.Close, contentDescription = tr("common.cancel"), tint = MiuixTheme.colorScheme.onBackground) } },
            endAction = { IconButton(onClick = { readWebDav() }, enabled = !webDavBusy) { Icon(MiuixIcons.Ok, contentDescription = tr("common.confirm"), tint = MiuixTheme.colorScheme.onBackground) } },
            onDismissRequest = { if (!webDavBusy) webDavSheet = false },
            onDismissFinished = {
                pendingWebDavChanges?.let { changes ->
                    pendingWebDavChanges = null
                    webDavChanges = changes
                    webDavChangesVisible = true
                }
            },
            insideMargin = DpSize(24.dp, 48.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                SmallTitle(text = tr("webdav.account_config"), textColor = accent,
                    insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                    ),
                ) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(value = webDavUrl, onValueChange = { webDavUrl = it }, label = tr("webdav.url"),
                            colors = sheetFieldColors())
                        TextField(value = webDavAccount, onValueChange = { webDavAccount = it }, label = tr("webdav.account"),
                            colors = sheetFieldColors())
                        TextField(value = webDavPassword, onValueChange = { webDavPassword = it; webDavPasswordEdited = true }, label = tr("webdav.password"),
                            visualTransformation = PasswordVisualTransformation(), singleLine = true,
                            colors = sheetFieldColors())
                    }
                }
                if (webDavBusy) Text(tr("webdav.reading"), color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        OverlayBottomSheet(
            show = webDavChangesVisible,
            title = tr("webdav.confirm_title"),
            startAction = { IconButton(onClick = { webDavChangesVisible = false }) { Icon(MiuixIcons.Close, contentDescription = tr("common.cancel"), tint = MiuixTheme.colorScheme.onBackground) } },
            endAction = { IconButton(onClick = { confirmWebDavChanges() }) { Icon(MiuixIcons.Ok, contentDescription = tr("common.confirm"), tint = MiuixTheme.colorScheme.onBackground) } },
            onDismissRequest = { webDavChangesVisible = false },
            onDismissFinished = { webDavChanges = emptyList() },
            insideMargin = DpSize(24.dp, 48.dp),
        ) {
            // The card's bottom edge sits 48dp above the screen bottom (sheet inside margin).
            Card(Modifier.fillMaxWidth(), colors = sheetCardColors()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (webDavChanges.isEmpty()) Text(tr("webdav.no_changes"))
                    webDavChanges.forEach { change ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(change.name, style = MiuixTheme.textStyles.body1, maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f))
                            Text("¥ ${Money.format(change.before)} → ¥ ${Money.format(change.after)}",
                                style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        }
      }
    }
    }
    }
    DialogHost(importVisible) { finished ->
        pendingBackup?.let { backup ->
            AppDialog(
                show = importVisible,
                title = tr("backup.import_title"),
                onDismiss = { if (!backupBusy) importVisible = false },
                onDismissFinished = { pendingBackupJson = null; finished() },
            ) {
                Note(tr("backup.confirm_summary", backup.accounts.size, backup.goals.size))
                Button(onClick = {
                    scope.launch {
                        backupBusy = true
                        try {
                            withContext(Dispatchers.IO) { store.importBackup(backup) }
                            hitokoto = backup.hitokoto
                            quoteChoice = backup.quoteEnabled
                            Languages.set(AppLanguage.entries[backup.language])
                            importVisible = false
                            refresh()
                            notify(tr("backup.imported"))
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            notify(tr("backup.import_failed", error.message ?: tr("common.save_failed")))
                        } finally { backupBusy = false }
                    }
                }, enabled = !backupBusy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) { Text(tr("backup.confirm_import")) }
                Button(onClick = { importVisible = false }, enabled = !backupBusy, modifier = Modifier.fillMaxWidth()) { Text(tr("common.cancel")) }
            }
        }
    }
    DialogHost(goalEditor) { finished ->
        GoalEditor(
            show = goalEditor,
            existing = editingGoal,
            accounts = accounts,
            onDismiss = { goalEditor = false },
            onDismissFinished = {
                editingGoalId = null
                finished()
                if (openAccountAfterGoal) {
                    openAccountAfterGoal = false
                    navigate(1)
                    editingAccountId = null
                    accountEditor = true
                }
            },
            onSave = { if (mutate { store.saveGoal(it) }) goalEditor = false },
            onAddAccount = { openAccountAfterGoal = true; goalEditor = false },
        )
    }
    DialogHost(accountEditor) { finished ->
        AccountEditor(
            show = accountEditor,
            existing = editingAccount,
            onDismiss = { accountEditor = false },
            onDismissFinished = { editingAccountId = null; finished() },
            onSave = { if (mutate { store.saveAccount(it) }) accountEditor = false },
        )
    }
    DialogHost(disclosure) { finished ->
        AppDialog(
            show = disclosure,
            title = tr("accessibility.read_ledger_balance"),
            onDismiss = { disclosure = false },
            onDismissFinished = finished,
        ) {
            Note(tr("accessibility.enable_in_settings_note"))
            Note(tr("accessibility.permission_summary"))
            Note(tr("accessibility.privacy_detail"))
            Button(onClick = { disclosure = false; openReaderSettings() }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary()) { Text(tr("accessibility.learn_and_open_settings")) }
            Button(onClick = { disclosure = false }, modifier = Modifier.fillMaxWidth()) { Text(tr("common.not_now")) }
        }
    }
    DialogHost(deletingGoalVisible) { finished ->
        deletingGoal?.let { goal ->
            AppDialog(
                show = deletingGoalVisible,
                title = tr("goal.delete_title"),
                onDismiss = { deletingGoalVisible = false },
                onDismissFinished = { deletingGoal = null; finished() },
            ) {
                Note(tr("goal.delete_message", goal.title))
                Button(onClick = { if (mutate { store.deleteGoal(goal.id) }) deletingGoalVisible = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error,
                        contentColor = MiuixTheme.colorScheme.onError)) { Text(tr("common.delete")) }
                Button(onClick = { deletingGoalVisible = false }, modifier = Modifier.fillMaxWidth()) { Text(tr("common.cancel")) }
            }
        }
    }
    DialogHost(deletingAccountVisible) { finished ->
        deletingAccount?.let { account ->
            AppDialog(
                show = deletingAccountVisible,
                title = tr("account.delete_title"),
                onDismiss = { deletingAccountVisible = false },
                onDismissFinished = { deletingAccount = null; finished() },
            ) {
                Note(tr("account.delete_linked_message", account.name))
                Button(onClick = { if (mutate { store.deleteAccount(account.id) }) deletingAccountVisible = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error,
                        contentColor = MiuixTheme.colorScheme.onError)) { Text(tr("common.delete")) }
                Button(onClick = { deletingAccountVisible = false }, modifier = Modifier.fillMaxWidth()) { Text(tr("common.cancel")) }
            }
        }
    }
}
}

/** Banner percentage: whole numbers at 0 decimals, otherwise exactly [decimals] places. */
private fun formatBannerPercent(progress: Float, decimals: Int): String {
    val percent = (progress * 100f).coerceIn(0f, 100f)
    return if (decimals <= 0) "${percent.toInt()}%"
    else String.format(Locale.ROOT, "%.${decimals.coerceIn(1, 5)}f%%", percent)
}

@Composable private fun Overview(goals: List<SavingGoal>, accounts: List<BankAccount>, appearance: Appearance, accent: Color) {
    val featured = goals.firstOrNull { it.id == appearance.featuredGoalId } ?: goals.firstOrNull()
    val completed = featured != null && featured.saved(accounts) >= featured.target
    val progress by animateFloatAsState(featured?.progress(accounts) ?: 0f, tween(650), label = "featured-progress")
    val percentText = formatBannerPercent(progress, appearance.bannerDecimals)
    val status = appearance.bannerStyle == 0
    val foreground = if (status) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onPrimary
    val subtle = foreground.copy(alpha = if (status) .7f else .82f)
    val base = if (status) MiuixTheme.colorScheme.primaryContainer else accent
    val fill = if (status) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onPrimary
    Card(
        modifier = Modifier.fillMaxWidth()
            .clearAndSetSemantics { contentDescription = tr("home.banner_description", featured?.title ?: tr("home.no_goal"), if (completed) tr("home.completed") else percentText) },
        cornerRadius = 20.dp,
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(color = base),
        pressFeedbackType = PressFeedbackType.Tilt,
        showIndication = false,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.align(Alignment.TopEnd).padding(18.dp)) {
                if (completed) Text("🎉", fontSize = 44.sp)
                else Symbol("star", foreground.copy(alpha = .22f), Modifier.size(44.dp))
            }
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("home.current_wish"), style = MiuixTheme.textStyles.footnote1, color = subtle)
                Text(featured?.title ?: tr("home.empty_create_goal"), style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold, color = foreground, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (featured == null) tr("account.add_before_goal") else
                        "¥ ${Money.format(featured.saved(accounts))} / ¥ ${Money.format(featured.target)}",
                        style = MiuixTheme.textStyles.footnote1, color = subtle, modifier = Modifier.weight(1f))
                    Text(if (completed) tr("home.completed") else percentText,
                        style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, color = foreground)
                }
                LinearProgressIndicator(progress = progress, height = 8.dp,
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = fill, backgroundColor = fill.copy(alpha = .20f)))
                if (featured != null) {
                    val remainingLabel = if (featured.remaining(accounts) == 0L) tr("goal.achieved")
                        else tr("goal.remaining_amount", Money.format(featured.remaining(accounts)))
                    Text(if (featured.deadline.isBlank()) remainingLabel
                        else tr("format.title_dot_detail", remainingLabel, featured.deadline),
                        color = subtle, style = MiuixTheme.textStyles.footnote2, maxLines = 1)
                }
            }
        }
    }
}

@Composable private fun GoalCard(goal: SavingGoal, accounts: List<BankAccount>, onEdit: () -> Unit, onDelete: () -> Unit, onSync: () -> Unit, compact: Boolean, decimals: Int) {
    val color = MiuixTheme.colorScheme.primary
    val progress by animateFloatAsState(goal.progress(accounts), tween(650), label = "goal-progress")
    val saved = goal.saved(accounts)
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile("star", color)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(goal.title, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(if (goal.mode == GoalMode.BALANCE) tr("goal.mode_balance") else tr("goal.count_from_creation"),
                    style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Badge(if (saved >= goal.target) tr("goal.completed") else tr("goal.in_progress"), color)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("¥ ${Money.format(saved)}", style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.Bold)
                Text(tr("goal.target_amount_line", Money.format(goal.target)), style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Text(formatBannerPercent(goal.progress(accounts), decimals), style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(progress = progress, height = 8.dp,
            modifier = Modifier.semantics { contentDescription = tr("home.progress_percent", goal.title) },
            colors = ProgressIndicatorDefaults.progressIndicatorColors(foregroundColor = color, backgroundColor = MiuixTheme.colorScheme.secondaryContainer))
        if (!compact) Text(if (saved >= goal.target) tr("home.goal_completed_celebration") else tr("home.remaining_amount_encourage", Money.format(goal.remaining(accounts))),
            style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (!compact && goal.deadline.isNotBlank()) {
            val days = runCatching { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(goal.deadline)) }.getOrNull()
            val advice = when {
                saved >= goal.target -> tr("home.completed_early")
                days == null -> ""
                days < 0 -> tr("goal.overdue")
                days == 0L -> tr("goal.due_today")
                else -> tr("goal.remaining_days", days, Money.format((goal.remaining(accounts) + days - 1) / days))
            }
            Note("${goal.deadline}  /  $advice")
        }
        val linked = accounts.filter { it.id in goal.accountIds }
        if (!compact) Text(linked.joinToString(" · ") { it.name }, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (!compact && goal.mode == GoalMode.GROWTH) Note(tr("goal.starting_balance", Money.format(goal.baseline)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSync, modifier = Modifier.weight(1f)) { Text(tr("account.update_balance")) }
            Button(onClick = onEdit) { Text(tr("common.edit")) }
            Button(onClick = onDelete) { Text(tr("common.delete")) }
        }
    }
}

@Composable private fun GoalEditor(
    show: Boolean,
    existing: SavingGoal?,
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onSave: (SavingGoal) -> Unit,
    onAddAccount: () -> Unit,
) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var amount by rememberSaveable(existing?.id) { mutableStateOf(existing?.let { Money.input(it.target) }.orEmpty()) }
    var date by rememberSaveable(existing?.id) { mutableStateOf(existing?.deadline.orEmpty()) }
    var selected by rememberSaveable(existing?.id) { mutableStateOf(existing?.accountIds?.toList() ?: emptyList<String>()) }
    var growth by rememberSaveable(existing?.id) { mutableStateOf(existing?.mode == GoalMode.GROWTH) }
    var titleError by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf("") }
    var dateError by remember { mutableStateOf("") }
    var accountError by remember { mutableStateOf("") }
    AppDialog(
        show = show,
        title = if (existing == null) tr("goal.new_wish") else tr("goal.edit_wish"),
        onDismiss = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        TextField(value = title, onValueChange = { title = it.take(40); titleError = "" },
            label = tr("goal.name_placeholder"), singleLine = true,
            colors = dialogFieldColors(titleError.isNotEmpty()))
        FieldError(titleError)
        TextField(value = amount, onValueChange = { amount = it.take(18); amountError = "" }, label = tr("goal.target_amount"), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = dialogFieldColors(amountError.isNotEmpty()))
        FieldError(amountError)
        TextField(value = date, onValueChange = { date = it.take(10); dateError = "" }, label = tr("goal.deadline_placeholder"), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            colors = dialogFieldColors(dateError.isNotEmpty()))
        FieldError(dateError)
        SmallTitle(text = tr("goal.linked_accounts"), textColor = MiuixTheme.colorScheme.primary,
            insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
        if (accounts.isEmpty()) {
            Note(tr("account.empty_before_goal"))
            Button(onClick = onAddAccount, modifier = Modifier.fillMaxWidth()) { Text(tr("account.add_first")) }
        } else {
            Card(Modifier.fillMaxWidth(), colors = sheetCardColors()) {
                accounts.forEach { account ->
                    CheckboxPreference(
                        title = account.name,
                        summary = "¥ ${Money.format(account.balance)}",
                        checked = account.id in selected,
                        onCheckedChange = { isChecked ->
                            selected = if (isChecked) selected + account.id else selected - account.id
                            accountError = ""
                        },
                    )
                }
            }
        }
        FieldError(accountError)
        SmallTitle(text = tr("goal.calculation_mode"), textColor = MiuixTheme.colorScheme.primary,
            insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
        Card(Modifier.fillMaxWidth(), colors = sheetCardColors()) {
            RadioButtonPreference(
                title = tr("goal.current_balance"),
                summary = tr("goal.mode_balance_summary"),
                selected = !growth,
                onClick = { growth = false },
            )
            RadioButtonPreference(
                title = tr("goal.start_now"),
                summary = tr("goal.mode_growth_summary"),
                selected = growth,
                onClick = { growth = true },
            )
        }
        Button(onClick = {
            val target = Money.parse(amount)
            val parsedDate = if (date.isBlank()) null else runCatching { LocalDate.parse(date) }.getOrNull()
            titleError = if (title.isBlank()) tr("goal.name_empty_error") else ""
            amountError = if (target == null || target <= 0) tr("goal.error_invalid_amount") else ""
            accountError = if (selected.isEmpty()) tr("goal.error_account_required") else ""
            dateError = when {
                date.isNotBlank() && (parsedDate == null || parsedDate.toString() != date) ->
                    tr("goal.error_date_format")
                parsedDate != null && parsedDate < LocalDate.now() && date != existing?.deadline ->
                    tr("goal.error_past_date")
                else -> ""
            }
            if (titleError.isEmpty() && amountError.isEmpty() && accountError.isEmpty() &&
                dateError.isEmpty() && target != null) {
                val mode = if (growth) GoalMode.GROWTH else GoalMode.BALANCE
                val unchanged = existing != null && existing.accountIds == selected.toSet() && existing.mode == mode
                val baseline = if (unchanged) existing.baseline else accounts.filter { it.id in selected }.sumOf { it.balance }
                onSave(SavingGoal(existing?.id ?: UUID.randomUUID().toString(), title.trim(), target,
                    selected.toSet(), mode, if (growth) baseline else 0, date, existing?.colorIndex ?: 0))
            }
        }, enabled = accounts.isNotEmpty(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) {
            Text(if (existing == null) tr("goal.start_wish") else tr("common.save_changes"))
        }
    }
}

@Composable private fun AccountEditor(
    show: Boolean,
    existing: BankAccount?,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onSave: (BankAccount) -> Unit,
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var balance by rememberSaveable(existing?.id) { mutableStateOf(existing?.let { Money.input(it.balance) }.orEmpty()) }
    var nameError by remember { mutableStateOf("") }
    var balanceError by remember { mutableStateOf("") }
    AppDialog(
        show = show,
        title = if (existing == null) tr("account.add_savings") else tr("account.edit"),
        onDismiss = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        Note(tr("account.name_hint_detail"))
        TextField(value = name, onValueChange = { name = it.take(40); nameError = "" }, label = tr("account.name_placeholder"), singleLine = true,
            colors = dialogFieldColors(nameError.isNotEmpty()))
        FieldError(nameError)
        TextField(value = balance, onValueChange = { balance = it.take(18); balanceError = "" }, label = tr("account.current_cny_balance"), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = dialogFieldColors(balanceError.isNotEmpty()))
        FieldError(balanceError)
        Note(tr("account.cny_only_note"))
        Button(onClick = {
            val cents = Money.parse(balance)
            nameError = if (name.isBlank()) tr("account.error_name_required") else ""
            balanceError = if (cents == null) tr("account.error_invalid_balance") else ""
            if (nameError.isEmpty() && balanceError.isEmpty() && cents != null) {
                val balanceChanged = existing == null || cents != existing.balance
                onSave(BankAccount(existing?.id ?: UUID.randomUUID().toString(), name.trim(), cents,
                    if (balanceChanged) System.currentTimeMillis() else existing.updatedAt,
                    if (balanceChanged) "account.source_manual" else existing.source))
            }
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) { Text(tr("account.save")) }
    }
}

/** Card colors that stay visible on a bottom-sheet background in the stock palette. */
@Composable private fun sheetCardColors(): CardColors {
    val surface = MiuixTheme.colorScheme.surfaceContainer
    return if (surface != MiuixTheme.colorScheme.background) {
        CardDefaults.defaultColors()
    } else {
        CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.secondaryContainer,
            contentColor = MiuixTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable private fun sheetFieldColors(): TextFieldColors = TextFieldDefaults.textFieldColors(
    backgroundColor = MiuixTheme.colorScheme.background,
    labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    borderColor = MiuixTheme.colorScheme.primary,
)

@Composable private fun dialogFieldColors(invalid: Boolean): TextFieldColors = TextFieldDefaults.textFieldColors(
    backgroundColor = if (invalid) MiuixTheme.colorScheme.errorContainer else MiuixTheme.colorScheme.secondaryContainer,
    labelColor = if (invalid) MiuixTheme.colorScheme.onErrorContainer else MiuixTheme.colorScheme.onSecondaryContainer,
    borderColor = if (invalid) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
)

@Composable private fun FieldError(text: String) {
    if (text.isNotEmpty()) Text(text, color = MiuixTheme.colorScheme.error, fontSize = 12.sp)
}

/**
 * Keeps a dialog composed until Miuix reports that its hide animation finished, so the exit
 * animation is not cut off by removing the composable. Draft state is keyed by the edited id,
 * so it is dropped together with the host and a later reopen starts from saved values.
 */
@Composable private fun DialogHost(show: Boolean, content: @Composable (onDismissFinished: () -> Unit) -> Unit) {
    var mounted by remember { mutableStateOf(show) }
    LaunchedEffect(show) { if (show) mounted = true }
    if (mounted) content { mounted = false }
}

@Composable private fun AppDialog(
    show: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    WindowBottomSheet(
        show = show,
        title = title,
        startAction = {
            IconButton(onClick = onDismiss) {
                Icon(MiuixIcons.Close, contentDescription = tr("common.cancel"),
                    tint = MiuixTheme.colorScheme.onBackground)
            }
        },
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        insideMargin = DpSize(24.dp, 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
        }
    }
}

@Composable private fun Symbol(kind: String, color: Color, modifier: Modifier) {
    val icon = when (kind) {
        "star" -> MiuixIcons.Favorites
        "bank" -> MiuixIcons.BankCards
        "link" -> MiuixIcons.Link
        else -> MiuixIcons.More
    }
    Icon(icon, contentDescription = null, modifier = modifier, tint = color)
}

/**
 * Shared by every card in one sortable page. The backing list is not reordered while a drag is
 * in progress; only these values change, and one final move is committed after the drop.
 */
private class SortDragState {
    var draggedId by mutableStateOf<String?>(null)
    var startIndex by mutableIntStateOf(-1)
    var targetIndex by mutableIntStateOf(-1)
    var startY by mutableFloatStateOf(0f)
    var pointerY by mutableFloatStateOf(0f)
    var scrollY by mutableFloatStateOf(0f)
    var stride by mutableFloatStateOf(0f)
    var settling by mutableStateOf(false)

    val active: Boolean get() = draggedId != null
    val distance: Float get() = pointerY - startY + scrollY

    fun begin(id: String, index: Int, y: Float, stride: Float) {
        draggedId = id
        startIndex = index
        targetIndex = index
        startY = y
        pointerY = y
        scrollY = 0f
        this.stride = stride
        settling = false
    }

    // A small hysteresis band around each midpoint stops a finger resting on a threshold from
    // swapping the same two slots back and forth.
    fun updateTarget(lastIndex: Int) {
        if (!active || stride <= 0f || lastIndex < 0) return
        val slot = startIndex + distance / stride
        val wanted = kotlin.math.round(slot).toInt().coerceIn(0, lastIndex)
        if (wanted == targetIndex) return
        val crossed = if (wanted > targetIndex) slot >= targetIndex + .58f
            else slot <= targetIndex - .58f
        if (crossed) targetIndex = wanted
    }

    fun end() { if (active) settling = true }

    fun clear() {
        draggedId = null
        startIndex = -1
        targetIndex = -1
        startY = 0f
        pointerY = 0f
        scrollY = 0f
        settling = false
    }
}

@Composable private fun SortCard(
    title: String, amount: String, id: String, order: MutableState<List<String>>, scrollState: ScrollState,
    viewportTop: Float, viewportHeight: Float, drag: SortDragState, onCommit: (List<String>) -> Unit,
) {
    val color = MiuixTheme.colorScheme.onSurfaceVariantActions
    val density = androidx.compose.ui.platform.LocalDensity.current
    var rowHeight by remember { mutableFloatStateOf(64f) }
    var handleCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val index = order.value.indexOf(id)
    val lastIndex = order.value.lastIndex
    val stride = rowHeight + with(density) { 12.dp.toPx() }
    val isDragged = drag.draggedId == id
    val dragActive = isDragged && drag.active
    val currentOnCommit by rememberUpdatedState(onCommit)
    // Committing a drop replaces the order state instance. pointerInput keeps its original
    // coroutine, so read the latest instance instead of the one captured at first composition.
    val currentOrder by rememberUpdatedState(order)

    // Other cards slide one slot while the list itself keeps its current order.
    val shiftTarget = when {
        !drag.active || dragActive -> 0f
        index > drag.startIndex && index <= drag.targetIndex -> -stride
        index >= drag.targetIndex && index < drag.startIndex -> stride
        else -> 0f
    }
    val animatedShift by animateFloatAsState(shiftTarget,
        animationSpec = if (drag.active) tween(140) else snap(), label = "sortShift")

    // The dragged card follows the finger. It stops at the first or last slot, so it cannot be
    // pulled into the empty space above or below the list.
    val slotOffset = (drag.targetIndex - drag.startIndex) * stride
    val followTarget = when {
        dragActive && !drag.settling ->
            drag.distance.coerceIn(-drag.startIndex * stride, (lastIndex - drag.startIndex) * stride)
        dragActive -> slotOffset
        else -> 0f
    }
    val follow by animateFloatAsState(followTarget,
        animationSpec = when {
            dragActive && !drag.settling -> snap()
            drag.active -> tween(170)
            else -> snap()
        }, label = "sortFollow")
    val lift by animateFloatAsState(if (dragActive) 1f else 0f,
        animationSpec = tween(160), label = "sortLift")

    // Animate the card onto its target slot first, then swap the list and clear the visual
    // offsets in the same frame. The layout jump equals the offset that has just reached zero.
    LaunchedEffect(drag.settling, dragActive) {
        if (dragActive && drag.settling) {
            delay(200)
            val from = drag.startIndex
            val to = drag.targetIndex
            if (from in currentOrder.value.indices && to in currentOrder.value.indices && from != to) {
                val next = currentOrder.value.toMutableList().apply { add(to, removeAt(from)) }
                currentOrder.value = next
                currentOnCommit(next)
            }
            drag.clear()
        }
    }

    val reorder: (Int) -> Boolean = { step ->
        val list = currentOrder.value
        val from = list.indexOf(id)
        val to = from + step
        if (drag.active || from !in list.indices || to !in list.indices) false else {
            val next = list.toMutableList().apply { add(to, removeAt(from)) }
            currentOrder.value = next
            currentOnCommit(next)
            true
        }
    }
    val edge = with(density) { 64.dp.toPx() }
    val direction = if (dragActive && !drag.settling && viewportHeight > 0f) when {
        drag.pointerY < viewportTop + edge -> -1
        drag.pointerY > viewportTop + viewportHeight - edge -> 1
        else -> 0
    } else 0
    LaunchedEffect(direction, scrollState) {
        while (direction != 0) {
            val scrolled = scrollState.scrollBy(direction * with(density) { 10.dp.toPx() })
            if (scrolled == 0f) break
            drag.scrollY += scrolled
            drag.updateTarget(currentOrder.value.lastIndex)
            delay(16)
        }
    }

    Card(Modifier.fillMaxWidth().zIndex(when {
        dragActive -> 2f
        animatedShift != 0f -> 1f
        else -> 0f
    })
        .onSizeChanged { rowHeight = it.height.toFloat() }
        .graphicsLayer {
            translationY = animatedShift + follow
            scaleX = 1f + .015f * lift
            scaleY = 1f + .015f * lift
            shadowElevation = with(density) { 8.dp.toPx() } * lift
        }) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = MiuixTheme.textStyles.body1)
            Spacer(Modifier.width(8.dp))
            Text("¥ $amount", modifier = Modifier.widthIn(max = 132.dp), fontSize = 13.sp, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Box(Modifier.size(48.dp).onGloballyPositioned { handleCoordinates = it }
                .semantics {
                    contentDescription = tr("sort.drag_handle", title)
                    customActions = listOf(
                        CustomAccessibilityAction(tr("common.move_up")) { reorder(-1) },
                        CustomAccessibilityAction(tr("common.move_down")) { reorder(1) },
                    )
                }
                .pointerInput(id) {
                    detectDragGestures(
                        onDragStart = { local ->
                            // A release animation lasts a few frames. If another gesture starts
                            // before it ends, commit the previous drop before taking over.
                            if (drag.settling) {
                                val from = drag.startIndex
                                val to = drag.targetIndex
                                if (from in currentOrder.value.indices &&
                                    to in currentOrder.value.indices && from != to) {
                                    val next = currentOrder.value.toMutableList().apply { add(to, removeAt(from)) }
                                    currentOrder.value = next
                                    currentOnCommit(next)
                                }
                                drag.clear()
                            }
                            val from = currentOrder.value.indexOf(id)
                            if (from >= 0) {
                                val start = handleCoordinates?.takeIf { it.isAttached }
                                    ?.localToWindow(local)?.y ?: 0f
                                drag.begin(id, from, start, rowHeight + with(density) { 12.dp.toPx() })
                            }
                        },
                        onDragEnd = {
                            if (drag.draggedId == id) drag.end()
                        },
                        onDragCancel = {
                            if (drag.draggedId == id) drag.clear()
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        // The handle sits inside the translated card, so read the real pointer
                        // position in window coordinates instead of accumulating moved layout.
                        val windowY = handleCoordinates?.takeIf { it.isAttached }
                            ?.localToWindow(change.position)?.y ?: (drag.pointerY + dragAmount.y)
                        drag.pointerY = windowY
                        drag.updateTarget(currentOrder.value.lastIndex)
                    }
                }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(22.dp)) {
                    val stroke = 1.8.dp.toPx()
                    listOf(.28f, .5f, .72f).forEach { fraction ->
                        val y = size.height * fraction
                        drawLine(color, Offset(size.width * .17f, y), Offset(size.width * .83f, y), stroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }
            }
        }
    }
}

@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().animateContentSize(),
        insideMargin = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(11.dp), content = content)
    }
}
@Composable private fun Note(text: String) {
    Text(text, fontSize = 12.sp, lineHeight = 20.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
}
@Composable private fun Badge(text: String, color: Color) {
    Box(Modifier.squircleSurface(color = color.copy(alpha = .12f), cornerRadius = 8.dp)
        .padding(horizontal = 9.dp, vertical = 5.dp)) {
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}
@Composable private fun IconTile(kind: String, color: Color) {
    Box(Modifier.size(44.dp).squircleSurface(color = color.copy(alpha = .1f), cornerRadius = 15.dp),
        contentAlignment = Alignment.Center) {
        Symbol(kind, color, Modifier.size(24.dp))
    }
}
@Composable private fun EmptyCard(kind: String, title: String, body: String, action: String, onClick: () -> Unit) {
    Panel {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            IconTile(kind, MiuixTheme.colorScheme.primary)
            Text(title, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
            Text(body, style = MiuixTheme.textStyles.footnote1, lineHeight = 22.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Button(onClick = onClick, colors = ButtonDefaults.buttonColorsPrimary()) { Text(action) }
        }
    }
}
@Composable private fun HelpStep(number: String, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Badge(number, MiuixTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Note(body)
        }
    }
}
private fun timeLabel(millis: Long): String = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(millis))
