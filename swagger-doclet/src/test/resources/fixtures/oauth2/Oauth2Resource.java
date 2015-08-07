package fixtures.oauth2;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * The Oauth2Resource represents a resource for testing swagger authorization
 * @version $Id$
 * @author conor.roche
 */
@Path("/oauth2")
public class Oauth2Resource {

	/**
	 * @scope write:pets
	 */
	@GET
	@Path("/customScope")
	public void customScope() {
		// do nothing
	}

	/**
	 * @scope write:pets
	 * @scope read:pets
	 */
	@GET
	@Path("/customScopes")
	public void customScopes() {
		// do nothing
	}

	@SuppressWarnings("javadoc")
	@GET
	@Path("/defaultNoAuth")
	public void defaultNoAuth() {
		// this method should have no auth applied
	}

	/**
	 * @authentication Required
	 */
	@GET
	@Path("/defaultAuth")
	public void defaultAuth() {
		// this method should have default scope auth applied
	}

	/**
	 * @authentication Not required
	 */
	@GET
	@Path("/noAuth1")
	public void noAuth1() {
		// this does not need authentication
	}

	/**
	 * @noAuth
	 */
	@GET
	@Path("/noAuth2")
	public void noAuth2() {
		// this does not need authentication
	}

}
