package dk.defiant.xml.digester.handlers;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import dk.defiant.xml.digester.DigesterEventHandler;
import dk.defiant.xml.digester.HandlerResponse;
import dk.defiant.xml.digester.responses.ContinueParsingResponse;

/**
 * Convenience class for extensions of DigesterEventHandler's that
 * don't require complex operation. This class will do a lot of the
 * boiler plate code for handling events including handling character
 * events. Extensions of this class should only worry about start
 * and end element events and when to reset or retrieve the character 
 * buffer.
 * 
 * @author jip
 *
 */
public abstract class SimpleDigesterEventHandler extends DigesterEventHandler {

	private final String uri;
	
	public SimpleDigesterEventHandler(String uri) {
		this.uri = uri;
	}
	
	@Override
	public final HandlerResponse handle(XMLEvent event, Object digestTarget) throws XMLStreamException {
		if (XMLEvent.START_ELEMENT == event.getEventType()) {
			StartElement element = event.asStartElement();
			if (uri.equals(element.getName().getNamespaceURI())) {
				return handle(element, digestTarget);
			}
		} else if (XMLEvent.END_ELEMENT == event.getEventType()) {
			EndElement element = event.asEndElement();
			if (uri.equals(element.getName().getNamespaceURI())) {
				return handle(element, digestTarget);
			}
		}
		return super.handle(event, digestTarget);
	}
	
	/**
	 * Handle start element event
	 * 
	 * @param element
	 * @param digestTarget
	 * @return
	 */
	public HandlerResponse handle(StartElement element, Object digestTarget) throws XMLStreamException {
		return new ContinueParsingResponse();
	}
	
	/**
	 * Handle end element event
	 * 
	 * @param element
	 * @param digestTarget
	 * @return
	 */
	public HandlerResponse handle(EndElement element, Object digestTarget) throws XMLStreamException {
		return new ContinueParsingResponse();
	}
}
