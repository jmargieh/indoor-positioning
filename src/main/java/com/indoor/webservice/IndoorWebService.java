package main.java.com.indoor.webservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Geometry;

@Path("/")
@Component("IndoorWebService")
public class IndoorWebService {

	private InputStream pointsInputStream = IndoorWebService.class.getResourceAsStream("/json/points.json");
	private InputStream obstaclesInputStream = IndoorWebService.class.getResourceAsStream("/json/obstacles.json");
	
	private List<SimpleFeature> pointsArray = new ArrayList<>(); 
	private List<SimpleFeature> obstaclesArray = new ArrayList<>(); 
	//private ArrayList<? extends Geometry> pointsArray;
	//private ArrayList<? extends Geometry> obstaclesArray;

	private void closeInputStreams() throws IOException {
		pointsInputStream.close();
		obstaclesInputStream.close();
	}

	public void initArrayLists() {

		SimpleFeature feature;
		FeatureJSON io = new FeatureJSON();
		Geometry g;
		// get points.json as InputStream
		try {
			Reader rd = GeoJSONUtil.toReader(this.pointsInputStream);
			FeatureIterator<SimpleFeature> features = io.streamFeatureCollection(rd);
			while (features.hasNext()) {
				feature = features.next();
				//g = (Geometry) feature.getAttribute("geometry");
				this.pointsArray.add(feature);
			}

			rd = GeoJSONUtil.toReader(this.obstaclesInputStream);
			features = io.streamFeatureCollection(rd);
			while (features.hasNext()) {
				feature = features.next();
				//g = (Geometry) feature.getAttribute("geometry");
				this.obstaclesArray.add(feature);
			}
			
			closeInputStreams();

		} catch (IOException e) {
			System.err.print(e.getMessage());
		}

	}

	@GET
	@Path("/points")
	public Response isPointInObstacle() throws IOException {

		initArrayLists();

		for (Iterator<SimpleFeature> oIterator = this.obstaclesArray.iterator(); oIterator.hasNext();) {
			Geometry obstacle = (Geometry)oIterator.next().getAttribute("geometry");
			for (Iterator<SimpleFeature> pIterator = this.pointsArray.iterator(); pIterator.hasNext();) {

				if (obstacle.contains((Geometry)pIterator.next().getAttribute("geometry"))) {
					pIterator.remove();
				}

			}
		}
		
		for (int i = 0; i < this.pointsArray.size(); i++) {
			System.out.println(this.pointsArray.get(i).getAttribute("title"));
		}


		return Response.status(200).entity("String").build();

	}

}
