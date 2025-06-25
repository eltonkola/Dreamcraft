package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Gamepad2: ImageVector
    get() {
        if (_Gamepad2 != null) return _Gamepad2!!
        
        _Gamepad2 = ImageVector.Builder(
            name = "Gamepad2",
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
                moveTo(6f, 11f)
                horizontalLineToRelative(4f)
                moveTo(8f, 9f)
                verticalLineToRelative(4f)
                moveToRelative(7f, -1f)
                horizontalLineToRelative(0.01f)
                moveTo(18f, 10f)
                horizontalLineToRelative(0.01f)
                moveToRelative(-0.69f, -5f)
                horizontalLineTo(6.68f)
                arcToRelative(4f, 4f, 0f, false, false, -3.978f, 3.59f)
                lineToRelative(-0.017f, 0.152f)
                curveTo(2.604f, 9.416f, 2f, 14.456f, 2f, 16f)
                arcToRelative(3f, 3f, 0f, false, false, 3f, 3f)
                curveToRelative(1f, 0f, 1.5f, -0.5f, 2f, -1f)
                lineToRelative(1.414f, -1.414f)
                arcTo(2f, 2f, 0f, false, true, 9.828f, 16f)
                horizontalLineToRelative(4.344f)
                arcToRelative(2f, 2f, 0f, false, true, 1.414f, 0.586f)
                lineTo(17f, 18f)
                curveToRelative(0.5f, 0.5f, 1f, 1f, 2f, 1f)
                arcToRelative(3f, 3f, 0f, false, false, 3f, -3f)
                curveToRelative(0f, -1.545f, -0.604f, -6.584f, -0.685f, -7.258f)
                quadToRelative(-0.01f, -0.075f, -0.017f, -0.151f)
                arcTo(4f, 4f, 0f, false, false, 17.32f, 5f)
            }
        }.build()
        
        return _Gamepad2!!
    }

private var _Gamepad2: ImageVector? = null

