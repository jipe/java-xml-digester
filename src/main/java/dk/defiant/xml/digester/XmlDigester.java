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

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.defiant.xml.digester.responses.BadHandlerResponse;

/**
 * Class for digesting XML into Java objects
 * 
 */
@ThreadSafe
public class XmlDigester {

	private static final Logger log = LoggerFactory.getLogger(XmlDigester.class);
	
	public class Context {
	    Stack<DigesterEventHandler> eventHandlers = new Stack<DigesterEventHandler>();
	    Stack<Object> digestTargets = new Stack<Object>();
	    int depth, ignoredElementDepth;
	    boolean ignoring;
	    XMLEventReader eventReader;
	    XMLEvent event;
	}
	
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
		Context context = new Context();
        eventHandler.setXmlDigester(this);
        eventHandler.setXmlDigesterContext(context);
		context.depth = 0;
		context.ignoredElementDepth = 0;
		context.ignoring = false;
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        context.eventReader = xmlInputFactory.createXMLEventReader(reader);
		context.eventHandlers.push(eventHandler);
		context.digestTargets.push(digestTarget);
		while (context.eventReader.hasNext() && context.eventHandlers.size() > 0) {
			context.event = context.eventReader.nextEvent();
			if (context.event.isStartElement()) {
			    context.depth++;
			} else if (context.event.isEndElement()) {
			    context.depth--;
			}
			if (context.ignoring && context.depth < context.ignoredElementDepth) {
				// We've escaped the ignored element
			    context.ignoring = false;
			}
			if (context.ignoring) {
				// Ignore event
				if (log.isDebugEnabled()) {
					if (context.event.isStartElement()) {
						log.debug("Ignoring element '{}'", context.event.asStartElement().getName().getLocalPart());
					}
				}
			} else {
				HandlerResponse response = context.eventHandlers.peek().handle(context.event, context.digestTargets.peek());
				if (HandlerResponse.Type.DELEGATE.equals(response.getType())) {
					// Handler wants to delegate parsing to another handler
					Object newDigestTarget = response.getDigestTarget() == null ? context.digestTargets.peek() : response.getDigestTarget();
					context.digestTargets.push(newDigestTarget);
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
					handler.setXmlDigesterContext(context);
					context.eventHandlers.push(handler);
					context.eventHandlers.peek().handle(context.event, context.digestTargets.peek());
				} else if (HandlerResponse.Type.FINISHED_PARSING.equals(response.getType())) {
					// Handler finished its parsing
				    context.eventHandlers.pop();
					if (context.eventHandlers.size() > 0) {
					    context.eventHandlers.peek().handle(context.event, context.digestTargets.peek());
					}
					context.digestTargets.pop();
				} else if (HandlerResponse.Type.ERROR.equals(response.getType())) {
					// Handler returned an error
					throw new RuntimeException("Handler returned " + BadHandlerResponse.class.getSimpleName());
				} else if (HandlerResponse.Type.IGNORE_ELEMENT.equals(response.getType())) {
					// Handler wants to ignore an element
				    context.ignoredElementDepth = context.depth;
				    context.ignoring = true;
				}
			}
		}
	}
	
	String getText(Context context) throws XMLStreamException {
		return context.eventReader.getElementText();
	}
	
	String getXmlFragment(Context context, boolean includeFragmentRoot) throws XMLStreamException {
		StringWriter writer = new StringWriter();
		int depth = 1; // Assuming we are viewing the start element event for the fragment
		while (context.eventReader.hasNext() && depth > 0) {
			if (depth > 1 || includeFragmentRoot) {
			    context.event.writeAsEncodedUnicode(writer);
			} 
			context.event = context.eventReader.nextEvent();
			if (XMLEvent.START_ELEMENT == context.event.getEventType()) {
				depth++;
			} else if (XMLEvent.END_ELEMENT == context.event.getEventType()) {
				depth--;
				if (depth == 0) {
				    context.event.writeAsEncodedUnicode(writer);
				}
			}
		}
		return writer.toString();
	}
	
	Map<QName, String> getAttributes(Context context) {
		Map<QName, String> result = new HashMap<QName, String>();
		if (context.event != null && XMLEvent.START_ELEMENT == context.event.getEventType()) {
			StartElement element = context.event.asStartElement();
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
