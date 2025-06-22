package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SkipForward: ImageVector
    get() {
        if (_SkipForward != null) return _SkipForward!!
        
        _SkipForward = ImageVector.Builder(
            name = "SkipForward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveToRelative(5f, 4f)
                lineToRelative(10f, 8f)
                lineToRelative(-10f, 8f)
                close()
                moveToRelative(14f, 1f)
                verticalLineToRelative(14f)
            }
        }.build()
        
        return _SkipForward!!
    }

private var _SkipForward: ImageVector? = null

