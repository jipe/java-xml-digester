package dk.defiant.xml.digester.responses;

import dk.defiant.xml.digester.HandlerResponse;
import dk.defiant.xml.digester.TextHandler;
import dk.defiant.xml.digester.handlers.GetTextHandler;

public class GetTextResponse extends HandlerResponse {

	public <T> GetTextResponse(TextHandler<T> textHandler) {
		super(Type.DELEGATE, new GetTextHandler<T>(textHandler));
	}
	
}
