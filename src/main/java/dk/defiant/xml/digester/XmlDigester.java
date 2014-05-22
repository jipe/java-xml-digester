package dk.defiant.xml.digester;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import net.jcip.annotations.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.defiant.xml.digester.responses.BadHandlerResponse;

/**
 * Class for digesting XML into Java objects
 * 
 */
@NotThreadSafe
public class XmlDigester {

	private static final Logger log = LoggerFactory.getLogger(XmlDigester.class);
	
	private Stack<DigesterEventHandler> eventHandlers = new Stack<DigesterEventHandler>();
	private Stack<Object> digestTargets = new Stack<Object>();
	private int depth, ignoredElementDepth;
	private boolean ignoring;

	private XMLEventReader eventReader;
	private XMLEvent event;
		
	/**
	 * Digest XML from a {@link String} into Java objects.
	 * 
	 * @param xml The string containing XML
	 * @param digestTarget The object to digest XML into
	 * @param eventHandler The event handler that will receive StAX XML events
	 * @throws XMLStreamException
	 */
	public final void digest(String xml, Object digestTarget, DigesterEventHandler eventHandler) throws XMLStreamException {
		digest(new StringReader(xml), digestTarget, eventHandler);
	}
	
	/**
	 * Digest XML obtained from an {@link InputStream} into Java objects.
	 * 
	 * @param input The input stream to read from
	 * @param charSetName The character set name to use for reading from input stream
	 * @param digestTarget The object to digest XML into
	 * @param eventHandler The event handler that will receive StAX XML events
	 * @throws UnsupportedEncodingException
	 * @throws XMLStreamException
	 */
	public final void digest(InputStream input, String charSetName, Object digestTarget, DigesterEventHandler eventHandler) throws UnsupportedEncodingException, XMLStreamException {
		InputStreamReader isr = new InputStreamReader(input, charSetName);
		digest(new BufferedReader(isr), digestTarget, eventHandler);
	}
	
	/**
	 * Digest XML obtained from a {@link Reader} into Java objects.
	 * 
	 * @param reader The reader to read from
	 * @param digestTarget The object to digest XML into
	 * @param eventHandler The event handler that will receive StAX XML events
	 * 
	 * @throws XMLStreamException
	 */
	public final void digest(Reader reader, Object digestTarget, DigesterEventHandler eventHandler) throws XMLStreamException {
		eventHandler.setXmlDigester(this);
		depth = 0;
		ignoredElementDepth = 0;
		ignoring = false;
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        eventReader = xmlInputFactory.createXMLEventReader(reader);
		eventHandlers.push(eventHandler);
		digestTargets.push(digestTarget);
		while (eventReader.hasNext() && eventHandlers.size() > 0) {
			event = eventReader.nextEvent();
			if (XMLEvent.START_ELEMENT == event.getEventType()) {
				depth++;
			} else if (XMLEvent.END_ELEMENT == event.getEventType()) {
				depth--;
			}
			if (ignoring && depth < ignoredElementDepth) {
				// We've escaped the ignored element
				ignoring = false;
			}
			if (ignoring) {
				// Ignore event
				if (log.isDebugEnabled()) {
					if (XMLEvent.START_ELEMENT == event.getEventType()) {
						log.debug("Ignoring element '{}'", event.asStartElement().getName().getLocalPart());
					}
				}
			} else {
				// Handler wants to delegate parsing to another handler
				HandlerResponse response = eventHandlers.peek().handle(event, digestTargets.peek());
				if (HandlerResponse.Type.DELEGATE.equals(response.getType())) {
					Object newDigestTarget = response.getDigestTarget() == null ? digestTargets.peek() : response.getDigestTarget();
					digestTargets.push(newDigestTarget);
					DigesterEventHandler handler = response.getHandler();
					if (handler == null) {
						// Only class of handler was given so we make a new instance of it
						Class<? extends DigesterEventHandler> handlerClass = response.getHandlerClass();
						if (handlerClass != null) {
							try {
								handler = handlerClass.newInstance();
							} catch (Exception e) {
								log.error("Error instantiating new digester event handler", e);
								throw new RuntimeException("Error instantiating new digester event handler", e);
							}
						}
					}
					handler.setXmlDigester(this);
					eventHandlers.push(handler);
					eventHandlers.peek().handle(event, digestTargets.peek());
				} else if (HandlerResponse.Type.FINISHED_PARSING.equals(response.getType())) {
					// Handler finished its parsing
					eventHandlers.pop();
					if (eventHandlers.size() > 0) {
						eventHandlers.peek().handle(event, digestTargets.peek());
					}
					digestTargets.pop();
				} else if (HandlerResponse.Type.ERROR.equals(response.getType())) {
					// Handler returned an error
					throw new RuntimeException("Handler returned " + BadHandlerResponse.class.getSimpleName());
				} else if (HandlerResponse.Type.IGNORE_ELEMENT.equals(response.getType())) {
					// Handler wants to ignore an element
					ignoredElementDepth = depth;
					ignoring = true;
				}
			}
		}
	}
	
	String getText() throws XMLStreamException {
		return eventReader.getElementText();
	}
	
	String getXmlFragment(boolean includeFragmentRoot) throws XMLStreamException {
		StringWriter writer = new StringWriter();
		int depth = 1; // Assuming we are viewing the start element event for the fragment
		while (eventReader.hasNext() && depth > 0) {
			if (depth > 1 || includeFragmentRoot) {
				event.writeAsEncodedUnicode(writer);
			} 
			event = eventReader.nextEvent();
			if (XMLEvent.START_ELEMENT == event.getEventType()) {
				depth++;
			} else if (XMLEvent.END_ELEMENT == event.getEventType()) {
				depth--;
				if (depth == 0) {
					event.writeAsEncodedUnicode(writer);
				}
			}
		}
		return writer.toString();
	}
	
	Map<QName, String> getAttributes() {
		Map<QName, String> result = new HashMap<QName, String>();
		if (event != null && XMLEvent.START_ELEMENT == event.getEventType()) {
			StartElement element = event.asStartElement();
			@SuppressWarnings("unchecked")
			Iterator<Attribute> attributes = element.getAttributes();
			while (attributes.hasNext()) {
				Attribute attribute = attributes.next();
				result.put(attribute.getName(), attribute.getValue());
			}
		}
		return result;
	}
}
