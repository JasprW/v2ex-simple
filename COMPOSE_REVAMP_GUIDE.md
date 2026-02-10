# V2EX Simple Compose Revamp 手册

> 本手册指导如何将传统 XML View 系统迁移到 Jetpack Compose，遵循 Material 3 Expressive 设计规范。

## 目录
1. [核心原则](#核心原则)
2. [项目架构](#项目架构)
3. [文件组织规范](#文件组织规范)
4. [Material 3 Expressive 规范](#material-3-expressive-规范)
5. [组件编写规范](#组件编写规范)
6. [迁移流程](#迁移流程)
7. [代码模板](#代码模板)
8. [最佳实践](#最佳实践)

---

## 核心原则

### 1. 分离关注点 (Separation of Concerns)
```
✅ 正确做法:
├── Screen.kt          # 页面入口，组合 UI 组件
├── ScreenContent.kt   # 纯 UI 组件，包含 @Preview
├── ScreenViewModel.kt # 业务逻辑和状态管理
└── ScreenUiState.kt   # UI 状态定义

❌ 避免:
├── Screen.kt          # UI + 业务逻辑混杂
└── 没有 Preview 的组件
```

### 2. 渐进式迁移
- **阶段 1**: 创建 Compose 实现（保留旧代码）
- **阶段 2**: 测试验收通过
- **阶段 3**: 删除旧代码（XML 布局、Adapter、Fragment）

### 3. 保持视觉一致性
- 重构时保持原有布局和整体样式
- 使用 Material 3 Expressive 组件替代原组件
- 验收后再删除旧资源

---

## 项目架构

### 目录结构
```
app/src/main/java/im/fdx/v2ex/ui/
├── compose/                          # 共享 Compose 组件
│   ├── theme/                        # 主题系统
│   │   ├── V2exTheme.kt             # 主题入口
│   │   ├── Color.kt                 # 颜色定义
│   │   ├── Type.kt                  # 字体排版
│   │   └── Shape.kt                 # 形状定义
│   ├── components/                   # 通用组件
│   │   ├── V2exTopAppBar.kt
│   │   ├── V2exLoadingIndicator.kt
│   │   ├── V2exErrorView.kt
│   │   ├── V2exEmptyView.kt
│   │   └── V2exCard.kt
│   └── preview/                      # 预览工具
│       └── PreviewUtils.kt
├── [feature]/                        # 功能模块
│   ├── [Feature]Activity.kt         # Activity 入口
│   ├── [Feature]ViewModel.kt        # ViewModel
│   ├── [Feature]UiState.kt          # UI 状态
│   ├── components/                   # 该模块专用组件
│   │   ├── [Component]Content.kt    # 纯 UI 组件（带 @Preview）
│   │   └── ...
│   └── compose/                      # Compose 页面路由
│       ├── [Feature]Route.kt        # 页面路由入口
│       └── ...
└── ...
```

### 命名规范

| 类型 | 命名 | 后缀 | 示例 |
|------|------|------|------|
| Activity | PascalCase | Activity | `TopicActivity` |
| ViewModel | PascalCase | ViewModel | `TopicViewModel` |
| UI State | PascalCase | UiState | `TopicUiState` |
| Route/页面 | PascalCase | Route | `TopicRoute` |
| Content 组件 | PascalCase | Content | `TopicHeaderContent` |
| Item 组件 | PascalCase | Item | `ReplyItem` |
| 通用组件 | PascalCase | 组件名 | `V2exCard` |

---

## 文件组织规范

### 1. Activity 文件
```kotlin
// TopicActivity.kt - 仅作为入口
class TopicActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            V2exTheme {
                TopicRoute(
                    viewModel = viewModel(),
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
```

### 2. Route 文件（业务逻辑层）
```kotlin
// TopicRoute.kt - 处理业务逻辑、状态管理
@Composable
fun TopicRoute(
    viewModel: TopicViewModel = viewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    TopicScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onNavigateBack = onNavigateBack,
        // ... 其他回调
    )
}
```

### 3. Screen 文件（纯 UI 层）
```kotlin
// TopicScreen.kt - 纯 UI，必须包含 @Preview
@Composable
fun TopicScreen(
    uiState: TopicUiState,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { V2exTopAppBar(/* ... */) },
        content = { paddingValues ->
            TopicContent(
                uiState = uiState,
                onRefresh = onRefresh,
                modifier = Modifier.padding(paddingValues)
            )
        }
    )
}

// ✅ 必须包含 Preview
@Preview(showBackground = true)
@Composable
private fun TopicScreenPreview() {
    V2exTheme {
        TopicScreen(
            uiState = TopicUiState.Success(
                topics = previewTopics
            ),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

// 多种状态 Preview
@Preview(showBackground = true, name = "Loading")
@Composable
private fun TopicScreenLoadingPreview() {
    V2exTheme {
        TopicScreen(
            uiState = TopicUiState.Loading,
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}
```

### 4. Content 组件文件（可复用 UI）
```kotlin
// TopicContent.kt - 独立组件，带 Preview
@Composable
fun TopicContent(
    uiState: TopicUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is TopicUiState.Loading -> V2exLoadingIndicator()
        is TopicUiState.Success -> TopicList(
            topics = uiState.topics,
            modifier = modifier
        )
        is TopicUiState.Error -> V2exErrorView(
            message = uiState.message,
            onRetry = onRefresh
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TopicContentPreview() {
    V2exTheme {
        TopicContent(
            uiState = TopicUiState.Success(previewTopics),
            onRefresh = {}
        )
    }
}
```

---

## Material 3 Expressive 规范

### 1. 颜色系统

使用 Material 3 Expressive 的扩展颜色系统：

```kotlin
// compose/theme/Color.kt
import androidx.compose.ui.graphics.Color

// Primary - 主要品牌色
val Primary40 = Color(0xFF006C4C)
val Primary90 = Color(0xFF8BF8C8)

// Secondary - 次要颜色
val Secondary40 = Color(0xFF4D6357)
val Secondary90 = Color(0xFFCEE9DA)

// Tertiary - 第三色（强调色）
val Tertiary40 = Color(0xFF3D6373)
val Tertiary90 = Color(0xFFC0E8FA)

// Surface - 表面色
val Surface = Color(0xFFF5FBF5)
val SurfaceVariant = Color(0xFFDCE5DD)

// Error - 错误色
val Error40 = Color(0xFFBA1A1A)
val Error90 = Color(0xFFFFDAD6)

// 扩展颜色 - Material 3 Expressive
val PrimaryFixed = Color(0xFF8BF8C8)
val PrimaryFixedDim = Color(0xFF6CDBAD)
val SecondaryFixed = Color(0xFFCEE9DA)
val TertiaryFixed = Color(0xFFC0E8FA)
```

### 2. 字体排版 (Typography)

```kotlin
// compose/theme/Type.kt
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val V2exTypography = Typography(
    // Display - 大标题
    displayLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline - 页面标题
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Title - 卡片标题
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body - 正文
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // Label - 标签
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

### 3. 形状 (Shapes)

```kotlin
// compose/theme/Shape.kt
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val V2exShapes = Shapes(
    // 小形状 - 按钮、小卡片
    small = RoundedCornerShape(8.dp),
    // 中形状 - 卡片、输入框
    medium = RoundedCornerShape(12.dp),
    // 大形状 - 对话框、底部弹窗
    large = RoundedCornerShape(16.dp),
    // 超大形状 - 全屏页面
    extraLarge = RoundedCornerShape(28.dp)
)
```

### 4. 间距系统

```kotlin
// compose/theme/Dimension.kt
import androidx.compose.ui.unit.dp

object V2exDimension {
    // 基础间距单位
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 16.dp
    val spaceLg = 24.dp
    val spaceXl = 32.dp
    val space2xl = 48.dp
    
    // 卡片内边距
    val cardPadding = 16.dp
    val cardPaddingCompact = 12.dp
    
    // 列表间距
    val listItemSpacing = 8.dp
    val listSectionSpacing = 16.dp
    
    // 屏幕边距
    val screenPadding = 16.dp
    val screenPaddingCompact = 12.dp
}
```

---

## 组件编写规范

### 1. 使用 Material 3 Expressive 组件

```kotlin
// ✅ 正确使用 Material 3 组件
import androidx.compose.material3.*

// 按钮
FilledButton(onClick = { }) { }
OutlinedButton(onClick = { }) { }
TextButton(onClick = { }) { }
ElevatedButton(onClick = { }) { }
FilledTonalButton(onClick = { }) { }

// 输入框
OutlinedTextField(
    value = text,
    onValueChange = { },
    label = { Text("标签") },
    supportingText = { Text("辅助文本") }
)

// 卡片
ElevatedCard(
    onClick = { },
    shape = MaterialTheme.shapes.medium
) {
    // 内容
}

// 列表项
ListItem(
    headlineContent = { Text("标题") },
    supportingContent = { Text("描述") },
    leadingContent = { Icon(Icons.Default.Person, null) },
    trailingContent = { Icon(Icons.Default.ChevronRight, null) }
)

// Chip
FilterChip(
    selected = selected,
    onClick = { },
    label = { Text("标签") }
)

// 分割线
HorizontalDivider()
VerticalDivider()
```

### 2. 使用 Material 标准图标

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*

// 导航
Icons.AutoMirrored.Filled.ArrowBack
Icons.AutoMirrored.Filled.ArrowForward
Icons.AutoMirrored.Filled.KeyboardArrowRight

// 操作
Icons.Filled.Add
Icons.Filled.Close
Icons.Filled.Done
Icons.Filled.Edit
Icons.Filled.Delete
Icons.Filled.Share
Icons.Filled.Favorite
Icons.Filled.FavoriteBorder

// 内容
Icons.Filled.Search
Icons.Filled.Menu
Icons.Filled.MoreVert
Icons.Filled.Settings
Icons.Filled.Notifications
Icons.Filled.Person
Icons.Filled.Home

// 通信
Icons.Outlined.Chat
Icons.Outlined.Mail
Icons.Outlined.Send

// 状态
Icons.Filled.Info
Icons.Filled.Warning
Icons.Filled.Error
Icons.Filled.CheckCircle
```

### 3. 组件参数顺序

```kotlin
@Composable
fun TopicItem(
    // 1. 必需参数
    topic: Topic,
    onClick: () -> Unit,
    // 2. 可选参数（带默认值）
    showDivider: Boolean = true,
    maxLines: Int = 3,
    // 3. Modifier 参数（始终放在最后）
    modifier: Modifier = Modifier,
) {
    // 实现
}
```

### 4. Modifier 使用规范

```kotlin
// ✅ 正确使用 Modifier
Card(
    modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = V2exDimension.screenPadding)
        .clickable(onClick = onClick)
) {
    Column(
        modifier = Modifier
            .padding(V2exDimension.cardPadding)
    ) {
        // 内容
    }
}

// ✅ 条件 Modifier
modifier = modifier.then(
    if (isSelected) {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
    } else {
        Modifier
    }
)
```

---

## 迁移流程

### 标准迁移步骤

```
步骤 1: 分析现有页面
├── 1.1 识别 Activity/Fragment 功能
├── 1.2 列出所有 UI 组件
├── 1.3 识别业务逻辑
└── 1.4 记录交互行为

步骤 2: 创建 Compose 实现（不删除旧代码）
├── 2.1 创建 ViewModel
├── 2.2 创建 UiState
├── 2.3 创建 Screen 组件（带 Preview）
├── 2.4 创建 Route 组件
├── 2.5 创建所需 Content 组件
└── 2.6 创建 Preview 数据

步骤 3: 验证 Compose 实现
├── 3.1 IDE Preview 检查
├── 3.2 单元测试 ViewModel
├── 3.3 运行应用验证
└── 3.4 对比原有样式

步骤 4: 替换旧实现
├── 4.1 Activity 改为 setContent
├── 4.2 测试完整流程
└── 4.3 标记旧代码为废弃

步骤 5: 验收测试
├── 5.1 功能测试
├── 5.2 UI 对比测试
├── 5.3 性能测试
└── 5.4 用户验收

步骤 6: 清理旧代码（验收通过后）
├── 6.1 删除 Fragment/Adapter
├── 6.2 删除 XML 布局
├── 6.3 删除无用资源
└── 6.4 代码审查
```

---

## 代码模板

### 1. 新页面模板

```kotlin
// [Feature]Activity.kt
package im.fdx.v2ex.ui.[feature]

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.compose.theme.V2exTheme

class [Feature]Activity : BaseActivity() {
    
    private val viewModel: [Feature]ViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdgeWindow()
        
        setContent {
            V2exTheme {
                [Feature]Route(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
```

```kotlin
// [Feature]UiState.kt
package im.fdx.v2ex.ui.[feature]

sealed interface [Feature]UiState {
    data object Loading : [Feature]UiState
    data class Success(val data: [DataType]) : [Feature]UiState
    data class Error(val message: String) : [Feature]UiState
}
```

```kotlin
// [Feature]ViewModel.kt
package im.fdx.v2ex.ui.[feature]

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class [Feature]ViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow<[Feature]UiState>([Feature]UiState.Loading)
    val uiState: StateFlow<[Feature]UiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = [Feature]UiState.Loading
            try {
                // 加载数据
                val data = fetchData()
                _uiState.value = [Feature]UiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = [Feature]UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    private suspend fun fetchData(): [DataType] {
        // 实现
    }
}
```

```kotlin
// compose/[Feature]Route.kt
package im.fdx.v2ex.ui.[feature].compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import im.fdx.v2ex.ui.[feature].[Feature]ViewModel

@Composable
fun [Feature]Route(
    viewModel: [Feature]ViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    [Feature]Screen(
        uiState = uiState,
        onRefresh = viewModel::loadData,
        onNavigateBack = onNavigateBack
    )
}
```

```kotlin
// [Feature]Screen.kt
package im.fdx.v2ex.ui.[feature].compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import im.fdx.v2ex.ui.compose.components.V2exTopAppBar
import im.fdx.v2ex.ui.compose.theme.V2exTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun [Feature]Screen(
    uiState: [Feature]UiState,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            V2exTopAppBar(
                title = "页面标题",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        [Feature]Content(
            uiState = uiState,
            onRefresh = onRefresh,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun [Feature]ScreenPreview() {
    V2exTheme {
        [Feature]Screen(
            uiState = [Feature]UiState.Success(previewData),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun [Feature]ScreenLoadingPreview() {
    V2exTheme {
        [Feature]Screen(
            uiState = [Feature]UiState.Loading,
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun [Feature]ScreenErrorPreview() {
    V2exTheme {
        [Feature]Screen(
            uiState = [Feature]UiState.Error("网络错误"),
            onRefresh = {},
            onNavigateBack = {}
        )
    }
}
```

```kotlin
// [Feature]Content.kt
package im.fdx.v2ex.ui.[feature].compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import im.fdx.v2ex.ui.compose.components.V2exEmptyView
import im.fdx.v2ex.ui.compose.components.V2exErrorView
import im.fdx.v2ex.ui.compose.components.V2exLoadingIndicator
import im.fdx.v2ex.ui.compose.theme.V2exDimension
import im.fdx.v2ex.ui.compose.theme.V2exTheme

@Composable
fun [Feature]Content(
    uiState: [Feature]UiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is [Feature]UiState.Loading -> {
                V2exLoadingIndicator()
            }
            is [Feature]UiState.Success -> {
                [Feature]List(
                    items = uiState.data,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is [Feature]UiState.Error -> {
                V2exErrorView(
                    message = uiState.message,
                    onRetry = onRefresh
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun [Feature]ContentSuccessPreview() {
    V2exTheme {
        [Feature]Content(
            uiState = [Feature]UiState.Success(previewData),
            onRefresh = {}
        )
    }
}

// 预览数据
internal val previewData = listOf(
    // 预览数据...
)
```

### 2. 通用组件模板

```kotlin
// compose/components/V2exCard.kt
package im.fdx.v2ex.ui.compose.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import im.fdx.v2ex.ui.compose.theme.V2exDimension
import im.fdx.v2ex.ui.compose.theme.V2exTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2exCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(V2exDimension.cardPadding),
            content = content
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun V2exCardPreview() {
    V2exTheme {
        V2exCard(onClick = {}) {
            Text(
                text = "卡片标题",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "卡片内容描述",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

```kotlin
// compose/components/V2exTopAppBar.kt
package im.fdx.v2ex.ui.compose.components

import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import im.fdx.v2ex.ui.compose.theme.V2exTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V2exTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(navigationIcon, contentDescription = "返回")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun V2exTopAppBarPreview() {
    V2exTheme {
        V2exTopAppBar(
            title = "页面标题"
        )
    }
}
```

---

## 最佳实践

### 1. 状态管理

```kotlin
// ✅ 使用 StateFlow + collectAsStateWithLifecycle
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// ✅ UI 状态应是不可变的 data class
data class TopicUiState(
    val isLoading: Boolean = false,
    val topics: List<Topic> = emptyList(),
    val errorMessage: String? = null
)

// ✅ 使用 rememberSaveable 保存配置变更
var selectedTab by rememberSaveable { mutableIntStateOf(0) }

// ✅ 使用 derivedStateOf 优化计算
val filteredItems by remember {
    derivedStateOf {
        items.filter { it.isVisible }
    }
}
```

### 2. 列表优化

```kotlin
// ✅ 使用 LazyColumn 替代 RecyclerView
LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(vertical = V2exDimension.spaceMd),
    verticalArrangement = Arrangement.spacedBy(V2exDimension.listItemSpacing)
) {
    items(
        items = topics,
        key = { it.id }
    ) { topic ->
        TopicItem(
            topic = topic,
            onClick = { onTopicClick(topic) }
        )
    }
}

// ✅ 使用 key 优化重组
items(
    items = topics,
    key = { it.id },
    contentType = { it::class.simpleName }
) { topic ->
    // 内容
}
```

### 3. 副作用处理

```kotlin
// ✅ 使用 LaunchedEffect 处理一次性事件
LaunchedEffect(Unit) {
    viewModel.loadData()
}

// ✅ 使用 SideEffect 同步外部系统
SideEffect {
    analytics.trackScreenView("TopicDetail")
}

// ✅ 使用 DisposableEffect 处理需要清理的资源
DisposableEffect(Unit) {
    val listener = object : SomeListener {
        override fun onEvent() { }
    }
    someManager.addListener(listener)
    onDispose {
        someManager.removeListener(listener)
    }
}
```

### 4. 动画使用

```kotlin
// ✅ 使用 Material 3 动画规范
import androidx.compose.animation.*
import androidx.compose.animation.core.*

// 内容出现动画
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically()
) {
    Content()
}

// 列表项动画
AnimatedContent(
    targetState = uiState,
    transitionSpec = {
        fadeIn(animationSpec = tween(300)) togetherWith
        fadeOut(animationSpec = tween(300))
    }
) { state ->
    when (state) {
        // 内容
    }
}
```

### 5. 无障碍支持

```kotlin
// ✅ 添加内容描述
Icon(
    imageVector = Icons.Filled.Favorite,
    contentDescription = "收藏"  // 不要为 null
)

// ✅ 使用语义修饰符
modifier = Modifier.semantics {
    contentDescription = "用户头像"
    stateDescription = if (isSelected) "已选择" else "未选择"
}

// ✅ 点击区域优化
modifier = Modifier
    .clickable(
        onClick = onClick,
        role = Role.Button,
        onClickLabel = "打开详情"
    )
```

---

## 预览指南

### 1. 多设备预览

```kotlin
@Preview(
    showBackground = true,
    name = "Phone",
    device = "spec:shape=Normal,width=360,height=640,unit=dp,dpi=480"
)
@Preview(
    showBackground = true,
    name = "Tablet",
    device = "spec:shape=Normal,width=800,height=1280,unit=dp,dpi=480"
)
@Preview(
    showBackground = true,
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun MultiPreview() {
    V2exTheme {
        Content()
    }
}
```

### 2. 交互式预览

```kotlin
@Preview(showBackground = true)
@Composable
private fun InteractivePreview() {
    V2exTheme {
        var isExpanded by remember { mutableStateOf(false) }
        
        Card(
            modifier = Modifier.clickable { isExpanded = !isExpanded }
        ) {
            Column {
                Text("点击展开")
                if (isExpanded) {
                    Text("展开的内容")
                }
            }
        }
    }
}
```

### 3. 预览工具类

```kotlin
// compose/preview/PreviewUtils.kt
package im.fdx.v2ex.ui.compose.preview

import androidx.compose.ui.tooling.preview.Preview

@Preview(
    showBackground = true,
    name = "Light"
)
annotation class LightPreview

@Preview(
    showBackground = true,
    name = "Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
annotation class DarkPreview

@LightPreview
@DarkPreview
annotation class ThemePreview
```

---

## 检查清单

### 提交前检查

- [ ] 所有 UI 组件都有 @Preview
- [ ] Preview 覆盖了 Loading/Success/Error 状态
- [ ] 业务逻辑在 ViewModel 中，不在 Composable 中
- [ ] 使用了 Material 3 Expressive 组件
- [ ] 使用了 Material 标准图标
- [ ] 遵循了颜色系统和字体排版规范
- [ ] 添加了合适的 contentDescription
- [ ] 旧代码未被删除（验收前）
- [ ] 代码通过 lint 检查

### 验收检查

- [ ] 功能与原实现一致
- [ ] UI 样式与原设计一致
- [ ] 性能没有明显退化
- [ ] 无障碍功能正常
- [ ] 深色主题适配
- [ ] 不同屏幕尺寸适配

---

## 附录

### 常用 Material 3 Expressive 组件速查

| 用途 | 组件 | 说明 |
|------|------|------|
| 主按钮 | `FilledButton` | 高强调操作 |
| 次按钮 | `OutlinedButton` | 中等强调操作 |
| 文字按钮 | `TextButton` | 低强调操作 |
| 提升按钮 | `ElevatedButton` | 需要阴影的按钮 |
| 色调按钮 | `FilledTonalButton` | 使用 secondary 颜色 |
| 浮动按钮 | `FloatingActionButton` | 主要操作 |
| 小浮动按钮 | `SmallFloatingActionButton` | 紧凑空间 |
| 扩展浮动按钮 | `ExtendedFloatingActionButton` | 带文字的 FAB |
| 卡片 | `Card` | 基础卡片 |
| 提升卡片 | `ElevatedCard` | 带阴影的卡片 |
| 填充卡片 | `FilledCard` | 背景填充色 |
| 输入框 | `OutlinedTextField` | 带边框输入框 |
| 纯文本输入 | `TextField` | 下划线样式 |
| 对话框 | `AlertDialog` | 警告/确认对话框 |
| 底部弹窗 | `ModalBottomSheet` | 从底部弹出的面板 |
| Chip | `AssistChip` | 辅助操作 |
| 过滤 Chip | `FilterChip` | 过滤/选择 |
| 输入 Chip | `InputChip` | 输入/标签 |
| 建议 Chip | `SuggestionChip` | 建议选项 |
| 分割线 | `HorizontalDivider` | 水平分割线 |
| 列表项 | `ListItem` | 标准列表项 |
| 开关 | `Switch` | 布尔开关 |
| 复选框 | `Checkbox` | 多选 |
| 单选按钮 | `RadioButton` | 单选 |
| 滑块 | `Slider` | 数值选择 |
| 进度条 | `LinearProgressIndicator` | 线性进度 |
| 圆形进度 | `CircularProgressIndicator` | 圆形进度 |
| 导航栏 | `NavigationBar` | 底部导航 |
| 导航抽屉 | `ModalNavigationDrawer` | 侧边抽屉 |
| 顶部应用栏 | `TopAppBar` | 页面标题栏 |
| 居中对齐应用栏 | `CenterAlignedTopAppBar` | 居中标题 |
| 底部应用栏 | `BottomAppBar` | 底部操作栏 |
| 标签页 | `TabRow` | 顶部标签 |
| 二级标签 | `SecondaryTabRow` | 次级标签 |
| 徽章 | `Badge` | 角标 |
| 提示 | `Tooltip` | 悬停提示 |
| 搜索栏 | `SearchBar` | 搜索输入 |
| 停靠搜索栏 | `DockedSearchBar` | 带 suggestions |
| 横幅 | `Banner` | 顶部通知条 |
| 走马灯 | `Marquee` | 滚动文字 |
| 时间选择器 | `TimePicker` | 选择时间 |
| 日期选择器 | `DatePicker` | 选择日期 |
| 范围选择器 | `DateRangePicker` | 选择日期范围 |

### 图标速查

```kotlin
// 导航
Icons.AutoMirrored.Filled.ArrowBack
Icons.AutoMirrored.Filled.ArrowForward
Icons.AutoMirrored.Filled.KeyboardArrowRight
Icons.AutoMirrored.Filled.KeyboardArrowLeft
Icons.AutoMirrored.Filled.Send

// 操作
Icons.Filled.Add
Icons.Filled.Close
Icons.Filled.Done
Icons.Filled.Edit
Icons.Filled.Delete
Icons.Filled.Share
Icons.Filled.MoreVert
Icons.Filled.MoreHoriz
Icons.Filled.Menu
Icons.Filled.Settings
Icons.Filled.Search
Icons.Filled.Refresh

// 内容
Icons.Filled.Home
Icons.Filled.Person
Icons.Filled.Notifications
Icons.Filled.Favorite
Icons.Filled.FavoriteBorder
Icons.Filled.Bookmark
Icons.Filled.BookmarkBorder
Icons.Filled.Star
Icons.Filled.StarBorder

// 状态
Icons.Filled.Info
Icons.Filled.Warning
Icons.Filled.Error
Icons.Filled.CheckCircle
Icons.Filled.RadioButtonUnchecked
Icons.Filled.RadioButtonChecked
Icons.Filled.CheckBox
Icons.Filled.CheckBoxOutlineBlank

// 通信
Icons.Filled.Chat
Icons.Filled.ChatBubble
Icons.Filled.ChatBubbleOutline
Icons.Filled.Mail
Icons.Filled.MailOutline
Icons.Filled.Call

// 媒体
Icons.Filled.PlayArrow
Icons.Filled.Pause
Icons.Filled.Stop
Icons.Filled.SkipNext
Icons.Filled.SkipPrevious
Icons.Filled.VolumeUp
Icons.Filled.VolumeOff

// 文件
Icons.Filled.Folder
Icons.Filled.InsertDriveFile
Icons.Filled.Image
Icons.Filled.AttachFile
Icons.Filled.Download
Icons.Filled.Upload
```

---

## 更新记录

| 日期 | 版本 | 更新内容 |
|------|------|----------|
| 2026-02-10 | 1.0 | 初始版本，包含完整迁移规范和模板 |

---

> **重要提醒**: 在验收通过前，**不要删除任何旧代码**。保持新旧代码并存，确保随时可以回滚。
