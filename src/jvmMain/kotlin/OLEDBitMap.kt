import java.awt.Color
import java.awt.Font
import java.awt.font.TextAttribute
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.text.AttributedString
import javax.imageio.ImageIO


fun BufferedImage.addText(text: String, x: Int, y: Int) {
    val g = graphics
    val attributedString = AttributedString(text)
    attributedString.addAttribute(TextAttribute.FONT, Font("Arial", Font.BOLD, 14))
    attributedString.addAttribute(TextAttribute.FOREGROUND, Color.WHITE)
    g.drawString(attributedString.iterator, x, y)
}

actual fun resourceByeArray(name: String): ByteArray {
    val image: BufferedImage = ImageIO.read(OLEDBitMap::class.java.getResource(name))

    image.addText("-5°", 8, 29)
    image.addText("19°", 41, 36)
    image.addText("348°", 78, 98)
    image.addText("105%", 81, 53)
    image.addText("103°", 88, 66)
    image.addText("43%→100%", 5, 121)
    image.addText("Auto", 10, 98)


    //val xxx = (image.raster.dataBuffer as DataBufferByte).data
    val result = ByteArray(2048) { byteIndex ->
        val page = byteIndex / 128
        val x = byteIndex % 128
        (image.raster.getSample(x, page * 8, 0)
            .xor(image.raster.getSample(x, page * 8 + 1, 0).shl(1))
            .xor(image.raster.getSample(x, page * 8 + 2, 0).shl(2))
            .xor(image.raster.getSample(x, page * 8 + 3, 0).shl(3))
            .xor(image.raster.getSample(x, page * 8 + 4, 0).shl(4))
            .xor(image.raster.getSample(x, page * 8 + 5, 0).shl(5))
            .xor(image.raster.getSample(x, page * 8 + 6, 0).shl(6))
            .xor(image.raster.getSample(x, page * 8 + 7, 0).shl(7))
                ).toByte()

    }
    /*(0 until 16).forEach { page ->
        (0 until 128).forEach { x ->

            (image.raster.getSample(x, page * 16, 0)
                .xor(image.raster.getSample(x, page * 16 + 1, 0).shl(1))
                .xor(image.raster.getSample(x, page * 16 + 2, 0).shl(2))
                .xor(image.raster.getSample(x, page * 16 + 3, 0).shl(3))
                .xor(image.raster.getSample(x, page * 16 + 4, 0).shl(4))
                .xor(image.raster.getSample(x, page * 16 + 5, 0).shl(5))
                .xor(image.raster.getSample(x, page * 16 + 6, 0).shl(6))
                .xor(image.raster.getSample(x, page * 16 + 7, 0).shl(7))
                    ).toByte()
        }
    }

    TODO()*/
    return result
}

class OLEDBitMap

fun main() {

    val image: BufferedImage = ImageIO.read(OLEDBitMap::class.java.getResource("background-oled.bmp"))
    (image.raster.dataBuffer as DataBufferByte).data
    val x = resourceByeArray("background-oled.bmp")
    println(image)


}