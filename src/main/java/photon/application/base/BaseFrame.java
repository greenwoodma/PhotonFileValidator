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
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import photon.application.MainForm;
import photon.application.utilities.PhotonCalcWorker;
import photon.file.ui.PhotonLayerImage;

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
				
				PhotonLayerImage layerImage = new PhotonLayerImage(baseForm.photonFile.getWidth(), baseForm.photonFile.getHeight());
				
				layerImage.drawLayer(baseForm.photonFile.getLayer(layer), 10);
				
				try {
					ImageIO.write(layerImage.getImage(), "PNG", new File("slice-"+layer+".png"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
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
							//baseForm.showMarginAndIslandInformation();
							
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
