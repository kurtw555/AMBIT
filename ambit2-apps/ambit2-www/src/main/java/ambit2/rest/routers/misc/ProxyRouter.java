package ambit2.rest.routers.misc;

import org.restlet.Context;

import ambit2.rest.ProxyResource;
import ambit2.rest.routers.MyRouter;

public class ProxyRouter extends MyRouter {

	public ProxyRouter(Context context) {
		super(context);
		attachDefault(ProxyResource.class);
		attach(String.format("/{%s}/{%s}", ProxyResource.servicekey, ProxyResource.resourcekey), ProxyResource.class);
	}

}
