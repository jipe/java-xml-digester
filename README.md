# Java XML Digester

The Java XML Digester library is a tool for building Java objects from XML
documents without having to have an XML Schema. It's designed to keep
your XML parsers short and comprehensive.

## Requirements

You need at least a JDK7 and a Maven 2 for building the project.

## How to build it

Doing a 

```
mvn install
```

will compile, test, package and install the jar to your local Maven repository.

## How to use it

The general idea is that you write a number of DigesterEventHandler implementations
that will work together in building the resulting Java object by each focusing on
different parts of the XML and building the corresponding parts of the Java object.
This is in order for the parser classes to be short and comprehensive.
The handlers are responsible for setting up context and delegating parsing to other
relevant handlers and signaling to the digester when they have done their part with the XML.

When delegating to another handler or returning from another handler, the XMLEvent that
triggered the delegation/return is sent to both handlers. This enables a handler to 
see an XMLEvent that should trigger a return and for the handler that is returned to to
capture the digest target from the handler it delegated to.

### Using a single handler to parse simple XML

For simple XML you can get away with writing a single DigesterEventHandler without
it getting too complicated:

```java
String xml = "<person><name>John Doe</name><date-of-birth>2001-01-01</date-of-birth></person>";
Person person = new Person();
XmlDigester digester = new XmlDigester();
PersonHandler handler = new PersonHandler();
digester.digest(xml, person, handler);
```

For this example we would probably have:

```java
public class Person {
  private String name;
  private Date dateOfBirth;
  
  public Person () {
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Date getDateOfBirth() {
    return dateOfBirth;
  }
  
  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }
}
```

and then a fairly simple handler for populating the person object with data from the XML document:

```java
public PersonHandler extends SimpleDigesterEventHandler {
  private Person person;
  
  public PersonHandler() {
    super("");
  }
  
  @Override
  public HandlerResponse handle(StartElement element, Object digestTarget) throws XMLStreamException {
    String localName = element.getName().getLocalPart();
    if ("person".equals(localName)) {
      person = (Person) digestTarget;
    } else if ("name".equals(localName)) {
      person.setName(getText());
    } else if ("date-of-birth".equals(localName)) {
      person.setDateOfBirth(someDateParsingFunction(getText());
    }
    return super.handle(element, digestTarget);
  }
  
  @Override
  public HandlerResponse handle(EndElement element, Object digestTarget) throws XMLStreamException {
    String localName = element.getName().getLocalPart();
    if ("person".equals(localName)) {
      return new FinishedParsingResponse();
    }
    return super.handle(element, digestTarget);
  }
}
```
