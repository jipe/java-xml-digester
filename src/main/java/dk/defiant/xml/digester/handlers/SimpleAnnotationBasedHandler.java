package dk.defiant.xml.digester.handlers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.defiant.xml.digester.DigesterEventHandler;
import dk.defiant.xml.digester.HandlerResponse;
import dk.defiant.xml.digester.annotations.Digest;
import dk.defiant.xml.digester.responses.FinishedParsingResponse;

/**
 * Extension of {@link DigesterEventHandler} that can digest simple XML (with certain restrictions on the XML)
 * into an object based on annotations.
 * 
 * The {@link SimpleAnnotationBasedHandler} can handle XML on a simple form like this:
 * <pre>
 *     &lt;my-complex-element id="123"&gt;
 *         &lt;number&gt;12345&lt;/number&gt;
 *         &lt;my-simple-element&gt;text value 1&lt;/my-simple-element&gt;
 *         &lt;my-simple-element&gt;text value 2&lt;/my-simple-element&gt;
 *         &lt;creation-date&gt;20010911&lt;/creation-date&gt;
 *     &lt;/my-complex-element&gt;
 * </pre>
 * Which can be digested using a {@link SimpleAnnotationBasedHandler} and the class
 * <pre>
 *     class MyComplexElement {
 *         {@literal @Digest("@id")}
 *         private int id;
 *         
 *         {@literal @Digest}
 *         private int number; // The field name matches the element name so annotation doesn't require an argument
 *         
 *         {@literal @Digest("my-simple-element")}
 *         private List&lt;String&gt; mySimpleElement = new ArrayList&lt;String&gt;();
 *         
 *         private Date creationDate;
 *         
 *         // Getters ...
 *         
 *         // Setters
 *         {@literal @Digest}
 *         public void setCreationDate(String dateString) {
 *             // Convert value of dateString into a Date object
 *             // ...
 *         }
 *     }
 * </pre>
 * <b>Limitations:</b>
 * <ul>
 *   <li>Fields implementing Collection must be initialised prior to digesting.</li>
 *   <li>when using {@literal @Digest} annotations on setters, the setter must only take one single String argument.</li>
 *   <li>Input XML must not contain child nodes with the same qname as the root name specified when calling the constructor.</li>
 * </ul>
 * 
 * @author jip
 *
 */
@ThreadSafe
public class SimpleAnnotationBasedHandler extends DigesterEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SimpleAnnotationBasedHandler.class);
	private static final Pattern genericTypePattern = Pattern.compile("<(.+?)>");
	private static final Map<String, Method> valueParsers = new HashMap<String, Method>();
	
	static {
		try {
			valueParsers.put(Boolean.class.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getBoolean", String.class));
			valueParsers.put(Boolean.TYPE.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getBoolean", String.class));
			
			valueParsers.put(Byte.class.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getByte", String.class));
			valueParsers.put(Byte.TYPE.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getByte", String.class));

			valueParsers.put(Character.class.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getChar", String.class));
			valueParsers.put(Character.TYPE.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getChar", String.class));

			valueParsers.put(Short.class.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getShort", String.class));
			valueParsers.put(Short.TYPE.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getShort", String.class));
			
			valueParsers.put(Integer.class.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getInt", String.class));
			valueParsers.put(Integer.TYPE.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getInt", String.class));
			
			valueParsers.put(Long.class.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getLong", String.class));
			valueParsers.put(Long.TYPE.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getLong", String.class));
			
			valueParsers.put(Float.class.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getFloat", String.class));
			valueParsers.put(Float.TYPE.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getFloat", String.class));
			
			valueParsers.put(Double.class.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getDouble", String.class));
			valueParsers.put(Double.TYPE.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getDouble", String.class));
			
			valueParsers.put(Locale.class.getCanonicalName(), SimpleAnnotationBasedHandler.class.getDeclaredMethod("getLocale", String.class));
		} catch (Exception e) {
			log.error("Error setting up value parsers", e);
			throw new RuntimeException ("Error setting up value parsers", e);
		}
	}
	
	private final Map<String, Field> digestedFields = new HashMap<String, Field>();
	private final Map<String, Method> digestedMethods = new HashMap<String, Method>();
	
	// The QName that the bean handler will consider start and end point for the parsing
	private final QName rootName;
	
	// The type of the object that is to be digested to
	private final Class<?> digestType;
	
	public SimpleAnnotationBasedHandler(QName rootName, Class<?> digestType) {
		this.rootName = rootName;
		this.digestType = digestType;
		while (!Object.class.equals(digestType)) {
			for (Method method : digestType.getDeclaredMethods()) {
				if (method.isAnnotationPresent(Digest.class)) {
					if (method.getParameterTypes().length == 1 && String.class.equals(method.getParameterTypes()[0])) {
						Digest digestAnnotation = method.getAnnotation(Digest.class);
						String value = digestAnnotation.value();
						if ("".equals(value)) {
							value = method.getName();
							if (value.startsWith("set")) {
								// Deduce name from setter name: setMyValue -> myValue
								value = value.substring("set".length());
								value = value.substring(0, 1).toLowerCase() + value.substring(1);
							}
						}
						if (!digestedMethods.containsKey(value)) {
							digestedMethods.put(value, method);
						}
					} else {
						log.warn("Annotated method {} must take a single String argument to be used for digesting", method.getName());
					}
				}
			}
			for (Field field : digestType.getDeclaredFields()) {
				if (field.isAnnotationPresent(Digest.class)) {
					Digest digestAnnotation = field.getAnnotation(Digest.class);
					String value = digestAnnotation.value();
					if ("".equals(value)) {
						// Use field name as value
						value = field.getName();
					}
					digestedFields.put(value, field);
				}
			}
			digestType = digestType.getSuperclass();
		}
	}
	
	@Override
	public HandlerResponse handle(XMLEvent event, Object digestTarget) throws XMLStreamException {
		if (event.isStartElement()) {
			StartElement element = event.asStartElement();
			String uri = element.getName().getNamespaceURI();
			if (rootName.getNamespaceURI().equals(uri)) {
				String localName = element.getName().getLocalPart();
				if (rootName.getLocalPart().equals(localName)) {
					// Check for attributes
					@SuppressWarnings("unchecked")
					Iterator<Attribute> attributes = element.getAttributes();
					while (attributes.hasNext()) {
						Attribute attribute = attributes.next();
						String name = "@" + attribute.getName().getLocalPart();
						if (digestedMethods.containsKey(name)) {
							callSetter(digestedMethods.get(name), digestTarget, attribute.getValue());
						} else if (digestedFields.containsKey(name)) {
							setFieldValue(digestedFields.get(name), digestTarget, attribute.getValue());
						}
					}
				}
				if (digestedFields.containsKey(localName) || digestedMethods.containsKey(localName)) {
					resetCharacterBuffer();
				}
			}
		} else if (event.isEndElement()) {
			EndElement element = event.asEndElement();
			if (rootName.equals(element.getName())) {
				return new FinishedParsingResponse();
			} else {
				String uri = element.getName().getNamespaceURI();
				if (rootName.getNamespaceURI().equals(uri)) {
					String localName = element.getName().getLocalPart();
					if (digestedMethods.containsKey(localName)) {
						callSetter(digestedMethods.get(localName), digestTarget, getCharacterBuffer());
					} else if (digestedFields.containsKey(localName)) {
						setFieldValue(digestedFields.get(localName), digestTarget, getCharacterBuffer());
					}
				} 
			}
		}
		return super.handle(event, digestTarget);
	}

	private void callSetter(Method method, Object o, String value) {
		if (method.getParameterTypes().length == 1) {
			try {
				method.invoke(o, value);
			} catch (Exception e) {
				log.error("Error calling setter", e);
			}
		} else {
			log.warn("Calling setter {} with wrong number of arguments", method.getName());
		}
	}


	private void setFieldValue(Field field, Object o, String value) {
		Class<?> type = field.getType();
		boolean accessible = field.isAccessible();
		field.setAccessible(true);
		if (String.class.equals(type)) {
			try {
				field.set(o, value);
			} catch (Exception e) {
				log.error("Error setting field value", e);
			}
		} else if (Collection.class.isAssignableFrom(type)) {
			Matcher m = genericTypePattern.matcher(field.toGenericString());
			if (m.find()) {
				String typeName = m.group(1);
				try {
					@SuppressWarnings("unchecked")
					Collection<Object> collection = (Collection<Object>) field.get(o);
					if (String.class.getCanonicalName().equals(typeName)) {
						collection.add(value);
					} else {
						collection.add(valueParsers.get(typeName).invoke(null, value));
					}
				} catch (Exception e) {
					log.error("Error adding value to collection", e);
				}
			}
		} else if (valueParsers.containsKey(type.getCanonicalName())) {
			try {
				Object parsedValue = valueParsers.get(type.getCanonicalName()).invoke(null, value);
				field.set(o, parsedValue);
			} catch (Exception e) {
				log.error("Error parsing and setting fieldValue", e);
			}
		} 
		field.setAccessible(accessible);
	}
	
	public Class<?> getDigestType() {
		return digestType;
	}
	
	// XXX: This method is used through reflection
	@SuppressWarnings("unused")
	private static boolean getBoolean(String value) {
		return Boolean.parseBoolean(value);
	}

	// XXX: This method is used through reflection
	@SuppressWarnings("unused")
	private static byte getByte(String value) {
		return Byte.parseByte(value);
	}

	// XXX: This method is used through reflection
	@SuppressWarnings("unused")
	private static char getChar(String value) {
		return value.charAt(0);
	}

	// XXX: This method is used through reflection
	@SuppressWarnings("unused")
	private static short getShort(String value) {
		return Short.parseShort(value);
	}

	// XXX: This method is used through reflection
	@SuppressWarnings("unused")
	private static int getInt(String value) {
		return Integer.parseInt(value);
	}

	// XXX: This method is used through reflection
	@SuppressWarnings("unused")
	private static long getLong(String value) {
		return Long.parseLong(value);
	}

	// XXX: This method is used through reflection
	@SuppressWarnings("unused")
	private static float getFloat(String value) {
		return Float.parseFloat(value);
	}
	
	// XXX: This method is used through reflection
	@SuppressWarnings("unused")
	private static double getDouble(String value) {
		return Double.parseDouble(value);
	}

	// XXX: This method is used through reflection
	@SuppressWarnings("unused")
	private static Locale getLocale(String value) {
		Locale locale = value.length() > 2 ? new Locale(value.substring(0, 2)) : new Locale(value);
		log.debug("Converted language string {} to Locale {}", value, locale);
		return locale;
	}

}
