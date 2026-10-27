package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.pet.AccessoryCatalog
import org.example.project.pet.GrowthEngine
import org.example.project.pet.ShopCatalog
import org.example.project.pet.ShopCategory
import org.example.project.pet.ShopItem

@Composable
fun ShopScreen(
    state: PetUiState,
    onBuy: (ShopItem) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedsLeft = (GrowthEngine.MAX_FEEDS_PER_DAY - state.feedsUsedToday).coerceAtLeast(0)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回主页")
            }
            Text(
                "金币商店",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            CoinChip(state.pet.coins)
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("金币怎么赚？", style = MaterialTheme.typography.titleSmall)
                Text("走路 每 1000 步 +1", style = MaterialTheme.typography.bodySmall)
                Text("喂食 每次 +${GrowthEngine.FEED_COIN_REWARD}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "聊天 每句 +${GrowthEngine.CHAT_COIN_PER_MESSAGE}（每日上限 ${GrowthEngine.MAX_CHAT_COINS_PER_DAY}）",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("完成每日任务 +5 ~ 8", style = MaterialTheme.typography.bodySmall)
            }
        }

        ShopSection(
            title = ShopCategory.FOOD.label,
            subtitle = "购买后立即喂食，每天最多喂 ${GrowthEngine.MAX_FEEDS_PER_DAY} 次（今日剩 $feedsLeft 次）",
        ) {
            ShopCatalog.items
                .filter { it.category == ShopCategory.FOOD }
                .forEach { item -> ShopItemRow(item, state, feedsLeft, onBuy) }
        }

        ShopSection(title = ShopCategory.TOY.label, subtitle = "一次性购买，永久拥有") {
            ShopCatalog.items
                .filter { it.category == ShopCategory.TOY }
                .forEach { item -> ShopItemRow(item, state, feedsLeft, onBuy) }
        }

        ShopSection(title = ShopCategory.ACCESSORY.label, subtitle = "购买后自动装备到对应槽位，可在「装扮」里更换") {
            ShopCatalog.items
                .filter { it.category == ShopCategory.ACCESSORY }
                .forEach { item -> ShopItemRow(item, state, feedsLeft, onBuy) }
        }

        Text(
            "普通粮在主页免费喂食（饱食度 +${ShopCatalog.NORMAL_FOOD.hungerRestore}）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CoinChip(coins: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.MonetizationOn,
            contentDescription = "金币",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "$coins",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun ShopSection(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun ShopItemRow(
    item: ShopItem,
    state: PetUiState,
    feedsLeft: Int,
    onBuy: (ShopItem) -> Unit,
) {
    val owned = item.id in state.pet.ownedItemIds
    val affordable = state.pet.coins >= item.price
    val isFood = item.category == ShopCategory.FOOD
    val enabled = when {
        owned -> false
        isFood -> feedsLeft > 0 && affordable
        else -> affordable
    }
    val actionLabel = when {
        isFood && feedsLeft <= 0 -> "今日已喂饱"
        !affordable -> "金币不足"
        isFood -> "喂食 ${item.price} 币"
        owned -> "已拥有"
        else -> "${item.price} 金币"
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        shopIcon(item),
                        contentDescription = item.title,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (owned) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "已拥有",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    FilledTonalButton(onClick = { onBuy(item) }, enabled = enabled) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

private fun shopIcon(item: ShopItem): ImageVector {
    AccessoryCatalog.byId[item.id]?.let { return accessorySlotIcon(it.slot) }
    return when (item.category) {
        ShopCategory.FOOD -> Icons.Filled.Restaurant
        ShopCategory.TOY -> Icons.Filled.Toys
        ShopCategory.ACCESSORY -> Icons.Filled.Checkroom
    }
}
