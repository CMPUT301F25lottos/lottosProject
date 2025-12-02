package com.example.lottos.events;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

/**
 * A utility class for generating QR (Quick Response) code images.
 * This class uses the ZXing (Zebra Crossing) library to encode a string
 * into a square QR code bitmap.
 */
public class QRCodeGenerator {

    /**
     * Generates a QR code bitmap from a given string content.
     * The generated QR code will have a black foreground and a white background.
     *
     * @param content The string data to be encoded into the QR code.
     *                This is typically a URL, event ID, or other identifier.
     * @param size    The desired width and height of the QR code bitmap in pixels.
     * @return A {@link Bitmap} object representing the generated QR code.
     *         Returns {@code null} if the encoding process fails due to a
     *         {@link WriterException}.
     */
    public static Bitmap generate(String content, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
            );

            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bmp;

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
