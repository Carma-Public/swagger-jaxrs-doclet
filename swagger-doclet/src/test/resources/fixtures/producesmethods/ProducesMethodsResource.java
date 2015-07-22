package fixtures.producesmethods;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Resource with multiple methods on the same path but differing produces 
 */
@Path("/producesmethods")
@SuppressWarnings("javadoc")
public class ProducesMethodsResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getDataJson() {
		return "json";
	}

	@GET
	@Produces(MediaType.APPLICATION_XML)
	public String getDataXml() {
		return "xml";
	}
}
