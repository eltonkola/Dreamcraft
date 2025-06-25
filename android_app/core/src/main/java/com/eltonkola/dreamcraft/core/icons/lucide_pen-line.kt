package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PenLine: ImageVector
    get() {
        if (_PenLine != null) return _PenLine!!
        
        _PenLine = ImageVector.Builder(
            name = "PenLine",
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
                moveTo(12f, 20f)
                horizontalLineToRelative(9f)
                moveTo(16.376f, 3.622f)
                arcToRelative(1f, 1f, 0f, false, true, 3.002f, 3.002f)
                lineTo(7.368f, 18.635f)
                arcToRelative(2f, 2f, 0f, false, true, -0.855f, 0.506f)
                lineToRelative(-2.872f, 0.838f)
                arcToRelative(0.5f, 0.5f, 0f, false, true, -0.62f, -0.62f)
                lineToRelative(0.838f, -2.872f)
                arcToRelative(2f, 2f, 0f, false, true, 0.506f, -0.854f)
                close()
            }
        }.build()
        
        return _PenLine!!
    }

private var _PenLine: ImageVector? = null

