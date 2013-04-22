package dk.defiant.xml.digester;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import org.junit.Before;
import org.junit.Test;

import dk.defiant.xml.digester.handlers.SimpleDigesterEventHandler;
import dk.defiant.xml.digester.responses.DelegateParsingResponse;
import dk.defiant.xml.digester.responses.FinishedParsingResponse;

public class SimpleDigesterEventHandlerTest {

	class Address {
		private String street;
		private String zip;
		
		public void setStreet(String street) {
			this.street = street;
		}
		
		public void setZip(String zip) {
			this.zip = zip;
		}
		
		public String getStreet() {
			return street;
		}
		
		public String getZip() {
			return zip;
		}
	}
	
	class Person {
		private String name;
		private int age;
		private Address address;
		
		public void setAddress(Address address) {
			this.address = address;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setAge(int age) {
			this.age = age;
		}
		
		public Address getAddress() {
			return address;
		}
		
		public String getName() {
			return name;
		}
		
		public int getAge() {
			return age;
		}
	}
	
	class PersonHandler extends SimpleDigesterEventHandler {

		private Person person;
		
		public PersonHandler() {
			super("");
		}

		@Override
		public HandlerResponse handle(StartElement element, Object digestTarget) {
			try {
				String localName = element.getName().getLocalPart();
				if ("person".equals(localName)) {
					person = (Person) digestTarget;
				} else if ("name".equals(localName)) {
					person.setName(getText());
				} else if ("age".equals(localName)) {
					person.setAge(Integer.parseInt(getText()));
				} else if ("address".equals(localName)) {
					return new DelegateParsingResponse(new AddressHandler(), new Address());
				}
				return super.handle(element, digestTarget);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public HandlerResponse handle(EndElement element, Object digestTarget) throws XMLStreamException {
			String localName = element.getName().getLocalPart();
			if ("person".equals(localName)) {
				return new FinishedParsingResponse();
			} else if ("address".equals(localName)) {
				person.setAddress((Address) digestTarget);
			}
			return super.handle(element, digestTarget);
		}
	}
	
	class AddressHandler extends SimpleDigesterEventHandler {
		
		private Address address;
		
		public AddressHandler() {
			super("");
		}
		
		@Override
		public HandlerResponse handle(StartElement element, Object digestTarget) {
			try {
				String localName = element.getName().getLocalPart();
				if ("address".equals(localName)) {
					address = (Address) digestTarget;
				} else if ("street".equals(localName)) {
					address.setStreet(getText());
				} else if ("zip".equals(localName)) {
					address.setZip(getText());
				}
				return super.handle(element, digestTarget);
			} catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public HandlerResponse handle(EndElement element, Object digestTarget) throws XMLStreamException {
			String localName = element.getName().getLocalPart();
			if ("address".equals(localName)) {
				return new FinishedParsingResponse();
			}
			return super.handle(element, digestTarget);
		}
	}
	
	private Person person;
	
	@Before
	public void beforeEach() throws Exception {
		XmlDigester digester = new XmlDigester();
		String xml = 
				"<person>" +
				"  <name>John Doe</name>" +
				"  <age>42</age>" +
				"  <address>" +
				"    <street>E-Street</street>" +
				"    <zip>1234</zip>" +
				"  </address>" +
				"</person>";
		
		person = new Person();
		digester.digest(xml, person, new PersonHandler());
	}
	
	@Test
	public void digestsPersonNameCorrectly() {
		assertEquals("John Doe", person.getName());
	}
	
	@Test
	public void digestsPersonAgeCorrectly() {
		assertEquals(42, person.getAge());
	}
	
	@Test
	public void digestsPersonAddressCorrectly() {
		assertNotNull(person.getAddress());
		assertEquals("E-Street", person.getAddress().getStreet());
		assertEquals("1234", person.getAddress().getZip());
	}
	
}
