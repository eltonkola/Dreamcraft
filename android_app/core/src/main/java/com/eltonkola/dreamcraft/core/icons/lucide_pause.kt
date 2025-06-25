package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Pause: ImageVector
    get() {
        if (_Pause != null) return _Pause!!
        
        _Pause = ImageVector.Builder(
            name = "Pause",
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
                moveTo(15f, 4f)
                horizontalLineTo(17f)
                arcTo(1f, 1f, 0f, false, true, 18f, 5f)
                verticalLineTo(19f)
                arcTo(1f, 1f, 0f, false, true, 17f, 20f)
                horizontalLineTo(15f)
                arcTo(1f, 1f, 0f, false, true, 14f, 19f)
                verticalLineTo(5f)
                arcTo(1f, 1f, 0f, false, true, 15f, 4f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 4f)
                horizontalLineTo(9f)
                arcTo(1f, 1f, 0f, false, true, 10f, 5f)
                verticalLineTo(19f)
                arcTo(1f, 1f, 0f, false, true, 9f, 20f)
                horizontalLineTo(7f)
                arcTo(1f, 1f, 0f, false, true, 6f, 19f)
                verticalLineTo(5f)
                arcTo(1f, 1f, 0f, false, true, 7f, 4f)
                close()
            }
        }.build()
        
        return _Pause!!
    }

private var _Pause: ImageVector? = null

