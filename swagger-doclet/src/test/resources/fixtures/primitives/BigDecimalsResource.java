package fixtures.primitives;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;

@Path("/primitives/bigdecimals")
@SuppressWarnings("javadoc")
public class BigDecimalsResource {

	@GET
	public BigDecimal get() {
		return new BigDecimal("1.2");
	}

	@POST
	public Response create(double value) {
		return Response.ok().build();
	}
}
