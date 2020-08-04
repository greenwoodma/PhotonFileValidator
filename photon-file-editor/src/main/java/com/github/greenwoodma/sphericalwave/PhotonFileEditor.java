package com.github.greenwoodma.sphericalwave;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.pushingpixels.flamingo.api.ribbon.JRibbonFrame;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

public class PhotonFileEditor extends JRibbonFrame {
    public static void main(String args[]) {
        SwingUtilities.invokeLater(() -> {
        	try {
				UIManager.setLookAndFeel(new SubstanceGraphiteLookAndFeel());
			} catch (UnsupportedLookAndFeelException e) {
				// TODO fail miserably!
				e.printStackTrace();
			}
        	
        	PhotonFileEditor editor = new PhotonFileEditor();
        	editor.setVisible(true);
        });
    }
    
    public PhotonFileEditor() {
        super("SphericalWave");   
        setSize(800,600);
    }
}
