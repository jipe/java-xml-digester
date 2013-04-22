package dk.defiant.xml.digester.responses;

import dk.defiant.xml.digester.HandlerResponse;

public class ContinueParsingResponse extends HandlerResponse {

	public ContinueParsingResponse() {
		super(HandlerResponse.Type.CONTINUE);
	}
	
}
