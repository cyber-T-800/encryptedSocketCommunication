package utils

object Utils {
    fun intToByteArray(n : Int) : ByteArray{
        return byteArrayOf(n.shr(24).toByte(), n.shr(16).toByte(), n.shr(8).toByte(), n.toByte())
    }

    fun byteArrayToInt(b : ByteArray) : Int{
        return b[0].toUByte().toInt().shl(24).or(b[1].toUByte().toInt().shl(16)).or(b[2].toUByte().toInt().shl(8)).or(b[3].toUByte().toInt())
    }
}