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

	@Option(names = "-h", required = true)
	Float height;

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

		int layer1 = (int)(height / photonFiles.get(0).getPhotonFileHeader().getLayerHeight());

		int layer2 = (int)(height / photonFiles.get(1).getPhotonFileHeader().getLayerHeight());

		List<PhotonFileLayer> layers1 = photonFiles.get(0).getLayers();
		List<PhotonFileLayer> layers2 = photonFiles.get(1).getLayers();

		layers1.subList(layer1, layers1.size()).clear();
		layers1.addAll(layers2.subList(layer2, layers2.size()));

		photonFiles.get(0).getPhotonFileHeader().setNumberLayers(layers1.size());

		photonFiles.get(0).saveFile(output);

		return 0;
	}
}