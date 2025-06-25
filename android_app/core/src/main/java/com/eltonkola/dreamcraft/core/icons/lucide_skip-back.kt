package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SkipBack: ImageVector
    get() {
        if (_SkipBack != null) return _SkipBack!!
        
        _SkipBack = ImageVector.Builder(
            name = "SkipBack",
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
                moveTo(19f, 20f)
                lineTo(9f, 12f)
                lineToRelative(10f, -8f)
                close()
                moveTo(5f, 19f)
                verticalLineTo(5f)
            }
        }.build()
        
        return _SkipBack!!
    }

private var _SkipBack: ImageVector? = null

