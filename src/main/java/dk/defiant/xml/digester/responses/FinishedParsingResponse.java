package dk.defiant.xml.digester.responses;

import dk.defiant.xml.digester.HandlerResponse;

public class FinishedParsingResponse extends HandlerResponse {

	public FinishedParsingResponse() {
		super(HandlerResponse.Type.FINISHED_PARSING);
	}
}
