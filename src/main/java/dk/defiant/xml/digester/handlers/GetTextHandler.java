package dk.defiant.xml.digester.handlers;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import dk.defiant.xml.digester.DigesterEventHandler;
import dk.defiant.xml.digester.HandlerResponse;
import dk.defiant.xml.digester.TextHandler;
import dk.defiant.xml.digester.responses.FinishedParsingResponse;

public class GetTextHandler<T> extends DigesterEventHandler {

	TextHandler<T> textHandler;
	
	public GetTextHandler(TextHandler<T> textHandler) {
		this.textHandler = textHandler;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public HandlerResponse handle(XMLEvent event, Object digestTarget) throws XMLStreamException {
		if (XMLEvent.START_ELEMENT == event.getEventType()) {
			resetCharacterBuffer();
		} else if (XMLEvent.END_ELEMENT == event.getEventType()) {
			textHandler.handle((T) digestTarget, getCharacterBuffer());
			return new FinishedParsingResponse();
		}
		return super.handle(event, digestTarget);
	}
	
}
