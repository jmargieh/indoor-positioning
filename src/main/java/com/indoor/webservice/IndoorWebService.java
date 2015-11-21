package main.java.com.indoor.webservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Path("/")
@Component("IndoorWebService")
public class IndoorWebService {

	@GET
	@Path("/points")
	public Response isPointInObstacle() throws IOException {

		InputStream res = IndoorWebService.class.getResourceAsStream("/json/points.json");

		BufferedReader reader = new BufferedReader(new InputStreamReader(res));
		String line = null, output="";
		while ((line = reader.readLine()) != null) {
			output += line;
		}
		reader.close();

		return Response.status(200).entity(output).build();

		
	}

}
