package dk.defiant.xml.digester.handlers;

import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import net.jcip.annotations.NotThreadSafe;
import dk.defiant.xml.digester.DigesterEventHandler;
import dk.defiant.xml.digester.HandlerResponse;
import dk.defiant.xml.digester.XmlHandler;
import dk.defiant.xml.digester.responses.FinishedParsingResponse;

@NotThreadSafe
public class GetXmlHandler<T> extends DigesterEventHandler {

	private StringWriter writer;
	private int depth = 0;
	private XmlHandler<T> handler;
	
	// Used to capture entire document instead of a fragment
	private boolean seenStartDocument = false;
	
	public GetXmlHandler(XmlHandler<T> handler) {
		this.handler = handler;
	}
	
	/** 
	 * Capture an XML document or fragment in a string.
	 * 
	 * if the handler sees a START_DOCUMENT event it will capture entire document until it
	 * reaches an END_DOCUMENT event. Otherwise it will capture a blob
	 */
	@SuppressWarnings("unchecked")
	@Override
	public HandlerResponse handle(XMLEvent event, Object digestTarget) throws XMLStreamException {
		if (writer == null) {
			writer = new StringWriter();
		}
		try {
			// TODO: Find a way to handle blobs that use namespaces defined before the blob
			event.writeAsEncodedUnicode(writer);
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (XMLEvent.START_ELEMENT == event.getEventType()) {
			depth++;
		} else if (XMLEvent.END_ELEMENT == event.getEventType()) {
			depth--;
			if (depth == 0 && !seenStartDocument) {
				// Finished parsing the XML blob so send it off to the handler
				// Should be OK to cast since user explicitly requested a GetXmlHandler for type T
				handler.handle((T) digestTarget, writer.toString());
				writer = null;
				return new FinishedParsingResponse();
			}
		} else if (XMLEvent.START_DOCUMENT == event.getEventType()) {
			seenStartDocument = true;
		} else if (XMLEvent.END_DOCUMENT == event.getEventType()) {
			handler.handle((T) digestTarget, writer.toString());
			writer = null;
			return new FinishedParsingResponse();
		}
		
		return super.handle(event, digestTarget);
	}
}
