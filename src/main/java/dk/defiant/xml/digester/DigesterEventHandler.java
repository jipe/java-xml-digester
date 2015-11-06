package dk.defiant.xml.digester;

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import net.jcip.annotations.NotThreadSafe;
import dk.defiant.xml.digester.responses.ContinueParsingResponse;
import dk.defiant.xml.digester.responses.DelegateParsingResponse;
import dk.defiant.xml.digester.responses.FinishedParsingResponse;

/**
 * Base class for handling StAX XML events from {@link XmlDigester}
 * 
 * @author jip
 *
 */
@NotThreadSafe
public abstract class DigesterEventHandler {

    private XmlDigester digester;
	private XmlDigester.Context digesterState;
	private StringBuilder characterBuffer = new StringBuilder();
	
	/**
	 * Get String of characters parsed since last reset of the character buffer
	 * 
	 * @return
	 */
	public final String getCharacterBuffer() {
		return characterBuffer.toString();
	}
	
	/**
	 * Reset the character buffer.
	 * 
	 * This is typically done from a handler when encountering an element 
	 * that has text content which is to be digested.
	 */
	public final void resetCharacterBuffer() {
		characterBuffer.setLength(0);
	}
	
	/**
	 * <h1>Handle an XMLEvent.</h1> 
	 * <p>
	 * A handler can tell the digester that it should delegate the event
	 * to another handler by returning a {@link DelegateParsingResponse} with
	 * appropriate arguments.
	 * </p>
	 * <p>
	 * A handler can tell the digester that it is finished parsing the
	 * XML it was interested in by returning a {@link FinishedParsingResponse}.
	 * The digester will then send further events to the handler that delegated
	 * handling to this handler and re-establish the digest target that was
	 * active prior to the delegation.
	 * <p>
	 * A handler can tell the digester that it should continue sending events
	 * by returning a {@link ContinueParsingResponse}.
	 * </p>
	 * <p>
	 * The default handler {@link DigesterEventHandler#handle(XMLEvent, Object)} captures character
	 * data in a buffer that can be reset and accessed so any descending class
	 * need only worry about resetting the character buffer on the right start
	 * elements and retrieving the character buffer on the right end elements.
	 * </p>
	 * <p>
	 * If character capturing is not desired call {@link DigesterEventHandler#handle(XMLEvent, Object, boolean)}
	 * passing {@literal false} as the third argument.
	 * </p>
	 * <h2>Example usage</h2>
	 * <pre>
	 * class MyHandler extends DigesterEventHandler {
	 * 
	 *     private MyRecord myRecord;
	 *     
	 *     {@literal @Override}
	 *     public handle(XMLEvent event, Object digestTarget) {
	 *         if (XMLEvent.START_ELEMENT == event.getEventType()) {
	 *             // Handle start element and maybe delegate to other handler
	 *             StartElement element = event.asStartElement();
	 *             if ("my-uri".equals(element.getName().getNamespaceUri()) {
	 *                 String localName = element.getName().getLocalPart();
	 *                 if ("my-record".equals(localName)) {
	 *                     myRecord = (MyRecord) digestTarget;
	 *                 } else if ("my-element".equals(localName)) {
	 *                     // This is an element we want to digest
	 *                     resetCharacterBuffer();
	 *                 } else if ("my-delegated-element".equals(localName)) {
	 *                     // We don't know how to handle this element, but we do
	 *                     // know someone who does - and it expects a MyDelegateElement as digestTarget
	 *                     return new DelegateParsingResponse(new MySecondHandler(), new MyDelegateElement());
	 *                 }
	 *             }
	 *         } else if (XMLEvent.END_ELEMENT == event.getEventType()) {
	 *             // Handle end element and maybe signal finished parsing
	 *             EndElement element = event.asEndElement();
	 *             if ("my-uri".equals(element.getName().getNamespaceUri()) {
	 *                 String localName = event.getName().getLocalPart();
	 *                 if ("my-record".equals(localName) {
	 *                     // We're finished parsing the element we know how to handle
	 *                     return new FinishedParsingResponse();
	 *                 } else if ("my-element".equals(localName)) {
	 *                     // The character buffer now holds the text we're interested in.
	 *                     myRecord.setText(getCharacterBuffer());
	 *                 } else if ("my-delegated-element".equals(localName)) {
	 *                     // We also get the event that triggered a FinishedParsingResponse in the delegate handler
	 *                     // and the digestTarget is the one that was available to the delegate handler
	 *                     myRecord.setMyDelegatedElement((MyDelegatedElement) digestTarget);
	 *                 }
	 *             }
	 *         }
	 *         // Let default handler handle characters (and other events which it just plows through)
	 *         return super.handle(event, digestTarget);
	 *     }
	 * }
	 * </pre>
	 * 
	 * @param event The XMLEvent to be handled.
	 * @param digestTarget The object which is to be digested to.
	 * @param captureCharacters Set to true to capture characters
	 * @return A handler response giving the digester information about where
	 *         to send further events
	 */
	public HandlerResponse handle(XMLEvent event, Object digestTarget, boolean captureCharacters) throws XMLStreamException {
		if (captureCharacters && XMLEvent.CHARACTERS == event.getEventType()) {
			Characters characters = event.asCharacters();
			characterBuffer.append(characters.getData());
		}
		return new ContinueParsingResponse();
	}
	
	/**
	 * <h1>Handle an XMLEvent.</h1> 
	 * <p>
	 * A handler can tell the digester that it should delegate the event
	 * to another handler by returning a {@link DelegateParsingResponse} with
	 * appropriate arguments.
	 * </p>
	 * <p>
	 * A handler can tell the digester that it is finished parsing the
	 * XML it was interested in by returning a {@link FinishedParsingResponse}.
	 * The digester will the send further events to the handler that delegated
	 * handling to this handler and re-establish the digest target that was
	 * active prior to the delegation.
	 * <p>
	 * A handlers can tell the digester that it should continue sending events
	 * by returning a {@link ContinueParsingResponse}.
	 * </p>
	 * <p>
	 * The default handler {@link DigesterEventHandler#handle(XMLEvent, Object)} captures character
	 * data in a buffer that can be reset and accessed so any descending class
	 * need only worry about resetting the character buffer on the right start
	 * elements and retrieving the character buffer on the right end elements.
	 * </p>
	 * <h2>Example usage</h2>
	 * <pre>
	 * class MyHandler extends DigesterEventHandler {
	 * 
	 *     private MyRecord myRecord;
	 *     
	 *     {@literal @Override}
	 *     public handle(XMLEvent event, Object digestTarget) {
	 *         if (XMLEvent.START_ELEMENT == event.getEventType()) {
	 *             // Handle start element and maybe delegate to other handler
	 *             StartElement element = event.asStartElement();
	 *             if ("my-uri".equals(element.getName().getNamespaceUri()) {
	 *                 String localName = element.getName().getLocalPart();
	 *                 if ("my-record".equals(localName)) {
	 *                     myRecord = (MyRecord) digestTarget;
	 *                 } else if ("my-element".equals(localName)) {
	 *                     // This is an element we want to digest
	 *                     resetCharacterBuffer();
	 *                 } else if ("my-delegated-element".equals(localName)) {
	 *                     // We don't know how to handle this element, but we do
	 *                     // know someone who does - and it expects a MyDelegateElement as digestTarget
	 *                     return new DelegateParsingResponse(new MySecondHandler(), new MyDelegateElement());
	 *                 }
	 *             }
	 *         } else if (XMLEvent.END_ELEMENT == event.getEventType()) {
	 *             // Handle end element and maybe signal finished parsing
	 *             EndElement element = event.asEndElement();
	 *             if ("my-uri".equals(element.getName().getNamespaceUri()) {
	 *                 String localName = event.getName().getLocalPart();
	 *                 if ("my-record".equals(localName) {
	 *                     // We're finished parsing the element we know how to handle
	 *                     return new FinishedParsingResponse();
	 *                 } else if ("my-element".equals(localName)) {
	 *                     // The character buffer now holds the text we're interested in.
	 *                     myRecord.setText(getCharacterBuffer());
	 *                 } else if ("my-delegated-element".equals(localName)) {
	 *                     // We also get the event that triggered a FinishedParsingResponse in the delegate handler
	 *                     // and the digestTarget is the one that was available to the delegate handler
	 *                     myRecord.setMyDelegatedElement((MyDelegatedElement) digestTarget);
	 *                 }
	 *             }
	 *         }
	 *         // Let default handler handle characters (and other events which it just plows through)
	 *         return super.handle(event, digestTarget);
	 *     }
	 * }
	 * </pre>
	 * 
	 * @param event The XMLEvent to be handled.
	 * @param digestTarget The object which is to be digested to.
	 * @return A handler response giving the digester information about where
	 *         to send further events
	 */
	public HandlerResponse handle(XMLEvent event, Object digestTarget) throws XMLStreamException {
		return handle(event, digestTarget, true);
	}
	
	protected String getText() throws XMLStreamException {
		return digester.getText(digesterState);
	}
	
	protected String getXmlFragment(boolean includeFragmentRoot) throws XMLStreamException {
		return digester.getXmlFragment(digesterState, includeFragmentRoot);
	}
	
	void setXmlDigester(XmlDigester digester) {
		this.digester = digester;
	}
	
	void setXmlDigesterContext(XmlDigester.Context state) {
	    this.digesterState = state;
	}
	
	public Map<QName, String> getAttributes() {
		return digester.getAttributes(digesterState);
	}
}
