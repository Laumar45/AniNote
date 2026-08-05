package com.laumar.anilista.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.laumar.anilista.R
import com.laumar.anilista.viewmodel.SortOrder

@Composable
fun SortToggle(
    selected: SortOrder,
    onSelectedChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        SortOrder.ASC to stringResource(R.string.sort_asc),
        SortOrder.DESC to stringResource(R.string.sort_desc)
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
                    activeContainerColor = MaterialTheme.colorScheme.secondary,
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                    activeBorderColor = MaterialTheme.colorScheme.secondary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                    inactiveBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}
