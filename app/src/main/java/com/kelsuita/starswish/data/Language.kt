package com.kelsuita.starswish.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class AppLanguage(val nativeName: String) {
    /** First-launch default: use whatever language the system is set to. */
    FollowSystem("跟随系统"),
    SimplifiedChinese("简体中文"),
    TraditionalChinese("繁體中文"),
    English("English"),
    ;

    /** Chinese copy is the only place the quote (一言) is offered. */
    val isChinese: Boolean get() = this == SimplifiedChinese || this == TraditionalChinese
}

/**
 * UI copy is written once in the call sites in simplified Chinese and looked up here, so a string
 * only needs an entry in the two translation tables below. [current] is snapshot state, so the
 * lookup inside [tr] is tracked by composition and switching languages recomposes the screens that
 * read it; the switch takes effect immediately without recreating the activity.
 */
object Languages {
    /** The stored choice, which may be [AppLanguage.FollowSystem]. */
    var choice by mutableStateOf(AppLanguage.FollowSystem)
        private set

    /** The language actually used for lookups, with [AppLanguage.FollowSystem] already resolved. */
    var current by mutableStateOf(AppLanguage.SimplifiedChinese)
        private set

    /** Applies a stored choice on start-up and whenever the user picks a new one. */
    fun set(value: AppLanguage) {
        choice = value
        current = resolve(value)
    }

    /**
     * Maps [AppLanguage.FollowSystem] onto the system UI language. The activity has no
     * `configChanges` for locale, so changing the system language recreates it and [set] runs
     * again with the new locale.
     */
    fun resolve(value: AppLanguage): AppLanguage = when (value) {
        AppLanguage.FollowSystem -> systemLanguage()
        else -> value
    }

    /** Chinese systems get the matching script; every other locale falls back to English. */
    private fun systemLanguage(): AppLanguage {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "zh" -> if (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO"))
                AppLanguage.TraditionalChinese else AppLanguage.SimplifiedChinese
            else -> AppLanguage.English
        }
    }

    fun tr(source: String): String = when (current) {
        AppLanguage.SimplifiedChinese, AppLanguage.FollowSystem -> source
        AppLanguage.TraditionalChinese -> Traditional[source] ?: source
        AppLanguage.English -> English[source] ?: source
    }

    fun tr(source: String, vararg args: Any): String =
        String.format(Locale.ROOT, tr(source), *args)
}

/** Localised lookup for UI copy. Source strings are simplified Chinese. */
fun tr(source: String): String = Languages.tr(source)

/** Localised lookup for templated UI copy, using the same `%s` / `%d` placeholders as [String.format]. */
fun tr(source: String, vararg args: Any): String = Languages.tr(source, *args)

// Strings that differ between simplified and traditional Chinese only. Anything missing falls back
// to the simplified source.
private val Traditional = mapOf(
    "%s · %s" to "%s · %s",
    "%s完成进度" to "%s完成進度",
    "「当前余额」：关联账户的余额合计。\n「从现在开始攒」：余额合计减去创建目标时的起始余额。若余额减少，进度也会减少；最低显示为 0%。" to
        "「目前餘額」：關聯帳戶的餘額合計。\n「從現在開始攢」：餘額合計減去建立目標時的起始餘額。若餘額減少，進度也會減少；最低顯示為 0%。",
    "Github链接" to "GitHub 連結",
    "Material 3 色彩规范版本" to "Material 3 色彩規範版本",
    "按当前余额计算" to "按目前餘額計算",
    "按名称排序" to "按名稱排序",
    "按时间排序" to "按時間排序",
    "绑定小星记账中的人民币储蓄账户。" to "綁定小星記帳中的人民幣儲蓄帳戶。",
    "保存失败" to "儲存失敗",
    "保存失败，请检查可用空间" to "儲存失敗，請檢查可用空間",
    "保存修改" to "儲存修改",
    "保存账户" to "儲存帳戶",
    "备份" to "備份",
    "备份包含 %s 个账户、%s 个目标，以及外观设置和一言。确认后将替换当前数据，无法撤销；建议先导出当前数据。" to
        "備份包含 %s 個帳戶、%s 個目標，以及外觀設定和一言。確認後將取代目前資料，無法復原；建議先匯出目前資料。",
    "备份包含重复的账户或目标" to "備份包含重複的帳戶或目標",
    "备份不包含读取中的临时请求及待确认余额。无障碍服务权限由系统管理，换设备后需要重新开启。" to
        "備份不包含讀取中的暫時要求及待確認餘額。無障礙服務權限由系統管理，換裝置後需要重新開啟。",
    "备份导入保存失败，请检查可用空间" to "備份匯入儲存失敗，請檢查可用空間",
    "备份格式或内容无效，请选择星愿导出的 JSON 文件" to "備份格式或內容無效，請選擇星願匯出的 JSON 檔案",
    "备份数据超出限制" to "備份資料超出限制",
    "备份文件不能超过 1 MB" to "備份檔案不能超過 1 MB",
    "备份文件为明文 JSON，包含账户名称与余额，请妥善保存。导入会替换现有账户、目标和外观设置，建议先导出当前数据。" to
        "備份檔案為明文 JSON，包含帳戶名稱與餘額，請妥善保存。匯入會取代現有帳戶、目標和外觀設定，建議先匯出目前資料。",
    "备份已导出" to "備份已匯出",
    "备份已导入" to "備份已匯入",
    "备份中的目标或关联账户无效" to "備份中的目標或關聯帳戶無效",
    "备份中的外观设置无效" to "備份中的外觀設定無效",
    "备份中的一言无效" to "備份中的一言無效",
    "备份中的账户数据无效" to "備份中的帳戶資料無效",
    "编辑" to "編輯",
    "编辑心愿" to "編輯心願",
    "编辑账户" to "編輯帳戶",
    "不会读取其他应用、收集截图或上传数据，不会执行点击、记账或转账。页面结构变化时可能无法识别，请使用手动更新。你可随时在系统设置关闭此权限。" to
        "不會讀取其他應用程式、收集截圖或上傳資料，不會執行點擊、記帳或轉帳。頁面結構變化時可能無法辨識，請使用手動更新。你可隨時在系統設定關閉此權限。",
    "不是支持的星愿备份文件或备份版本" to "不是支援的星願備份檔案或備份版本",
    "操作失败，请重试" to "操作失敗，請重試",
    "查看读取说明与权限" to "查看讀取說明與權限",
    "查看项目，更新应用，看看Kelsuita在偷懒吗。" to "查看專案，更新應用程式，看看 Kelsuita 在偷懶嗎。",
    "查看小星记账官方接口文档" to "查看小星記帳官方介面文件",
    "橙色" to "橙色",
    "创建你的第一个目标" to "建立你的第一個目標",
    "创建我的目标" to "建立我的目標",
    "创建心愿" to "建立心願",
    "从创建时开始积累" to "從建立時開始累積",
    "从现在开始攒" to "從現在開始攢",
    "当前人民币余额（元）" to "目前人民幣餘額（元）",
    "当前余额" to "目前餘額",
    "当前心愿" to "目前心願",
    "导出备份" to "匯出備份",
    "导出失败：%s" to "匯出失敗：%s",
    "导入备份" to "匯入備份",
    "导入备份？" to "匯入備份？",
    "导入失败：%s" to "匯入失敗：%s",
    "导入与导出" to "匯入與匯出",
    "倒序" to "倒序",
    "等待读取账户详情" to "等待讀取帳戶詳情",
    "等待你确认" to "等待你確認",
    "第一个心愿，从这里开始" to "第一個心願，從這裡開始",
    "点按核对并确认余额  ›" to "點按核對並確認餘額  ›",
    "点击读取，进入小星的账户详情页，再回到这里核对并确认余额。若页面不可识别，可手动更新。" to
        "點擊讀取，進入小星的帳戶詳情頁，再回到這裡核對並確認餘額。若頁面無法辨識，可手動更新。",
    "调色板风格" to "調色盤風格",
    "读取并确认" to "讀取並確認",
    "读取小星记账余额" to "讀取小星記帳餘額",
    "读取小星余额" to "讀取小星餘額",
    "返回目标列表" to "返回目標列表",
    "返回设置" to "返回設定",
    "返回账户列表" to "返回帳戶列表",
    "该账户已删除" to "該帳戶已刪除",
    "给心愿起个名字吧" to "給心願起個名字吧",
    "跟随系统" to "跟隨系統",
    "跟随系统语言（%s）" to "跟隨系統語言（%s）",
    "更新于 %s" to "更新於 %s",
    "更新余额" to "更新餘額",
    "关联账户" to "關聯帳戶",
    "关于" to "關於",
    "管理读取权限" to "管理讀取權限",
    "还差 ¥ %s" to "還差 ¥ %s",
    "还差 ¥ %s，继续慢慢靠近。" to "還差 ¥ %s，繼續慢慢靠近。",
    "还没有账户，先添加一个账户再创建目标。" to "還沒有帳戶，先新增一個帳戶再建立目標。",
    "横幅目标" to "橫幅目標",
    "忽略" to "忽略",
    "积累中" to "累積中",
    "价格排序" to "價格排序",
    "减少目标卡片的留白" to "減少目標卡片的留白",
    "将删除「%s」，关联账户会保留。此操作无法撤销。" to "將刪除「%s」，關聯帳戶會保留。此操作無法復原。",
    "将删除「%s」。如有目标关联此账户，请先编辑目标解除关联。" to "將刪除「%s」。若有目標關聯此帳戶，請先編輯目標解除關聯。",
    "将账户、目标、外观设置和一言保存为 JSON 文件" to "將帳戶、目標、外觀設定和一言儲存為 JSON 檔案",
    "截止日期 YYYY-MM-DD（选填）" to "截止日期 YYYY-MM-DD（選填）",
    "今天是计划完成日" to "今天是計畫完成日",
    "仅当你点击「读取小星余额」后，两分钟内识别指定账户详情中的名称及人民币余额。识别结果须返回本应用确认，才会更新目标。" to
        "僅當你點擊「讀取小星餘額」後，兩分鐘內辨識指定帳戶詳情中的名稱及人民幣餘額。辨識結果須返回本應用程式確認，才會更新目標。",
    "仅在深色模式下使用纯黑页面背景" to "僅在深色模式下使用純黑頁面背景",
    "仅支持人民币。余额来自记账数据或手动填写，与银行实际余额可能有差异。" to
        "僅支援人民幣。餘額來自記帳資料或手動填寫，與銀行實際餘額可能有差異。",
    "仅支持人民币储蓄账户，不将信用卡额度当作存款。" to "僅支援人民幣儲蓄帳戶，不將信用卡額度當作存款。",
    "紧凑目标列表" to "緊湊目標列表",
    "进度计算方式" to "進度計算方式",
    "进度如何计算" to "進度如何計算",
    "注意" to "注意",
    "捐助我" to "捐助我",
    "开启页面读取，同步账户余额" to "開啟頁面讀取，同步帳戶餘額",
    "开启这个心愿" to "開啟這個心願",
    "苦尽甘来" to "苦盡甘來",
    "蓝色" to "藍色",
    "了解并前往系统设置" to "了解並前往系統設定",
    "两分钟内打开小星记账中的对应账户详情，检测到详情页后自动读取顶部人民币余额，再返回确认。" to
        "兩分鐘內開啟小星記帳中的對應帳戶詳情，偵測到詳情頁後自動讀取頂部人民幣餘額，再返回確認。",
    "旅行、应急金、心仪的礼物……\n给目标起个名字，让积累看得见。" to
        "旅行、緊急預備金、心儀的禮物……\n給目標起個名字，讓累積看得見。",
    "绿色" to "綠色",
    "名称须与小星记账的账户名称完全一致；不需要银行卡号、密码。" to
        "名稱須與小星記帳的帳戶名稱完全一致；不需要金融卡號、密碼。",
    "模糊" to "模糊",
    "莫奈取色" to "莫內取色",
    "默认" to "預設",
    "目标 ¥ %s" to "目標 ¥ %s",
    "目标菜单" to "目標選單",
    "目标金额（元）" to "目標金額（元）",
    "目标进度条与图标跟随应用强调色，可在设置中统一调整。" to
        "目標進度條與圖示跟隨應用程式強調色，可在設定中統一調整。",
    "目标列表" to "目標列表",
    "目标排序" to "目標排序",
    "目标已达成" to "目標已達成",
    "目标之间独立计算；同一账户可关联多个目标，不代表资金已分开存放。" to
        "目標之間獨立計算；同一帳戶可關聯多個目標，不代表資金已分開存放。",
    "排序" to "排序",
    "排序菜单" to "排序選單",
    "启用顶栏和底栏的模糊效果" to "啟用頂欄和底欄的模糊效果",
    "起始余额 ¥ %s" to "起始餘額 ¥ %s",
    "浅色" to "淺色",
    "强调色" to "強調色",
    "请Kelsuita吃包辣条" to "請 Kelsuita 吃包辣條",
    "请打开「%s」的账户详情页，显示余额后返回星愿确认" to "請開啟「%s」的帳戶詳情頁，顯示餘額後返回星願確認",
    "请输入大于 0 的有效金额，最多两位小数" to "請輸入大於 0 的有效金額，最多兩位小數",
    "请输入有效余额，最多两位小数" to "請輸入有效餘額，最多兩位小數",
    "请输入账户名称" to "請輸入帳戶名稱",
    "请先在关联目标中解除该账户" to "請先在關聯目標中解除該帳戶",
    "请选择今天或之后的日期" to "請選擇今天或之後的日期",
    "请选择至少一个账户" to "請選擇至少一個帳戶",
    "取消" to "取消",
    "取消本次读取" to "取消本次讀取",
    "确认替换并导入" to "確認取代並匯入",
    "确认同步" to "確認同步",
    "日期格式应为 YYYY-MM-DD，例如 2027-12-31" to "日期格式應為 YYYY-MM-DD，例如 2027-12-31",
    "色彩规范" to "色彩規範",
    "删除" to "刪除",
    "删除这个心愿？" to "刪除這個心願？",
    "删除这个账户？" to "刪除這個帳戶？",
    "上移" to "上移",
    "尚无目标" to "尚無目標",
    "设置" to "設定",
    "设置读取权限" to "設定讀取權限",
    "深色" to "深色",
    "深色模式纯黑背景" to "深色模式純黑背景",
    "剩余 %s 天 · 每天约 ¥ %s" to "剩餘 %s 天 · 每天約 ¥ %s",
    "识别于 %s。请核对小星页面，确认后才会更新进度。" to "辨識於 %s。請核對小星頁面，確認後才會更新進度。",
    "使用步骤" to "使用步驟",
    "使用悬浮底栏" to "使用懸浮底欄",
    "悬浮底栏样式" to "懸浮底欄樣式",
    "底部导航以悬浮样式显示，横屏时同样生效" to "底部導覽以懸浮樣式顯示，橫向時同樣生效",
    "使用与小星记账完全一致的账户名称。初始余额请手动填写；之后可读取并确认更新。" to
        "使用與小星記帳完全一致的帳戶名稱。初始餘額請手動填寫；之後可讀取並確認更新。",
    "收起导航栏" to "收合導覽列",
    "首页" to "首頁",
    "首页横幅" to "首頁橫幅",
    "首页横幅 %s %s" to "首頁橫幅 %s %s",
    "数据留在你的手机" to "資料留在你的手機",
    "添加储蓄账户" to "新增儲蓄帳戶",
    "添加第一个账户" to "新增第一個帳戶",
    "添加与小星记账完全相同的账户名称，\n就能为攒钱目标提供余额。" to
        "新增與小星記帳完全相同的帳戶名稱，\n就能為存錢目標提供餘額。",
    "添加账户" to "新增帳戶",
    "通过 JSON 文件备份或恢复数据" to "透過 JSON 檔案備份或還原資料",
    "拖动排序 %s" to "拖曳排序 %s",
    "外观" to "外觀",
    "外观设置保存失败" to "外觀設定儲存失敗",
    "未设截止日期" to "未設截止日期",
    "未授权" to "未授權",
    "未找到小星记账，请先安装后重试" to "找不到小星記帳，請先安裝後重試",
    "我的账户" to "我的帳戶",
    "无法读取所选文件" to "無法讀取所選檔案",
    "无法读取文件" to "無法讀取檔案",
    "无法写入所选文件" to "無法寫入所選檔案",
    "无法写入文件" to "無法寫入檔案",
    "无需登录。启用一言后，应用会在没有缓存时向一言接口请求一句话；账户、目标与已确认余额只保存在应用私有存储，不会发送给一言接口。不收集页面截图，不保存完整页面文字。卸载应用会删除数据。" to
        "無需登入。啟用一言後，應用程式會在沒有快取時向一言介面請求一句話；帳戶、目標與已確認餘額只保存在應用程式私人儲存空間，不會傳送給一言介面。不收集頁面截圖，不保存完整頁面文字。解除安裝應用程式會刪除資料。",
    "下移" to "下移",
    "先连接一份积累" to "先連結一份累積",
    "先去添加账户" to "先去新增帳戶",
    "先添加账户，再设定目标" to "先新增帳戶，再設定目標",
    "小星记账" to "小星記帳",
    "小星暂未提供公开的余额查询接口。此功能只识别你主动打开的账户详情，需要手动确认，不会在后台实时同步。" to
        "小星暫未提供公開的餘額查詢介面。此功能只辨識你主動開啟的帳戶詳情，需要手動確認，不會在背景即時同步。",
    "心愿达成，值得为自己庆祝！" to "心願達成，值得為自己慶祝！",
    "心愿名称，如：去看一次极光" to "心願名稱，如：去看一次極光",
    "新建目标" to "新增目標",
    "新建一个心愿" to "新增一個心願",
    "星愿" to "星願",
    "星愿 · 余额读取完成" to "星願 · 餘額讀取完成",
    "星愿，已读取，请返回星愿确认余额。点按打开账户确认页" to "星願，已讀取，請返回星願確認餘額。點按開啟帳戶確認頁",
    "星愿攒钱" to "星願攢錢",
    "星愿攒钱 · 使用说明与隐私" to "星願攢錢 · 使用說明與隱私",
    "Kelsuita倾情出品" to "Kelsuita傾情出品",
    "修改目标名称、金额、日期不会重置起点。修改关联账户或切换计算方式时，会重新记录起点。" to
        "修改目標名稱、金額、日期不會重設起點。修改關聯帳戶或切換計算方式時，會重新記錄起點。",
    "需要 Android 13 或更高版本" to "需要 Android 13 或更新版本",
    "选择 JSON 备份，确认后替换现有数据" to "選擇 JSON 備份，確認後取代現有資料",
    "选择应用的明暗模式" to "選擇應用程式的明暗模式",
    "选择应用的显示语言" to "選擇應用程式的顯示語言",
    "选择应用强调色" to "選擇應用程式強調色",
    "页面读取仅在你发起后两分钟内生效，并限定为小星记账。不会自动点击、修改账单、访问银行应用或执行转账。" to
        "頁面讀取僅在你發起後兩分鐘內生效，並限定為小星記帳。不會自動點擊、修改帳單、存取銀行應用程式或執行轉帳。",
    "页面读取已开启 · 按需读取" to "頁面讀取已開啟 · 視需要讀取",
    "一言加载中…" to "一言載入中…",
    "一言暂时不可用，点击重试" to "一言暫時無法使用，點擊重試",
    "已超过计划日期，可编辑延长计划" to "已超過計畫日期，可編輯延長計畫",
    "已达成" to "已達成",
    "已读取，请返回星愿" to "已讀取，請返回星願",
    "已授权" to "已授權",
    "已提前完成这份心愿" to "已提前完成這份心願",
    "以当前合计余额为起点，只计算之后新增的积累。" to "以目前合計餘額為起點，只計算之後新增的累積。",
    "以所选账户的当前余额合计作为已攒金额。" to "以所選帳戶的目前餘額合計作為已存金額。",
    "应用" to "應用程式",
    "用于推导 MIUIX 调色板的色调算法" to "用於推導 MIUIX 調色盤的色調演算法",
    "余额排序" to "餘額排序",
    "语言" to "語言",
    "语言设置保存失败" to "語言設定儲存失敗",
    "在系统设置启用「星愿攒钱 · 读取小星账户」后，应用会获得读取小星记账可见文字的能力。" to
        "在系統設定啟用「星願攢錢 · 讀取小星帳戶」後，應用程式會取得讀取小星記帳可見文字的能力。",
    "攒钱进度 · %s 个目标" to "存錢進度 · %s 個目標",
    "暂不开启" to "暫不開啟",
    "展开导航栏" to "展開導覽列",
    "账户" to "帳戶",
    "账户菜单" to "帳戶選單",
    "账户名称，如：招商银行储蓄卡" to "帳戶名稱，如：招商銀行儲蓄卡",
    "账户名称已存在" to "帳戶名稱已存在",
    "账户排序" to "帳戶排序",
    "账户已删除" to "帳戶已刪除",
    "正在处理备份，请稍候…" to "正在處理備份，請稍候…",
    "主题" to "主題",
    "主题、强调色、首页样式与模糊效果" to "主題、強調色、首頁樣式與模糊效果",
    "主题模式" to "主題模式",
    "主题设置" to "主題設定",
    "紫色" to "紫色",
    "自定义名称、金额、截止日期，选择关联账户及进度计算方式。" to "自訂名稱、金額、截止日期，選擇關聯帳戶及進度計算方式。",
    "自动选择" to "自動選擇",
    "最多添加 100 个目标" to "最多新增 100 個目標",
    "最多添加 50 个账户" to "最多新增 50 個帳戶",
    // Stored account sources, translated only when displayed.
    "手动录入" to "手動輸入",
    "小星页面识别 · 已确认" to "小星頁面辨識 · 已確認",
    "预测性返回手势" to "預測性返回手勢",
    "使用系统的预测性返回动画" to "使用系統的預測性返回動畫",
    "关闭星愿无障碍服务" to "關閉星願無障礙服務",
    "请在系统设置中关闭星愿的无障碍服务" to "請在系統設定中關閉星願的無障礙服務",
    "备份中的设置无效" to "備份中的設定無效",
    "将账户、目标、全部设置和一言保存为 JSON 文件" to "將帳戶、目標、全部設定和一言儲存為 JSON 檔案",
    "备份文件为明文 JSON，包含账户名称与余额，请妥善保存。导入会替换现有账户、目标和全部设置，建议先导出当前数据。" to
        "備份檔案為明文 JSON，包含帳戶名稱與餘額，請妥善保存。匯入會取代現有帳戶、目標和全部設定，建議先匯出目前資料。",
    "备份包含 %s 个账户、%s 个目标，以及全部设置和一言。确认后将替换当前数据，无法撤销；建议先导出当前数据。" to
        "備份包含 %s 個帳戶、%s 個目標，以及全部設定和一言。確認後將取代目前資料，無法復原；建議先匯出目前資料。",
)

private val English = mapOf(
    "%s · %s" to "%s · %s",
    "%s完成进度" to "%s progress",
    "「当前余额」：关联账户的余额合计。\n「从现在开始攒」：余额合计减去创建目标时的起始余额。若余额减少，进度也会减少；最低显示为 0%。" to
        "Current balance: the sum of the linked accounts.\nStart from now: that sum minus the starting balance recorded when the goal was created. If the balances drop, progress drops too and never shows below 0%.",
    "Github链接" to "GitHub",
    "Material 3 色彩规范版本" to "Material 3 color spec version",
    "按当前余额计算" to "Count the current balance",
    "按名称排序" to "Sort by name",
    "按时间排序" to "Sort by date",
    "绑定小星记账中的人民币储蓄账户。" to "Link a CNY savings account from Xiaoxing Ledger.",
    "保存失败" to "Save failed",
    "保存失败，请检查可用空间" to "Save failed. Check the available storage.",
    "保存修改" to "Save changes",
    "保存账户" to "Save account",
    "备份" to "Backup",
    "备份包含 %s 个账户、%s 个目标，以及外观设置和一言。确认后将替换当前数据，无法撤销；建议先导出当前数据。" to
        "The backup contains %s accounts, %s goals and appearance settings. Importing replaces the current data and cannot be undone; export the current data first.",
    "备份包含重复的账户或目标" to "The backup contains duplicate accounts or goals.",
    "备份不包含读取中的临时请求及待确认余额。无障碍服务权限由系统管理，换设备后需要重新开启。" to
        "The backup leaves out pending read requests and unconfirmed balances. The accessibility permission is managed by the system and must be granted again on a new device.",
    "备份导入保存失败，请检查可用空间" to "Saving the imported backup failed. Check the available storage.",
    "备份格式或内容无效，请选择星愿导出的 JSON 文件" to "The backup format or content is invalid. Choose a JSON file exported by Starwish.",
    "备份数据超出限制" to "The backup exceeds the size limits.",
    "备份文件不能超过 1 MB" to "A backup file must be 1 MB or smaller.",
    "备份文件为明文 JSON，包含账户名称与余额，请妥善保存。导入会替换现有账户、目标和外观设置，建议先导出当前数据。" to
        "The backup is plain JSON with account names and balances, so keep it safe. Importing replaces the current accounts, goals and appearance settings; export the current data first.",
    "备份已导出" to "Backup exported",
    "备份已导入" to "Backup imported",
    "备份中的目标或关联账户无效" to "The backup contains an invalid goal or linked account.",
    "备份中的外观设置无效" to "The backup contains invalid appearance settings.",
    "备份中的一言无效" to "The backup contains invalid data.",
    "备份中的账户数据无效" to "The backup contains invalid account data.",
    "编辑" to "Edit",
    "编辑心愿" to "Edit wish",
    "编辑账户" to "Edit account",
    "不会读取其他应用、收集截图或上传数据，不会执行点击、记账或转账。页面结构变化时可能无法识别，请使用手动更新。你可随时在系统设置关闭此权限。" to
        "It does not read other apps, collect screenshots or upload data, and never taps, records transactions or transfers money. If the page layout changes it may stop recognizing text; update by hand then. You can turn the permission off in system settings at any time.",
    "不是支持的星愿备份文件或备份版本" to "This is not a Starwish backup, or its version is not supported.",
    "操作失败，请重试" to "Something went wrong. Try again.",
    "查看读取说明与权限" to "Read how page reading works",
    "查看项目，更新应用，看看Kelsuita在偷懒吗。" to "Browse the project, update the app and see whether Kelsuita is slacking off.",
    "查看小星记账官方接口文档" to "Read the Xiaoxing Ledger documentation",
    "橙色" to "Orange",
    "创建你的第一个目标" to "Create your first goal",
    "创建我的目标" to "Create my goal",
    "创建心愿" to "Create a wish",
    "从创建时开始积累" to "Count from when it was created",
    "从现在开始攒" to "Start saving now",
    "当前人民币余额（元）" to "Current CNY balance (yuan)",
    "当前余额" to "Current balance",
    "当前心愿" to "Current wish",
    "导出备份" to "Export backup",
    "导出失败：%s" to "Export failed: %s",
    "导入备份" to "Import backup",
    "导入备份？" to "Import backup?",
    "导入失败：%s" to "Import failed: %s",
    "导入与导出" to "Import and export",
    "倒序" to "Reversed",
    "等待读取账户详情" to "Waiting for the account page",
    "等待你确认" to "Waiting for your confirmation",
    "第一个心愿，从这里开始" to "Your first wish starts here",
    "点按核对并确认余额  ›" to "Tap to review and confirm  ›",
    "点击读取，进入小星的账户详情页，再回到这里核对并确认余额。若页面不可识别，可手动更新。" to
        "Tap Read, open the account page in Xiaoxing, then come back here to review and confirm the balance. If the page cannot be recognized, update it by hand.",
    "调色板风格" to "Palette style",
    "读取并确认" to "Read and confirm",
    "读取小星记账余额" to "Read the Xiaoxing Ledger balance",
    "读取小星余额" to "Read Xiaoxing balance",
    "返回目标列表" to "Back to goals",
    "返回设置" to "Back to settings",
    "返回账户列表" to "Back to accounts",
    "该账户已删除" to "This account was deleted",
    "给心愿起个名字吧" to "Give the wish a name",
    "跟随系统" to "Follow system",
    "跟随系统语言（%s）" to "Follow the system language (%s)",
    "更新于 %s" to "Updated %s",
    "更新余额" to "Update balance",
    "关联账户" to "Linked accounts",
    "关于" to "About",
    "管理读取权限" to "Manage the reading permission",
    "还差 ¥ %s" to "¥ %s to go",
    "还差 ¥ %s，继续慢慢靠近。" to "¥ %s to go — keep getting closer.",
    "还没有账户，先添加一个账户再创建目标。" to "No accounts yet. Add one before creating a goal.",
    "横幅目标" to "Banner goal",
    "忽略" to "Dismiss",
    "积累中" to "In progress",
    "价格排序" to "Sort by amount",
    "减少目标卡片的留白" to "Tighter goal cards",
    "将删除「%s」，关联账户会保留。此操作无法撤销。" to "This deletes \"%s\". Linked accounts are kept. It cannot be undone.",
    "将删除「%s」。如有目标关联此账户，请先编辑目标解除关联。" to "This deletes \"%s\". If a goal links this account, edit the goal to unlink it first.",
    "将账户、目标、外观设置和一言保存为 JSON 文件" to "Save accounts, goals and appearance settings as a JSON file",
    "截止日期 YYYY-MM-DD（选填）" to "Deadline YYYY-MM-DD (optional)",
    "今天是计划完成日" to "Today is the planned finish date",
    "仅当你点击「读取小星余额」后，两分钟内识别指定账户详情中的名称及人民币余额。识别结果须返回本应用确认，才会更新目标。" to
        "Only within two minutes of tapping Read Xiaoxing balance does it read the name and CNY balance from the account page you open. The result must be confirmed here before a goal is updated.",
    "仅在深色模式下使用纯黑页面背景" to "Use a pure black page background in dark mode",
    "仅支持人民币。余额来自记账数据或手动填写，与银行实际余额可能有差异。" to
        "CNY only. Balances come from your ledger data or manual entry and may differ from your bank balance.",
    "仅支持人民币储蓄账户，不将信用卡额度当作存款。" to "CNY savings accounts only; credit limits are not treated as savings.",
    "紧凑目标列表" to "Compact goal list",
    "进度计算方式" to "How progress is calculated",
    "进度如何计算" to "How progress works",
    "注意" to "Important",
    "捐助我" to "Support me",
    "开启页面读取，同步账户余额" to "Enable page reading to sync balances",
    "开启这个心愿" to "Start this wish",
    "苦尽甘来" to "All that saving paid off",
    "蓝色" to "Blue",
    "了解并前往系统设置" to "Open system settings",
    "两分钟内打开小星记账中的对应账户详情，检测到详情页后自动读取顶部人民币余额，再返回确认。" to
        "Open the matching account page in Xiaoxing Ledger within two minutes. Once the page is detected the CNY balance at the top is read, then come back to confirm.",
    "旅行、应急金、心仪的礼物……\n给目标起个名字，让积累看得见。" to
        "A trip, an emergency fund, a gift you have been eyeing…\nName your goal and watch it grow.",
    "绿色" to "Green",
    "名称须与小星记账的账户名称完全一致；不需要银行卡号、密码。" to
        "The name must match the Xiaoxing Ledger account exactly; no card number or password is needed.",
    "模糊" to "Blur",
    "莫奈取色" to "Monet colors",
    "默认" to "Default",
    "目标 ¥ %s" to "Goal ¥ %s",
    "目标菜单" to "Goal menu",
    "目标金额（元）" to "Goal amount (yuan)",
    "目标进度条与图标跟随应用强调色，可在设置中统一调整。" to "Progress bars and icons follow the app accent; change it in settings.",
    "目标列表" to "Goals",
    "目标排序" to "Reorder goals",
    "目标已达成" to "Goal reached",
    "目标之间独立计算；同一账户可关联多个目标，不代表资金已分开存放。" to
        "Goals are calculated independently; linking one account to several goals does not mean the money is stored separately.",
    "排序" to "Sort",
    "排序菜单" to "Sort menu",
    "启用顶栏和底栏的模糊效果" to "Blur the top bar and the bottom bar",
    "起始余额 ¥ %s" to "Starting balance ¥ %s",
    "浅色" to "Light",
    "强调色" to "Accent color",
    "请Kelsuita吃包辣条" to "Buy Kelsuita a snack",
    "请打开「%s」的账户详情页，显示余额后返回星愿确认" to "Open the account page for \"%s\", then come back to Starwish to confirm.",
    "请输入大于 0 的有效金额，最多两位小数" to "Enter an amount above 0 with at most two decimals",
    "请输入有效余额，最多两位小数" to "Enter a valid balance with at most two decimals",
    "请输入账户名称" to "Enter an account name",
    "请先在关联目标中解除该账户" to "Unlink this account from its goals first",
    "请选择今天或之后的日期" to "Pick today or a later date",
    "请选择至少一个账户" to "Choose at least one account",
    "取消" to "Cancel",
    "取消本次读取" to "Cancel this read",
    "确认替换并导入" to "Replace and import",
    "确认同步" to "Confirm sync",
    "日期格式应为 YYYY-MM-DD，例如 2027-12-31" to "Use YYYY-MM-DD, for example 2027-12-31",
    "色彩规范" to "Color spec",
    "删除" to "Delete",
    "删除这个心愿？" to "Delete this wish?",
    "删除这个账户？" to "Delete this account?",
    "上移" to "Move up",
    "尚无目标" to "No goal yet",
    "设置" to "Settings",
    "设置读取权限" to "Set up page reading",
    "深色" to "Dark",
    "深色模式纯黑背景" to "Pure black dark background",
    "剩余 %s 天 · 每天约 ¥ %s" to "%s days left · about ¥ %s a day",
    "识别于 %s。请核对小星页面，确认后才会更新进度。" to "Read at %s. Check the Xiaoxing page; progress updates only after you confirm.",
    "使用步骤" to "How to use",
    "使用悬浮底栏" to "Use floating navigation bar",
    "悬浮底栏样式" to "Floating bar style",
    "底部导航以悬浮样式显示，横屏时同样生效" to "Show the bottom navigation as a floating bar, including in landscape",
    "使用与小星记账完全一致的账户名称。初始余额请手动填写；之后可读取并确认更新。" to
        "Use exactly the same account name as in Xiaoxing Ledger. Enter the starting balance by hand; afterwards you can read and confirm updates.",
    "收起导航栏" to "Collapse navigation rail",
    "首页" to "Home",
    "首页横幅" to "Home banner",
    "首页横幅 %s %s" to "Home banner %s %s",
    "数据留在你的手机" to "Your data stays on your phone",
    "添加储蓄账户" to "Add a savings account",
    "添加第一个账户" to "Add your first account",
    "添加与小星记账完全相同的账户名称，\n就能为攒钱目标提供余额。" to
        "Add an account with exactly the same name as in Xiaoxing Ledger,\nand it can supply balances for your goals.",
    "添加账户" to "Add account",
    "通过 JSON 文件备份或恢复数据" to "Back up or restore data with a JSON file",
    "拖动排序 %s" to "Reorder %s",
    "外观" to "Appearance",
    "外观设置保存失败" to "Saving appearance settings failed.",
    "未设截止日期" to "No deadline",
    "未授权" to "Not granted",
    "未找到小星记账，请先安装后重试" to "Xiaoxing Ledger is not installed. Install it and try again.",
    "我的账户" to "My accounts",
    "无法读取所选文件" to "The selected file cannot be read",
    "无法读取文件" to "Cannot read the file",
    "无法写入所选文件" to "The selected file cannot be written",
    "无法写入文件" to "Cannot write the file",
    "无需登录。启用一言后，应用会在没有缓存时向一言接口请求一句话；账户、目标与已确认余额只保存在应用私有存储，不会发送给一言接口。不收集页面截图，不保存完整页面文字。卸载应用会删除数据。" to
        "No login. Accounts, goals and confirmed balances stay in private app storage and are never sent anywhere. No page screenshots are collected and no full page text is stored. Uninstalling deletes the data.",
    "下移" to "Move down",
    "先连接一份积累" to "Link some savings first",
    "先去添加账户" to "Add an account first",
    "先添加账户，再设定目标" to "Add an account, then set a goal",
    "小星记账" to "Xiaoxing Ledger",
    "小星暂未提供公开的余额查询接口。此功能只识别你主动打开的账户详情，需要手动确认，不会在后台实时同步。" to
        "Xiaoxing has no public balance API. This only reads the account page you open yourself, needs your confirmation and never syncs in the background.",
    "心愿达成，值得为自己庆祝！" to "Wish fulfilled — time to celebrate!",
    "心愿名称，如：去看一次极光" to "Wish name, e.g. See the aurora",
    "新建目标" to "New goal",
    "新建一个心愿" to "New wish",
    "星愿" to "Starwish",
    "星愿 · 余额读取完成" to "Starwish · Balance read",
    "星愿，已读取，请返回星愿确认余额。点按打开账户确认页" to "Starwish read the balance. Return to Starwish to confirm. Tap to open the confirmation page",
    "星愿攒钱" to "Starwish",
    "星愿攒钱 · 使用说明与隐私" to "Starwish · Help and privacy",
    "Kelsuita倾情出品" to "Made by Kelsuita",
    "修改目标名称、金额、日期不会重置起点。修改关联账户或切换计算方式时，会重新记录起点。" to
        "Changing a goal's name, amount or date keeps its starting point. Changing the linked accounts or the calculation mode records a new starting point.",
    "需要 Android 13 或更高版本" to "Requires Android 13 or later",
    "选择 JSON 备份，确认后替换现有数据" to "Choose a JSON backup; importing replaces the current data",
    "选择应用的明暗模式" to "Choose the app's light or dark mode",
    "选择应用的显示语言" to "Choose the app language",
    "选择应用强调色" to "Choose the app accent color",
    "页面读取仅在你发起后两分钟内生效，并限定为小星记账。不会自动点击、修改账单、访问银行应用或执行转账。" to
        "Page reading only works for two minutes after you start it, and only for Xiaoxing Ledger. It never taps, edits transactions, opens bank apps or transfers money.",
    "页面读取已开启 · 按需读取" to "Page reading on · read on demand",
    "已超过计划日期，可编辑延长计划" to "Past the planned date; edit to extend it",
    "已达成" to "Reached",
    "已读取，请返回星愿" to "Balance read — return to Starwish",
    "已授权" to "Granted",
    "已提前完成这份心愿" to "Finished ahead of schedule",
    "以当前合计余额为起点，只计算之后新增的积累。" to "Start from the current combined balance and count only what is added afterwards.",
    "以所选账户的当前余额合计作为已攒金额。" to "Count the current combined balance of the chosen accounts as saved.",
    "应用" to "App",
    "用于推导 MIUIX 调色板的色调算法" to "Tone algorithm used to derive the MIUIX palette",
    "余额排序" to "Sort by balance",
    "语言" to "Language",
    "语言设置保存失败" to "Saving the language failed.",
    "在系统设置启用「星愿攒钱 · 读取小星账户」后，应用会获得读取小星记账可见文字的能力。" to
        "After you enable \"Starwish · Read Xiaoxing accounts\" in system settings, the app can read the text visible in Xiaoxing Ledger.",
    "攒钱进度 · %s 个目标" to "Saving progress · %s goals",
    "暂不开启" to "Not now",
    "展开导航栏" to "Expand navigation rail",
    "账户" to "Accounts",
    "账户菜单" to "Account menu",
    "账户名称，如：招商银行储蓄卡" to "Account name, e.g. China Merchants Bank debit card",
    "账户名称已存在" to "An account with this name already exists",
    "账户排序" to "Reorder accounts",
    "账户已删除" to "Account deleted",
    "正在处理备份，请稍候…" to "Working on the backup…",
    "主题" to "Theme",
    "主题、强调色、首页样式与模糊效果" to "Theme, accent, home style and blur",
    "主题模式" to "Theme mode",
    "主题设置" to "Theme settings",
    "紫色" to "Purple",
    "自定义名称、金额、截止日期，选择关联账户及进度计算方式。" to "Set a name, amount and deadline, choose linked accounts and how progress is calculated.",
    "自动选择" to "Automatic",
    "最多添加 100 个目标" to "You can add at most 100 goals",
    "最多添加 50 个账户" to "You can add at most 50 accounts",
    // Stored account sources, translated only when displayed.
    "手动录入" to "Manual entry",
    "小星页面识别 · 已确认" to "Read from Xiaoxing · confirmed",
    "预测性返回手势" to "Predictive back gestures",
    "使用系统的预测性返回动画" to "Use the system predictive-back animation",
    "关闭星愿无障碍服务" to "Turn off Starwish accessibility service",
    "请在系统设置中关闭星愿的无障碍服务" to "Turn off Starwish's accessibility service in system settings",
    "备份中的设置无效" to "The backup contains invalid settings.",
    "将账户、目标、全部设置和一言保存为 JSON 文件" to "Save accounts, goals, all settings and the quote as a JSON file",
    "备份文件为明文 JSON，包含账户名称与余额，请妥善保存。导入会替换现有账户、目标和全部设置，建议先导出当前数据。" to
        "The backup is plain JSON with account names and balances, so keep it safe. Importing replaces the current accounts, goals and all settings; export the current data first.",
    "备份包含 %s 个账户、%s 个目标，以及全部设置和一言。确认后将替换当前数据，无法撤销；建议先导出当前数据。" to
        "The backup contains %s accounts, %s goals and all settings. Importing replaces the current data and cannot be undone; export the current data first.",
)
