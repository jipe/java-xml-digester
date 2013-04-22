package dk.defiant.xml.digester;

public abstract class HandlerResponse {

	public enum Type {
		CONTINUE,
		FINISHED_PARSING,
		DELEGATE,
		ERROR, 
		IGNORE_ELEMENT,
	}
	
	private Type type;
	private DigesterEventHandler handler;
	private Class<? extends DigesterEventHandler> handlerClass;
	private Object digestTarget;
	
	public HandlerResponse(Type type) {
		this(type, null, null, null);
	}
	
	public HandlerResponse(Type type, DigesterEventHandler handler) {
		this(type, handler, null, null);
	}
	
	public HandlerResponse(Type type, DigesterEventHandler handler, 
			Object digestTarget) {
		this(type, handler, digestTarget, null);
	}
	
	public HandlerResponse(Type type, DigesterEventHandler handler, Object digestTarget, 
			Class<? extends DigesterEventHandler> handlerClass) {
		this.type = type;
		this.handler = handler;
		this.digestTarget = digestTarget;
		this.handlerClass = handlerClass;
	}
	
	public Type getType() {
		return type;
	}
	
	public DigesterEventHandler getHandler() {
		return handler;
	}
	
	public Object getDigestTarget() {
		return digestTarget;
	}
	
	public Class<? extends DigesterEventHandler> getHandlerClass() {
		return handlerClass;
	}
}
