package im.fdx.v2ex.ui.notification

import im.fdx.v2ex.model.NotificationModel

data class NotificationUiState(
    val isLoading: Boolean = false,
    val unreadCount: Int = -1,
    val notifications: List<NotificationModel> = emptyList(),
    val isEmpty: Boolean = false,
)
