package fixtures.issue44;

import fixtures.issue17.*;
import fixtures.issue17.User2;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
@SuppressWarnings("javadoc")
public class Resource {

	@GET
    @Path("/message")
	public String getMessage() {
		return "success";
	}

}
