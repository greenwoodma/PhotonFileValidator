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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import photon.application.MainForm;
import photon.application.utilities.PhotonFixWorker;
import photon.file.PhotonFile;
import photon.file.parts.PhotonFileLayer;

public class FixDialog extends JDialog {
    
    public JButton buttonOK;
    public JButton startButton;
    private JLabel progressInfo;

    private StringBuilder information;

    public FixDialog(final MainForm mainForm) {
        super(mainForm.frame);
        
        JPanel contentPane = new JPanel(new BorderLayout());
    	contentPane.setBorder(new EmptyBorder(5,5,5,5));
    	
    	contentPane.add(new JLabel("Try to fix minor errors, where pixels only connects by the corners or the space between pixels is only one pixel."),BorderLayout.NORTH);
    	
    	progressInfo = new JLabel();
        progressInfo.setText("");
        progressInfo.setVerticalAlignment(1);
        progressInfo.setVerticalTextPosition(1);
        JScrollPane textPane = new JScrollPane(progressInfo);
                
        textPane.setPreferredSize(new Dimension(10, 200));
        
        contentPane.add(textPane,BorderLayout.CENTER);
    	
        buttonOK = new JButton();
        buttonOK.setText("Cancel");
        
        startButton = new JButton();
        startButton.setText("Start");
        
        JPanel buttonBar =  new JPanel();
        buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.X_AXIS));
        buttonBar.add(Box.createHorizontalGlue());
        buttonBar.add(startButton);
        buttonBar.add(Box.createHorizontalStrut(5));
        buttonBar.add(buttonOK);
        
        contentPane.add(buttonBar, BorderLayout.SOUTH);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	dispose();
            }
        });

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                buttonOK.setEnabled(false);
                PhotonFixWorker photonFixWorker = new PhotonFixWorker(FixDialog.this, mainForm.photonFile, mainForm);
                photonFixWorker.execute();
            }
        });

        setContentPane(contentPane);    
    }
    
    public void setInformation(PhotonFile photonFile) {
        information = null;
        setTitle("Fix pixels errors");

        StringBuilder builder = new StringBuilder("<h4>Information:</h4>");
        builder.append("<p>Layers with islands: ").append(photonFile.getIslandLayerCount()).append("</p><br>");

        for (int layerNo : photonFile.getIslandLayers()) {
            PhotonFileLayer layer = photonFile.getLayer(layerNo);
            builder.append(String.format("<p>Layer %6d have %9d island pixels</p>", layerNo, layer.getIsLandsCount()));
        }

        showProgressHtml(builder.toString());

    }

    public void appendInformation(String str) {
        if (information == null) {
            information = new StringBuilder();
            information.append("<h4>Fixing progress:</h4>");
        }
        information.append(str);
        showProgressHtml(information.toString());
    }

    public void showProgress(String progress) {
        progressInfo.setText("<html>" + progress.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\n", "<br/>") + "</html>");
    }

    public void showProgressHtml(String progress) {
        progressInfo.setText("<html>" + progress + "</html>");
    }
}
