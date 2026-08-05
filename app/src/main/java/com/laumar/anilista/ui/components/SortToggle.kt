package com.laumar.anilista.ui.components

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.laumar.anilista.viewmodel.SortOrder

@Composable
fun SortToggle(
    selected: SortOrder,
    onSelectedChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val green = Color(0xFF4CAF50)

    val options = listOf(
        SortOrder.ASC to "1 - 10",
        SortOrder.DESC to "10 - 1"
    )

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (order, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = { onSelectedChange(order) },
                selected = selected == order,
                icon = { SegmentedButtonDefaults.Icon(selected == order) },
                label = { Text(label) },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = green,
                    activeContentColor = Color.White,
                    activeBorderColor = green,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = Color.Gray,
                    inactiveBorderColor = Color.Gray
                )
            )
        }
    }
}
