package client

import utils.CryptoUtils
import utils.Utils
import java.lang.NumberFormatException
import java.net.ConnectException
import java.net.Socket
import java.security.KeyPair
import java.security.PublicKey

class Client {
    companion object {
        const val DEFAULT_IP = "127.0.0.1"
        const val DEFAULT_PORT = 7777
    }

    private val userName : String
    private val keyPair : KeyPair
    private lateinit var serverPublicKey : PublicKey

    private var lastTypedIP = DEFAULT_IP
    private var lastTypedPort = DEFAULT_PORT

    private var socket: Socket

    init {
        socket = createSocket()
        println("Successfully connected to server")

        println("Generation crypto keys...")
        keyPair = CryptoUtils.generateCryptoKeys(2048)
        println("Crypto keys successfully generated")

        establishEncryptedConnection()

        userName = typeUsername()
        sendMessage(encryptMessage(userName))

        receiveThread().start()
        writeThread().start()

    }

    fun receiveThread() : Thread {
        return Thread {
            while (true) {
                println(String(decryptMessage(receiveMessage())))
            }
        }
    }

    fun writeThread() : Thread{
        return Thread{
            while(true){
                sendMessage(encryptMessage(readLine()!!))
            }

        }
    }

    fun establishEncryptedConnection(){
        println("sending public key to server ...")
        sendMessage(keyPair.public.encoded)

        println("public key was send")
        println("receive server public key...")
        serverPublicKey = CryptoUtils.publicKeyFromBytes(receiveMessage())

        println("server public key was received...")

        println("encrypted connection was successfully established")
        println("\n\n")
    }

    fun receiveMessage() : ByteArray{
        var lenByteArray : ByteArray = ByteArray(4)
        socket.getInputStream().read(lenByteArray, 0, 4)
        var len = Utils.byteArrayToInt(lenByteArray)

        var messageByteArray : ByteArray = ByteArray(len)
        socket.getInputStream().read(messageByteArray, 0, len)

        return messageByteArray
    }

    private fun sendMessage(message : String){
        sendMessage(message.toByteArray())
    }

    private fun sendMessage(message: ByteArray){
        val outputStream = socket.getOutputStream()
        outputStream.write(Utils.intToByteArray(message.size))
        outputStream.write(message)
    }

    private fun encryptMessage(message: ByteArray) : ByteArray{
        return CryptoUtils.encryptMessage(serverPublicKey, message)
    }

    private fun encryptMessage(message: String) : ByteArray{
        return encryptMessage(message.toByteArray())
    }

    private fun decryptMessage(message: ByteArray) : ByteArray{
        return CryptoUtils.decryptMessage(keyPair.private, message)
    }

    private fun createSocket(): Socket {
        var result: Socket
        while (true) {
            try {
                result = Socket(typeIP(), typePort())

            } catch (e: ConnectException) {
                println("Can't connect to server, try check IP address or port, or try again later")
                continue
            }
            break
        }
        return result
    }

    private fun typeIP(): String {
        var result: String
        while (true) {
            println("type server address (leave empty for $lastTypedIP): ")
            result = readLine()!!
            if (result == "") {
                return lastTypedIP
            }
            if (result.split(".").size == 4) {
                for (s in result.split("."))
                    try {
                        s.toInt()
                    } catch (e: NumberFormatException) {
                        println("invalid IP address format")
                        continue
                    }
                break
            } else {
                println("invalid IPv4 address format")
            }
        }
        lastTypedIP = result
        return result
    }

    private fun typePort(): Int {
        var result: Int
        while (true) {
            println("type server port (leave empty for $lastTypedPort): ")
            val portString: String = readLine()!!
            if (portString == "")
                return lastTypedPort
            try {
                result = portString.toInt()
            } catch (e: NumberFormatException) {
                print("port must be number")
                continue
            }
            break
        }
        lastTypedPort = result
        return result
    }

    private fun typeUsername() : String{
        var result : String
        println("Type username: ")
        while (true){
            result = readLine()!!
            if(result != "")
                return result
            println("Username can't be empty line, try again")
        }
    }
}

fun main(){
    Client()
}