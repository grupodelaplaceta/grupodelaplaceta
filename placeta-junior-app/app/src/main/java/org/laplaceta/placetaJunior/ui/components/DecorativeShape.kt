package org.laplaceta.placetajunior.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Forma geométrica GDLP — hexágono irregular estilizado
 * Basado EXACTAMENTE en forma.svg del diseño corporativo
 * viewBox="0 0 372 384"
 * Se puede tintar con cualquier color de la paleta (#FF3333, #FF6600, #D6CE52, etc.)
 */
@Composable
fun GdlpShape(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val s = minOf(size.width / 372f, size.height / 384f)
        val path = buildFormaPath(s)
        drawPath(path, color, style = Fill)
    }
}

@Composable
fun GdlpShapeOutline(
    modifier: Modifier = Modifier,
    color: Color,
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier) {
        val s = minOf(size.width / 372f, size.height / 384f)
        val path = buildFormaPath(s)
        drawPath(path, color, style = Stroke(width = strokeWidth))
    }
}

private fun DrawScope.buildFormaPath(s: Float): Path {
    fun x(v: Float) = v * s
    fun y(v: Float) = v * s

    return Path().apply {
        // Path exterior (hexágono irregular)
        moveTo(x(200.542969f), y(68.210938f))
        lineTo(x(212.851562f), y(82.246094f))
        cubicTo(x(217.613281f), y(87.675781f), x(224.960938f), y(90.066406f), x(232.003906f), y(88.46875f))
        lineTo(x(250.210938f), y(84.347656f))
        cubicTo(x(261.8125f), y(81.722656f), x(273.078125f), y(89.90625f), x(274.164062f), y(101.753906f))
        lineTo(x(275.871094f), y(120.34375f))
        cubicTo(x(276.53125f), y(127.53125f), x(281.074219f), y(133.785156f), x(287.710938f), y(136.632812f))
        lineTo(x(304.863281f), y(144.003906f))
        cubicTo(x(315.792969f), y(148.695312f), x(320.09375f), y(161.9375f), x(314.011719f), y(172.164062f))
        lineTo(x(304.464844f), y(188.203125f))
        cubicTo(x(300.773438f), y(194.410156f), x(300.773438f), y(202.136719f), x(304.464844f), y(208.34375f))
        lineTo(x(314.011719f), y(224.386719f))
        cubicTo(x(320.09375f), y(234.609375f), x(315.792969f), y(247.851562f), x(304.863281f), y(252.546875f))
        lineTo(x(287.710938f), y(259.914062f))
        cubicTo(x(281.074219f), y(262.761719f), x(276.53125f), y(269.015625f), x(275.871094f), y(276.207031f))
        lineTo(x(274.164062f), y(294.792969f))
        cubicTo(x(273.078125f), y(306.640625f), x(261.8125f), y(314.824219f), x(250.210938f), y(312.199219f))
        lineTo(x(232.003906f), y(308.078125f))
        cubicTo(x(224.960938f), y(306.484375f), x(217.613281f), y(308.871094f), x(212.851562f), y(314.300781f))
        lineTo(x(200.542969f), y(328.335938f))
        cubicTo(x(192.703125f), y(337.28125f), x(178.777344f), y(337.28125f), x(170.933594f), y(328.335938f))
        lineTo(x(158.628906f), y(314.300781f))
        cubicTo(x(153.867188f), y(308.871094f), x(146.515625f), y(306.484375f), x(139.476562f), y(308.078125f))
        lineTo(x(121.269531f), y(312.199219f))
        cubicTo(x(109.664062f), y(314.824219f), x(98.402344f), y(306.640625f), x(97.3125f), y(294.792969f))
        lineTo(x(95.605469f), y(276.207031f))
        cubicTo(x(94.945312f), y(269.015625f), x(90.402344f), y(262.761719f), x(83.769531f), y(259.914062f))
        lineTo(x(66.617188f), y(252.546875f))
        cubicTo(x(55.6875f), y(247.851562f), x(51.382812f), y(234.609375f), x(57.46875f), y(224.386719f))
        lineTo(x(67.015625f), y(208.34375f))
        cubicTo(x(70.707031f), y(202.136719f), x(70.707031f), y(194.410156f), x(67.015625f), y(188.203125f))
        lineTo(x(57.46875f), y(172.164062f))
        cubicTo(x(51.382812f), y(161.9375f), x(55.6875f), y(148.695312f), x(66.617188f), y(144.003906f))
        lineTo(x(83.769531f), y(136.632812f))
        cubicTo(x(90.402344f), y(133.785156f), x(94.945312f), y(127.53125f), x(95.605469f), y(120.34375f))
        lineTo(x(97.3125f), y(101.753906f))
        cubicTo(x(98.402344f), y(89.90625f), x(109.664062f), y(81.722656f), x(121.269531f), y(84.347656f))
        lineTo(x(139.476562f), y(88.46875f))
        cubicTo(x(146.515625f), y(90.066406f), x(153.867188f), y(87.675781f), x(158.628906f), y(82.246094f))
        lineTo(x(170.933594f), y(68.210938f))
        cubicTo(x(178.777344f), y(59.265625f), x(192.703125f), y(59.265625f), x(200.542969f), y(68.210938f))
        close()

        // Path interior (segunda capa del hexágono)
        moveTo(x(206.199219f), y(12.371094f))
        lineTo(x(223.195312f), y(31.757812f))
        cubicTo(x(229.769531f), y(39.253906f), x(239.921875f), y(42.550781f), x(249.644531f), y(40.351562f))
        lineTo(x(274.792969f), y(34.65625f))
        cubicTo(x(290.816406f), y(31.03125f), x(306.375f), y(42.332031f), x(307.875f), y(58.695312f))
        lineTo(x(310.234375f), y(84.367188f))
        cubicTo(x(311.144531f), y(94.300781f), x(317.417969f), y(102.933594f), x(326.582031f), y(106.871094f))
        lineTo(x(350.269531f), y(117.046875f))
        cubicTo(x(365.367188f), y(123.53125f), x(371.308594f), y(141.816406f), x(362.90625f), y(155.9375f))
        lineTo(x(349.722656f), y(178.09375f))
        cubicTo(x(344.625f), y(186.664062f), x(344.625f), y(197.335938f), x(349.722656f), y(205.90625f))
        lineTo(x(362.90625f), y(228.0625f))
        cubicTo(x(371.308594f), y(242.183594f), x(365.367188f), y(260.46875f), x(350.269531f), y(266.953125f))
        lineTo(x(326.582031f), y(277.128906f))
        cubicTo(x(317.417969f), y(281.066406f), x(311.144531f), y(289.699219f), x(310.234375f), y(299.628906f))
        lineTo(x(307.875f), y(325.304688f))
        cubicTo(x(306.375f), y(341.667969f), x(290.816406f), y(352.96875f), x(274.792969f), y(349.34375f))
        lineTo(x(249.644531f), y(343.648438f))
        cubicTo(x(239.921875f), y(341.449219f), x(229.769531f), y(344.746094f), x(223.195312f), y(352.242188f))
        lineTo(x(206.199219f), y(371.628906f))
        cubicTo(x(195.367188f), y(383.984375f), x(176.136719f), y(383.984375f), x(165.304688f), y(371.628906f))
        lineTo(x(148.304688f), y(352.242188f))
        cubicTo(x(141.730469f), y(344.746094f), x(131.582031f), y(341.449219f), x(121.855469f), y(343.648438f))
        lineTo(x(96.707031f), y(349.34375f))
        cubicTo(x(80.683594f), y(352.96875f), x(65.125f), y(341.667969f), x(63.625f), y(325.304688f))
        lineTo(x(61.269531f), y(299.628906f))
        cubicTo(x(60.355469f), y(289.699219f), x(54.082031f), y(281.066406f), x(44.921875f), y(277.128906f))
        lineTo(x(21.230469f), y(266.953125f))
        cubicTo(x(6.132812f), y(260.46875f), x(0.191406f), y(242.183594f), x(8.59375f), y(228.0625f))
        lineTo(x(21.777344f), y(205.90625f))
        cubicTo(x(26.878906f), y(197.335938f), x(26.878906f), y(186.664062f), x(21.777344f), y(178.09375f))
        lineTo(x(8.59375f), y(155.9375f))
        cubicTo(x(0.191406f), y(141.816406f), x(6.132812f), y(123.53125f), x(21.230469f), y(117.046875f))
        lineTo(x(44.921875f), y(106.871094f))
        cubicTo(x(54.082031f), y(102.933594f), x(60.355469f), y(94.300781f), x(61.269531f), y(84.367188f))
        lineTo(x(63.625f), y(58.695312f))
        cubicTo(x(65.125f), y(42.332031f), x(80.683594f), y(31.03125f), x(96.707031f), y(34.65625f))
        lineTo(x(121.855469f), y(40.351562f))
        cubicTo(x(131.582031f), y(42.550781f), x(141.730469f), y(39.253906f), x(148.304688f), y(31.757812f))
        lineTo(x(165.304688f), y(12.371094f))
        cubicTo(x(176.136719f), y(0.015625f), x(195.367188f), y(0.015625f), x(206.199219f), y(12.371094f))
        close()
    }
}
