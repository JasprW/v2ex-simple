package im.fdx.v2ex.ui.member.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import im.fdx.v2ex.R
import im.fdx.v2ex.ui.compose.theme.V2exTheme

data class MemberProfileUiModel(
    val username: String,
    val avatarUrl: String,
    val tagline: String? = null,
    val intro: String? = null,
    val joinedDescription: String = "",
    val isOnline: Boolean = false,
    val location: String? = null,
    val github: String? = null,
    val twitter: String? = null,
    val website: String? = null,
    val btc: String? = null,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MemberScreen(
    uiModel: MemberProfileUiModel?,
    tabs: List<String>,
    selectedTab: Int,
    isFollowed: Boolean,
    isBlocked: Boolean,
    showRelationActions: Boolean,
    onBack: () -> Unit,
    onSelectTab: (Int) -> Unit,
    onToggleFollow: () -> Unit,
    onToggleBlock: () -> Unit,
    onReport: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenGithub: () -> Unit,
    onOpenTwitter: () -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenBitcoin: () -> Unit,
    modifier: Modifier = Modifier,
    pagerContent: @Composable (Modifier) -> Unit,
) {
    var actionExpanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = uiModel?.username.orEmpty())
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.topic_action_back),
                        )
                    }
                },
                actions = {
                    if (showRelationActions) {
                        IconButton(onClick = onToggleFollow) {
                            Icon(
                                imageVector = if (isFollowed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFollowed) stringResource(id = R.string.unfollow) else stringResource(id = R.string.follow),
                            )
                        }
                        Box {
                            IconButton(onClick = { actionExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(id = R.string.topic_action_more),
                                )
                            }
                            DropdownMenu(
                                expanded = actionExpanded,
                                onDismissRequest = { actionExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isBlocked) stringResource(id = R.string.cancel_block) else stringResource(id = R.string.block)) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Filled.Block, contentDescription = null)
                                    },
                                    onClick = {
                                        actionExpanded = false
                                        onToggleBlock()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.report_abuse)) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Filled.Report, contentDescription = null)
                                    },
                                    onClick = {
                                        actionExpanded = false
                                        onReport()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            MemberProfileCard(
                model = uiModel,
                onOpenLocation = onOpenLocation,
                onOpenGithub = onOpenGithub,
                onOpenTwitter = onOpenTwitter,
                onOpenWebsite = onOpenWebsite,
                onOpenBitcoin = onOpenBitcoin,
            )

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onSelectTab(index) },
                        text = {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }

            pagerContent(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun MemberProfileCard(
    model: MemberProfileUiModel?,
    onOpenLocation: () -> Unit,
    onOpenGithub: () -> Unit,
    onOpenTwitter: () -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenBitcoin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (model == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
            )
            return@Column
        }

        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    AsyncImage(
                        model = model.avatarUrl,
                        contentDescription = stringResource(id = R.string.profile),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    if (model.isOnline) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.username,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (model.joinedDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = model.joinedDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!model.tagline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = model.tagline,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (!model.intro.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = model.intro,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            val linkItems = buildList {
                if (!model.location.isNullOrBlank()) add(LinkAction(Icons.Filled.LocationOn, onOpenLocation, stringResource(id = R.string.member_link_location)))
                if (!model.twitter.isNullOrBlank()) add(LinkAction(Icons.Filled.Tag, onOpenTwitter, stringResource(id = R.string.member_link_twitter)))
                if (!model.github.isNullOrBlank()) add(LinkAction(Icons.Filled.Code, onOpenGithub, stringResource(id = R.string.member_link_github)))
                if (!model.btc.isNullOrBlank()) add(LinkAction(Icons.Filled.TravelExplore, onOpenBitcoin, stringResource(id = R.string.member_link_bitcoin)))
                if (!model.website.isNullOrBlank()) add(LinkAction(Icons.Filled.Language, onOpenWebsite, stringResource(id = R.string.member_link_website)))
            }

            if (linkItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    linkItems.forEach { item ->
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.contentDescription,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(onClick = item.onClick),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

        }
    }
}

private data class LinkAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
    val contentDescription: String,
)

@Preview(showBackground = true)
@Composable
private fun MemberScreenPreview() {
    V2exTheme {
        MemberScreen(
            uiModel = MemberProfileUiModel(
                username = "Livid",
                avatarUrl = "",
                tagline = "V2EX 创始人",
                intro = "Write less, do more.",
                joinedDescription = "加入于 2009-01-01, 第 1 号会员",
                isOnline = true,
                location = "Shanghai",
                github = "livid",
                website = "v2ex.com",
            ),
            tabs = listOf("主题 (42)", "回复 (128)"),
            selectedTab = 0,
            isFollowed = true,
            isBlocked = false,
            showRelationActions = true,
            onBack = {},
            onSelectTab = {},
            onToggleFollow = {},
            onToggleBlock = {},
            onReport = {},
            onOpenLocation = {},
            onOpenGithub = {},
            onOpenTwitter = {},
            onOpenWebsite = {},
            onOpenBitcoin = {},
            pagerContent = {
                Box(modifier = it.fillMaxSize())
            },
        )
    }
}
