package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.experimental.xor

class ByteExtractor(private val image: BufferedImage) {
    private var y = 0
    private var x = 0

    fun hasNextByte(): Boolean = y < image.height && x < image.width

    fun nextByte(): Byte {
        var result = 0
        var bits = 0
        var nextBitValue = 128
        while (bits < 8) {
            val bit = nextBit()
            result += bit * nextBitValue
            nextBitValue /= 2
            bits++
        }
        return result.toByte()
    }

    private fun nextBit(): Int {
        val pixel = image.getRGB(x, y)
        x++
        if (x == image.width) {
            x = 0
            y++
        }
        return pixel and 1
    }
}

fun getShowArguments(): Array<String> {
    println("Input image file:")
    val fileName = readLine()!!
    println("Password:")
    val password = readLine()!!
    return arrayOf(fileName, password)
}

fun show() {
    val (fileName, password) = getShowArguments()
    val image = ImageIO.read(File(fileName))
    val encryptedMessage = image.decode()
    val message = encryptXOR(encryptedMessage, password.toByteArray()).toString(Charsets.UTF_8)
    println("Message:")
    println(message)
}

fun getHideArguments(): Array<String> {
    println("Input image file:")
    val inputFileName = readLine()!!
    println("Output image file:")
    val outputFileName = readLine()!!
    println("Message to hide:")
    val message = readLine()!!
    println("Password:")
    val password = readLine()!!
    return arrayOf(inputFileName, outputFileName, message, password)
}

fun encryptXOR(message: ByteArray, password: ByteArray): ByteArray {
    val result = ByteArray(message.size)
    for (index in 0 until message.size) {
        result[index] = message[index] xor password[index % password.size]
    }
    return result
}

fun hide() {
    val (inputFileName, outputFileName, message, password) = getHideArguments()

    val inputFile: File
    val inputImage: BufferedImage
    try {
        inputFile = File(inputFileName)
        inputImage = ImageIO.read(inputFile)
    } catch (e: IOException) {
        println("Can't read input file!")
        return
    }

    val encryptedMessage = encryptXOR(message.toByteArray(), password.toByteArray())

    val outputImage = inputImage.encode(encryptedMessage)
    if (outputImage == null){
        println("The input image is not large enough to hold this message.")
        return
    }

    val outputFile = File(outputFileName)
    ImageIO.write(outputImage, "png", outputFile)
    println("Message saved in $outputFileName image.")
    return
}

fun BufferedImage.encode(message: ByteArray): BufferedImage? {
    val width = this.width
    val height = this.height
    val nBitsToUse = width * height             // one bit per pixel

    val sizeInBytes = message.size + 3
    val messageArray = message.copyOf(sizeInBytes)
    messageArray[sizeInBytes - 3] = 0
    messageArray[sizeInBytes - 2] = 0
    messageArray[sizeInBytes - 1] = 3
    val sizeInBits = sizeInBytes * 8

    if (sizeInBits > nBitsToUse) {
        return null
    }

    val bitArray = messageArray.toBitArray()
    val outputImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    var index = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val inPixel = Color(this.getRGB(x, y))
            val outPixel = if (index < bitArray.size) {
                val outBlue: UByte = inPixel.blue.toUByte() and 0b11111110u or bitArray[index].toUByte()
                Color(inPixel.red, inPixel.green, outBlue.toInt())
            } else {
                inPixel
            }
            outputImage.setRGB(x, y, outPixel.rgb)
            index++
        }
    }
    return outputImage
}

fun BufferedImage.decode(): ByteArray {
    val extractor = ByteExtractor(this)
    val byteMessage = mutableListOf<Byte>()
    while (extractor.hasNextByte() &&
                (byteMessage.size < 3 ||
                byteMessage[byteMessage.size - 3] != (0).toByte() ||
                byteMessage[byteMessage.size - 2] != (0).toByte() ||
                byteMessage[byteMessage.size - 1] != (3).toByte())) {
        byteMessage.add(extractor.nextByte())
    }
    if (!extractor.hasNextByte()) {
        println("<< There's no message!")
    }
    return byteMessage.dropLast(3).toByteArray()
}

fun ByteArray.toBitArray(): ByteArray {
    val result = ByteArray(8 * this.size)
    for ((byteIndex, byte) in this.withIndex()) {
        var tempByte = byte
        for(i in 7 downTo 0) {
            result[byteIndex * 8 + i] = (tempByte % 2).toByte()
            tempByte = (tempByte / 2).toByte()
        }
    }
    return result
}

fun main() {
    while (true) {
        println("Task (hide, show, exit):")
        val command = readLine()!!
        when(command) {
            "exit" -> println("Bye!")
            "hide" -> hide()
            "show" -> show()
            else -> println("Wrong task: [input String]")
        }
        if (command == "exit") {
            break
        }
    }
}
