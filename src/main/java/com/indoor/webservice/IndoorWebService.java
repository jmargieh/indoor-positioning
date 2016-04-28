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

	private HashMap<String, JSONObject> heatMaps = new HashMap<String, JSONObject>();
	
	@GET
	@Path("/processGeoJsons")
	@Produces("application/json")
	public Response processGeoJsons(@QueryParam("space") String space, @QueryParam("obstacles") String obstacles, @QueryParam("points") String points) throws IOException {
		
		IndoorNavigationProcessing indoorNavPro = new IndoorNavigationProcessing(space,obstacles, points);
		UUID uuid = UUID.randomUUID();
		String result = "{\"result\":" + "\"" + indoorNavPro.getResult() + "\" ," + "\"uuid\":" + "\"" + uuid.toString() + "\"}";
		
		JSONObject heatmapObject = indoorNavPro.GenerateHeatMap();
		heatMaps.put(uuid.toString(), heatmapObject);
		
		return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(result).build();

	}
	
	@GET
	@Path("/generateHeatMap/{uuid}")
	@Produces("application/json")
	public Response generateHeatMap(@PathParam("uuid") String uuid) throws IOException {
		JSONObject heatmapObject = this.heatMaps.get(uuid);
		return Response.status(200).type(MediaType.APPLICATION_JSON_TYPE).entity(heatmapObject.toJSONString()).build();

	}
	
	@GET
	@Path("/generatePoints/{pointsNumber}/{deviceNumber}")
	public Response generateRandomGeoJSONPoints(@PathParam("pointsNumber") int pointsNumber, @PathParam("deviceNumber") int deviceNumber) throws IOException {
		String res = RandomGeoJsonPointGenerator.generatePathOfPoints(pointsNumber,deviceNumber);

		return Response.status(200).entity(res).build();

	}

}
