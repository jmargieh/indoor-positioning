package main.java.com.indoor.helpers;
import com.vividsolutions.jts.geom.Geometry;

public class CustomLineString {
	
	private Geometry line;
	private double rate;
	
	
	public CustomLineString(Geometry line, double rate) {
		this.line = line;
		this.rate = rate;
	}

}
