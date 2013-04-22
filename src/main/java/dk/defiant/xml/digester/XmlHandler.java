package dk.defiant.xml.digester;

public interface XmlHandler<T> {

	void handle(T target, String xml);
	
}
