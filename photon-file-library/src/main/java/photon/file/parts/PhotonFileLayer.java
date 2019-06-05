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

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;

/**
 * by bn on 01/07/2018.
 */
public class PhotonFileLayer {
    private float layerPositionZ;
    private float layerExposure;
    private float layerOffTimeSeconds;
    private int dataAddress;
    private int dataSize;
    private int unknown1;
    private int unknown2;
    private int unknown3;
    private int unknown4;

    private byte[] imageData;

    private byte[] packedLayerImage;

    private ArrayList<BitSet> islandRows;
    private int isLandsCount;
    private long pixels;

    private ArrayList<PhotonFileLayer> antiAliasLayers = new ArrayList<>();

    private boolean extendsMargin;
    private PhotonFileHeader photonFileHeader;
    public boolean isCalculated;

    private Rectangle boundingBox;

    private PhotonFileLayer(PhotonInputStream ds) throws Exception {
        layerPositionZ = ds.readFloat();
        layerExposure = ds.readFloat();
        layerOffTimeSeconds = ds.readFloat();

        dataAddress = ds.readInt();
        dataSize = ds.readInt();

        unknown1 = ds.readInt();
        unknown2 = ds.readInt();
        unknown3 = ds.readInt();
        unknown4 = ds.readInt();
    }

    public PhotonFileLayer(PhotonFileLayer photonFileLayer, PhotonFileHeader photonFileHeader) {
        layerPositionZ = photonFileLayer.layerPositionZ;
        layerExposure = photonFileLayer.layerExposure;
        layerOffTimeSeconds = photonFileLayer.layerOffTimeSeconds;
        dataAddress = photonFileLayer.dataAddress;
        dataAddress = photonFileLayer.dataSize;

        this.photonFileHeader = photonFileHeader;

        // Dont copy data, we are building new AA layers anyway
        //this.imageData = copy();
        //this.packedLayerImage = copy();
    }

    public int savePos(int dataPosition) throws Exception {
        dataAddress = dataPosition;
        return dataPosition + dataSize;
    }

    public void save(PhotonOutputStream os) throws Exception {
        os.writeFloat(layerPositionZ);
        os.writeFloat(layerExposure);
        os.writeFloat(layerOffTimeSeconds);

        os.writeInt(dataAddress);
        os.writeInt(dataSize);

        os.writeInt(unknown1);
        os.writeInt(unknown2);
        os.writeInt(unknown3);
        os.writeInt(unknown4);
    }
    
    public PhotonFileLayer(PhotonFileLayer orig) throws Exception {
    	layerPositionZ = orig.layerPositionZ;
        layerExposure = orig.layerExposure;
        layerOffTimeSeconds = orig.layerOffTimeSeconds;

        dataAddress = orig.dataAddress;
        dataSize = orig.dataSize;

        unknown1 = orig.unknown1;
        unknown2 = orig.unknown2;
        unknown3 = orig.unknown3;
        unknown4 = orig.unknown4;
        
        photonFileHeader = orig.photonFileHeader;
        
        saveLayer(orig.getLayer());
    }

    public void saveData(PhotonOutputStream os) throws Exception {
        os.write(imageData, 0, dataSize);
        
        
        //This seems to have got dropped when AA was added
        //os.writeByte(0);
    }

    public static int getByteSize() {
        return 4 + 4 + 4 + 4 + 4 + 4 + 4 + 4 + 4;
    }

    public ArrayList<BitSet> unpackImage(int resolutionX) {
        pixels = 0;
        resolutionX = resolutionX - 1;
        ArrayList<BitSet> unpackedImage = new ArrayList<>();
        BitSet currentRow = new BitSet();
        unpackedImage.add(currentRow);
        int x = 0;
        for (byte rle : imageData) {
            int length = rle & 0x7F;
            boolean color = (rle & 0x80) == 0x80;
            if (color) {
                pixels += length;
            }
            int endPosition = x + (length - 1);
            int lineEnd = Integer.min(endPosition, resolutionX);
            if (color) {
                currentRow.set(x, 1 + lineEnd);
            }
            if (endPosition > resolutionX) {
                currentRow = new BitSet();
                unpackedImage.add(currentRow);
                lineEnd = endPosition - (resolutionX + 1);
                if (color) {
                    currentRow.set(0, 1 + lineEnd);
                }
            }
            x = lineEnd + 1;
            if (x > resolutionX) {
                if (unpackedImage.size() == photonFileHeader.getResolutionY()) {
				    // if we would add an extra row beyond the height of the
				    // layer data then stop to keep the data consistent
                    break;
                }
                currentRow = new BitSet();
                unpackedImage.add(currentRow);
                x = 0;
            }
        }

        return unpackedImage;
    }

    private void aaPixels(ArrayList<BitSet> unpackedImage, PhotonLayer photonLayer) {
        photonLayer.clear();

        for (int y = 0; y < unpackedImage.size(); y++) {
            BitSet currentRow = unpackedImage.get(y);
            if (currentRow != null) {
                for (int x = 0; x < currentRow.length(); x++) {
                    if (currentRow.get(x)) {
                        photonLayer.unSupported(x, y);
                    }
                }
            }
        }
    }

    private void unknownPixels(ArrayList<BitSet> unpackedImage, PhotonLayer photonLayer) {
        photonLayer.clear();

        for (int y = 0; y < unpackedImage.size(); y++) {
            BitSet currentRow = unpackedImage.get(y);
            if (currentRow != null) {
                for (int x = 0; x < currentRow.length(); x++) {
                    if (currentRow.get(x)) {
                        photonLayer.supported(x, y);
                    }
                }
            }
        }
    }

    private void calculate(ArrayList<BitSet> unpackedImage, ArrayList<BitSet> previousUnpackedImage, PhotonLayer photonLayer, PhotonFileLayer previousLayer) {
        islandRows = new ArrayList<>();
        isLandsCount = 0;

        photonLayer.clear();

        PhotonLayer layerData = null;
        
        if (previousLayer != null) layerData = previousLayer.getLayer();

        for (int y = 0; y < unpackedImage.size(); y++) {
            BitSet currentRow = unpackedImage.get(y);
            BitSet prevRow = previousUnpackedImage != null ? previousUnpackedImage.get(y) : null;
            if (currentRow != null) {
                for (int x = 0; x < currentRow.length(); x++) {
                    if (currentRow.get(x)) {
                    	
                        if (prevRow == null || (prevRow.get(x) && (layerData.get(x, y) == PhotonLayer.SUPPORTED || layerData.get(x, y) == PhotonLayer.CONNECTED))) {
                            photonLayer.supported(x, y);
                        } else {
                            photonLayer.island(x, y);
                        }
                    }
                }
            }
        }

        photonLayer.reduce();

        isLandsCount = photonLayer.setIslands(islandRows);
    }


    public static List<PhotonFileLayer> readLayers(PhotonFileHeader photonFileHeader, byte[] file, int margin, IPhotonProgress iPhotonProgress) throws Exception {
        PhotonLayer photonLayer = new PhotonLayer(photonFileHeader.getResolutionX(), photonFileHeader.getResolutionY());

        List<PhotonFileLayer> layers = new ArrayList<>();

        int antiAliasLevel = 1;
        if (photonFileHeader.getVersion() > 1) {
            antiAliasLevel = photonFileHeader.getAntiAliasingLevel();
        }

        int layerCount = photonFileHeader.getNumberOfLayers();

        try (PhotonInputStream ds = new PhotonInputStream(new ByteArrayInputStream(file, photonFileHeader.getLayersDefinitionOffsetAddress(), file.length))) {
            Hashtable<Integer, PhotonFileLayer> layerMap = new Hashtable<>();
            for (int i = 0; i < layerCount; i++) {

                iPhotonProgress.showInfo("Reading photon file layer " + (i + 1) + "/" + photonFileHeader.getNumberOfLayers());

                PhotonFileLayer layer = new PhotonFileLayer(ds);
                layer.photonFileHeader = photonFileHeader;
                layer.imageData = Arrays.copyOfRange(file, layer.dataAddress, layer.dataAddress + layer.dataSize);
                layers.add(layer);
                layerMap.put(i, layer);
            }

            if (antiAliasLevel > 1) {
                for (int a = 0; a < (antiAliasLevel - 1); a++) {
                    for (int i = 0; i < layerCount; i++) {
                        iPhotonProgress.showInfo("Reading photon file AA " + (2 + a) + "/" + antiAliasLevel + " layer " + (i + 1) + "/" + photonFileHeader.getNumberOfLayers());

                        PhotonFileLayer layer = new PhotonFileLayer(ds);
                        layer.photonFileHeader = photonFileHeader;
                        layer.imageData = Arrays.copyOfRange(file, layer.dataAddress, layer.dataAddress + layer.dataSize);

                        layerMap.get(i).addAntiAliasLayer(layer);

                    }
                }
            }
        }

        photonLayer.unLink();
        System.gc();

        return layers;
    }

    private void addAntiAliasLayer(PhotonFileLayer layer) {
        antiAliasLayers.add(layer);
    }

    public static void calculateAALayers(PhotonFileHeader photonFileHeader, List<PhotonFileLayer> layers, PhotonAaMatrix photonAaMatrix, IPhotonProgress iPhotonProgress) throws Exception {
        PhotonLayer photonLayer = new PhotonLayer(photonFileHeader.getResolutionX(), photonFileHeader.getResolutionY());
        int[][] source = new int[photonFileHeader.getResolutionY()][photonFileHeader.getResolutionX()];

        int i = 0;
        for (PhotonFileLayer layer : layers) {
            ArrayList<BitSet> unpackedImage = layer.unpackImage(photonFileHeader.getResolutionX());

            iPhotonProgress.showInfo("Calculating AA for photon file layer " + i + "/" + photonFileHeader.getNumberOfLayers());


            for (int y = 0; y < photonFileHeader.getResolutionY(); y++) {
                for (int x = 0; x < photonFileHeader.getResolutionX(); x++) {
                    source[y][x] = 0;
                }
            }

            for (int y = 0; y < unpackedImage.size(); y++) {
                BitSet currentRow = unpackedImage.get(y);
                if (currentRow != null) {
                    for (int x = 0; x < currentRow.length(); x++) {
                        if (currentRow.get(x)) {
                            source[y][x] = 255;
                        }
                    }
                }
            }

            // Calc
            int[][] target = photonAaMatrix.calc(source);

            int aaTresholdDiff = 255 / photonFileHeader.getAntiAliasingLevel();
            int aaTreshold = 0;
            for (PhotonFileLayer aaFileLayer : layer.antiAliasLayers) {
                photonLayer.clear();
                aaTreshold += aaTresholdDiff;

                for (int y = 0; y < photonFileHeader.getResolutionY(); y++) {
                    for (int x = 0; x < photonFileHeader.getResolutionX(); x++) {
                        if (target[y][x] >= aaTreshold) {
                            photonLayer.supported(x, y);
                        }
                    }
                }

                aaFileLayer.saveLayer(photonLayer);
            }

            i++;
        }
        photonLayer.unLink();
        System.gc();

    }

    public static void calculateLayers(PhotonFileHeader photonFileHeader, List<PhotonFileLayer> layers, int margin, IPhotonProgress iPhotonProgress) throws Exception {
        PhotonLayer photonLayer = new PhotonLayer(photonFileHeader.getResolutionX(), photonFileHeader.getResolutionY());
        ArrayList<BitSet> previousUnpackedImage = null;
        PhotonFileLayer previousLayer = null;
        int i = 0;
        for (PhotonFileLayer layer : layers) {
            ArrayList<BitSet> unpackedImage = layer.unpackImage(photonFileHeader.getResolutionX());

            iPhotonProgress.showInfo("Calculating photon file layer " + i + "/" + photonFileHeader.getNumberOfLayers());

            layer.boundingBox = calculateBoundingBox(unpackedImage);

            if (margin > 0) {
                Rectangle printArea = new Rectangle(margin, margin, photonFileHeader.getResolutionX() - (margin * 2),
                        photonFileHeader.getResolutionY() - (margin * 2));

                layer.extendsMargin = !printArea.contains(layer.boundingBox);
            }

            layer.unknownPixels(unpackedImage, photonLayer);

            layer.calculate(unpackedImage, previousUnpackedImage, photonLayer, previousLayer);

            if (previousUnpackedImage != null) {
                previousUnpackedImage.clear();
            }
            previousUnpackedImage = unpackedImage;
            previousLayer = layer;

            layer.packedLayerImage = photonLayer.packLayerImage();
            layer.isCalculated = true;

            if (photonFileHeader.getVersion() > 1) {
                for (PhotonFileLayer aaFileLayer : layer.antiAliasLayers) {
                    ArrayList<BitSet> aaUnpackedImage = aaFileLayer.unpackImage(photonFileHeader.getResolutionX());
                    PhotonLayer aaPhotonLayer = new PhotonLayer(photonFileHeader.getResolutionX(), photonFileHeader.getResolutionY());
                    aaFileLayer.unknownPixels(aaUnpackedImage, aaPhotonLayer);
                    aaFileLayer.packedLayerImage = aaPhotonLayer.packLayerImage();
                    aaFileLayer.isCalculated = false;
                }
            }

            i++;
        }
        photonLayer.unLink();
        System.gc();
    }

    public static void calculateLayers(PhotonFileHeader photonFileHeader, List<PhotonFileLayer> layers, int margin, int layerNo) throws Exception {
        PhotonLayer photonLayer = new PhotonLayer(photonFileHeader.getResolutionX(), photonFileHeader.getResolutionY());
        ArrayList<BitSet> previousUnpackedImage = null;
        PhotonFileLayer previousLayer = null;

        if (layerNo>0) {
            previousUnpackedImage = layers.get(layerNo-1).unpackImage(photonFileHeader.getResolutionX());
            previousLayer = layers.get(layerNo-1);
        }

        for (int i=0; i<2; i++) {
            PhotonFileLayer layer = layers.get(layerNo + i);
            ArrayList<BitSet> unpackedImage = layer.unpackImage(photonFileHeader.getResolutionX());

            layer.boundingBox = calculateBoundingBox(unpackedImage);

            if (margin > 0) {
                Rectangle printArea = new Rectangle(margin, margin, photonFileHeader.getResolutionX() - (margin * 2),
                        photonFileHeader.getResolutionY() - (margin * 2));

                layer.extendsMargin = !printArea.contains(layer.boundingBox);
            }

            layer.unknownPixels(unpackedImage, photonLayer);

            layer.calculate(unpackedImage, previousUnpackedImage, photonLayer, previousLayer);

            if (previousUnpackedImage != null) {
                previousUnpackedImage.clear();
            }
            previousUnpackedImage = unpackedImage;
            previousLayer = layer;

            layer.packedLayerImage = photonLayer.packLayerImage();
            layer.isCalculated = true;

            i++;
        }
        photonLayer.unLink();
        System.gc();
    }

    public ArrayList<PhotonRow> getRows() {
        return PhotonLayer.getRows(packedLayerImage, photonFileHeader.getResolutionX(), isCalculated);
    }

    public ArrayList<BitSet> getIslandRows() {
        return islandRows;
    }

    public int getIsLandsCount() {
        return isLandsCount;
    }

    public long getPixels() {
        return pixels;
    }

    public float getLayerPositionZ() {
        return layerPositionZ;
    }

    public void setLayerPositionZ(float layerPositionZ) {
        this.layerPositionZ = layerPositionZ;
    }

    public float getLayerExposure() {
        return layerExposure;
    }

    public float getLayerOffTime() {
        return layerOffTimeSeconds;
    }

    public void setLayerExposure(float layerExposure) {
        this.layerExposure = layerExposure;
    }

    public void setLayerOffTimeSeconds(float layerOffTimeSeconds) {
        this.layerOffTimeSeconds = layerOffTimeSeconds;
    }

    public void unLink() {
        imageData = null;
        packedLayerImage = null;
        if (islandRows!=null) {
            islandRows.clear();
        }
        photonFileHeader = null;
    }

    public boolean doExtendMargin() {
        return extendsMargin;
    }

    public Rectangle getBoundingBox() {
        return boundingBox;
    }

    private static Rectangle calculateBoundingBox(ArrayList<BitSet> unpackedImage) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < unpackedImage.size(); ++i) {
            BitSet row = unpackedImage.get(i);

            if (!row.isEmpty()) {
                minY = Math.min(minY, i);
                maxY = Math.max(maxY, i);

                int column = row.nextSetBit(0);
                while (column != -1) {
                    minX = Math.min(minX, column);
                    maxX = Math.max(maxX, column);

                    column = row.nextSetBit(column + 1);
                }
            }
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public PhotonLayer getLayer() {
        PhotonLayer photonLayer = new PhotonLayer(photonFileHeader.getResolutionX(), photonFileHeader.getResolutionY());
        photonLayer.unpackLayerImage(packedLayerImage);
        return photonLayer;
    }

    public void getUpdateLayer(PhotonLayer photonLayer) {
        photonLayer.unpackLayerImage(packedLayerImage);
    }

    public void updateLayerIslands(PhotonLayer photonLayer) {
        islandRows = new ArrayList<>();
        isLandsCount = photonLayer.setIslands(islandRows);
    }

    public void saveLayer(PhotonLayer photonLayer) throws Exception {
        this.packedLayerImage = photonLayer.packLayerImage();
        this.imageData = photonLayer.packImageData();
        this.dataSize = imageData.length;
        islandRows = new ArrayList<>();
        isLandsCount = photonLayer.setIslands(islandRows);
    }

    public ArrayList<BitSet> getUnknownRows() {
        return unpackImage(photonFileHeader.getResolutionX());
    }

    public PhotonFileLayer getAntiAlias(int a) {
        if (antiAliasLayers.size() > a) {
            return antiAliasLayers.get(a);
        }
        return null;
    }

    public ArrayList<PhotonFileLayer> getAntiAlias() {
        return antiAliasLayers;
    }
}
