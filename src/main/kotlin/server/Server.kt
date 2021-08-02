package server

import utils.CryptoUtils
import utils.Utils
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException
import java.net.BindException

import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

class Server {
    companion object {
        const val DEFAULT_PORT = 7777
    }
    private var users : List<User> = listOf()

    private var lastTypedPort = DEFAULT_PORT
    private var serverSocket: ServerSocket
    private var keyPair : KeyPair

    init {
        serverSocket = createServerSocket()
        println("successfully created server...")
        println("Generation crypto keys ...")
        keyPair = CryptoUtils.generateCryptoKeys(2048)
        println("Crypto keys successfully generated")

        acceptThread().start()
    }

    private fun createServerSocket(): ServerSocket {
        var result: ServerSocket
        while (true) {
            try {
                result = ServerSocket(typePort())
                break
            } catch (e: BindException) {
                println("port already in use, try another port")
                continue
            } catch (e: IllegalArgumentException) {
                println("Illegal port number")
                continue
            }
        }
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
                println("port must be number")
                continue
            }
            break
        }
        lastTypedPort = result
        return result
    }

    private fun acceptThread(): Thread {
        return Thread {
            while (true)
                users += User(serverSocket.accept(), this)
        }
    }

    class User(socket : Socket, server : Server){

        var input : InputStream = socket.getInputStream()
        var output : OutputStream = socket.getOutputStream()
        var username : String
        var server = server

        lateinit var publicKey : PublicKey

        init{
            println("User connected from address: ${socket.inetAddress}")

            establishEncryptedConnection(server.keyPair.public)

            println("Getting user username ...")
            username = String(decryptMessage(receiveMessage()))
            println("User username set as: $username")

            receiveThread().start()
        }

        fun establishEncryptedConnection(serverPublicKey : PublicKey){
            println()
            println("establishing encrypted connection...")
            println("sending public key to user ...")
            sendMessage(serverPublicKey.encoded)

            println("public key was send")
            println("receive user public key...")
            publicKey = CryptoUtils.publicKeyFromBytes(receiveMessage())

            println("user public key was received...")

            println("encrypted connection was successfully established")
            println()
        }

        fun receiveThread() : Thread {
            return Thread {
                while (true) {
                    try {
                        var receivedMessage = decryptMessage(receiveMessage())
                        println(String(receivedMessage))
                        for(u in server.users)
                            if(u != this)
                                u.sendMessage(u.encryptMessage(receivedMessage))
                    }catch (e : SocketException){
                        println("Client has disconnected")
                        server.users -= this
                        break
                    }
                }
            }
        }

        fun receiveMessage() : ByteArray{
            var lenByteArray : ByteArray = ByteArray(4)
            input.read(lenByteArray, 0, 4)
            var len = Utils.byteArrayToInt(lenByteArray)

            var messageByteArray : ByteArray = ByteArray(len)
            input.read(messageByteArray, 0, len)

            return messageByteArray
        }

        private fun sendMessage(message : String){
            sendMessage(message.toByteArray())
        }

        private fun sendMessage(message : ByteArray){
            output.write(Utils.intToByteArray(message.size))
            output.write(message)
        }

        private fun encryptMessage(message: ByteArray) : ByteArray{
            return CryptoUtils.encryptMessage(publicKey, message)
        }

        private fun encryptMessage(message: String) : ByteArray{
            return encryptMessage(message.toByteArray())
        }

        private fun decryptMessage(message: ByteArray) : ByteArray{
            return CryptoUtils.decryptMessage(server.keyPair.private, message)
        }
    }
}

fun main(){
    Server()
}