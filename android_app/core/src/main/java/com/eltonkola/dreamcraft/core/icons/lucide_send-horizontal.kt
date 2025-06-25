package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SendHorizontal: ImageVector
    get() {
        if (_SendHorizontal != null) return _SendHorizontal!!
        
        _SendHorizontal = ImageVector.Builder(
            name = "SendHorizontal",
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
                moveToRelative(3f, 3f)
                lineToRelative(3f, 9f)
                lineToRelative(-3f, 9f)
                lineToRelative(19f, -9f)
                close()
                moveToRelative(3f, 9f)
                horizontalLineToRelative(16f)
            }
        }.build()
        
        return _SendHorizontal!!
    }

private var _SendHorizontal: ImageVector? = null

