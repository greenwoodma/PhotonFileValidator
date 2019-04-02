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

package photon.file.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.BitSet;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JViewport;

import photon.file.parts.PhotonFileLayer;
import photon.file.parts.PhotonLine;
import photon.file.parts.PhotonRow;

/**
 * by bn on 02/07/2018.
 */
public class PhotonLayerImage extends JLabel {
    private int width;
    private int height;
    private float scale = 1f;
    private BufferedImage image;

    public PhotonLayerImage(int width, int height) {
        this.width = width;
        this.height = height;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        //setPreferredSize(new Dimension(width, height));
        
    }
    
    public BufferedImage getImage() {
    	return image;
    }
    
    public float getScale() {
    	return scale;
    }
    
   
    public void reScale(int width, int height) {
        reScale(scale, width, height);
    }

    public void reScale(float scale, int width, int height) {
        this.scale = scale;
        this.width = width;
        this.height = height;
        
        JViewport parent  = (JViewport)getParent();
        
        
        this.scale = scale * height < parent.getHeight() ? (float)parent.getHeight()/height : scale;
        
        
        
        image = new BufferedImage((int) (width * this.scale), (int) (height * this.scale), BufferedImage.TYPE_INT_RGB);
        //setPreferredSize(new Dimension((int) (width * scale), (int) (height * scale)));
        
    }

    public void drawLayer(PhotonFileLayer layer, int margin) {
        if (layer != null) {
            Graphics2D g = image.createGraphics();
            g.scale(scale, scale);

            g.clearRect(0, 0, width, height);

            if (margin > 0) {
                int x2 = (width - 1) - margin;
                int y2 = (height - 1) - margin;
                g.setColor(Color.decode("#009999"));
                g.drawLine(margin, margin, x2, margin);
                g.drawLine(margin, margin, margin, y2);
                g.drawLine(margin, y2, x2, y2);
                g.drawLine(x2, margin, x2, y2);
            }

            if (layer.isCalculated) {
                if (layer.getIsLandsCount() < 100) {
                    int columnNumber = 0;
                    g.setColor(Color.decode("#550000"));
                    for (BitSet column : layer.getIslandRows()) {
                        drawCross(g, columnNumber, column);
                        columnNumber++;
                    }

                }

                ArrayList<PhotonRow> rows = layer.getRows();
                if (rows != null) {
                    int columnNumber = 0;
                    for (PhotonRow row : rows) {
                        int i = 0;
                        for (PhotonLine line : row.lines) {
                            int end = i + line.length;
                            if (line.color != Color.black) {
                                g.setColor(line.color);
                                g.drawLine(i, columnNumber, end, columnNumber);
                            }
                            i = end;
                        }
                        columnNumber++;
                    }
                }

            } else {
                g.setColor(Color.decode("#008888"));
                int columnNumber = 0;
                for (BitSet column : layer.getUnknownRows()) {
                    drawDot(g, columnNumber, column);
                    columnNumber++;
                }
            }
            
            g.dispose();
        }
        
        setIcon(new ImageIcon(image));
    }

    private void drawDot(Graphics2D g, int columnNumber, BitSet column) {
        if (!column.isEmpty()) {
            for (int i = column.nextSetBit(0); i >= 0; i = column.nextSetBit(i + 1)) {
                g.drawLine(i, columnNumber, i, columnNumber);
            }
        }
    }

    private void drawCross(Graphics2D g, int columnNumber, BitSet column) {
        if (!column.isEmpty()) {
            for (int i = column.nextSetBit(0); i >= 0; i = column.nextSetBit(i + 1)) {
                g.drawLine(0, columnNumber, width - 1, columnNumber);
                g.drawLine(i, 0, i, height - 1);
            }
        }
    }

}
