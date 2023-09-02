package com.example.simplebluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val REQUEST_ENABLE_BT = 2023
    private val mHandler = Handler(Looper.getMainLooper())

    private val requestEnableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                scanNetwork()
            } else {
                // TODO: Show dialog to user
                Toast.makeText(this@MainActivity, "Not Granted", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initBluetoothAdapter()
        if (checkBluetoothPermission()) {
            Log.e("TAG", "onCreate: checkBluetoothPermission")

            if (bluetoothAdapter.isEnabled) {
                Log.e("TAG", "onCreate: isEnabled")
                scanNetwork()
            } else {
                requestBluetoothEnable()
                Log.e("TAG", "onCreate: !isEnabled")
            }
        } else {
            Log.e("TAG", "onCreate: requestBluetoothPermission")

            requestBluetoothPermission()
        }
    }


    private fun checkBluetoothPermission(): Boolean = ActivityCompat.checkSelfPermission(
        this@MainActivity,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        this@MainActivity,
        Manifest.permission.BLUETOOTH_SCAN
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun scanNetwork() {

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        bluetoothAdapter.startDiscovery()
        //val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices

        /*pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address

            Log.e("DEVICE NAME", "THIS IS THE DEVICE NAME ${deviceName}")
            Log.e("DEVICE ADDRESS", "THIS IS THE DEVICE NAME ${deviceHardwareAddress}")
        }*/
    }

    private fun initBluetoothAdapter() {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter!!
    }

    private fun requestBluetoothPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
            REQUEST_ENABLE_BT
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                scanNetwork()
            } else {
                // TODO:
                Toast.makeText(this@MainActivity, "Not Granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestBluetoothEnable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        requestEnableBtLauncher.launch(enableBtIntent)
    }

    private val receiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name.orEmpty()
                    val deviceHardwareAddress = device?.address // MAC address
                    device?.fetchUuidsWithSdp()
                    val uuids = device?.uuids
                    Log.e("TAG", "onReceive: $deviceName ${uuids?.get(0)?.toString()} $deviceHardwareAddress")
                    if (deviceName == "Episafe")
                        ConnectThread(device!!, mHandler).start()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice, private val mHandler: Handler) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        }

        public override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                //manageMyConnectedSocket(socket)
                Log.e("TAG", "run: ${socket.isConnected} ${socket.remoteDevice.name}")
                if (socket.isConnected) {
                    val bluetoothService = MyBluetoothService(mHandler)
                    Log.e("","before is connecteed")
                    bluetoothService.createConnectedThread(socket)
                    //connectedThread.start()
                }
            }
        }


        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("TAG", "Could not close the client socket", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

}

object mHandler : Handler() {
    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        Log.e("TAG", "handleMessage: ${msg.arg1}")
        val readBuf = msg.obj as ByteArray
        val readMessage = String(readBuf, 0, msg.arg1)
        Log.e("TAG", "handleMessage: $readMessage")
    }
}
