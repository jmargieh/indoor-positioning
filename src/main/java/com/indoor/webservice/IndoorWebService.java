package main.java.com.indoor.webservice;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import main.java.com.indoor.dao.IndoorNavigationProcessing;
import main.java.com.indoor.helpers.RandomGeoJsonPointGenerator;

import org.springframework.stereotype.Component;

@Path("/")
@Component("IndoorWebService")
public class IndoorWebService {

	@GET
	@Path("/points")
	public Response processGeoJsons() throws IOException {

		IndoorNavigationProcessing indoorNavPro = new IndoorNavigationProcessing();
		return Response.status(200).entity("   -----If reached here we succeed!").build();

	}
	
	@GET
	@Path("/generatePoints/{pointsNumber}/{deviceNumber}")
	public Response generateRandomGeoJSONPoints(@PathParam("pointsNumber") int pointsNumber, @PathParam("deviceNumber") int deviceNumber) throws IOException {
		String res = RandomGeoJsonPointGenerator.generatePathOfPoints(pointsNumber,deviceNumber);

		return Response.status(200).entity(res).build();

	}

}
