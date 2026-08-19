package com.laumar.aninote.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laumar.aninote.R
import com.laumar.aninote.viewmodel.SortOrder

@Composable
fun SortToggle(
    selected: SortOrder,
    onSelectedChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        SortOrder.DESC to stringResource(R.string.sort_recent),
        SortOrder.ASC to stringResource(R.string.sort_oldest)
    )

    SingleChoiceSegmentedButtonRow(modifier = modifier.height(34.dp)) {
        options.forEachIndexed { index, (order, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                onClick = { onSelectedChange(order) },
                selected = selected == order,
                icon = {},
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
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
