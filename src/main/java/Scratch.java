import java.util.ArrayList;
import java.util.List;

import com.github.skjolberg.packing.Box;
import com.github.skjolberg.packing.BoxItem;
import com.github.skjolberg.packing.Container;
import com.github.skjolberg.packing.LargestAreaFitFirstPackager;
import com.github.skjolberg.packing.Level;
import com.github.skjolberg.packing.Packager;
import com.github.skjolberg.packing.Placement;

public class Scratch {
	public static void main(String args[]) {
		List<Container> containers = new ArrayList<Container>();
		containers.add(new Container(10, 10, 1, Integer.MAX_VALUE)); // x y z and weight
		Packager packager = new LargestAreaFitFirstPackager(containers);
		
		List<BoxItem> products = new ArrayList<BoxItem>();
		//products.add(new BoxItem(new Box("Foot", 6, 10, 1, 1), 1));
		products.add(new BoxItem(new Box("Leg", 10, 4, 1, 1), 3));
		//products.add(new BoxItem(new Box("Arm", 4, 10, 2, 50), 1));
			
		// match a single container
		Container match = packager.pack(products);
		
		if (match != null) {
			Level level = match.getLevels().get(0);
			for (Placement p : level) {
				System.out.println(p.getSpace()+" holds " +p.getBox());
			}
		}
		else {
			System.out.println("falied");
		}
	}
}
