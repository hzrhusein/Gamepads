package com.example.psgamepad

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import java.util.concurrent.Executors

/** Membungkus Bluetooth HID Device profile (Android 9+) supaya HP tampil sebagai gamepad. */
@SuppressLint("MissingPermission")
class HidManager(private val context: Context, private val listener: Listener) {

    interface Listener {
        fun onStatus(text: String)
    }

    companion object {
        const val REPORT_ID = 1
        private const val PREFS = "psgamepad"
        private const val KEY_LAST_HOST = "last_host"

        // Gamepad ala PS3: 16 tombol, hat (D-pad), stick kiri X/Y, stick kanan Z/Rz,
        // trigger analog Rx/Ry. Total report 9 byte.
        private val DESCRIPTOR: ByteArray = intArrayOf(
            0x05, 0x01, 0x09, 0x05, 0xA1, 0x01, 0x85, REPORT_ID,
            // 16 tombol
            0x05, 0x09, 0x19, 0x01, 0x29, 0x10, 0x15, 0x00, 0x25, 0x01,
            0x75, 0x01, 0x95, 0x10, 0x81, 0x02,
            // hat switch (D-pad)
            0x05, 0x01, 0x09, 0x39, 0x15, 0x01, 0x25, 0x08, 0x35, 0x00,
            0x46, 0x3B, 0x01, 0x65, 0x14, 0x75, 0x04, 0x95, 0x01, 0x81, 0x42,
            0x65, 0x00, 0x35, 0x00, 0x45, 0x00,
            // padding 4 bit
            0x75, 0x04, 0x95, 0x01, 0x81, 0x03,
            // X, Y, Z, Rz (-127..127)
            0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x09, 0x32, 0x09, 0x35,
            0x15, 0x81, 0x25, 0x7F, 0x75, 0x08, 0x95, 0x04, 0x81, 0x02,
            // Rx, Ry (0..255) = trigger L2, R2
            0x09, 0x33, 0x09, 0x34, 0x15, 0x00, 0x26, 0xFF, 0x00,
            0x75, 0x08, 0x95, 0x02, 0x81, 0x02,
            0xC0
        ).map { it.toByte() }.toByteArray()
    }

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()
    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null

    private val sdp = BluetoothHidDeviceAppSdpSettings(
        "PS Gamepad",
        "Virtual gamepad",
        "PSGamepad",
        BluetoothHidDevice.SUBCLASS2_GAMEPAD,
        DESCRIPTOR
    )

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered) {
                listener.onStatus("Siap. Pasangkan dari PC, atau tekan Pilih PC")
                autoReconnect()
            } else {
                listener.onStatus("Pendaftaran HID gagal")
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTING -> listener.onStatus("Menghubungkan...")
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device
                    prefs.edit().putString(KEY_LAST_HOST, device.address).apply()
                    listener.onStatus("Terhubung ke ${device.name ?: device.address}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (host?.address == device.address) host = null
                    listener.onStatus("Terputus. Siap dipasangkan lagi")
                }
            }
        }
    }

    private fun autoReconnect() {
        val addr = prefs.getString(KEY_LAST_HOST, null) ?: return
        val device = bondedDevices().firstOrNull { it.address == addr } ?: return
        hid?.connect(device)
    }

    fun start() {
        if (hid != null) return
        val a = adapter
        if (a == null) {
            listener.onStatus("Perangkat tidak punya Bluetooth")
            return
        }
        if (!a.isEnabled) {
            listener.onStatus("Aktifkan Bluetooth dulu")
            return
        }
        a.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    val h = proxy as BluetoothHidDevice
                    hid = h
                    h.registerApp(sdp, null, null, executor, callback)
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                hid = null
                host = null
            }
        }, BluetoothProfile.HID_DEVICE)
    }

    fun stop() {
        hid?.let {
            it.unregisterApp()
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
        }
        hid = null
        host = null
    }

    fun bondedDevices(): List<BluetoothDevice> = adapter?.bondedDevices.orEmpty().toList()

    fun connect(device: BluetoothDevice) {
        hid?.connect(device)
    }

    fun disconnect() {
        host?.let { hid?.disconnect(it) }
    }

    fun send(data: ByteArray) {
        val h = hid ?: return
        val d = host ?: return
        h.sendReport(d, REPORT_ID, data)
    }
}
