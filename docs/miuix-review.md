# Miuix 0.9.4 审查记录

审查工作区：`opencode/miuix-review`，基于 `v1.1.0`。原工作区与发布标签未修改。

## 依据与范围

- 用户指定的[快速开始](https://compose-miuix-ui.github.io/miuix/zh_CN/guide/getting-started)和[模糊指南](https://compose-miuix-ui.github.io/miuix/zh_CN/guide/blur)。
- Miuix 0.9.4 固定源码快照 `39c40f99844227b853f0049a0933b1f3ae6c00ba` 的 Scaffold、TopAppBar、Example MainPage / PageUtils、LayerBackdrop / LayerBackdropModifier / DrawBackdropModifier / TextureEffect。
- 应用主题、导航、设置、目标和账户编辑、备份校验与存储，以及余额读取代码。
- 用户指定的 Wasm 演示地址文本抓取仅返回 “App 0%”，未完成交互式演示核对；示例用法以相同固定版本源码为依据。

## 已修正

1. **状态栏没有对应的背景采集内容。** 外层 Scaffold 没有 topBar，却将其顶部系统 inset 作为整个 Pager 的 padding；同时消费了所有外层 padding。内层 TopAppBar 与采集视口被整体推到状态栏下方。现在外层不处理顶部 inset，由各页 TopAppBar 处理，采集视口覆盖整个页面。滚动内容的顶部 padding 仍置于 scroll modifier 内侧，保持初始避让和滚动穿过顶栏的行为。
2. **Pager 相邻页面共用一个 LayerBackdrop。** 一个 backdrop 只有一份 graphics layer / 坐标，多个采集 owner 会互相覆盖。现在每页独立持有，底栏引用目标标签对应的 backdrop。采集放在独立内容 Box 上，顶栏和底栏均不在采集子树中，避免自采集。
3. **二级页底部避让依赖被整体消费的外层 padding。** 内容取内外 Scaffold 底部 padding 的较大值，兼顾带底栏的标签页与没有底栏的二级页面。
4. **账户选择没有选中状态的无障碍语义。** 目标编辑器账户行改用 `toggleable` 和 Checkbox role，让辅助服务获取选中状态。
5. **正常账户数据可能无法导出备份。** 存储允许 `Savings` 与 `savings` 两个不同名称，但备份校验原来忽略大小写而拒绝导出。备份与存储采用一致的精确名称判定，并新增往返回归测试。
6. **切换标签后底栏模糊失效。** 补充复现「首页 → 账户 → 设置 → 首页」，发现底栏继续显示清晰的背景文字。仅替换 backdrop 引用不足以恢复模糊；以 backdrop 和 Pager 的 `settledPage` 为 key 重建底栏消费者，使其在来源切换及页面就位时重新绑定绘制状态。修改后同一路径以及三轮连续切换、返回首页继续滚动均保持模糊。上栏实现未修改。截图：`bottom-switched.png`（修复前）、`bottom-keyed.png`、`bottom-repeat.png`（修复后）。此前仅检查切页后的页面布局，没有充分验证底栏效果，本项补足该遗漏。

## 验证

- `testDebugUnitTest`：12 项通过。
- `assembleDebug`、`assembleRelease`：通过。
- `lintDebug`：0 errors、14 warnings；未通过关闭规则隐藏警告。
- Android 35 隔离模拟器：使用测试账户及 8 个目标，检查浅色 / 深色滚动、状态栏图标可读性、顶栏 / 底栏实际模糊、标签切换、设置与备份二级页面及系统返回。
- SwiftShader 下出现 Skia GLSL 编译错误 `ERROR: 0:176: 'w' : memory exhausted`，效果变透明；降低分辨率不能解决。切换模拟器为宿主 GPU 后该错误消失，截图可见滚动文字在状态栏 / 顶栏和底栏后被模糊。因此 SwiftShader 的截图不能作为模糊生效证据。
- 测试截图位于本机 `Temp/opencode/miuix-review-emulator/host.png`、`dark.png`、`settings.png`、`backup.png`；测试数据仅写入隔离模拟器。

## 验证边界

尚未覆盖真实手机 GPU / 厂商系统、Android 26–32 回退、横屏 / 刘海 / 平板、大字体 / TalkBack 实际朗读、预测返回全手势及文件选择器导入导出完整流程。当前代码保留 API 33 与 shader 能力门控；模拟器结果不能代替这些平台验证。本次未升级依赖；审查修复经用户确认后同步回主工作区，纳入 1.1.5 发布。
