package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val TriangleAlert: ImageVector
    get() {
        if (_TriangleAlert != null) return _TriangleAlert!!
        
        _TriangleAlert = ImageVector.Builder(
            name = "TriangleAlert",
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
                moveToRelative(21.73f, 18f)
                lineToRelative(-8f, -14f)
                arcToRelative(2f, 2f, 0f, false, false, -3.48f, 0f)
                lineToRelative(-8f, 14f)
                arcTo(2f, 2f, 0f, false, false, 4f, 21f)
                horizontalLineToRelative(16f)
                arcToRelative(2f, 2f, 0f, false, false, 1.73f, -3f)
                moveTo(12f, 9f)
                verticalLineToRelative(4f)
                moveToRelative(0f, 4f)
                horizontalLineToRelative(0.01f)
            }
        }.build()
        
        return _TriangleAlert!!
    }

private var _TriangleAlert: ImageVector? = null

