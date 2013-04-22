package dk.defiant.xml.digester;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.defiant.xml.digester.handlers.SimpleDigesterEventHandler;
import dk.defiant.xml.digester.responses.FinishedParsingResponse;

public class XmlDigesterTest {

	private static final Logger log = LoggerFactory.getLogger(XmlDigesterTest.class);
	
	class Person {
		String addressBlob;
		String name;
		
		public void setAddressBlob(String addressBlob) {
			this.addressBlob = addressBlob;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getAddressBlob() {
			return addressBlob;
		}
		
		public String getName() {
			return name;
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
				} else if ("address".equals(element.getName().getLocalPart())) {
					person.setAddressBlob(getXmlFragment(true));
				} else if ("name".equals(localName)) {
					person.setName(getText());
				}
				return super.handle(element, digestTarget);
			} catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
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

	@Test
	public void digestsXmlBlob() throws Exception {
		XmlDigester digester = new XmlDigester();
		Person person = new Person();
		String xml =
				"<person>" +
				"  <name>John Doe</name>" +
				"  <address>" +
				"    <!-- Address -->" +
				"    <street>street</street>" +
				"    <zip>1234</zip>" +
				"  </address>" +
				"</person>";
		
		digester.digest(xml, person, new PersonHandler());
		log.debug("Person address {}", person.getAddressBlob());
	}
}
