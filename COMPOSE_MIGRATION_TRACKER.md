# Jetpack Compose 页面改造进度 Tracker

> 本文档记录 V2EX Simple 应用从传统 XML View 系统迁移到 Jetpack Compose 的完整进度。
>
> **参考分支**: `jet_compose-backup-20260210` 已完成 Topic Detail 的 Compose 化改造（包含完整的主题系统、ViewModel、Repository、富文本解析）。

## 总体进度

| 类别 | 总数 | 已完成 | 进行中 | 待开始 | 完成率 |
|------|------|--------|--------|--------|--------|
| **Activities** | 19 | 1 | 0 | 18 | 5% |
| **Fragments** | 10 | 1 | 0 | 9 | 10% |
| **Adapters** | 12 | 1 | 0 | 11 | 8% |

**整体进度: 7%**

### 已完成 ✅
- **Topic Detail 页面** (参考 `jet_compose-backup-20260210` 分支)
  - `V2exTheme` - Compose 主题系统 (支持 Material3、动态颜色、AMOLED 模式)
  - `TopicActivity` - 使用 `setContent` + Compose
  - `TopicPagerRoute` - 主题分页路由
  - `TopicDetailRoute` - 主题详情页面 (1366 行)
  - `TopicDetailViewModel` - MVVM 架构 (556 行)
  - `TopicDetailRepository` - 数据仓库
  - `TopicHtmlParser` + `TopicRichText` - 富文本渲染
  - 已删除: TopicFragment, TopicAdapter, XML 布局文件

---

## 详细进度列表

### 1. 核心导航与主界面 (Core Navigation)

#### MainActivity
- **文件路径**: `im.fdx.v2ex.ui.main.MainActivity`
- **布局文件**: `activity_main.xml`
- **当前实现**: XML + findViewById
- **复杂度**: 🔴 高
- **功能描述**: 
  - 主界面，带导航抽屉 (Navigation Drawer)
  - Tab 页面管理 (ViewPager2 + ChipTabAdapter)
  - 主题列表 (TopicsFragment)
  - 搜索入口、通知入口
- **改造难点**:
  - Navigation Drawer 需要迁移为 Compose NavigationDrawer
  - ViewPager2 需要替换为 Compose 的 HorizontalPager
  - Tab 系统需要重构为 Compose TabRow
- **依赖页面**: TopicsFragment, TopicsRVAdapter
- **状态**: ⬜ 待开始
- **优先级**: 🔥 高
- **预计工时**: 3-4 天

---

### 2. 主题详情相关 (Topic Detail) ✅ 已完成

> **参考实现**: `jet_compose-backup-20260210` 分支已完成完整改造

#### TopicActivity
- **文件路径**: `im.fdx.v2ex.ui.topic.TopicActivity`
- **当前实现**: ✅ **Compose** (参考分支)
- **实现方式**: `setContent { V2exTheme { TopicPagerRoute(...) } }`
- **复杂度**: 🟡 中
- **功能描述**:
  - 主题详情容器 Activity
  - 使用 Compose HorizontalPager 实现左右滑动切换主题
  - 纯 Compose 实现，无 Fragment
- **状态**: ✅ **已完成**
- **参考文件**: 
  - `TopicActivity.kt` (使用 setContent)
  - `TopicPagerRoute.kt` (分页路由)

#### TopicDetailRoute (原 TopicFragment + TopicAdapter)
- **文件路径**: `im.fdx.v2ex.ui.topic.compose.TopicDetailRoute`
- **当前实现**: ✅ **Compose** (参考分支)
- **代码规模**: 1366 行
- **复杂度**: 🔴 高
- **功能描述**:
  - 主题详情内容页面 (替代原 TopicFragment + TopicAdapter)
  - LazyColumn 显示主题标题、内容、作者信息、回复列表
  - 悬浮操作按钮 (FloatingActionButton)
  - 下拉刷新 (SwipeRefresh)
  - 富文本渲染 (TopicRichText)
  - 回复输入弹窗
- **关键组件**:
  - `TopicHeader` - 主题头部
  - `ReplyList` - 回复列表
  - `FloatingActions` - 悬浮操作栏
  - `TopicRichText` - 富文本组件
- **状态**: ✅ **已完成**

#### TopicDetailViewModel
- **文件路径**: `im.fdx.v2ex.ui.topic.TopicDetailViewModel`
- **当前实现**: ✅ **ViewModel + StateFlow** (参考分支)
- **代码规模**: 556 行
- **复杂度**: 🟡 中
- **功能描述**:
  - 主题详情业务逻辑
  - 状态管理 (UiState, ContentState)
  - 收藏、感谢、回复等操作
- **状态**: ✅ **已完成**

#### TopicDetailRepository
- **文件路径**: `im.fdx.v2ex.ui.topic.data.TopicDetailRepository`
- **当前实现**: ✅ **Repository 模式** (参考分支)
- **复杂度**: 🟡 中
- **功能描述**:
  - 数据获取和缓存
  - 网络请求封装
- **状态**: ✅ **已完成**

#### 富文本渲染 (替代 GoodTextView)
- **文件路径**: 
  - `im.fdx.v2ex.ui.topic.compose.richtext.TopicHtmlParser`
  - `im.fdx.v2ex.ui.topic.compose.richtext.TopicRichText`
- **当前实现**: ✅ **Compose** (参考分支)
- **复杂度**: 🟡 中
- **功能描述**:
  - HTML 解析
  - Compose 富文本渲染
  - 支持链接、图片、代码块等
- **状态**: ✅ **已完成**

#### V2exTheme (主题系统)
- **文件路径**: `im.fdx.v2ex.ui.compose.theme.V2exTheme`
- **当前实现**: ✅ **Material3** (参考分支)
- **复杂度**: 🟢 低
- **功能描述**:
  - Material3 主题
  - 动态颜色支持 (Android 12+)
  - 深色/浅色主题
  - AMOLED 纯黑模式
- **状态**: ✅ **已完成**

---

### 3. 其他页面 (待改造)

#### ReplyComposerBottomSheet
- **文件路径**: `im.fdx.v2ex.ui.topic.ReplyComposerBottomSheet`
- **布局文件**: `bottom_sheet_reply_composer.xml`
- **当前实现**: BottomSheetDialogFragment (当前分支) / Compose (参考分支)
- **复杂度**: 🟢 低
- **功能描述**:
  - 回复输入底部弹窗
  - 富文本编辑器、图片选择
- **改造难点**:
  - 使用 Compose ModalBottomSheet
  - 文本编辑器需要 Compose TextField
- **状态**: ⬜ 待开始
- **优先级**: 🔥 中
- **预计工时**: 1 天

---

### 3. 用户相关 (Member)

#### MemberActivity
- **文件路径**: `im.fdx.v2ex.ui.member.MemberActivity`
- **布局文件**: `activity_member.xml`
- **当前实现**: XML + ViewBinding
- **复杂度**: 🟡 中
- **功能描述**:
  - 用户资料详情页
  - TabLayout + ViewPager2 (用户主题、用户回复)
  - 用户头像、简介、统计数据
- **改造难点**:
  - TabLayout 转为 Compose TabRow
  - ViewPager2 转为 HorizontalPager
- **依赖页面**: UserReplyFragment, ReplyAdapter
- **状态**: ⬜ 待开始
- **优先级**: 🔥 中
- **预计工时**: 2-3 天

#### UserReplyFragment
- **文件路径**: `im.fdx.v2ex.ui.member.UserReplyFragment`
- **布局文件**: `fragment_user_reply.xml`
- **当前实现**: XML + ViewBinding
- **复杂度**: 🟢 低
- **功能描述**:
  - 用户回复列表
  - RecyclerView + ReplyAdapter
- **状态**: ⬜ 待开始
- **优先级**: 🔥 中 (跟随 MemberActivity)
- **预计工时**: 1 天

#### ReplyAdapter
- **文件路径**: `im.fdx.v2ex.ui.member.ReplyAdapter`
- **当前实现**: RecyclerView.Adapter
- **复杂度**: 🟢 低
- **改造方案**: LazyColumn
- **状态**: ⬜ 待开始
- **优先级**: 🔥 中
- **预计工时**: 0.5 天

---

### 4. 节点相关 (Node)

#### NodeActivity
- **文件路径**: `im.fdx.v2ex.ui.node.NodeActivity`
- **布局文件**: `activity_node.xml`
- **当前实现**: XML + ViewBinding
- **复杂度**: 🟡 中
- **功能描述**:
  - 节点详情页面
  - 显示节点信息、节点主题列表
  - 关注/取消关注节点
- **改造难点**:
  - 主题列表转为 LazyColumn
  - 节点头部信息卡片化
- **状态**: ⬜ 待开始
- **优先级**: 🔥 中
- **预计工时**: 2 天

#### AllNodesActivity
- **文件路径**: `im.fdx.v2ex.ui.node.AllNodesActivity`
- **布局文件**: `activity_all_nodes.xml`
- **当前实现**: XML + findViewById
- **复杂度**: 🟢 低
- **功能描述**:
  - 所有节点列表
  - RecyclerView + AllNodesAdapter
  - 搜索节点
- **状态**: ⬜ 待开始
- **优先级**: 🔥 中
- **预计工时**: 1-2 天

#### AllNodesAdapter / AllNodesAdapterNew
- **文件路径**: 
  - `im.fdx.v2ex.ui.node.AllNodesAdapter`
  - `im.fdx.v2ex.ui.node.AllNodesAdapterNew`
- **当前实现**: RecyclerView.Adapter
- **复杂度**: 🟢 低
- **状态**: ⬜ 待开始
- **优先级**: 🔥 中

---

### 5. 收藏相关 (Favorites)

#### FavorActivity
- **文件路径**: `im.fdx.v2ex.ui.favor.FavorActivity`
- **布局文件**: `activity_follow_activity.xml`
- **当前实现**: XML + findViewById
- **复杂度**: 🟡 中
- **功能描述**:
  - 收藏页面
  - TabLayout + ViewPager (主题收藏、节点收藏)
- **依赖页面**: NodeFavorFragment, FavorViewPagerAdapter
- **状态**: ⬜ 待开始
- **优先级**: 🔥 低
- **预计工时**: 2 天

#### NodeFavorFragment
- **文件路径**: `im.fdx.v2ex.ui.favor.NodeFavorFragment`
- **布局文件**: `fragment_node_favor.xml`
- **当前实现**: XML + ViewBinding
- **复杂度**: 🟢 低
- **功能描述**:
  - 收藏的节点列表
- **状态**: ⬜ 待开始
- **优先级**: 🔥 低

---

### 6. 主题列表 (Topic Lists)

#### TopicsFragment
- **文件路径**: `im.fdx.v2ex.ui.main.TopicsFragment`
- **布局文件**: `fragment_topics.xml`
- **当前实现**: XML + findViewById
- **复杂度**: 🟡 中
- **功能描述**:
  - 首页主题列表
  - RecyclerView + TopicsRVAdapter
  - 支持下拉刷新、上拉加载更多
  - 不同 Tab 类型（最新、热门、节点）
- **改造难点**:
  - 需要支持分页加载 (Paging3)
  - 多种主题类型渲染
- **依赖组件**: TopicsRVAdapter, MyDiffCallback
- **状态**: ⬜ 待开始
- **优先级**: 🔥 高 (影响主界面)
- **预计工时**: 2-3 天

#### TopicsRVAdapter
- **文件路径**: `im.fdx.v2ex.ui.main.TopicsRVAdapter`
- **当前实现**: RecyclerView.Adapter + DiffUtil
- **复杂度**: 🟢 低
- **改造方案**: LazyColumn + items()
- **状态**: ⬜ 待开始
- **优先级**: 🔥 高
- **预计工时**: 1 天

---

### 7. 功能性页面 (Functional)

#### SearchActivity
- **文件路径**: `im.fdx.v2ex.ui.main.SearchActivity`
- **布局文件**: `activity_search.xml`
- **当前实现**: XML + findViewById
- **复杂度**: 🟡 中
- **功能描述**:
  - 搜索页面
  - 搜索历史、搜索建议
  - 搜索结果列表
- **改造难点**:
  - 搜索框需要 Compose SearchBar
  - 历史记录流式布局 (Flexbox) 转为 FlowRow
- **状态**: ⬜ 待开始
- **优先级**: 🔥 中
- **预计工时**: 2 天

#### NewTopicActivity
- **文件路径**: `im.fdx.v2ex.ui.main.NewTopicActivity`
- **布局文件**: `activity_new_topic.xml`
- **当前实现**: XML + ViewBinding
- **复杂度**: 🟡 中
- **功能描述**:
  - 发布新主题
  - 节点选择、标题输入、内容编辑
  - 图片上传
- **改造难点**:
  - 富文本编辑器复杂
  - 图片选择和预览
- **状态**: ⬜ 待开始
- **优先级**: 🔥 中
- **预计工时**: 2-3 天

#### LoginActivity
- **文件路径**: `im.fdx.v2ex.ui.LoginActivity`
- **布局文件**: `activity_login.xml`
- **当前实现**: XML + ViewBinding
- **复杂度**: 🟢 低
- **功能描述**:
  - 登录页面
  - 用户名/密码输入、验证码、2FA
- **状态**: ⬜ 待开始
- **优先级**: 🔥 低
- **预计工时**: 1 天

#### NotificationActivity
- **文件路径**: `im.fdx.v2ex.ui.NotificationActivity`
- **布局文件**: `activity_notification.xml`
- **当前实现**: XML + findViewById
- **复杂度**: 🟢 低
- **功能描述**:
  - 通知列表
  - RecyclerView + NotificationAdapter
- **状态**: ⬜ 待开始
- **优先级**: 🔥 低
- **预计工时**: 1 天

#### SettingsActivity
- **文件路径**: `im.fdx.v2ex.ui.SettingsActivity`
- **布局文件**: `activity_settings.xml`, `preference_settings.xml`
- **当前实现**: XML + PreferenceFragmentCompat
- **复杂度**: 🟢 低
- **功能描述**:
  - 设置页面
  - 使用 AndroidX Preference
- **改造难点**:
  - Preference 系统需要完全重写为 Compose
- **状态**: ⬜ 待开始
- **优先级**: 🔥 低
- **预计工时**: 1-2 天

#### TabSettingActivity
- **文件路径**: `im.fdx.v2ex.ui.TabSettingActivity`
- **布局文件**: `activity_tab_setting.xml`
- **当前实现**: XML + findViewById
- **复杂度**: 🟢 低
- **功能描述**:
  - 首页 Tab 管理
  - 拖拽排序、添加/删除 Tab
- **改造难点**:
  - 拖拽排序需要 Compose 拖放手势
- **状态**: ⬜ 待开始
- **优先级**: 🔥 低
- **预计工时**: 1-2 天

#### WebViewActivity
- **文件路径**: `im.fdx.v2ex.ui.WebViewActivity`
- **布局文件**: `activity_web_view.xml`
- **当前实现**: XML + findViewById
- **复杂度**: 🟢 低
- **功能描述**:
  - 内置浏览器
  - 主要使用 WebView
- **改造建议**: 可以保持 XML，内部仅包含 WebView
- **状态**: ⬜ 待开始
- **优先级**: 🔥 低
- **预计工时**: 0.5 天

#### PhotoActivity
- **文件路径**: `im.fdx.v2ex.ui.PhotoActivity`
- **布局文件**: `activity_photo.xml`
- **当前实现**: XML + findViewById
- **复杂度**: 🟢 低
- **功能描述**:
  - 图片查看器
  - 使用 PhotoView 实现缩放
- **改造建议**: 可以使用 Compose Coil + zoomable
- **状态**: ⬜ 待开始
- **优先级**: 🔥 低
- **预计工时**: 1 天

#### TestActivity
- **文件路径**: `im.fdx.v2ex.ui.TestActivity`
- **布局文件**: `activity_test.xml`
- **当前实现**: XML
- **复杂度**: 🟢 低
- **功能描述**: 测试页面
- **状态**: ⬜ 待开始
- **优先级**: 🔥 最低
- **预计工时**: 0.5 天

---

### 8. 其他组件 (Other Components)

#### BaseActivity
- **文件路径**: `im.fdx.v2ex.ui.BaseActivity`
- **当前实现**: AppCompatActivity
- **复杂度**: 🟢 低
- **功能描述**:
  - 基础 Activity，提供主题、字体缩放、Edge-to-Edge 支持
- **改造建议**: 可以继续作为 ComponentActivity 基类，或完全移除
- **状态**: ⬜ 待开始
- **优先级**: 🔥 高 (影响所有 Activity)

#### BaseFragment
- **文件路径**: `im.fdx.v2ex.ui.BaseFragment`
- **当前实现**: Fragment
- **复杂度**: 🟢 低
- **状态**: ⬜ 待开始
- **优先级**: 🔥 中

#### ChipTabAdapter
- **文件路径**: `im.fdx.v2ex.ui.main.ChipTabAdapter`
- **当前实现**: RecyclerView.Adapter
- **复杂度**: 🟢 低
- **功能描述**: 主页 Tab Chip 适配器
- **改造方案**: 使用 Compose Chip 组件
- **状态**: ⬜ 待开始
- **优先级**: 🔥 高

#### MyViewPagerAdapter
- **文件路径**: `im.fdx.v2ex.ui.main.MyViewPagerAdapter`
- **当前实现**: FragmentStateAdapter
- **复杂度**: 🟢 低
- **功能描述**: 主页 ViewPager 适配器
- **改造方案**: 使用 HorizontalPager
- **状态**: ⬜ 待开始
- **优先级**: 🔥 高

#### SimpleNodesTextAdapter
- **文件路径**: `im.fdx.v2ex.ui.node.SimpleNodesTextAdapter`
- **复杂度**: 🟢 低
- **状态**: ⬜ 待开始

#### FavorViewPagerAdapter
- **文件路径**: `im.fdx.v2ex.ui.favor.FavorViewPagerAdapter`
- **复杂度**: 🟢 低
- **状态**: ⬜ 待开始

#### NotificationAdapter
- **文件路径**: `im.fdx.v2ex.ui.NotificationAdapter`
- **复杂度**: 🟢 低
- **状态**: ⬜ 待开始

---

## 改造路线图

### ✅ 阶段 0: Topic Detail 参考实现 (已完成)
> 参考分支: `jet_compose-backup-20260210`

- [x] 添加 Jetpack Compose 依赖 (BOM 2025.01.00)
- [x] 配置 Compose 编译器
- [x] 创建 Compose 主题系统 (Material3 + 动态颜色 + AMOLED)
- [x] Topic Detail 完整 Compose 化
  - [x] TopicActivity (setContent)
  - [x] TopicPagerRoute (HorizontalPager)
  - [x] TopicDetailRoute (1366 行，LazyColumn + 富文本)
  - [x] TopicDetailViewModel (MVVM + StateFlow)
  - [x] TopicDetailRepository
  - [x] 富文本渲染 (TopicHtmlParser + TopicRichText)
  - [x] 删除 TopicFragment, TopicAdapter, XML 布局

### 阶段 1: 基础设施迁移 (第 1 周)
将参考分支的 Compose 基础设施迁移到当前分支:
- [ ] 添加 Jetpack Compose 依赖
- [ ] 复制 V2exTheme 主题系统
- [ ] 复制 Topic Detail Compose 实现
- [ ] 验证 Topic Detail 功能正常
- [ ] 创建共享 Compose 组件库
  - [ ] Loading 组件
  - [ ] Error 组件
  - [ ] Empty 组件
  - [ ] 主题卡片组件
  - [ ] 回复组件

### 阶段 2: 独立简单页面 (第 2-3 周)
- [ ] LoginActivity
- [ ] SettingsActivity
- [ ] NotificationActivity
- [ ] PhotoActivity (可用 Coil + zoomable 替代 PhotoView)
- [ ] WebViewActivity (保持简单)
- [ ] TestActivity

### 阶段 3: 核心列表页面 (第 4-6 周)
- [ ] TopicsFragment + TopicsRVAdapter → TopicsScreen
- [ ] ChipTabAdapter → Compose Chip
- [ ] MyViewPagerAdapter → HorizontalPager
- [ ] MainActivity 主页改造 (Navigation Drawer + ViewPager)

### 阶段 4: 复杂页面 (第 7-9 周)
- [ ] MemberActivity (TabLayout → TabRow)
- [ ] UserReplyFragment
- [ ] SearchActivity (Flexbox → FlowRow)
- [ ] NewTopicActivity

### 阶段 5: 剩余页面 (第 10-11 周)
- [ ] NodeActivity
- [ ] AllNodesActivity
- [ ] FavorActivity
- [ ] NodeFavorFragment
- [ ] TabSettingActivity

### 阶段 6: 清理和优化 (第 12 周)
- [ ] 移除未使用的 XML 布局文件
- [ ] 移除 Adapter 类
- [ ] 代码审查和优化
- [ ] 性能测试
- [ ] Bug 修复

---

## 依赖配置

### 参考分支的实际配置 (app/build.gradle.kts)

```kotlin
dependencies {
    // Compose BOM (参考分支使用 2025.01.00)
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    
    // Compose 核心库
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    // Material3
    implementation("androidx.compose.material3:material3")
    
    // Compose 与 Android 集成
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
}
```

### buildFeatures 配置

```kotlin
android {
    buildFeatures {
        viewBinding = true
        compose = true
    }
}
```

### 参考分支的关键依赖版本

| 依赖 | 版本 |
|------|------|
| BOM | 2025.01.00 |
| activity-compose | 1.10.1 |
| lifecycle-runtime-compose | 2.9.4 |
| lifecycle-viewmodel-compose | 2.9.4 |
| compileSdk | 36 |
| targetSdk | 36 |

---

## 通用 Compose 组件规划

### 1. 基础组件
- `V2exTheme` - 应用主题
- `V2exSurface` - 统一表面容器
- `V2exTopAppBar` - 顶部导航栏
- `V2exBottomBar` - 底部导航栏
- `V2exLoadingIndicator` - 加载指示器
- `V2exErrorView` - 错误视图
- `V2exEmptyView` - 空数据视图

### 2. 业务组件
- `TopicItem` - 主题列表项
- `TopicDetailHeader` - 主题详情头部
- `ReplyItem` - 回复项
- `NodeItem` - 节点项
- `MemberCard` - 用户信息卡片
- `NotificationItem` - 通知项
- `SearchBar` - 搜索栏

### 3. 交互组件
- `SwipeRefresh` - 下拉刷新
- `LoadMoreIndicator` - 加载更多
- `ImageViewer` - 图片查看
- `BottomSheet` - 底部弹窗
- `DropdownMenu` - 下拉菜单

---

## 注意事项

### 1. 与现有代码的兼容性
- 使用 `ComposeView` 在现有 XML 中嵌入 Compose
- 使用 `AndroidView` 在 Compose 中嵌入传统 View（如 WebView）
- 逐步迁移，不要一次性全改

### 2. 状态管理
- 使用 ViewModel 管理业务状态
- 使用 remember/rememberSaveable 管理 UI 状态
- 使用 StateFlow 进行数据流管理

### 3. 主题适配
- 遵循 Material3 设计规范
- 适配深色/浅色主题
- 保持与现有主题颜色一致

### 4. 性能优化
- 使用 `LazyColumn` 替代 RecyclerView
- 使用 `remember` 缓存计算结果
- 使用 `derivedStateOf` 优化频繁计算
- 避免不必要重组

### 5. 测试策略
- 为每个 Compose 组件编写预览 (Preview)
- 编写单元测试验证 ViewModel 逻辑
- 编写 UI 测试验证交互流程

---

## 参考实现详情 (jet_compose-backup-20260210 分支)

### 文件变更统计

```
app/build.gradle.kts                               |   21 +-
app/src/main/java/im/fdx/v2ex/ui/compose/theme/V2exTheme.kt |  146 +++
app/src/main/java/im/fdx/v2ex/ui/topic/TopicActivity.kt     |  244 ++--
app/src/main/java/im/fdx/v2ex/ui/topic/TopicAdapter.kt      |  478 ------- (已删除)
app/src/main/java/im/fdx/v2ex/ui/topic/TopicFragment.kt     |  822 ------------ (已删除)
app/src/main/java/im/fdx/v2ex/ui/topic/TopicDetailViewModel.kt |  556 ++++++
app/src/main/java/im/fdx/v2ex/ui/topic/compose/TopicDetailRoute.kt | 1366 +++++++++++++++++++
app/src/main/java/im/fdx/v2ex/ui/topic/compose/TopicPagerRoute.kt   |   71 +
app/src/main/java/im/fdx/v2ex/ui/topic/compose/richtext/TopicHtmlParser.kt |  242 +++
app/src/main/java/im/fdx/v2ex/ui/topic/compose/richtext/TopicRichText.kt |  152 +++
app/src/main/java/im/fdx/v2ex/ui/topic/data/TopicDetailRepository.kt |  170 +++
app/src/main/res/layout/activity_details.xml       |   15 - (已删除)
app/src/main/res/layout/activity_details_content.xml       |  134 -- (已删除)
app/src/main/res/menu/menu_details.xml             |   40 - (已删除)
```

### 新增文件结构

```
app/src/main/java/im/fdx/v2ex/ui/
├── compose/
│   └── theme/
│       └── V2exTheme.kt                    # 主题系统
├── topic/
│   ├── TopicActivity.kt                    # 改为 setContent
│   ├── TopicDetailViewModel.kt             # ViewModel
│   ├── data/
│   │   └── TopicDetailRepository.kt        # Repository
│   └── compose/
│       ├── TopicPagerRoute.kt              # 分页路由 (71 行)
│       ├── TopicDetailRoute.kt             # 详情页面 (1366 行)
│       └── richtext/
│           ├── TopicHtmlParser.kt          # HTML 解析 (242 行)
│           └── TopicRichText.kt            # 富文本组件 (152 行)
```

### 已删除文件

- `TopicFragment.kt` (822 行 XML 实现)
- `TopicAdapter.kt` (478 行 RecyclerView Adapter)
- `ReplyComposerBottomSheet.kt` (集成到 TopicDetailRoute)
- `BottomReplyList.kt`
- `res/layout/activity_details.xml`
- `res/layout/activity_details_content.xml`
- `res/layout/bottom_sheet_reply_composer.xml`
- `res/layout/item_topic_with_comments.xml`
- `res/menu/menu_details.xml`

### TopicDetailRoute 核心组件

```kotlin
// TopicDetailRoute.kt 主要 Composable 函数
- TopicDetailRoute()           // 主入口
- TopicDetailScreen()          // 屏幕容器
- TopicHeader()                // 主题头部
- ReplyList()                  // 回复列表
- ReplyItem()                  // 回复项
- FloatingActions()            // 悬浮操作按钮
- ReplyComposerSheet()         // 回复输入弹窗
- TopicRichText()              // 富文本渲染
- LoadingState / ErrorState / EmptyState
```

### 架构模式

```
TopicActivity (setContent)
└── TopicPagerRoute (HorizontalPager)
    └── TopicDetailRoute (核心页面)
        ├── TopicDetailViewModel (状态管理)
        │   └── StateFlow<TopicDetailUiState>
        ├── TopicDetailRepository (数据层)
        └── 纯 Compose UI (无 Fragment)
```

---

## 更新记录

| 日期 | 更新人 | 内容 |
|------|--------|------|
| 2026-02-10 | Assistant | 初始版本，完成页面梳理和进度规划 |
| 2026-02-10 | Assistant | 更新：发现 `jet_compose-backup-20260210` 分支已完成 Topic Detail Compose 化，添加参考实现详情 |

---

## 附录

### 文件结构参考

```
app/src/main/java/im/fdx/v2ex/ui/
├── main/
│   ├── MainActivity.kt
│   ├── TopicsFragment.kt
│   ├── SearchActivity.kt
│   ├── NewTopicActivity.kt
│   ├── ChipTabAdapter.kt
│   ├── MyViewPagerAdapter.kt
│   ├── TopicsRVAdapter.kt
│   └── ...
├── topic/
│   ├── TopicActivity.kt
│   ├── TopicFragment.kt
│   ├── TopicAdapter.kt
│   ├── ReplyComposerBottomSheet.kt
│   └── ...
├── member/
│   ├── MemberActivity.kt
│   ├── UserReplyFragment.kt
│   └── ReplyAdapter.kt
├── node/
│   ├── NodeActivity.kt
│   ├── AllNodesActivity.kt
│   └── ...
├── favor/
│   ├── FavorActivity.kt
│   └── NodeFavorFragment.kt
├── LoginActivity.kt
├── SettingsActivity.kt
├── NotificationActivity.kt
├── TabSettingActivity.kt
├── WebViewActivity.kt
├── PhotoActivity.kt
├── TestActivity.kt
├── BaseActivity.kt
└── BaseFragment.kt
```

### 资源文件清单

```
app/src/main/res/layout/
├── activity_main.xml
├── activity_details.xml
├── activity_details_content.xml
├── activity_member.xml
├── activity_node.xml
├── activity_all_nodes.xml
├── activity_search.xml
├── activity_new_topic.xml
├── activity_login.xml
├── activity_settings.xml
├── activity_notification.xml
├── activity_tab_setting.xml
├── activity_web_view.xml
├── activity_photo.xml
├── activity_test.xml
├── activity_follow_activity.xml
├── fragment_topics.xml
├── fragment_user_reply.xml
├── fragment_node_favor.xml
├── item_topic_view.xml
├── item_comments.xml
├── item_reply_view.xml
├── item_topic_detail_view.xml
├── item_notification.xml
├── item_node.xml
├── bottom_sheet_reply_composer.xml
└── ...
```
