package dk.defiant.xml.digester.handlers;

import java.util.Map;

import javax.xml.namespace.QName;

public interface SimpleContentHandler {

	void handle(Object target, Map<QName, String> attributes, String text);
	
}
