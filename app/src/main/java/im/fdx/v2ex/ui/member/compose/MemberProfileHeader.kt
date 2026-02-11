package im.fdx.v2ex.ui.member.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import im.fdx.v2ex.ui.compose.theme.V2exTheme
import im.fdx.v2ex.ui.member.Member
import im.fdx.v2ex.ui.member.MemberUiState
import im.fdx.v2ex.utils.TimeUtil

/**
 * Member 页面的用户资料头部
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberProfileHeader(
    uiState: MemberUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when (uiState) {
            is MemberUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MemberUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is MemberUiState.Success -> {
                val member = uiState.member
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 头像和基本信息行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 头像
                        AsyncImage(
                            model = member.avatarLargeUrl,
                            contentDescription = "用户头像",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        // 用户名和在线状态
                        Column(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = member.username,
                                    style = MaterialTheme.typography.headlineSmall
                                )

                                // 在线状态指示器
                                if (uiState.isOnline) {
                                    Icon(
                                        imageVector = Icons.Default.Circle,
                                        contentDescription = "在线",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // 注册时间
                            Text(
                                text = "加入于 ${TimeUtil.getAbsoluteTime(member.created)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // ID 信息
                            Text(
                                text = "第 ${member.id} 号会员",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 签名
                    val tagline = member.tagline
                    if (!tagline.isNullOrEmpty()) {
                        Text(
                            text = tagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 个人简介
                    val bio = member.bio
                    if (!bio.isNullOrEmpty()) {
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 社交链接
                    val socialLinks = listOfNotNull(
                        member.location?.takeIf { it.isNotEmpty() }?.let {
                            Triple(Icons.Default.LocationOn, it, "location")
                        },
                        member.github?.takeIf { it.isNotEmpty() }?.let {
                            Triple(Icons.Default.Code, it, "github")
                        },
                        member.twitter?.takeIf { it.isNotEmpty() }?.let {
                            Triple(Icons.Default.Language, "@$it", "twitter")
                        },
                        member.website?.takeIf { it.isNotEmpty() }?.let {
                            Triple(Icons.Default.Language, it, "website")
                        },
                        member.btc?.takeIf { it.isNotEmpty() }?.let {
                            Triple(Icons.Default.AccountBalanceWallet, it, "bitcoin")
                        }
                    )

                    if (socialLinks.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            socialLinks.forEach { (icon, text, type) ->
                                SocialChip(
                                    icon = icon,
                                    text = text,
                                    onClick = { /* 处理点击 */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialChip(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun MemberProfileHeaderPreview() {
    V2exTheme {
        MemberProfileHeader(
            uiState = MemberUiState.Success(
                member = Member(
                    id = "12345",
                    username = "testuser",
                    tagline = "这是一个测试用户的签名",
                    bio = "这里是个人简介，介绍自己的一些信息。",
                    avatar_normal = "",
                    created = "1234567890",
                    location = "北京",
                    github = "testuser",
                    twitter = "testuser",
                    website = "https://example.com",
                    btc = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
                ),
                isFollowed = false,
                isBlocked = false,
                isOnline = true,
                isMe = false
            )
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun MemberProfileHeaderLoadingPreview() {
    V2exTheme {
        MemberProfileHeader(
            uiState = MemberUiState.Loading
        )
    }
}

@Preview(showBackground = true, name = "Minimal")
@Composable
private fun MemberProfileHeaderMinimalPreview() {
    V2exTheme {
        MemberProfileHeader(
            uiState = MemberUiState.Success(
                member = Member(
                    id = "12345",
                    username = "minimaluser",
                    avatar_normal = "",
                    created = "1234567890"
                ),
                isFollowed = false,
                isBlocked = false,
                isOnline = false,
                isMe = false
            )
        )
    }
}
