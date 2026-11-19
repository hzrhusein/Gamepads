package com.example.psgamepad

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@SuppressLint("MissingPermission", "ClickableViewAccessibility")
class MainActivity : AppCompatActivity(), HidManager.Listener {

    private lateinit var hid: HidManager
    private lateinit var status: TextView
    private lateinit var chkHaptic: CheckBox
    private val state = GamepadState()
    private var lastReport = ByteArray(0)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) hid.start()
            else status.setText(R.string.need_permission)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        // Layar penuh (sembunyikan status bar & navigation bar)
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        status = findViewById(R.id.status)
        chkHaptic = findViewById(R.id.chkHaptic)
        hid = HidManager(this, this)

        findViewById<Button>(R.id.btnConnect).setOnClickListener { showDevicePicker() }
        findViewById<Button>(R.id.btnDiscoverable).setOnClickListener { makeDiscoverable() }
        findViewById<Button>(R.id.btnDisconnect).setOnClickListener { hid.disconnect() }

        bindButton(R.id.btnCross, GamepadState.CROSS)
        bindButton(R.id.btnCircle, GamepadState.CIRCLE)
        bindButton(R.id.btnSquare, GamepadState.SQUARE)
        bindButton(R.id.btnTriangle, GamepadState.TRIANGLE)
        bindButton(R.id.btnL1, GamepadState.L1)
        bindButton(R.id.btnR1, GamepadState.R1)
        bindButton(R.id.btnL2, GamepadState.L2)
        bindButton(R.id.btnR2, GamepadState.R2)
        bindButton(R.id.btnSelect, GamepadState.SELECT)
        bindButton(R.id.btnStart, GamepadState.START)
        bindButton(R.id.btnL3, GamepadState.L3)
        bindButton(R.id.btnR3, GamepadState.R3)
        bindButton(R.id.btnPs, GamepadState.PS)

        bindDpad(R.id.dpUp, GamepadState.Dpad.UP)
        bindDpad(R.id.dpDown, GamepadState.Dpad.DOWN)
        bindDpad(R.id.dpLeft, GamepadState.Dpad.LEFT)
        bindDpad(R.id.dpRight, GamepadState.Dpad.RIGHT)

        findViewById<AnalogStickView>(R.id.stickLeft).onMove = { x, y ->
            state.setStick(true, x, y)
            push()
        }
        findViewById<AnalogStickView>(R.id.stickRight).onMove = { x, y ->
            state.setStick(false, x, y)
            push()
        }

        ensurePermissionsAndStart()
    }

    override fun onResume() {
        super.onResume()
        // Coba lagi kalau sebelumnya Bluetooth belum aktif
        if (hasBluetoothPermissions()) hid.start()
    }

    override fun onDestroy() {
        hid.stop()
        super.onDestroy()
    }

    override fun onStatus(text: String) {
        runOnUiThread { status.text = text }
    }

    private fun hasBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return REQUIRED_31.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun ensurePermissionsAndStart() {
        if (hasBluetoothPermissions()) hid.start()
        else permissionLauncher.launch(REQUIRED_31)
    }

    private fun showDevicePicker() {
        val devices = hid.bondedDevices()
        if (devices.isEmpty()) {
            Toast.makeText(this, R.string.no_paired, Toast.LENGTH_LONG).show()
            return
        }
        val names = devices.map { it.name ?: it.address }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.pick_pc)
            .setItems(names) { _, i -> hid.connect(devices[i]) }
            .show()
    }

    private fun makeDiscoverable() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        startActivity(intent)
    }

    /** Kirim report hanya kalau ada perubahan, supaya Bluetooth tidak kebanjiran. */
    private fun push() {
        val report = state.toReport()
        if (!report.contentEquals(lastReport)) {
            lastReport = report
            hid.send(report)
        }
    }

    private fun bindButton(id: Int, bit: Int) = touch(id) { down -> state.setButton(bit, down) }

    private fun bindDpad(id: Int, dir: GamepadState.Dpad) = touch(id) { down -> state.setDpad(dir, down) }

    private fun touch(id: Int, onChange: (Boolean) -> Unit) {
        findViewById<View>(id).setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    if (chkHaptic.isChecked) v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onChange(true)
                    push()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    onChange(false)
                    push()
                }
            }
            true
        }
    }

    companion object {
        private val REQUIRED_31 = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    }
}
