package top.chiloven.daymark.core.designsystem.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import top.chiloven.daymark.core.designsystem.R
import top.chiloven.daymark.core.model.Category

@Composable
fun Category.displayName(): String = when (id) {
    1L -> stringResource(R.string.ds_category_birthday)
    2L -> stringResource(R.string.ds_category_anniversary)
    3L -> stringResource(R.string.ds_category_holiday)
    4L -> stringResource(R.string.ds_category_milestone)
    else -> stringResource(R.string.ds_category_other)
}
