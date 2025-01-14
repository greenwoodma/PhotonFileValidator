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

package photon.file;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import photon.file.parts.DummyPhotonLoadProgress;
import photon.file.parts.IPhotonProgress;
import photon.file.parts.PhotonAaMatrix;
import photon.file.parts.PhotonFileHeader;
import photon.file.parts.PhotonFileLayer;
import photon.file.parts.PhotonFilePreview;
import photon.file.parts.PhotonFilePrintParameters;
import photon.file.parts.PhotonLayer;
import photon.file.parts.PhotonOutputStream;

/**
 * by bn on 30/06/2018.
 */
public class PhotonFile {
    private PhotonFileHeader photonFileHeader;
    private PhotonFilePrintParameters photonFilePrintParameters;
    private PhotonFilePreview previewOne;
    private PhotonFilePreview previewTwo;
    private List<PhotonFileLayer> layers;

    private StringBuilder islandList;
    private int islandLayerCount;
    private ArrayList<Integer> islandLayers;

    private int margin;
    private ArrayList<Integer> marginLayers;

    private Rectangle2D boundingBox;

    public PhotonFile readFile(File file) throws Exception {
        return readFile(getBinaryData(file));
    }

    public PhotonFile readFile(File file, IPhotonProgress iPhotonProgress) throws Exception {
        return readFile(getBinaryData(file), iPhotonProgress);
    }

    public PhotonFile readFile(byte[] file) throws Exception {
        return readFile(file, new DummyPhotonLoadProgress());
    }
    
    public List<PhotonFileLayer> getLayers() {
    	return layers;
    }

    public PhotonFile readFile(byte[] file, IPhotonProgress iPhotonProgress) throws Exception {
        iPhotonProgress.showInfo("Reading photon file header information...");
        photonFileHeader = new PhotonFileHeader(file);
        iPhotonProgress.showInfo("Reading photon large preview image information...");
        previewOne = new PhotonFilePreview(photonFileHeader.getPreviewOneOffsetAddress(), file);
        iPhotonProgress.showInfo("Reading photon small preview image information...");
        previewTwo = new PhotonFilePreview(photonFileHeader.getPreviewTwoOffsetAddress(), file);
        if (photonFileHeader.getVersion() > 1) {
            iPhotonProgress.showInfo("Reading Print parameters information...");
            photonFilePrintParameters = new PhotonFilePrintParameters(photonFileHeader.getPrintParametersOffsetAddress(), file);
        }
        iPhotonProgress.showInfo("Reading photon layers information...");
        layers = PhotonFileLayer.readLayers(photonFileHeader, file, margin, iPhotonProgress);
        resetMarginAndIslandInfo();

        return this;
    }

    public void saveFile(File file) throws Exception {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        writeFile(fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public byte[] saveFile() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeFile(baos);
        return baos.toByteArray();
    }

    private void writeFile(OutputStream outputStream) throws Exception {
        int antiAliasLevel = 1;
        if (photonFileHeader.getVersion() > 1) {
            antiAliasLevel = photonFileHeader.getAntiAliasingLevel();
        }

        int headerPos = 0;
        int previewOnePos = headerPos + photonFileHeader.getByteSize();
        int previewTwoPos = previewOnePos + previewOne.getByteSize();
        int layerDefinitionPos = previewTwoPos + previewTwo.getByteSize();

        int parametersPos = 0;
        if (photonFileHeader.getVersion() > 1) {
            parametersPos = layerDefinitionPos;
            layerDefinitionPos = parametersPos + photonFilePrintParameters.getByteSize();
        }

        int dataPosition = layerDefinitionPos + (PhotonFileLayer.getByteSize() * photonFileHeader.getNumberOfLayers() * antiAliasLevel);


        PhotonOutputStream os = new PhotonOutputStream(outputStream);

        photonFileHeader.save(os, previewOnePos, previewTwoPos, layerDefinitionPos, parametersPos);
        previewOne.save(os, previewOnePos);
        previewTwo.save(os, previewTwoPos);

        if (photonFileHeader.getVersion() > 1) {
            photonFilePrintParameters.save(os);
        }

        // Optimize order for speed read on photon
        for (int i = 0; i < photonFileHeader.getNumberOfLayers(); i++) {
            PhotonFileLayer layer = layers.get(i);
            dataPosition = layer.savePos(dataPosition);
            if (antiAliasLevel > 1) {
                for (int a = 0; a < (antiAliasLevel - 1); a++) {
                    dataPosition = layer.getAntiAlias(a).savePos(dataPosition);
                }
            }
        }

        // Order for backward compatibility with photon/cbddlp version 1
        for (int i = 0; i < photonFileHeader.getNumberOfLayers(); i++) {
            layers.get(i).save(os);
        }

        if (antiAliasLevel > 1) {
            for (int a = 0; a < (antiAliasLevel - 1); a++) {
                for (int i = 0; i < photonFileHeader.getNumberOfLayers(); i++) {
                    layers.get(i).getAntiAlias(a).save(os);
                }
            }
        }

        // Optimize order for speed read on photon
        for (int i = 0; i < photonFileHeader.getNumberOfLayers(); i++) {
            PhotonFileLayer layer = layers.get(i);
            layer.saveData(os);
            if (antiAliasLevel > 1) {
                for (int a = 0; a < (antiAliasLevel - 1); a++) {
                    layer.getAntiAlias(a).saveData(os);
                }
            }
        }
    }


    private byte[] getBinaryData(File entry) throws Exception {
        if (entry.isFile()) {
            int fileSize = (int) entry.length();
            byte[] fileData = new byte[fileSize];

            InputStream stream = new FileInputStream(entry);
            int bytesRead = 0;
            while (bytesRead < fileSize) {
                int readCount = stream.read(fileData, bytesRead, fileSize - bytesRead);
                if (readCount < 0) {
                    throw new IOException("Could not read all bytes of the file");
                }
                bytesRead += readCount;
            }

            return fileData;
        }
        return null;
    }

    public String getInformation() {
        if (photonFileHeader == null) return "";
        return String.format("T: %.3f", photonFileHeader.getLayerHeight()) +
                ", E: " + formatSeconds(photonFileHeader.getNormalExposure()) +
                ", O: " + formatSeconds(photonFileHeader.getOffTime()) +
                ", BE: " + formatSeconds(photonFileHeader.getBottomExposureTimeSeconds()) +
                String.format(", BL: %d", photonFileHeader.getBottomLayers());
    }

    public String formatSeconds(float time) {
        if (time % 1 == 0) {
            return String.format("%.0fs", time);
        } else {
            return String.format("%.1fs", time);
        }
    }

    public int getIslandLayerCount() {
        if (islandList == null) {
            findIslands();
        }
        return islandLayerCount;
    }

    public ArrayList<Integer> getIslandLayers() {
        if (islandList == null) {
            findIslands();
        }
        return islandLayers;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public ArrayList<Integer> getMarginLayers() {
        if (marginLayers == null) {
            return new ArrayList<>();
        }
        return marginLayers;
    }

    public String getMarginInformation() {
        if (marginLayers == null) {
            return "No safety margin set, printing to the border.";
        } else {
            if (marginLayers.size() == 0) {
                return "The model is within the defined safety margin (" + this.margin + " pixels).";
            } else if (marginLayers.size() == 1) {
                return "The layer " + marginLayers.get(0) + " contains model parts that extend beyond the margin.";
            }
            StringBuilder marginList = new StringBuilder();
            int count = 0;
            for (int layer : marginLayers) {
                if (count > 10) {
                    marginList.append(", ...");
                    break;
                } else {
                    if (marginList.length() > 0) marginList.append(", ");
                    marginList.append(layer);
                }
                count++;
            }
            return "The layers " + marginList.toString() + " contains model parts that extend beyond the margin.";
        }
    }

    public String getLayerInformation() {
        if (islandList == null) {
            findIslands();
        }
        if (islandLayerCount == 0) {
            return "Whoopee, all is good, no unsupported areas";
        } else if (islandLayerCount == 1) {
            return "Unsupported islands found in layer " + islandList.toString();
        }
        return "Unsupported islands found in layers " + islandList.toString();
    }

    private void findIslands() {
        if (islandLayers != null) {
            islandLayers.clear();
            islandList = new StringBuilder();
            islandLayerCount = 0;
            if (layers != null) {
                for (int i = 0; i < photonFileHeader.getNumberOfLayers(); i++) {
                    PhotonFileLayer layer = layers.get(i);
                    if (layer.getIsLandsCount() > 0) {
                        if (islandLayerCount < 11) {
                            if (islandLayerCount == 10) {
                                islandList.append(", ...");
                            } else {
                                if (islandList.length() > 0) islandList.append(", ");
                                islandList.append(i);
                            }
                        }
                        islandLayerCount++;
                        islandLayers.add(i);
                    }
                }
            }
        }
    }

    public int getWidth() {
        return photonFileHeader.getResolutionX();
    }

    public int getHeight() {
        return photonFileHeader.getResolutionY();
    }

    public int getLayerCount() {
        return photonFileHeader.getNumberOfLayers();
    }

    public PhotonFileLayer getLayer(int i) {
        if (layers != null && layers.size() > i) {
            return layers.get(i);
        }
        return null;
    }

    public long getPixels() {
        long total = 0;
        if (layers != null) {
            for (PhotonFileLayer layer : layers) {
                total += layer.getPixels();
            }
        }
        return total;
    }

    public PhotonFileHeader getPhotonFileHeader() {
        return photonFileHeader;
    }

    public PhotonFilePreview getPreviewOne() {
        return previewOne;
    }

    public PhotonFilePreview getPreviewTwo() {
        return previewTwo;
    }


    public void unLink() {
        while (!layers.isEmpty()) {
            PhotonFileLayer layer = layers.remove(0);
            layer.unLink();
        }
        if (islandLayers != null) {
            islandLayers.clear();
        }
        if (marginLayers != null) {
            marginLayers.clear();
        }
        photonFileHeader.unLink();
        photonFileHeader = null;
        previewOne.unLink();
        previewOne = null;
        previewTwo.unLink();
        previewTwo = null;
        System.gc();
    }


    public void adjustLayerSettings() {
        for (int i = 0; i < layers.size(); i++) {
            PhotonFileLayer layer = layers.get(i);
            if (i < photonFileHeader.getBottomLayers()) {
                layer.setLayerExposure(photonFileHeader.getBottomExposureTimeSeconds());
            } else {
                layer.setLayerExposure(photonFileHeader.getNormalExposure());
            }
            layer.setLayerOffTimeSeconds(photonFileHeader.getOffTimeSeconds());
        }
    }

    public void fixLayers(IPhotonProgress progres) throws Exception {
        PhotonLayer layer = null;
        for(int layerNo : islandLayers) {
            progres.showInfo("Checking layer " + layerNo);

            // Unpack the layer data to the layer utility class
            PhotonFileLayer fileLayer = layers.get(layerNo);
            if (layer==null) {
                layer = fileLayer.getLayer();
            } else {
                fileLayer.getUpdateLayer(layer);
            }

            int changed = fixit(progres, layer, fileLayer, 10);
            if (changed==0) {
                progres.showInfo(", but nothing could be done.");
            } else {
                fileLayer.saveLayer(layer);
                calculate(layerNo);
            }

            progres.showInfo("<br>");
        }

        findIslands();

        calculate(progres);
    }

    public void removeIslands() throws Exception {
    	for (int i = 0 ; i < getLayerCount() ; ++i) {

        	PhotonFileLayer current  = getLayer(i);

        	if (current.getIsLandsCount() == 0) continue;

        	PhotonLayer layerData = current.getLayer();

			// can we use the row islands to speed this up?
			for (int h = 0; h < getHeight(); h++) {
				if (layerData.rowIslands[h] > 0) {
					for (int w = 0; w < getWidth(); w++) {
						if (layerData.get(w, h) == PhotonLayer.ISLAND) {

							layerData.remove(w, h, PhotonLayer.ISLAND);
						}
					}
				}
			}

            current.saveLayer(layerData);
        }

    	findIslands();
    }

    private int fixit(IPhotonProgress progres, PhotonLayer layer, PhotonFileLayer fileLayer, int loops) throws Exception {
        int changed = layer.fixlayer();
        if (changed>0) {
            layer.reduce();
            fileLayer.updateLayerIslands(layer);
            progres.showInfo(", " + changed + " pixels changed");
            if (loops>0) {
                changed += fixit(progres, layer, fileLayer, loops -1);
            }
        }
        return changed;
    }

    public void calculateAaLayers(IPhotonProgress progres, PhotonAaMatrix photonAaMatrix) throws Exception {
        PhotonFileLayer.calculateAALayers(photonFileHeader, layers, photonAaMatrix, progres);
    }

    public void calculate(IPhotonProgress progres) throws Exception {
        PhotonFileLayer.calculateLayers(photonFileHeader, layers, margin, progres);
        resetMarginAndIslandInfo();
    }

    public void calculate(int layerNo) throws Exception {
        PhotonFileLayer.calculateLayers(photonFileHeader, layers, margin, layerNo);
        resetMarginAndIslandInfo();
    }

    private void resetMarginAndIslandInfo() {
        islandList = null;
        islandLayerCount = 0;
        islandLayers = new ArrayList<>();

        if (margin > 0) {
            marginLayers = new ArrayList<>();
            for (int i = 0; i < layers.size(); ++i) {
                if (layers.get(i).doExtendMargin()) {
                    marginLayers.add(i);
                }
            }
        }

        for (PhotonFileLayer layer : layers) {
            boundingBox = boundingBox == null ? layer.getBoundingBox()
                    : boundingBox.createUnion(layer.getBoundingBox());
        }
    }

    public float getZdrift() {
        float expectedHeight = photonFileHeader.getLayerHeight() * (photonFileHeader.getNumberOfLayers()-1);
        float actualHeight = layers.get(layers.size()-1).getLayerPositionZ();
        return expectedHeight - actualHeight;

    }

    public void fixLayerHeights() {
        int index = 0;
        for(PhotonFileLayer layer : layers) {
            layer.setLayerPositionZ(index * photonFileHeader.getLayerHeight());
            index++;
        }
    }

    public boolean hasAA() {
        return (photonFileHeader.getVersion()>1 && photonFileHeader.getAntiAliasingLevel()>1);
    }

    public int getAALevels() {

        if (photonFileHeader.getVersion()>1) {
            return photonFileHeader.getAntiAliasingLevel();
        }
        return 1;
    }

    public PhotonFilePrintParameters getPhotonFileParameters() {
        return photonFilePrintParameters;
    }

    public void changeToVersion2() {
        photonFileHeader.makeVersion(2);
        photonFilePrintParameters = new PhotonFilePrintParameters(photonFileHeader.getBottomLayers());
    }

    // only call this when recalculating AA levels
    public void setAALevels(int levels) {
        if (photonFileHeader.getVersion()>1) {
            if (levels < photonFileHeader.getAntiAliasingLevel()) {
                reduceAaLevels(levels);
            }
            if (levels > photonFileHeader.getAntiAliasingLevel()) {
                increaseAaLevels(levels);
            }
        }
    }

    private void increaseAaLevels(int levels) {
        // insert base layer to the correct count, as we are to recalc the AA anyway
        for(PhotonFileLayer photonFileLayer : layers) {
            while (photonFileLayer.getAntiAlias().size()<(levels-1)) {
                photonFileLayer.getAntiAlias().add(new PhotonFileLayer(photonFileLayer, photonFileHeader));
            }
        }
        photonFileHeader.setAntiAliasingLevel(levels);
    }

    private void reduceAaLevels(int levels) {
        // delete any layers to the correct count, as we are to recalc the AA anyway
        for(PhotonFileLayer photonFileLayer : layers) {
            while (photonFileLayer.getAntiAlias().size()>(levels-1)) {
                photonFileLayer.getAntiAlias().remove(0);
            }
        }
        photonFileHeader.setAntiAliasingLevel(levels);
    }


}

