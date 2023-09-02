package com.example.simplebluetooth

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream

private const val TAG = "MY_APP_DEBUG_TAG"

// Defines several constants used when transmitting messages between the
// service and the UI.
const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2
// ... (Add other message types here as needed.)

class MyBluetoothService(
    // handler that gets info from Bluetooth service
    private val handler: Handler
) {

    inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmBuffer: ByteArray = ByteArray(400 * 1024)

        override fun run() {
            var numBytes: Int // bytes returned from read()
            Log.e("", "inside connecteed")

            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    mmBuffer
                )

                val readMessage = String(readMsg.obj as ByteArray, 0, readMsg.arg1)
                //  Log.e(TAG, "run: $readMessage")
                val message = readMessage.extractData()
                // 21:56:25#36:3695748:12
//                Log.e(TAG, "run: $message")
                Log.e(TAG, "run: ${message.size}", )
                // readMsg.sendToTarget()
            }

        }


        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }

    }

    fun createConnectedThread(socket: BluetoothSocket) {
        val connectedThread = ConnectedThread(socket)
        connectedThread.start()
    }
}