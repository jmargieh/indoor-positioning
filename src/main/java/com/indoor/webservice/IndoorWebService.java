package main.java.com.indoor.webservice;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import main.java.com.indoor.dao.IndoorNavigationProcessing;
import main.java.com.indoor.helpers.RandomGeoJsonPointGenerator;

import org.springframework.stereotype.Component;

@Path("/")
@Component("IndoorWebService")
public class IndoorWebService {

	private HashMap<String, IndoorNavigationProcessing> indoorPosMaps = new HashMap<String, IndoorNavigationProcessing>();
	
	/**
	 * 
	 * @param space geojson file
	 * @param obstacles geojson file
	 * @param points geojson file
	 * @return indoorNavPro result, indicating if the processing has succeeded
	 * @throws IOException
	 */
	@GET
	@Path("/processGeoJsons")
	@Produces("application/json")
	public Response processGeoJsons(@QueryParam("space") String space, @QueryParam("obstacles") String obstacles, @QueryParam("points") String points) throws IOException {
		
		//process
		IndoorNavigationProcessing indoorNavPro = new IndoorNavigationProcessing(space,obstacles, points);
		UUID uuid = UUID.randomUUID();
		String result = "{\"result\":" + "\"" + indoorNavPro.getResult() + "\" ," + "\"uuid\":" + "\"" + uuid.toString() + "\"}";
		
		indoorPosMaps.put(uuid.toString(), indoorNavPro);
		return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(result).build();
	}
	
	/**
	 * 
	 * @param uuid processing uuid
	 * @return json heatmap [ [lng,lat,count], [..], [..] ]
	 * @throws IOException
	 */
	@GET
	@Path("/generateHeatMap/{uuid}")
	@Produces("application/json")
	public Response generateHeatMap(@PathParam("uuid") String uuid) throws IOException {
		
		// create & preserve processing uuid and generate a heatmap
		JSONObject heatmapObject = this.indoorPosMaps.get(uuid).GenerateHeatMap();
		return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(heatmapObject.toJSONString()).build();
	}
	
	@GET
	@Path("/generateDevicesPaths/{uuid}")
	@Produces("application/json")
	public Response generateDevicesPaths(@PathParam("uuid") String uuid) throws IOException {
		// create & preserve processing uuid and generate a heatmap
		JSONObject devicesPathsObject = this.indoorPosMaps.get(uuid).GenerateDevicesPaths();
		return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(devicesPathsObject.toJSONString()).build();
	}
	
	@GET
	@Path("/generateRandomPoints/{pointsNumber}/{deviceNumber}")
	public Response generateRandomGeoJSONPoints(@PathParam("pointsNumber") int pointsNumber, @PathParam("deviceNumber") int deviceNumber) throws IOException {
		String res = RandomGeoJsonPointGenerator.generatePathOfPoints(pointsNumber,deviceNumber);

		return Response.status(200).entity(res).build();

	}

}
