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

package photon.application.base;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import photon.application.MainForm;
import photon.application.utilities.PhotonCalcWorker;
import photon.file.parts.PhotonFileLayer;
import photon.file.parts.PhotonLayer;

/**
 * by bn on 31/07/2018.
 */
public class BaseFrame extends JFrame implements AWTEventListener {
    public MainForm baseForm;

    public BaseFrame(String title) throws HeadlessException {
        super(title);
        this.getToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("Extras");
        menuBar.add(menu);

        JMenuItem menuItem = new JMenuItem("Export Layer");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int layer = baseForm.layerSlider.getValue();

                try {
                    baseForm.photonFile.getLayer(layer).getLayer().exportAsPNG(new File("slice-" + layer + ".png"));
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Import Layer");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                baseForm.layerInfo.setForeground(Color.red);
                baseForm.layerInfo.setText("Importing layer...");

                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        PhotonCalcWorker calcWorker = new PhotonCalcWorker(baseForm);

                        int layer = baseForm.layerSlider.getValue();

                        try {
                            PhotonFileLayer fileLayer = baseForm.photonFile.getLayer(layer);
                            PhotonLayer layerData = fileLayer.getLayer();// new
                                                                         // PhotonLayer(baseForm.photonFile.getWidth(),
                                                                         // baseForm.photonFile.getHeight());
                            layerData.importFromPNG(new File("slice-" + layer + ".png"));
                            fileLayer.saveLayer(layerData);
                            baseForm.photonFile.calculate(layer);

                        } catch (Exception e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        calcWorker.execute();
                    }

                };

                thread.start();

            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Import Layers");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                baseForm.layerInfo.setForeground(Color.red);
                baseForm.layerInfo.setText("Importing layer...");

                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        PhotonCalcWorker calcWorker = new PhotonCalcWorker(baseForm);

                        int layer = baseForm.layerSlider.getValue();

                        try {

                            File[] files = (new File("slices")).listFiles();
                            Arrays.sort(files);

                            for (File file : files) {

                                PhotonFileLayer fileLayer = baseForm.photonFile.getLayer(layer);
                                PhotonLayer layerData = fileLayer.getLayer();// new
                                                                             // PhotonLayer(baseForm.photonFile.getWidth(),
                                                                             // baseForm.photonFile.getHeight());
                                layerData.importFromPNG(file);
                                fileLayer.saveLayer(layerData);
                                baseForm.photonFile.calculate(layer);
                                ++layer;
                            }

                        } catch (Exception e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        calcWorker.execute();
                    }

                };

                thread.start();

            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Remove Islands");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                baseForm.layerInfo.setForeground(Color.red);
                baseForm.layerInfo.setText("Removing islands...");

                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        PhotonCalcWorker calcWorker = new PhotonCalcWorker(baseForm);

                        try {
                            baseForm.photonFile.removeIslands();
                            // baseForm.showMarginAndIslandInformation();

                        } catch (Exception e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        calcWorker.execute();
                    }

                };

                thread.start();

            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Set Z Position");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int layer = baseForm.layerSlider.getValue();
                // TODO Auto-generated method stub

                PhotonFileLayer fileLayer = baseForm.photonFile.getLayer(layer);

                String zPos = JOptionPane.showInputDialog(BaseFrame.this, "Set Z position",
                        fileLayer.getLayerPositionZ());

                if (zPos != null && zPos.length() > 0) {
                    try {
                        float value = Float.parseFloat(zPos);
                        fileLayer.setLayerPositionZ(value);
                        baseForm.showLayerInformation(layer, fileLayer);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Set Exposure Time");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int layer = baseForm.layerSlider.getValue();

                PhotonFileLayer fileLayer = baseForm.photonFile.getLayer(layer);

                String exposure = JOptionPane.showInputDialog(BaseFrame.this, "Set Exposure Time",
                        fileLayer.getLayerExposure());

                if (exposure != null && exposure.length() > 0) {
                    try {
                        float value = Float.parseFloat(exposure);
                        fileLayer.setLayerExposure(value);
                        baseForm.showLayerInformation(layer, fileLayer);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Duplicate Layer");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    int layer = baseForm.layerSlider.getValue();

                    PhotonFileLayer orig = baseForm.photonFile.getLayer(layer);

                    PhotonFileLayer duplicate = new PhotonFileLayer(orig);

                    List<PhotonFileLayer> layers = baseForm.photonFile.getLayers();
                    layers.add(layer, duplicate);
                    baseForm.photonFile.getPhotonFileHeader().setNumberLayers(layers.size());

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                PhotonCalcWorker calcWorker = new PhotonCalcWorker(baseForm);

                calcWorker.execute();
            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Insert New Layer");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    int layer = baseForm.layerSlider.getValue();

                    PhotonFileLayer orig = baseForm.photonFile.getLayer(layer);

                    PhotonFileLayer duplicate = new PhotonFileLayer(orig);

                    duplicate.saveLayer(
                            new PhotonLayer(baseForm.photonFile.getWidth(), baseForm.photonFile.getHeight()));

                    List<PhotonFileLayer> layers = baseForm.photonFile.getLayers();
                    layers.add(layer, duplicate);
                    baseForm.photonFile.getPhotonFileHeader().setNumberLayers(layers.size());

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                PhotonCalcWorker calcWorker = new PhotonCalcWorker(baseForm);

                calcWorker.execute();
            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Remove Earlier Layers");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int layer = baseForm.layerSlider.getValue();

                if (layer == 0)
                    return;

                List<PhotonFileLayer> layers = baseForm.photonFile.getLayers();

                layers.subList(0, layer).clear();

                baseForm.photonFile.getPhotonFileHeader().setNumberLayers(layers.size());

                baseForm.layerSlider.setValue(0);

                PhotonCalcWorker calcWorker = new PhotonCalcWorker(baseForm);

                calcWorker.execute();
            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Delete Current Layer");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int layer = baseForm.layerSlider.getValue();

                List<PhotonFileLayer> layers = baseForm.photonFile.getLayers();
                layers.remove(layer);
                baseForm.photonFile.getPhotonFileHeader().setNumberLayers(layers.size());

                PhotonCalcWorker calcWorker = new PhotonCalcWorker(baseForm);

                calcWorker.execute();
            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Remove Later Layers");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int layer = baseForm.layerSlider.getValue();

                if (layer == baseForm.photonFile.getLayerCount() - 1)
                    return;

                List<PhotonFileLayer> layers = baseForm.photonFile.getLayers();

                layers.subList(layer + 1, baseForm.photonFile.getLayerCount()).clear();

                baseForm.photonFile.getPhotonFileHeader().setNumberLayers(layers.size());

                PhotonCalcWorker calcWorker = new PhotonCalcWorker(baseForm);

                calcWorker.execute();
            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Recalculate Z offsets");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                baseForm.photonFile.fixLayerHeights();
                int layer = baseForm.layerSlider.getValue();
                PhotonFileLayer fileLayer = baseForm.photonFile.getLayer(layer);
                baseForm.showLayerInformation(layer, fileLayer);
            }
        });

        menu.add(menuItem);

        menuItem = new JMenuItem("Update Preview");
        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    baseForm.photonFile.getPreviewOne().encodeImageData();
                    baseForm.photonFile.getPreviewTwo().encodeImageData();
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        menu.add(menuItem);

        setJMenuBar(menuBar);
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if(event instanceof KeyEvent){
            KeyEvent key = (KeyEvent)event;
            if(key.getID()== KeyEvent.KEY_PRESSED){ //Handle key presses

                if ((key.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
                    baseForm.handleKeyEvent(key);
                }
            }
        }
    }

    public void setMainForm(MainForm mainForm) {
        baseForm = mainForm;
        mainForm.frame = this;
    }
}
