/*
 * MIT License
 *
 * Copyright (c) 2018 Bonosoft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package photon.file.parts;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;

import javax.imageio.ImageIO;

/**
 * by bn on 01/07/2018.
 */
public class PhotonFilePreview {
    private int resolutionX;
    private int resolutionY;
    private int imageAddress;

    private byte[] rawImageData;

    private int[] imageData;

    public PhotonFilePreview(int previewAddress, byte[] file) throws Exception {
        byte[] data = Arrays.copyOfRange(file, previewAddress, previewAddress + 16);
        PhotonInputStream ds = new PhotonInputStream(new ByteArrayInputStream(data));

        resolutionX = ds.readInt();
        resolutionY = ds.readInt();
        imageAddress = ds.readInt();
        int dataSize = ds.readInt();

        rawImageData = Arrays.copyOfRange(file, imageAddress, imageAddress + dataSize);

        decodeImageData();
    }

    public void save(PhotonOutputStream os, int startAddress) throws Exception {
        os.writeInt(resolutionX);
        os.writeInt(resolutionY);
        os.writeInt(startAddress + 4+4+4+4);
        os.writeInt(rawImageData.length);
        os.write(rawImageData, 0, rawImageData.length);
    }

    public int getByteSize() {
        return 4+4+4+4 + rawImageData.length;
    }

    private void decodeImageData() {
    	
    	/**
    	 * Decodes a RLE byte array from a *.photon file
         * Encoding scheme:
         *   The color (R,G,B) of a pixel spans 2 bytes (little endian) and
         *   each color component is 5 bits: RRRRR GGG GG X BBBBB
         *   If the X bit is set, then the next 2 bytes (little endian) masked
         *   with 0xFFF represents how many more times to repeat that pixel.
    	 */

        imageData = new int[resolutionX * resolutionY];
        int d = 0;
        for (int i = 0; i < rawImageData.length; i++) {
            int dot = rawImageData[i] & 0xFF | ((rawImageData[++i] & 0xFF) << 8);

            int color =   ((dot & 0xF800) << 8) | ((dot & 0x07C0) << 5) | ((dot & 0x001F) << 3);

//            int red = ((dot >> 11) & 0x1F) << 3;
//            int green = ((dot >> 6) & 0x1F) << 3;
//            int blue = (dot & 0x1F) << 3;
//            color = red<<16 | green<<8 | blue;

            int repeat = 1;
            if ((dot & 0x0020) == 0x0020) {
                repeat += rawImageData[++i] & 0xFF | ((rawImageData[++i] & 0x0F) << 8);
            }

            while (repeat > 0) {
                imageData[d++] = color;
                repeat--;
            }
        }
    }

    public void encodeImageData() throws Exception {

        BufferedImage image = ImageIO.read(new File("preview.jpg"));

        resolutionX = image.getWidth();
        resolutionY = image.getHeight();

        BufferedImage temp = new BufferedImage(resolutionX, resolutionY, BufferedImage.TYPE_USHORT_555_RGB);

        Graphics2D graphics = temp.createGraphics();

        graphics.drawImage(image, 0, 0, null);

        graphics.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int length = 0;
        short current = 0;
        byte first = 0, second = 0;

        for (int y = 0; y < resolutionY; ++y) {

            for (int x = 0; x < resolutionX; ++x) {
                short next = ((short[]) temp.getRaster().getDataElements(x, y, null))[0];

                if (next != current) {
                    if (length > 0) {
                        writeRLE(baos, first, second, length);
                    }

                    current = next;
                    length = 1;
                    byte r = (byte) (temp.getRaster().getSample(x, y, 0));
                    byte g = (byte) (temp.getRaster().getSample(x, y, 1));
                    byte b = (byte) (temp.getRaster().getSample(x, y, 2));

                    first = (byte) (((g & 0x3) << 6) | b);
                    second = (byte) ((r << 3) | (g >> 2));

                } else {
                    length++;
                }
            }
        }

        writeRLE(baos, first, second, length);

        rawImageData = baos.toByteArray();
    }

    private static void writeRLE(ByteArrayOutputStream baos, byte first, byte second, int length) {
        if (length > 0xFFF) {
            // when reading the data in the length is masked with 0xFFF limiting
            // us to a single run of 4095 pixels. if we have more pixels than
            // that then we need to store multiple runs of the same colour
            writeRLE(baos, first, second, 0xFFF);
            writeRLE(baos, first, second, length - 0xFFF);
        } else if (length > 1) {
            // we won't go past 0xFFF but we do have a run of more than one
            // pixel of the same colour so we write out the colour and the
            // number of extra pixels we need over and above the one implicit in
            // the fact we are storing a colour
            baos.write((byte) (first | 0x20));
            baos.write(second);
            length = length - 1;//
            baos.write((byte) length);
            baos.write((byte) ((length >> 8) | 0x30));
        } else {
            // in the final case we have a single pixel of colour which is
            // followed by a pixel of a different colour so we just write out
            // the colour info with no length of run info
            baos.write(first);
            baos.write(second);
        }
    }

    public int getResolutionX() {
        return resolutionX;
    }

    public int getResolutionY() {
        return resolutionY;
    }

    public int[] getImageData() {
        return imageData;
    }

    public void unLink() {
        rawImageData = null;
        imageData = null;
    }

}
