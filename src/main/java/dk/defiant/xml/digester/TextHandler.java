package dk.defiant.xml.digester;

public interface TextHandler<T> {

	void handle(T target, String text);
	
}
