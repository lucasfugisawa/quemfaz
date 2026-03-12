package com.fugisawa.quemfaz.ui.preview

import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
annotation class LightDarkPreview

@Preview(name = "Light", showBackground = true, showSystemUi = true)
@Preview(name = "Dark", showBackground = true, showSystemUi = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
annotation class LightDarkScreenPreview
