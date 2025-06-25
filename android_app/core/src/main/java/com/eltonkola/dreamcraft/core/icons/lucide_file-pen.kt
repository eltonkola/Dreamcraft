package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FilePen: ImageVector
    get() {
        if (_FilePen != null) return _FilePen!!
        
        _FilePen = ImageVector.Builder(
            name = "FilePen",
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
                moveTo(12.5f, 22f)
                horizontalLineTo(18f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
                verticalLineTo(7f)
                lineToRelative(-5f, -5f)
                horizontalLineTo(6f)
                arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
                verticalLineToRelative(9.5f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 2f)
                verticalLineToRelative(4f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
                horizontalLineToRelative(4f)
                moveToRelative(-6.622f, 7.626f)
                arcToRelative(1f, 1f, 0f, true, false, -3.004f, -3.004f)
                lineToRelative(-5.01f, 5.012f)
                arcToRelative(2f, 2f, 0f, false, false, -0.506f, 0.854f)
                lineToRelative(-0.837f, 2.87f)
                arcToRelative(0.5f, 0.5f, 0f, false, false, 0.62f, 0.62f)
                lineToRelative(2.87f, -0.837f)
                arcToRelative(2f, 2f, 0f, false, false, 0.854f, -0.506f)
                close()
            }
        }.build()
        
        return _FilePen!!
    }

private var _FilePen: ImageVector? = null

