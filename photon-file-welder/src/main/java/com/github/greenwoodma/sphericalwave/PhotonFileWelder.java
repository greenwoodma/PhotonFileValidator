package com.github.greenwoodma.sphericalwave;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import photon.file.PhotonFile;
import photon.file.parts.PhotonFileLayer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "Photon File Welder")
public class PhotonFileWelder implements Callable<Integer> {

	@Parameters(arity = "2")
	List<File> files;

	@Option(names = "-o", required = true)
	File output;

	@Option(names = "-h", required = true, arity = "1..*")
	List<Float> height;

	public static void main(String args[]) {
		int exitCode = new CommandLine(new PhotonFileWelder()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {

		List<PhotonFile> photonFiles = new ArrayList<PhotonFile>();		

		for (File file : files) {
			PhotonFile pf = new PhotonFile();
			pf.setMargin(0);
			pf.readFile(file);
			photonFiles.add(pf);
		}
		
		height.add(0, 0f);
		
		List<PhotonFileLayer> combined = new ArrayList<PhotonFileLayer>();
		
		for (int i = 0 ; i < height.size() ; ++i) {
			PhotonFile current = photonFiles.get(i%2);
			
			float layerThickness = current.getPhotonFileHeader().getLayerHeight();
			
			float startHeight = height.get(i);
			float endHeight = i != height.size() -1 ? height.get(i+1) : current.getLayer(current.getLayerCount()-1).getLayerPositionZ();
			
			
			System.out.println("\n"+startHeight+" --> " + endHeight);
			System.out.println(current.getPhotonFileHeader().getLayerHeight());
			System.out.println(current.getLayerCount());
			
			int firstLayer = (int)(startHeight / layerThickness);
			int lastLayer = (int)(endHeight / layerThickness)-1;
			
			System.out.println(firstLayer+": "+current.getLayer(firstLayer).getLayerPositionZ());
			System.out.println(lastLayer+": "+current.getLayer(lastLayer).getLayerPositionZ());
			
			combined.addAll(current.getLayers().subList(firstLayer, lastLayer+1));
			
			System.out.println(combined.get(combined.size()-1).getLayerPositionZ());
		}

		photonFiles.get(0).getLayers().clear();
		photonFiles.get(0).getLayers().addAll(combined);
		photonFiles.get(0).getPhotonFileHeader().setNumberLayers(combined.size());

		photonFiles.get(0).saveFile(output);

		return 0;
	}
}