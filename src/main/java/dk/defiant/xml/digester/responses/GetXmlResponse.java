package dk.defiant.xml.digester.responses;

import dk.defiant.xml.digester.HandlerResponse;
import dk.defiant.xml.digester.XmlHandler;
import dk.defiant.xml.digester.handlers.GetXmlHandler;

public class GetXmlResponse extends HandlerResponse {

	public <T> GetXmlResponse(XmlHandler<T> handler) {
		super(Type.DELEGATE, new GetXmlHandler<T>(handler));
	}
	
}
