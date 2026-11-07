/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * AppsItem.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blankj.utilcode.util.AppUtils

@Composable
fun AppsItem(
    name: String,
    pkg: String,
    isChecked: Boolean,
    onClick: () -> Unit = {},
    activityType: Int? = null,
    onActivityTypeChange: ((Int) -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.clickable {
            onClick()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = AppUtils.getAppIcon(pkg),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .padding(6.dp),
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis
                )
                if (pkg.isNotEmpty()) {
                    Text(
                        text = pkg,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (activityType != null && onActivityTypeChange != null) {
                    var expanded by remember { mutableStateOf(false) }
                    val currentType = activityType
                    val typeLabel = when (currentType) {
                        0 -> "Playing"
                        1 -> "Streaming"
                        2 -> "Listening"
                        3 -> "Watching"
                        5 -> "Competing"
                        else -> "Playing"
                    }
                    val typeIcon = when (currentType) {
                        0 -> Icons.Default.SportsEsports
                        1 -> Icons.Default.LiveTv
                        2 -> Icons.Default.Headphones
                        3 -> Icons.Default.Visibility
                        5 -> Icons.Default.SportsMma
                        else -> Icons.Default.SportsEsports
                    }
                    val typeColor = when (currentType) {
                        0 -> Color(0xFF1976D2)
                        1 -> Color(0xFF9C27B0)
                        2 -> Color(0xFF43A047)
                        3 -> Color(0xFFFF9800)
                        5 -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Box(modifier = Modifier.padding(top = 4.dp)) {
                        SuggestionChip(
                            onClick = { expanded = true },
                            label = {
                                Text(
                                    text = typeLabel,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = typeColor
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = typeColor.copy(alpha = 0.12f),
                                labelColor = typeColor
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = typeColor.copy(alpha = 0.4f),
                                borderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf(
                                0 to "Playing",
                                1 to "Streaming",
                                2 to "Listening",
                                3 to "Watching",
                                5 to "Competing"
                            ).forEach { (typeVal, label) ->
                                DropdownMenuItem(
                                    text = { Text(text = label) },
                                    leadingIcon = {
                                        val icon = when (typeVal) {
                                            0 -> Icons.Default.SportsEsports
                                            1 -> Icons.Default.LiveTv
                                            2 -> Icons.Default.Headphones
                                            3 -> Icons.Default.Visibility
                                            5 -> Icons.Default.SportsMma
                                            else -> Icons.Default.SportsEsports
                                        }
                                        Icon(imageVector = icon, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        if (currentType == typeVal) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        onActivityTypeChange(typeVal)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            KSwitch(
                checked = isChecked,
                modifier = Modifier.padding(start = 12.dp, end = 6.dp),
            )
        }
    }
}