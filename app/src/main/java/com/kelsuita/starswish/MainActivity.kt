package com.kelsuita.starswish

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.squircle.squircleSurface
import com.kelsuita.starswish.data.*
import com.kelsuita.starswish.data.fetchHitokoto
import com.kelsuita.starswish.sync.XiaoxingAccessibilityService
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.UUID
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

private val Blue = Color(0xFF3975F6)
private val AccentColors = listOf(Blue, Color(0xFF2EAA8C), Color(0xFFB6573B), Color(0xFF8068C8))
@Serializable
private data class AppRoute(val page: Int) : NavKey
private val NavIcons = listOf(MiuixIcons.Home, MiuixIcons.BankCards, MiuixIcons.Settings)
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
        setContent {
            val appearance = remember(revision) { store.appearance() }
            val dark = when (appearance.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            // Null selects the platform wallpaper palette only for the Monet option.
            val seed = AccentColors.getOrNull(appearance.accentIndex - 2)
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
                    SavingsApp(store, revision, { revision++ }, appearance, dark, ::isReaderEnabled, ::openReaderSettings, ::openXiaoxing,
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

@Composable
private fun SavingsApp(
    store: SavingsStore, revision: Int, refresh: () -> Unit,
    appearance: Appearance,
    dark: Boolean,
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
    val expandedNavigation = configuration.screenWidthDp >= 1000
    val goalGrid = configuration.screenWidthDp >= 840
    val accountGrid = configuration.screenWidthDp >= 1000
    val layoutDirection = LocalLayoutDirection.current
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    var hitokoto by remember { mutableStateOf(store.hitokoto()) }
    var quoteLoading by remember { mutableStateOf(false) }
    var quoteRetry by remember { mutableIntStateOf(0) }
    LaunchedEffect(quoteRetry) {
        if (hitokoto == null || quoteRetry > 0) {
            quoteLoading = true
            val fetched = fetchHitokoto()
            if (fetched != null) {
                store.saveHitokoto(fetched)
                hitokoto = fetched
            } else if (hitokoto == null) {
                // Keep the failure in memory only: the next launch should retry instead of
                // treating an error message as saved content (and exporting it in backups).
                hitokoto = "一言暂时不可用，点击重试"
            }
            quoteLoading = false
        }
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
    var editingGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var goalEditor by rememberSaveable { mutableStateOf(false) }
    var editingAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    var accountEditor by rememberSaveable { mutableStateOf(false) }
    var disclosure by rememberSaveable { mutableStateOf(false) }
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
                duration = SnackbarDuration.Short,
            )
        }
    }
    fun mutate(block: () -> Unit): Boolean = runCatching { block(); refresh(); true }
        .getOrElse { notify(it.message ?: "操作失败，请重试"); false }
    // Reordering is visually complete before this runs. Persist it on a background thread so
    // the synchronous SharedPreferences commit cannot stall the frame after the card settles.
    fun commitSortOrder(previous: List<String>, write: () -> Unit, restore: (List<String>) -> Unit) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { sortCommitLock.withLock { write() } }
            }
            if (result.isSuccess) refresh() else {
                restore(previous)
                notify(result.exceptionOrNull()?.message ?: "操作失败，请重试")
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
                    checkNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "无法写入所选文件" }
                        .bufferedWriter(Charsets.UTF_8).use { it.write(text) }
                }
                notify("备份已导出")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                notify("导出失败：${error.message ?: "无法写入文件"}")
            } finally { backupBusy = false }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            backupBusy = true
            try {
                pendingBackupJson = withContext(Dispatchers.IO) {
                    val bytes = checkNotNull(context.contentResolver.openInputStream(uri)) { "无法读取所选文件" }
                        .use { input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= BackupData.MAX_BYTES) { "备份文件不能超过 1 MB" }
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                    require(bytes.size <= BackupData.MAX_BYTES) { "备份文件不能超过 1 MB" }
                    BackupData.fromJson(bytes.toString(Charsets.UTF_8)).toJson()
                }
                importVisible = true
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                notify("导入失败：${error.message ?: "无法读取文件"}")
            } finally { backupBusy = false }
        }
    }
    fun capture(account: BankAccount) {
        if (!readerEnabled()) { disclosure = true; return }
        if (mutate { store.startCapture(account.id) }) {
            if (!openXiaoxing()) {
                mutate { store.cancelCapture() }
                notify("未找到小星记账，请先安装后重试")
            } else notify("请打开「${account.name}」的账户详情页，显示余额后返回星愿确认")
        }
    }
    val accent = MiuixTheme.colorScheme.primary
    // Grouped cards use surfaceContainer; surface keeps their boundary visible in
    // the stock palette too (background and surfaceContainer are both white).
    val pageBackground = if (dark && appearance.pureBlackBackground) Color.Black else MiuixTheme.colorScheme.surface
    // Pager destinations share a single root route. Register their back handler above NavDisplay
    // so the entry's own navigation-event bridge cannot take priority over it.
    val pagerBackState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = pagerBackState,
        isBackEnabled = backStack.size == 1 && selectedTab != 0 &&
            !goalEditor && !accountEditor && !disclosure &&
            !deletingGoalVisible && !deletingAccountVisible && !importVisible,
        onBackCompleted = { navigate(0) },
    )
    val backProgress = (pagerBackState.transitionState as? NavigationEventTransitionState.InProgress)
        ?.latestEvent?.progress ?: 0f
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
    if (wideLayout) {
        NavigationRail(
            expanded = expandedNavigation,
            color = pageBackground,
            showDivider = true,
        ) {
            listOf("首页" to NavIcons[0], "账户" to NavIcons[1], "设置" to NavIcons[2]).forEachIndexed { index, item ->
                NavigationRailItem(
                    selected = railPage == index,
                    onClick = { navigate(index) },
                    icon = item.second,
                    label = item.first,
                )
            }
        }
    }
    NavDisplay(
        backStack = backStack,
        // Route transitions translate/scale the outgoing entry. In the wide shell the rail is a
        // sibling before NavDisplay, so an unclipped outgoing entry would paint over the rail and
        // leave a ghost there until the next full redraw.
        modifier = Modifier.weight(1f).fillMaxHeight().clipToBounds(),
        onBack = { backStack.removeLastOrNull() },
    ) {
      entry<AppRoute> { route ->
        val page = if (route.page >= 3) route.page else selectedTab
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
        val bottomBackdrop = if (!wideLayout && blurActive && route.page < 3) rememberLayerBackdrop {
            drawRect(pageBackground)
            drawContent()
        } else null
        val barColor = if (blurActive) Color.Transparent else pageBackground
        Scaffold(
          bottomBar = {
            if (!wideLayout && page < 3) {
                Box(Modifier.fillMaxWidth()
                    .then(if (bottomBackdrop != null) Modifier.textureBlur(bottomBackdrop, RectangleShape, BlurDefaults.BlurRadius,
                        colors = BlurDefaults.blurColors(blendColors = listOf(BlendColorEntry(pageBackground.copy(alpha = .8f))))) else Modifier)
                    .background(barColor)) {
                    NavigationBar(color = barColor) {
                        listOf("首页" to NavIcons[0], "账户" to NavIcons[1], "设置" to NavIcons[2]).forEachIndexed { index, item ->
                            // NavigationBarItem already applies weight(1f) inside its RowScope.
                            NavigationBarItem(selected = page == index, onClick = { navigate(index) },
                                icon = item.second, label = item.first,
                                colors = NavigationBarDefaults.navigationBarItemColors(selectedContentColor = accent))
                        }
                    }
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
                AdaptiveTopAppBar(
                    wide = wideLayout,
                    title = when (displayedPage) { 0 -> "星愿"; 1 -> "账户"; 2 -> "设置"; 4 -> "主题设置"; 5 -> "导入与导出"; 6 -> "目标排序"; 7 -> "账户排序"; else -> "关于" },
                    subtitle = "",
                    color = barColor,
                    scrollBehavior = contentScrollBehavior,
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
                                        Text(if (displayedPage == 1) "绑定小星记账中的人民币储蓄账户。"
                                            else if (quoteLoading && quoteRetry == 0) "一言加载中…" else hitokoto ?: "一言加载中…",
                                            modifier = Modifier.fillMaxWidth()
                                                .then(if (displayedPage == 2) Modifier.clickable(enabled = !quoteLoading) { quoteRetry++ } else Modifier)
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
                                DropdownItem("按名称排序", selected = sortCriterion == 0, onClick = { sortCriterion = 0; applySort(0, descending) }),
                                DropdownItem("按时间排序", selected = sortCriterion == 1, onClick = { sortCriterion = 1; applySort(1, descending) }),
                                DropdownItem(if (sortingGoals) "价格排序" else "余额排序", selected = sortCriterion == 2,
                                    onClick = { sortCriterion = 2; applySort(2, descending) }),
                            )),
                            DropdownEntry(listOf(DropdownItem("倒序", selected = descending, onClick = {
                                if (sortCriterion >= 0) applySort(sortCriterion, !descending)
                                else if (sortingGoals) mutate { store.reorderGoals(goals.map { it.id }.reversed()) }
                                else mutate { store.reorderAccounts(accounts.map { it.id }.reversed()) }
                                descending = !descending
                            }))),
                        ), collapseOnSelection = false) { Icon(MiuixIcons.More, contentDescription = "排序菜单", modifier = Modifier.size(24.dp)) }
                    }
                },
                navigationIcon = {
                    if (displayedPage >= 3) IconButton(onClick = { backStack.removeLastOrNull() }) {
                        Icon(MiuixIcons.Back, contentDescription = if (displayedPage == 6) "返回目标列表"
                            else if (displayedPage == 7) "返回账户列表" else "返回设置")
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
                        Text("目标列表", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        OverlayIconDropdownMenu(entry = DropdownEntry(listOf(
                            DropdownItem("新建目标", onClick = { editingGoalId = null; goalEditor = true }),
                            DropdownItem("排序", onClick = { navigate(6) }),
                        ))) { Icon(MiuixIcons.More, contentDescription = "目标菜单", modifier = Modifier.size(24.dp)) }
                    }
                    if (goals.isEmpty()) {
                        EmptyCard("star", "第一个心愿，从这里开始", "旅行、应急金、心仪的礼物……\n给目标起个名字，让积累看得见。", "创建我的目标") {
                            editingGoalId = null; goalEditor = true
                        }
                    }
                    val goalCard: @Composable (SavingGoal) -> Unit = { goal ->
                        GoalCard(goal, accounts,
                            onEdit = { editingGoalId = goal.id; goalEditor = true },
                            onDelete = { deletingGoal = goal; deletingGoalVisible = true },
                            onSync = { navigate(1) }, compact = appearance.compactList)
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
                    Note("目标之间独立计算；同一账户可关联多个目标，不代表资金已分开存放。")
                }
                1 -> {
                    if (candidate != null) {
                        val account = accounts.firstOrNull { it.id == candidate.accountId }
                        Panel {
                            Badge("等待你确认", accent)
                            Text(account?.name ?: "账户已删除", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("¥ ${Money.format(candidate.balance)}", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Note("识别于 ${timeLabel(candidate.capturedAt)}。请核对小星页面，确认后才会更新进度。")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { mutate { store.clearCandidate() } }, modifier = Modifier.weight(1f)) { Text("忽略") }
                                Button(onClick = { mutate { store.acceptCandidate() } }, enabled = account != null,
                                    modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()) { Text("确认同步") }
                            }
                        }
                    }
                    if (request != null && request.expiresAt > System.currentTimeMillis()) {
                        Panel {
                            Text("等待读取账户详情", fontWeight = FontWeight.Bold)
                            Note("两分钟内打开小星记账中的对应账户详情，检测到详情页后自动读取顶部人民币余额，再返回确认。")
                            Button(onClick = { mutate { store.cancelCapture() } }) { Text("取消本次读取") }
                        }
                    }
                    Panel {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconTile("link", accent)
                            Column(Modifier.weight(1f)) {
                                Text("小星记账", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(if (enabled) "页面读取已开启 · 按需读取" else "开启页面读取，同步账户余额", fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                            Badge(if (enabled) "已授权" else "未授权", accent)
                        }
                        Note("小星暂未提供公开的余额查询接口。此功能只识别你主动打开的账户详情，需要手动确认，不会在后台实时同步。")
                        Button(onClick = { disclosure = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (enabled) "查看读取说明与权限" else "设置读取权限")
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("我的账户", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        OverlayIconDropdownMenu(entry = DropdownEntry(listOf(
                            DropdownItem("添加账户", onClick = { editingAccountId = null; accountEditor = true }),
                            DropdownItem("排序", onClick = { navigate(7) }),
                        ))) { Icon(MiuixIcons.More, contentDescription = "账户菜单", modifier = Modifier.size(24.dp)) }
                    }
                    if (accounts.isEmpty()) EmptyCard("bank", "先连接一份积累", "添加与小星记账完全相同的账户名称，\n就能为攒钱目标提供余额。", "添加第一个账户") {
                        editingAccountId = null; accountEditor = true
                    }
                    val accountCard: @Composable (BankAccount) -> Unit = { account ->
                        Panel {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconTile("bank", accent)
                                Column(Modifier.weight(1f)) {
                                    Text(account.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text(account.source, fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                }
                            }
                            Text("¥ ${Money.format(account.balance)}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text("更新于 ${timeLabel(account.updatedAt)}", fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { capture(account) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColorsPrimary()) {
                                    Text("读取小星余额", fontSize = 13.sp)
                                }
                                Button(onClick = { editingAccountId = account.id; accountEditor = true }) { Text("编辑", fontSize = 13.sp) }
                                Button(onClick = { deletingAccount = account; deletingAccountVisible = true }) { Text("删除", fontSize = 13.sp) }
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
                    Note("仅支持人民币。余额来自记账数据或手动填写，与银行实际余额可能有差异。")
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
                    SmallTitle(text = "外观", textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        ArrowPreference(title = "主题设置", summary = "主题、强调色、首页样式与模糊效果", onClick = { navigate(4) })
                        SwitchPreference(title = "模糊", summary = if (android.os.Build.VERSION.SDK_INT >= 33) "启用顶栏和底栏的模糊效果" else "需要 Android 13 或更高版本",
                            enabled = android.os.Build.VERSION.SDK_INT >= 33,
                            checked = appearance.blurEnabled,
                            onCheckedChange = { value -> mutate { store.saveAppearance(appearance.copy(blurEnabled = value)) } })
                    }
                    SmallTitle(text = "备份", textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        ArrowPreference(title = "导入与导出", summary = "通过 JSON 文件备份或恢复数据", onClick = { navigate(5) })
                    }
                    SmallTitle(text = "应用", textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        ArrowPreference(title = "关于", summary = "星愿攒钱 · 使用说明与隐私", onClick = { navigate(3) })
                    }
                }
                4 -> {
                    SmallTitle(text = "主题", textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        OverlayDropdownPreference(title = "主题模式", summary = "选择应用的明暗模式",
                            items = listOf("跟随系统", "浅色", "深色"), selectedIndex = appearance.themeMode,
                            onSelectedIndexChange = { value -> mutate { store.saveAppearance(appearance.copy(themeMode = value)) } })
                        SwitchPreference(title = "深色模式纯黑背景", summary = "仅在深色模式下使用纯黑页面背景",
                            checked = appearance.pureBlackBackground,
                            onCheckedChange = { value -> mutate { store.saveAppearance(appearance.copy(pureBlackBackground = value)) } })
                        OverlayDropdownPreference(title = "强调色", summary = "选择应用强调色",
                            items = listOf("默认", "莫奈取色", "蓝色", "绿色", "橙色", "紫色"),
                            selectedIndex = appearance.accentIndex,
                            onSelectedIndexChange = { value -> mutate { store.saveAppearance(appearance.copy(accentIndex = value)) } })
                        OverlayDropdownPreference(title = "调色板风格", summary = "用于推导 MIUIX 调色板的色调算法",
                            enabled = appearance.accentIndex >= 2,
                            items = ThemePaletteStyle.entries.map { it.name }, selectedIndex = appearance.paletteStyle,
                            onSelectedIndexChange = { value -> mutate { store.saveAppearance(appearance.copy(paletteStyle = value)) } })
                        OverlayDropdownPreference(title = "色彩规范", summary = "Material 3 色彩规范版本",
                            enabled = appearance.accentIndex >= 2,
                            items = listOf("2021", "2025"), selectedIndex = appearance.colorSpec,
                            onSelectedIndexChange = { value -> mutate { store.saveAppearance(appearance.copy(colorSpec = value)) } })
                    }
                    SmallTitle(text = "首页", textColor = accent,
                        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        OverlayDropdownPreference(title = "首页横幅", items = listOf("浅色", "深色"),
                            selectedIndex = appearance.bannerStyle,
                            onSelectedIndexChange = { value -> mutate { store.saveAppearance(appearance.copy(bannerStyle = value)) } })
                        if (goals.isNotEmpty()) OverlayDropdownPreference(title = "横幅目标",
                            items = listOf("自动选择") + goals.map { it.title },
                            selectedIndex = goals.indexOfFirst { it.id == appearance.featuredGoalId }.let { if (it < 0) 0 else it + 1 },
                            onSelectedIndexChange = { value -> mutate { store.saveAppearance(appearance.copy(featuredGoalId = if (value == 0) "" else goals[value - 1].id)) } })
                        SwitchPreference(title = "紧凑目标列表", summary = "减少目标卡片的留白",
                            checked = appearance.compactList,
                            onCheckedChange = { value -> mutate { store.saveAppearance(appearance.copy(compactList = value)) } })
                    }
                }
                5 -> {
                    Card(Modifier.fillMaxWidth()) {
                        ArrowPreference(title = "导出备份", summary = "将账户、目标、外观设置和一言保存为 JSON 文件",
                            enabled = !backupBusy, onClick = {
                                exportLauncher.launch("starswish-backup-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())}.json")
                            })
                        ArrowPreference(title = "导入备份", summary = "选择 JSON 备份，确认后替换现有数据",
                            enabled = !backupBusy, onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) })
                    }
                    Note(if (backupBusy) "正在处理备份，请稍候…" else "备份文件为明文 JSON，包含账户名称与余额，请妥善保存。导入会替换现有账户、目标和外观设置，建议先导出当前数据。")
                    Note("备份不包含读取中的临时请求及待确认余额。无障碍服务权限由系统管理，换设备后需要重新开启。")
                }
                3 -> {
                    Panel {
                        IconTile("star", accent)
                        Text("星愿攒钱", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("$versionName  /  Made by Kelsuita & GPT6", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    Card(Modifier.fillMaxWidth()) {
                        ArrowPreference(title = "Github链接", summary = "查看项目，更新应用，看看Kelsuita在偷懒吗。", onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/kelsuita/starswish")))
                        })
                        ArrowPreference(title = "捐助我", summary = "请Kelsuita吃包辣条", onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://afdian.com/a/kelsuita")))
                        })
                    }
                    Panel {
                        Text("使用步骤", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        HelpStep("01", "添加账户", "名称须与小星记账的账户名称完全一致；不需要银行卡号、密码。")
                        HelpStep("02", "创建心愿", "自定义名称、金额、截止日期，选择关联账户及进度计算方式。")
                        HelpStep("03", "读取并确认", "点击读取，进入小星的账户详情页，再回到这里核对并确认余额。若页面不可识别，可手动更新。")
                    }
                    Panel {
                        Text("数据留在你的手机", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Note("无需登录。打开设置页时会向一言接口请求一句话；账户、目标与已确认余额只保存在应用私有存储，不会发送给一言接口。不收集页面截图，不保存完整页面文字。卸载应用会删除数据。")
                        Note("页面读取仅在你发起后两分钟内生效，并限定为小星记账。不会自动点击、修改账单、访问银行应用或执行转账。")
                        Button(onClick = { disclosure = true }, modifier = Modifier.fillMaxWidth()) { Text("管理读取权限") }
                    }
                    Panel {
                        Text("进度如何计算", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Note("「当前余额」：关联账户的余额合计。\n「从现在开始攒」：余额合计减去创建目标时的起始余额。若余额减少，进度也会减少；最低显示为 0%。")
                        Note("修改目标名称、金额、日期不会重置起点。修改关联账户或切换计算方式时，会重新记录起点。")
                    }
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://cxincx.com/support/scheme.html")))
                    }, modifier = Modifier.fillMaxWidth()) { Text("查看小星记账官方接口文档") }
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
        }
      }
    }
    }
    DialogHost(importVisible) { finished ->
        pendingBackup?.let { backup ->
            AppDialog(
                show = importVisible,
                title = "导入备份？",
                onDismiss = { if (!backupBusy) importVisible = false },
                onDismissFinished = { pendingBackupJson = null; finished() },
            ) {
                Note("备份包含 ${backup.accounts.size} 个账户、${backup.goals.size} 个目标，以及外观设置和一言。确认后将替换当前数据，无法撤销；建议先导出当前数据。")
                Button(onClick = {
                    scope.launch {
                        backupBusy = true
                        try {
                            withContext(Dispatchers.IO) { store.importBackup(backup) }
                            hitokoto = backup.hitokoto
                            importVisible = false
                            refresh()
                            notify("备份已导入")
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            notify("导入失败：${error.message ?: "保存失败"}")
                        } finally { backupBusy = false }
                    }
                }, enabled = !backupBusy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) { Text("确认替换并导入") }
                Button(onClick = { importVisible = false }, enabled = !backupBusy, modifier = Modifier.fillMaxWidth()) { Text("取消") }
            }
        }
    }
    DialogHost(goalEditor) { finished ->
        GoalEditor(
            show = goalEditor,
            existing = editingGoal,
            accounts = accounts,
            onDismiss = { goalEditor = false },
            onDismissFinished = { editingGoalId = null; finished() },
            onSave = { if (mutate { store.saveGoal(it) }) goalEditor = false },
            onAddAccount = { goalEditor = false; navigate(1); editingAccountId = null; accountEditor = true },
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
            title = "读取小星记账余额",
            onDismiss = { disclosure = false },
            onDismissFinished = finished,
        ) {
            Note("在系统设置启用「星愿攒钱 · 读取小星账户」后，应用会获得读取小星记账可见文字的能力。")
            Note("仅当你点击「读取小星余额」后，两分钟内识别指定账户详情中的名称及人民币余额。识别结果须返回本应用确认，才会更新目标。")
            Note("不会读取其他应用、收集截图或上传数据，不会执行点击、记账或转账。页面结构变化时可能无法识别，请使用手动更新。你可随时在系统设置关闭此权限。")
            Button(onClick = { disclosure = false; openReaderSettings() }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary()) { Text("了解并前往系统设置") }
            Button(onClick = { disclosure = false }, modifier = Modifier.fillMaxWidth()) { Text("暂不开启") }
        }
    }
    DialogHost(deletingGoalVisible) { finished ->
        deletingGoal?.let { goal ->
            AppDialog(
                show = deletingGoalVisible,
                title = "删除这个心愿？",
                onDismiss = { deletingGoalVisible = false },
                onDismissFinished = { deletingGoal = null; finished() },
            ) {
                Note("将删除「${goal.title}」，关联账户会保留。此操作无法撤销。")
                Button(onClick = { if (mutate { store.deleteGoal(goal.id) }) deletingGoalVisible = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error,
                        contentColor = MiuixTheme.colorScheme.onError)) { Text("删除") }
                Button(onClick = { deletingGoalVisible = false }, modifier = Modifier.fillMaxWidth()) { Text("取消") }
            }
        }
    }
    DialogHost(deletingAccountVisible) { finished ->
        deletingAccount?.let { account ->
            AppDialog(
                show = deletingAccountVisible,
                title = "删除这个账户？",
                onDismiss = { deletingAccountVisible = false },
                onDismissFinished = { deletingAccount = null; finished() },
            ) {
                Note("将删除「${account.name}」。如有目标关联此账户，请先编辑目标解除关联。")
                Button(onClick = { if (mutate { store.deleteAccount(account.id) }) deletingAccountVisible = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.error,
                        contentColor = MiuixTheme.colorScheme.onError)) { Text("删除") }
                Button(onClick = { deletingAccountVisible = false }, modifier = Modifier.fillMaxWidth()) { Text("取消") }
            }
        }
    }
}

@Composable private fun Overview(goals: List<SavingGoal>, accounts: List<BankAccount>, appearance: Appearance, accent: Color) {
    val featured = goals.firstOrNull { it.id == appearance.featuredGoalId } ?: goals.firstOrNull()
    val completed = featured != null && featured.saved(accounts) >= featured.target
    val progress by animateFloatAsState(featured?.progress(accounts) ?: 0f, tween(650), label = "featured-progress")
    val status = appearance.bannerStyle == 0
    val foreground = if (status) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onPrimary
    val subtle = foreground.copy(alpha = if (status) .7f else .82f)
    val base = if (status) MiuixTheme.colorScheme.primaryContainer else accent
    val fill = if (status) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onPrimary
    Box(Modifier.fillMaxWidth().squircleSurface(color = base, cornerRadius = 22.dp)
        .clearAndSetSemantics { contentDescription = "首页横幅 ${featured?.title ?: "尚无目标"} ${if (completed) "苦尽甘来" else "${(progress * 100).toInt()}%"}" }) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-18).dp).size(134.dp), contentAlignment = Alignment.Center) {
            if (completed) Text("🎉", fontSize = 86.sp, modifier = Modifier.graphicsLayer { alpha = .28f })
            else Symbol("star", foreground.copy(alpha = .12f), Modifier.size(120.dp))
        }
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("攒钱进度 · ${goals.size} 个目标", fontSize = 12.sp, color = subtle)
            Text(featured?.title ?: "创建你的第一个目标", fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold, color = foreground, maxLines = 2)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (featured == null) "先添加账户，再设定目标" else
                    "¥ ${Money.format(featured.saved(accounts))} / ¥ ${Money.format(featured.target)}",
                    fontSize = 13.sp, color = subtle, modifier = Modifier.weight(1f))
                Text(if (completed) "苦尽甘来" else "${(progress * 100).toInt()}%",
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, color = foreground)
            }
            LinearProgressIndicator(progress = progress, height = 9.dp,
                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                    foregroundColor = fill, backgroundColor = fill.copy(alpha = .20f)))
            if (featured != null) Text("${if (featured.remaining(accounts) == 0L) "目标已达成" else "还差 ¥ ${Money.format(featured.remaining(accounts))}"} · ${if (featured.deadline.isBlank()) "未设截止日期" else featured.deadline}",
                color = subtle, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable private fun GoalCard(goal: SavingGoal, accounts: List<BankAccount>, onEdit: () -> Unit, onDelete: () -> Unit, onSync: () -> Unit, compact: Boolean) {
    val color = MiuixTheme.colorScheme.primary
    val progress by animateFloatAsState(goal.progress(accounts), tween(650), label = "goal-progress")
    val saved = goal.saved(accounts)
    Panel(compact = compact) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile("star", color)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(goal.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(if (goal.mode == GoalMode.BALANCE) "按当前余额计算" else "从创建时开始积累", fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Badge(if (saved >= goal.target) "已达成" else "积累中", color)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("¥ ${Money.format(saved)}", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("目标 ¥ ${Money.format(goal.target)}", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Text("${(goal.progress(accounts) * 100).toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(progress = progress, height = 10.dp,
            modifier = Modifier.semantics { contentDescription = "${goal.title}完成进度" },
            colors = ProgressIndicatorDefaults.progressIndicatorColors(foregroundColor = color, backgroundColor = MiuixTheme.colorScheme.secondaryContainer))
        if (!compact) Text(if (saved >= goal.target) "心愿达成，值得为自己庆祝！" else "还差 ¥ ${Money.format(goal.remaining(accounts))}，继续慢慢靠近。",
            fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (!compact && goal.deadline.isNotBlank()) {
            val days = runCatching { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(goal.deadline)) }.getOrNull()
            val advice = when {
                saved >= goal.target -> "已提前完成这份心愿"
                days == null -> ""
                days < 0 -> "已超过计划日期，可编辑延长计划"
                days == 0L -> "今天是计划完成日"
                else -> "剩余 $days 天 · 每天约 ¥ ${Money.format((goal.remaining(accounts) + days - 1) / days)}"
            }
            Note("${goal.deadline}  /  $advice")
        }
        val linked = accounts.filter { it.id in goal.accountIds }
        if (!compact) Text(linked.joinToString(" · ") { it.name }, fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        if (!compact && goal.mode == GoalMode.GROWTH) Note("起始余额 ¥ ${Money.format(goal.baseline)}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSync, modifier = Modifier.weight(1f)) { Text("更新余额", fontSize = 12.sp) }
            Button(onClick = onEdit) { Text("编辑", fontSize = 12.sp) }
            Button(onClick = onDelete) { Text("删除", fontSize = 12.sp) }
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
        title = if (existing == null) "新建一个心愿" else "编辑心愿",
        onDismiss = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        TextField(value = title, onValueChange = { title = it.take(40); titleError = "" },
            label = "心愿名称，如：去看一次极光", singleLine = true,
            colors = dialogFieldColors(titleError.isNotEmpty()))
        FieldError(titleError)
        TextField(value = amount, onValueChange = { amount = it.take(18); amountError = "" }, label = "目标金额（元）", singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = dialogFieldColors(amountError.isNotEmpty()))
        FieldError(amountError)
        TextField(value = date, onValueChange = { date = it.take(10); dateError = "" }, label = "截止日期 YYYY-MM-DD（选填）", singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            colors = dialogFieldColors(dateError.isNotEmpty()))
        FieldError(dateError)
        Text("关联账户", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (accounts.isEmpty()) {
            Note("还没有账户，先添加一个账户再创建目标。")
            Button(onClick = onAddAccount, modifier = Modifier.fillMaxWidth()) { Text("先去添加账户") }
        }
        accounts.forEach { account ->
            val checked = account.id in selected
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(if (checked) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.secondaryContainer)
                .toggleable(value = checked, role = Role.Checkbox,
                    onValueChange = { selected = if (it) selected + account.id else selected - account.id; accountError = "" })
                .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Checkbox(state = if (checked) ToggleableState.On else ToggleableState.Off, onClick = null,
                    modifier = Modifier.clearAndSetSemantics {})
                Column(Modifier.weight(1f)) {
                    Text(account.name, fontSize = 14.sp)
                    Text("¥ ${Money.format(account.balance)}", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
        FieldError(accountError)
        Text("进度计算方式", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeOption("当前余额", !growth, Modifier.weight(1f)) { growth = false }
            ModeOption("从现在开始攒", growth, Modifier.weight(1f)) { growth = true }
        }
        Note(if (growth) "以当前合计余额为起点，只计算之后新增的积累。" else "以所选账户的当前余额合计作为已攒金额。")
        Note("目标进度条与图标跟随应用强调色，可在设置中统一调整。")
        Button(onClick = {
            val target = Money.parse(amount)
            val parsedDate = if (date.isBlank()) null else runCatching { LocalDate.parse(date) }.getOrNull()
            titleError = if (title.isBlank()) "给心愿起个名字吧" else ""
            amountError = if (target == null || target <= 0) "请输入大于 0 的有效金额，最多两位小数" else ""
            accountError = if (selected.isEmpty()) "请选择至少一个账户" else ""
            dateError = when {
                date.isNotBlank() && (parsedDate == null || parsedDate.toString() != date) ->
                    "日期格式应为 YYYY-MM-DD，例如 2027-12-31"
                parsedDate != null && parsedDate < LocalDate.now() && date != existing?.deadline ->
                    "请选择今天或之后的日期"
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
            Text(if (existing == null) "开启这个心愿" else "保存修改")
        }
    }
}

@Composable private fun RowScope.ModeOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(16.dp))
        .background(if (selected) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.secondaryContainer)
        .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.clearAndSetSemantics {})
        Text(label, fontSize = 13.sp)
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
        title = if (existing == null) "添加储蓄账户" else "编辑账户",
        onDismiss = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        Note("使用与小星记账完全一致的账户名称。初始余额请手动填写；之后可读取并确认更新。")
        TextField(value = name, onValueChange = { name = it.take(40); nameError = "" }, label = "账户名称，如：招商银行储蓄卡", singleLine = true,
            colors = dialogFieldColors(nameError.isNotEmpty()))
        FieldError(nameError)
        TextField(value = balance, onValueChange = { balance = it.take(18); balanceError = "" }, label = "当前人民币余额（元）", singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = dialogFieldColors(balanceError.isNotEmpty()))
        FieldError(balanceError)
        Note("仅支持人民币储蓄账户，不将信用卡额度当作存款。")
        Button(onClick = {
            val cents = Money.parse(balance)
            nameError = if (name.isBlank()) "请输入账户名称" else ""
            balanceError = if (cents == null) "请输入有效余额，最多两位小数" else ""
            if (nameError.isEmpty() && balanceError.isEmpty() && cents != null) {
                val balanceChanged = existing == null || cents != existing.balance
                onSave(BankAccount(existing?.id ?: UUID.randomUUID().toString(), name.trim(), cents,
                    if (balanceChanged) System.currentTimeMillis() else existing.updatedAt,
                    if (balanceChanged) "手动录入" else existing.source))
            }
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) { Text("保存账户") }
    }
}

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

/**
 * Wide windows use the pinned small app bar so the page starts closer to the top; the large
 * collapsing title would waste most of a landscape window's height. Both variants share the
 * same slots and scroll behaviour, so switching orientation keeps the bar state hoisted.
 */
@Composable private fun AdaptiveTopAppBar(
    wide: Boolean,
    title: String,
    subtitle: String = "",
    color: Color,
    scrollBehavior: ScrollBehavior,
    bottomContent: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
) {
    if (wide) {
        SmallTopAppBar(title = title, subtitle = subtitle, color = color,
            scrollBehavior = scrollBehavior, bottomContent = bottomContent,
            actions = actions, navigationIcon = navigationIcon)
    } else {
        TopAppBar(title = title, subtitle = subtitle, color = color,
            scrollBehavior = scrollBehavior, bottomContent = bottomContent,
            actions = actions, navigationIcon = navigationIcon)
    }
}

@Composable private fun AppDialog(
    show: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    WindowDialog(show = show, title = title, onDismissRequest = onDismiss, onDismissFinished = onDismissFinished) {
        Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()),
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
                    contentDescription = "拖动排序 $title"
                    customActions = listOf(
                        CustomAccessibilityAction("上移") { reorder(-1) },
                        CustomAccessibilityAction("下移") { reorder(1) },
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

@Composable private fun Panel(compact: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().animateContentSize(), cornerRadius = 20.dp) {
        Column(Modifier.padding(if (compact) 13.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 11.dp), content = content)
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
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(body, fontSize = 13.sp, lineHeight = 22.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            Button(onClick = onClick, colors = ButtonDefaults.buttonColorsPrimary()) { Text(action, fontSize = 14.sp) }
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
