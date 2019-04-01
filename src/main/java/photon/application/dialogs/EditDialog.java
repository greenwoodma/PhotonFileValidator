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

package photon.application.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import photon.application.MainForm;
import photon.file.PhotonFile;
import photon.file.parts.PhotonDot;
import photon.file.parts.PhotonFileLayer;
import photon.file.parts.PhotonLayer;
import photon.file.ui.PhotonEditPanel;

public class EditDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel infoText;
    private JPanel editArea;

    private EditDialog me;
    private MainForm mainForm;
    private PhotonFile photonFile;
    private PhotonFileLayer fileLayer;
    private PhotonLayer layer;
    private int layerNo;
    private int layerX;
    private int layerY;
    private HashSet<PhotonDot> dots;

    public PhotonDot pressedDot;


    public EditDialog(MainForm mainForm) {
        super(mainForm.frame);
        this.me = this;
        this.mainForm = mainForm;
        
        editArea = new PhotonEditPanel(780, 480);
        
        contentPane = new JPanel(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        infoText = new JLabel(" ");
        
        buttonOK = new JButton("OK");
        buttonCancel = new JButton("Cancel");
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(buttonOK);
        buttonPanel.add(Box.createHorizontalStrut(5));
        buttonPanel.add(buttonCancel);
        
        contentPane.add(infoText, BorderLayout.NORTH);
        contentPane.add(editArea, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {onOK();}
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {onCancel();}
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        editArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressedDot = getPosition(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PhotonDot releasedDot = getPosition(e);
                if (pressedDot != null && releasedDot != null) {
                    int x1 = Integer.min(pressedDot.x, releasedDot.x);
                    int x2 = Integer.max(pressedDot.x, releasedDot.x);
                    int y1 = Integer.min(pressedDot.y, releasedDot.y);
                    int y2 = Integer.max(pressedDot.y, releasedDot.y);

                    for (int x = x1; x <= x2; x++) {
                        for (int y = y1; y <= y2; y++) {
                            me.handleClick(x, y);
                        }
                    }
                }
            }
        });
    }

    private void handleClick(int x, int y) {
        Color color = isSet(x, y);
        ((PhotonEditPanel) editArea).drawDot(x, y, layer, color);
        editArea.repaint();
    }

    private Color isSet(int x, int y) {
        byte original = layer.get(layerX + x, layerY + y);
        boolean result = original != PhotonLayer.OFF;

        PhotonDot dot = new PhotonDot(layerX + x, layerY + y);
        if (dots.contains(dot)) {
            dots.remove(dot);

            switch (layer.get(layerX + x, layerY + y)) {
                case PhotonLayer.SUPPORTED:
                    return Color.decode("#008800");

                case PhotonLayer.CONNECTED:
                    return Color.decode("#FFFF00");

                case PhotonLayer.ISLAND:
                    return Color.decode("#FF0000");

                default:
                    return Color.black;

            }

        } else {
            dots.add(dot);
            return result ? Color.darkGray : Color.cyan;
        }
    }

    private void onOK() {
        if (dots.size() > 0) {
            for (PhotonDot dot : dots) {
                byte type = layer.get(dot.x, dot.y);
                if (type == PhotonLayer.OFF) {
                    layer.island(dot.x, dot.y);
                } else {
                    layer.remove(dot.x, dot.y, type);
                }
            }
            try {
                fileLayer.saveLayer(layer);
                photonFile.calculate(layerNo);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            mainForm.changeLayer();
            mainForm.showMarginAndIslandInformation();
        }
        dispose();
        
        mainForm.calc();
    }

    private void onCancel() {
        dispose();
    }


    public void setInformation(PhotonFile photonFile, int layerNo, int mouseX, int mouseY) {
        this.dots = new HashSet<>();
        this.layerNo = layerNo;
        this.photonFile = photonFile;
        this.fileLayer = photonFile.getLayer(layerNo);
        this.layer = fileLayer.getLayer();
        int indexX = (mouseX < 38) ? 1 : mouseX - 38;
        int indexY = (mouseY < 23) ? 1 : mouseY - 23;

        if (indexX + 74 >= photonFile.getWidth()) {
            indexX = photonFile.getWidth() - 74;
        }
        if (indexY + 44 >= photonFile.getHeight()) {
            indexY = photonFile.getHeight() - 44;
        }

        layerX = indexX - 1;
        layerY = indexY - 1;

        infoText.setText("Showing column " + indexX + " to " + (indexX + 74) + ", in row " + indexY + " to " + (indexY + 44) + ")");
        ((PhotonEditPanel) editArea).drawLayer(layerX, layerY, layer);
        editArea.repaint();
    }


    private void createUIComponents() {
         
    }

    public static PhotonDot getPosition(MouseEvent e) {
        if (e.getX() > 15 && e.getY() > 15) {
            int x = (e.getX() - 15) / 10;
            int y = (e.getY() - 15) / 10;
            if (x < 75 && y < 45) {
                return new PhotonDot(x, y);
            }
        }
        return null;
    }

}
