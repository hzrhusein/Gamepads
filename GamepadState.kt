package com.example.psgamepad

import kotlin.math.hypot
import kotlin.math.roundToInt

/** Status semua kontrol ala PS3, diubah menjadi HID report 9 byte. */
class GamepadState {

    companion object {
        const val CROSS = 0
        const val CIRCLE = 1
        const val SQUARE = 2
        const val TRIANGLE = 3
        const val L1 = 4
        const val R1 = 5
        const val L2 = 6
        const val R2 = 7
        const val SELECT = 8
        const val START = 9
        const val L3 = 10
        const val R3 = 11
        const val PS = 12
    }

    enum class Dpad { UP, DOWN, LEFT, RIGHT }

    /** Zona mati analog stick (0..1). Naikkan kalau stick terasa "drift". */
    var deadZone = 0.10f

    private var buttons = 0
    private var up = false
    private var down = false
    private var left = false
    private var right = false
    private var lx = 0
    private var ly = 0
    private var rx = 0
    private var ry = 0
    private var l2 = 0
    private var r2 = 0

    fun setButton(bit: Int, pressed: Boolean) {
        buttons = if (pressed) buttons or (1 shl bit) else buttons and (1 shl bit).inv()
        // L2/R2 juga mengisi axis trigger (0..255)
        if (bit == L2) l2 = if (pressed) 255 else 0
        if (bit == R2) r2 = if (pressed) 255 else 0
    }

    fun setDpad(dir: Dpad, pressed: Boolean) {
        when (dir) {
            Dpad.UP -> up = pressed
            Dpad.DOWN -> down = pressed
            Dpad.LEFT -> left = pressed
            Dpad.RIGHT -> right = pressed
        }
    }

    /** x, y dalam -1..1 (y positif = ke bawah). */
    fun setStick(isLeft: Boolean, x: Float, y: Float) {
        var ax = x
        var ay = y
        val len = hypot(x, y)
        if (len < deadZone) {
            ax = 0f
            ay = 0f
        } else {
            val scale = ((len - deadZone) / (1f - deadZone)).coerceAtMost(1f) / len
            ax = x * scale
            ay = y * scale
        }
        val ix = (ax * 127).roundToInt().coerceIn(-127, 127)
        val iy = (ay * 127).roundToInt().coerceIn(-127, 127)
        if (isLeft) {
            lx = ix; ly = iy
        } else {
            rx = ix; ry = iy
        }
    }

    // 1=N, 2=NE, 3=E, 4=SE, 5=S, 6=SW, 7=W, 8=NW, 0=netral
    private fun hat(): Int = when {
        up && right -> 2
        right && down -> 4
        down && left -> 6
        left && up -> 8
        up -> 1
        right -> 3
        down -> 5
        left -> 7
        else -> 0
    }

    /**
     * Susunan: 2 byte tombol | 1 byte hat | X, Y (stick kiri) | Z, Rz (stick kanan)
     * | Rx, Ry (trigger L2, R2)
     */
    fun toReport(): ByteArray = byteArrayOf(
        (buttons and 0xFF).toByte(),
        ((buttons shr 8) and 0xFF).toByte(),
        hat().toByte(),
        lx.toByte(), ly.toByte(),
        rx.toByte(), ry.toByte(),
        l2.toByte(), r2.toByte()
    )
}
