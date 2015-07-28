package fixtures.resourceinheritance;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * The AbstractResource represents a base resource class
 */
@SuppressWarnings("javadoc")
@Path("/foo")
public abstract class AbstractResource {

	@GET
	@Path("{id}")
	public String getById(@PathParam("id") String id) {
		return getResourceById(id);
	}

	protected abstract String getResourceById(String id);
}
