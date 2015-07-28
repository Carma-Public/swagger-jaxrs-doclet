package fixtures.resourceinheritance;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@SuppressWarnings("javadoc")
public class ConcreteResource extends AbstractResource {

	@GET
	@Path("bar")
	public String bar() {
		return "bar";
	}

	@Override
	protected String getResourceById(String id) {
		return "Concrete Resource with id " + id;
	}
}
