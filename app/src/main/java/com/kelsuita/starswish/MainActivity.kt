package com.starsavings.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
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
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import com.starsavings.app.data.*
import com.kelsuita.starswish.data.fetchHitokoto
import com.starsavings.app.sync.XiaoxingAccessibilityService
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
private val NavIcons = listOf(
    ImageVector.Builder("首页", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f); lineTo(2f, 10f); lineTo(4f, 10f); lineTo(4f, 21f)
            lineTo(10f, 21f); lineTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 21f)
            lineTo(20f, 21f); lineTo(20f, 10f); lineTo(22f, 10f); close()
        }
    }.build(),
    ImageVector.Builder("账户", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f); lineTo(2f, 7f); lineTo(2f, 9f); lineTo(22f, 9f); lineTo(22f, 7f); close()
            moveTo(4f, 11f); lineTo(7f, 11f); lineTo(7f, 18f); lineTo(4f, 18f); close()
            moveTo(10.5f, 11f); lineTo(13.5f, 11f); lineTo(13.5f, 18f); lineTo(10.5f, 18f); close()
            moveTo(17f, 11f); lineTo(20f, 11f); lineTo(20f, 18f); lineTo(17f, 18f); close()
            moveTo(2f, 20f); lineTo(22f, 20f); lineTo(22f, 22f); lineTo(2f, 22f); close()
        }
    }.build(),
    ImageVector.Builder("设置", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.4f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) {
            moveTo(3f, 6f); lineTo(21f, 6f); moveTo(3f, 12f); lineTo(21f, 12f)
            moveTo(3f, 18f); lineTo(21f, 18f)
            moveTo(8f, 3f); lineTo(8f, 9f); moveTo(16f, 9f); lineTo(16f, 15f)
            moveTo(10f, 15f); lineTo(10f, 21f)
        }
    }.build()
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
        const val ACTION_CONFIRM_BALANCE = "com.starsavings.app.action.CONFIRM_BALANCE"
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
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    val selectedTab = pagerState.targetPage
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    var hitokoto by remember { mutableStateOf(store.hitokoto()) }
    var quoteLoading by remember { mutableStateOf(false) }
    var quoteRetry by remember { mutableIntStateOf(0) }
    LaunchedEffect(quoteRetry) {
        if (hitokoto == null || quoteRetry > 0) {
            // Record the first attempt before networking; opening settings or restarting never retries it.
            if (hitokoto == null) {
                hitokoto = "一言暂时不可用，点击重试"
                store.saveHitokoto(hitokoto!!)
            }
            quoteLoading = true
            fetchHitokoto()?.let { quote ->
                store.saveHitokoto(quote)
                hitokoto = quote
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
    var editingGoal by remember { mutableStateOf<SavingGoal?>(null) }
    var goalEditor by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<BankAccount?>(null) }
    var accountEditor by remember { mutableStateOf(false) }
    var disclosure by remember { mutableStateOf(false) }
    var deletingGoal by remember { mutableStateOf<SavingGoal?>(null) }
    var deletingAccount by remember { mutableStateOf<BankAccount?>(null) }
    LaunchedEffect(confirmBalanceRequested) {
        if (confirmBalanceRequested) {
            navigate(1)
            goalEditor = false
            accountEditor = false
            disclosure = false
            deletingGoal = null
            deletingAccount = null
            refresh()
            onConfirmationOpened()
        }
    }
    val accounts = remember(revision) { store.accounts() }
    val goals = remember(revision) { store.goals() }
    val candidate = remember(revision) { store.candidate() }
    val request = remember(revision) { store.request() }
    val enabled = remember(revision) { readerEnabled() }
    fun notify(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    fun mutate(block: () -> Unit): Boolean = runCatching { block(); refresh(); true }
        .getOrElse { notify(it.message ?: "操作失败，请重试"); false }
    var pendingBackup by remember { mutableStateOf<BackupData?>(null) }
    var backupBusy by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            backupBusy = true
            try {
                val text = store.exportBackup().toJson()
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
                pendingBackup = withContext(Dispatchers.IO) {
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
                    BackupData.fromJson(bytes.toString(Charsets.UTF_8))
                }
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
            !goalEditor && !accountEditor && !disclosure,
        onBackCompleted = { navigate(0) },
    )
    val backProgress = (pagerBackState.transitionState as? NavigationEventTransitionState.InProgress)
        ?.latestEvent?.progress ?: 0f
    NavDisplay(backStack = backStack, onBack = { backStack.removeLastOrNull() }) {
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
        val backdrop = backdrops[if (route.page < 3) selectedTab else 0]
        val barColor = if (blurActive) Color.Transparent else pageBackground
        Scaffold(
          bottomBar = {
            // Reattach the blur consumer once the pager has placed its final source.
            // Updating only the backdrop can retain the consumer's cached sampling state.
            if (page < 3) key(backdrop, pagerState.settledPage) {
            Box(Modifier.fillMaxWidth()
                .then(if (backdrop != null) Modifier.textureBlur(backdrop, RectangleShape, 25f,
                    colors = BlurDefaults.blurColors(blendColors = listOf(BlendColorEntry(pageBackground.copy(alpha = .8f))))) else Modifier)
                .background(barColor)) {
                NavigationBar(color = barColor) {
                listOf("首页" to NavIcons[0], "账户" to NavIcons[1], "设置" to NavIcons[2]).forEachIndexed { index, item ->
                    NavigationBarItem(selected = page == index, onClick = { navigate(index) },
                        icon = item.second, label = item.first, modifier = Modifier.weight(1f),
                        colors = NavigationBarDefaults.navigationBarItemColors(selectedContentColor = accent))
                }
                }
            }
            }
          }, containerColor = pageBackground,
          // Each page's TopAppBar owns the status-bar inset. Do not offset the pager.
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            val pageContent: @Composable (Int) -> Unit = { displayedPage ->
        val backdrop = backdrops[if (route.page < 3) displayedPage else 0]
        val contentScrollBehavior = MiuixScrollBehavior()
        val pageScrollState = key(displayedPage) { rememberScrollState() }
        Scaffold(topBar = {
            Box(Modifier.fillMaxWidth()
                .then(if (backdrop != null) Modifier.textureBlur(backdrop, RectangleShape, 25f,
                    colors = BlurDefaults.blurColors(blendColors = listOf(BlendColorEntry(pageBackground.copy(alpha = .8f))))) else Modifier)
                .background(if (backdrop != null) Color.Transparent else pageBackground)) {
                TopAppBar(title = when (displayedPage) { 0 -> "星愿"; 1 -> "账户"; 2 -> "设置"; 4 -> "主题设置"; 5 -> "导入与导出"; else -> "关于" },
                subtitle = "",
                color = barColor, scrollBehavior = contentScrollBehavior,
                navigationIcon = {
                    if (displayedPage >= 3) IconButton(onClick = { backStack.removeLastOrNull() }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回设置")
                    }
                })
            }
        }, containerColor = pageBackground) { pagePadding ->
        Box(Modifier.fillMaxSize()
            .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)) {
        Column(Modifier.fillMaxSize()
            .background(pageBackground)
            .overScrollVertical()
            .nestedScroll(contentScrollBehavior.nestedScrollConnection)
            .verticalScroll(pageScrollState)
            .padding(start = 16.dp, end = 16.dp, top = pagePadding.calculateTopPadding(),
                bottom = maxOf(padding.calculateBottomPadding(), pagePadding.calculateBottomPadding())),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(2.dp))
            when (displayedPage) {
                0 -> {
                    Overview(goals, accounts, appearance, accent)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("目标列表", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Button(onClick = { editingGoal = null; goalEditor = true },
                            colors = ButtonDefaults.buttonColorsPrimary(), minHeight = 40.dp,
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 9.dp)) { Text("＋ 新建目标", fontSize = 13.sp) }
                    }
                    if (goals.isEmpty()) {
                        EmptyCard("star", "第一个心愿，从这里开始", "旅行、应急金、心仪的礼物……\n给目标起个名字，让积累看得见。", "创建我的目标") {
                            editingGoal = null; goalEditor = true
                        }
                    }
                    goals.forEach { goal ->
                        GoalCard(goal, accounts,
                            onEdit = { editingGoal = goal; goalEditor = true },
                            onDelete = { deletingGoal = goal },
                            onSync = { navigate(1) }, compact = appearance.compactList)
                    }
                    Note("目标之间独立计算；同一账户可关联多个目标，不代表资金已分开存放。")
                }
                1 -> {
                    Note("绑定小星记账中的人民币储蓄账户。")
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
                        Button(onClick = { editingAccount = null; accountEditor = true },
                            colors = ButtonDefaults.buttonColorsPrimary(), insideMargin = PaddingValues(16.dp, 9.dp)) {
                            Text("＋ 添加账户", fontSize = 13.sp)
                        }
                    }
                    if (accounts.isEmpty()) EmptyCard("bank", "先连接一份积累", "添加与小星记账完全相同的账户名称，\n就能为攒钱目标提供余额。", "添加第一个账户") {
                        editingAccount = null; accountEditor = true
                    }
                    accounts.forEach { account ->
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
                                Button(onClick = { editingAccount = account; accountEditor = true }) { Text("编辑", fontSize = 13.sp) }
                                Button(onClick = { deletingAccount = account }) { Text("删除", fontSize = 13.sp) }
                            }
                        }
                    }
                    Note("仅支持人民币。余额来自记账数据或手动填写，与银行实际余额可能有差异。")
                }
                2 -> {
                    Text(if (quoteLoading && quoteRetry == 0) "一言加载中…" else hitokoto ?: "一言加载中…",
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !quoteLoading) { quoteRetry++ }
                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                        fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
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
        Box(Modifier.fillMaxSize()) {
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
    pendingBackup?.let { backup -> AppDialog("导入备份？", { if (!backupBusy) pendingBackup = null }) {
        Note("备份包含 ${backup.accounts.size} 个账户、${backup.goals.size} 个目标，以及外观设置和一言。确认后将替换当前数据，无法撤销；建议先导出当前数据。")
        Button(onClick = {
            scope.launch {
                backupBusy = true
                try {
                    withContext(Dispatchers.IO) { store.importBackup(backup) }
                    hitokoto = backup.hitokoto
                    pendingBackup = null
                    refresh()
                    notify("备份已导入")
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    notify("导入失败：${error.message ?: "保存失败"}")
                } finally { backupBusy = false }
            }
        }, enabled = !backupBusy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) { Text("确认替换并导入") }
        Button(onClick = { pendingBackup = null }, enabled = !backupBusy, modifier = Modifier.fillMaxWidth()) { Text("取消") }
    } }
    if (goalEditor) GoalEditor(editingGoal, accounts, onDismiss = { goalEditor = false }, onSave = {
        if (mutate { store.saveGoal(it) }) goalEditor = false
    }, onAddAccount = { goalEditor = false; navigate(1); editingAccount = null; accountEditor = true })
    if (accountEditor) AccountEditor(editingAccount, onDismiss = { accountEditor = false }, onSave = {
        if (mutate { store.saveAccount(it) }) accountEditor = false
    })
    if (disclosure) AppDialog("读取小星记账余额", { disclosure = false }) {
        Note("在系统设置启用「星愿攒钱 · 读取小星账户」后，应用会获得读取小星记账可见文字的能力。")
        Note("仅当你点击「读取小星余额」后，两分钟内识别指定账户详情中的名称及人民币余额。识别结果须返回本应用确认，才会更新目标。")
        Note("不会读取其他应用、收集截图或上传数据，不会执行点击、记账或转账。页面结构变化时可能无法识别，请使用手动更新。你可随时在系统设置关闭此权限。")
        Button(onClick = { disclosure = false; openReaderSettings() }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColorsPrimary()) { Text("了解并前往系统设置") }
        Button(onClick = { disclosure = false }, modifier = Modifier.fillMaxWidth()) { Text("暂不开启") }
    }
    deletingGoal?.let { goal -> AppDialog("删除这个心愿？", { deletingGoal = null }) {
        Note("将删除「${goal.title}」，关联账户会保留。此操作无法撤销。")
        Button(onClick = { if (mutate { store.deleteGoal(goal.id) }) deletingGoal = null },
            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) { Text("确认删除") }
    } }
    deletingAccount?.let { account -> AppDialog("删除这个账户？", { deletingAccount = null }) {
        Note("将删除「${account.name}」。如有目标关联此账户，请先编辑目标解除关联。")
        Button(onClick = { if (mutate { store.deleteAccount(account.id) }) deletingAccount = null },
            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) { Text("确认删除") }
    } }
}

@Composable private fun Overview(goals: List<SavingGoal>, accounts: List<BankAccount>, appearance: Appearance, accent: Color) {
    val featured = goals.firstOrNull { it.id == appearance.featuredGoalId } ?: goals.firstOrNull()
    val progress by animateFloatAsState(featured?.progress(accounts) ?: 0f, tween(650), label = "featured-progress")
    val status = appearance.bannerStyle == 0
    val foreground = if (status) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onPrimary
    val subtle = foreground.copy(alpha = if (status) .7f else .82f)
    val base = if (status) MiuixTheme.colorScheme.primaryContainer else accent
    val fill = if (status) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onPrimary
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(base)
        .semantics { contentDescription = "首页横幅 ${featured?.title ?: "尚无目标"} ${(progress * 100).toInt()}%" }) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-18).dp).size(134.dp), contentAlignment = Alignment.Center) {
            Symbol("star", foreground.copy(alpha = .12f), Modifier.size(120.dp))
        }
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("攒钱进度 · ${goals.size} 个目标", fontSize = 12.sp, color = subtle)
            Text(featured?.title ?: "创建你的第一个目标", fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold, color = foreground, maxLines = 2)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (featured == null) "先添加账户，再设定目标" else
                    "¥ ${Money.format(featured.saved(accounts))} / ¥ ${Money.format(featured.target)}",
                    fontSize = 13.sp, color = subtle, modifier = Modifier.weight(1f))
                Text("${(progress * 100).toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = foreground)
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

@Composable private fun GoalEditor(existing: SavingGoal?, accounts: List<BankAccount>, onDismiss: () -> Unit, onSave: (SavingGoal) -> Unit, onAddAccount: () -> Unit) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var amount by rememberSaveable(existing?.id) { mutableStateOf(existing?.let { Money.input(it.target) }.orEmpty()) }
    var date by rememberSaveable(existing?.id) { mutableStateOf(existing?.deadline.orEmpty()) }
    var selected by rememberSaveable(existing?.id) { mutableStateOf(existing?.accountIds?.toList() ?: emptyList<String>()) }
    var growth by rememberSaveable(existing?.id) { mutableStateOf(existing?.mode == GoalMode.GROWTH) }
    var error by remember { mutableStateOf("") }
    AppDialog(if (existing == null) "新建一个心愿" else "编辑心愿", onDismiss) {
        TextField(value = title, onValueChange = { title = it.take(40) }, label = "心愿名称，如：去看一次极光", singleLine = true)
        TextField(value = amount, onValueChange = { amount = it.take(18) }, label = "目标金额（元）", singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        TextField(value = date, onValueChange = { date = it.take(10) }, label = "截止日期 YYYY-MM-DD（选填）", singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
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
                    onValueChange = { selected = if (it) selected + account.id else selected - account.id }).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (checked) "●" else "○", color = MiuixTheme.colorScheme.primary, fontSize = 20.sp)
                Column(Modifier.weight(1f)) {
                    Text(account.name, fontSize = 14.sp)
                    Text("¥ ${Money.format(account.balance)}", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                }
            }
        }
        Text("进度计算方式", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { growth = false }, modifier = Modifier.weight(1f),
                colors = if (!growth) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors()) { Text("当前余额", fontSize = 12.sp) }
            Button(onClick = { growth = true }, modifier = Modifier.weight(1f),
                colors = if (growth) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors()) { Text("从现在开始攒", fontSize = 12.sp) }
        }
        Note(if (growth) "以当前合计余额为起点，只计算之后新增的积累。" else "以所选账户的当前余额合计作为已攒金额。")
        Note("目标进度条与图标跟随应用强调色，可在设置中统一调整。")
        if (error.isNotEmpty()) Text(error, color = MiuixTheme.colorScheme.error, fontSize = 13.sp)
        Button(onClick = {
            val target = Money.parse(amount)
            val parsedDate = if (date.isBlank()) null else runCatching { LocalDate.parse(date) }.getOrNull()
            error = when {
                title.isBlank() -> "给心愿起个名字吧"
                target == null || target <= 0 -> "请输入大于 0 的有效金额，最多两位小数"
                selected.isEmpty() -> "请选择至少一个账户"
                date.isNotBlank() && (parsedDate == null || parsedDate.toString() != date) -> "日期格式应为 YYYY-MM-DD，例如 2027-12-31"
                parsedDate != null && parsedDate < LocalDate.now() && date != existing?.deadline -> "请选择今天或之后的日期"
                else -> ""
            }
            if (error.isEmpty() && target != null) {
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

@Composable private fun AccountEditor(existing: BankAccount?, onDismiss: () -> Unit, onSave: (BankAccount) -> Unit) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var balance by rememberSaveable(existing?.id) { mutableStateOf(existing?.let { Money.input(it.balance) }.orEmpty()) }
    var error by remember { mutableStateOf("") }
    AppDialog(if (existing == null) "添加储蓄账户" else "编辑账户", onDismiss) {
        Note("使用与小星记账完全一致的账户名称。初始余额请手动填写；之后可读取并确认更新。")
        TextField(value = name, onValueChange = { name = it.take(40) }, label = "账户名称，如：招商银行储蓄卡", singleLine = true)
        TextField(value = balance, onValueChange = { balance = it.take(18) }, label = "当前人民币余额（元）", singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Note("仅支持人民币储蓄账户，不将信用卡额度当作存款。")
        if (error.isNotEmpty()) Text(error, color = MiuixTheme.colorScheme.error, fontSize = 13.sp)
        Button(onClick = {
            val cents = Money.parse(balance)
            error = when { name.isBlank() -> "请输入账户名称"; cents == null -> "请输入有效余额，最多两位小数"; else -> "" }
            if (error.isEmpty() && cents != null) {
                val balanceChanged = existing == null || cents != existing.balance
                onSave(BankAccount(existing?.id ?: UUID.randomUUID().toString(), name.trim(), cents,
                    if (balanceChanged) System.currentTimeMillis() else existing.updatedAt,
                    if (balanceChanged) "手动录入" else existing.source))
            }
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColorsPrimary()) { Text("保存账户") }
    }
}

@Composable private fun AppDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    WindowDialog(show = true, title = title, onDismissRequest = onDismiss) {
        Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            content()
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
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = .1f)).padding(horizontal = 9.dp, vertical = 5.dp)) {
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}
@Composable private fun IconTile(kind: String, color: Color) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(color.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
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
@Composable private fun Symbol(kind: String, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val stroke = Stroke(1.8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        when (kind) {
            "star" -> {
                val path = Path()
                repeat(10) { i ->
                    val angle = (i * Math.PI / 5 - Math.PI / 2)
                    val radius = if (i % 2 == 0) .44f else .21f
                    val x = w * (.5f + radius * kotlin.math.cos(angle).toFloat())
                    val y = h * (.5f + radius * kotlin.math.sin(angle).toFloat())
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close(); drawPath(path, color, style = stroke)
            }
            "bank" -> {
                val path = Path().apply { moveTo(w * .12f, h * .32f); lineTo(w * .5f, h * .1f); lineTo(w * .88f, h * .32f); close() }
                drawPath(path, color, style = stroke)
                listOf(.24f, .5f, .76f).forEach { x -> drawLine(color, Offset(w * x, h * .45f), Offset(w * x, h * .76f), stroke.width) }
                drawLine(color, Offset(w * .12f, h * .88f), Offset(w * .88f, h * .88f), stroke.width)
            }
            "link" -> {
                drawLine(color, Offset(w * .25f, h * .65f), Offset(w * .65f, h * .25f), stroke.width)
                drawCircle(color, w * .2f, Offset(w * .3f, h * .7f), style = stroke)
                drawCircle(color, w * .2f, Offset(w * .7f, h * .3f), style = stroke)
            }
            else -> {
                drawCircle(color, w * .4f, style = stroke)
                listOf(.3f, .5f, .7f).forEach { drawCircle(color, w * .045f, Offset(w * it, h * .5f)) }
            }
        }
    }
}
private fun timeLabel(millis: Long): String = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(millis))
