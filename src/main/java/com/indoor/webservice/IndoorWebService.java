package main.java.com.indoor.webservice;

import java.io.IOException;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import main.java.com.indoor.dao.IndoorNavigationProcessing;
import org.springframework.stereotype.Component;

@Path("/")
@Component("IndoorWebService")
public class IndoorWebService {

	@GET
	@Path("/points")
	public Response isPointInObstacle() throws IOException {

		IndoorNavigationProcessing indoorNavPro = new IndoorNavigationProcessing();
		return Response.status(200).entity("   -----If reached here we succeed!").build();

	}

}
