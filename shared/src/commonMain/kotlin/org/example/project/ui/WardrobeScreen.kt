package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.pet.Accessory
import org.example.project.pet.AccessoryCatalog
import org.example.project.pet.AccessorySlot
import org.example.project.pet.AccessoryUnlock
import org.example.project.pet.equippedAccessoryIds
import org.example.project.pet.equippedId
import org.example.project.pet.level
import org.example.project.pet.stageBackgroundId
import org.example.project.pet.unlockHint
import org.example.project.pet.unlockedAccessoryIds
import org.example.project.ui.cat.AccessoryBackground
import org.example.project.ui.cat.CatPet

@Composable
fun WardrobeScreen(
    state: PetUiState,
    onEquip: (String) -> Unit,
    onClearSlot: (AccessorySlot) -> Unit,
    onOpenShop: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unlocked = remember(state.pet, state.achievements) {
        unlockedAccessoryIds(
            state.pet,
            state.achievements.filter { it.unlocked }.map { it.achievement.name }.toSet(),
        )
    }
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
                "装扮",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                state.pet.stageBackgroundId?.let {
                    AccessoryBackground(it, Modifier.matchParentSize())
                }
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CatPet(
                        mood = state.pet.mood,
                        onTap = {},
                        accessories = state.pet.equippedAccessoryIds,
                        level = state.pet.level,
                        modifier = Modifier.size(190.dp),
                        drawBackground = false,
                    )
                }
            }
        }

        Text(
            "每个槽位可以装备一件，背景会铺在猫咪身后。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AccessorySlot.entries.forEach { slot ->
            SlotSection(
                slot = slot,
                state = state,
                unlocked = unlocked,
                onEquip = onEquip,
                onClearSlot = onClearSlot,
                onOpenShop = onOpenShop,
            )
        }
    }
}

@Composable
private fun SlotSection(
    slot: AccessorySlot,
    state: PetUiState,
    unlocked: Set<String>,
    onEquip: (String) -> Unit,
    onClearSlot: (AccessorySlot) -> Unit,
    onOpenShop: () -> Unit,
) {
    val equippedId = state.pet.equippedId(slot)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(accessorySlotIcon(slot), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(slot.label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            if (equippedId.isNotEmpty()) {
                TextButton(onClick = { onClearSlot(slot) }) { Text("取下") }
            }
        }
        AccessoryCatalog.forSlot(slot).forEach { accessory ->
            AccessoryRow(
                accessory = accessory,
                equipped = accessory.id == equippedId,
                unlocked = accessory.id in unlocked,
                onEquip = onEquip,
                onOpenShop = onOpenShop,
            )
        }
    }
}

@Composable
private fun AccessoryRow(
    accessory: Accessory,
    equipped: Boolean,
    unlocked: Boolean,
    onEquip: (String) -> Unit,
    onOpenShop: () -> Unit,
) {
    val isShopItem = accessory.unlock is AccessoryUnlock.Shop
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (unlocked) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    accessorySlotIcon(accessory.slot),
                    contentDescription = accessory.title,
                    modifier = Modifier.size(20.dp),
                    tint = if (unlocked) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    accessory.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (unlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (unlocked) accessory.description else accessory.unlockHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            when {
                equipped -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "已装备",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                unlocked -> FilledTonalButton(onClick = { onEquip(accessory.id) }) { Text("装备") }
                isShopItem -> OutlinedButton(onClick = onOpenShop) { Text("去商店") }
                else -> Icon(
                    Icons.Filled.Lock,
                    contentDescription = "未解锁",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

internal fun accessorySlotIcon(slot: AccessorySlot): ImageVector = when (slot) {
    AccessorySlot.HEAD -> Icons.Filled.Checkroom
    AccessorySlot.NECK -> Icons.Filled.Loyalty
    AccessorySlot.BACKGROUND -> Icons.Filled.Landscape
}
