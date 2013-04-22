package dk.defiant.xml.digester;

import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;

import dk.defiant.xml.digester.responses.ContinueParsingResponse;

public class DigesterStreamHandler {

	public HandlerResponse handleStartElement(int depth, QName name, Iterator<Attribute> attributes) {
		return new ContinueParsingResponse(); 
	}
	
	public HandlerResponse handleEndElement(int depth, QName name) {
		return new ContinueParsingResponse();
	}

	protected void resetCharacterBuffer() {
		
	}
	
	protected String getCharacterBuffer() {
		return null;
	}
}
