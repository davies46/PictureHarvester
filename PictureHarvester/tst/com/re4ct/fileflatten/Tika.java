package com.re4ct.fileflatten;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.jpeg.JpegParser;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class Tika {
	final static Logger log = Logger.getLogger(Tika.class);

	class Handler implements ContentHandler {

		@Override
		public void setDocumentLocator(Locator locator) {
			log.info("setDocumentLocator");
		}

		@Override
		public void startDocument() throws SAXException {
			log.info("startDocument");

		}

		@Override
		public void endDocument() throws SAXException {
			log.info("endDocument");

		}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			log.info("startPrefixMapping");

		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
			log.info("endPrefixMapping");

		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
			log.info("startElement " + localName);
			for (int attNo = 0; attNo < atts.getLength(); attNo++) {
				String name = atts.getLocalName(attNo);
				String val = atts.getValue(attNo);
				log.info(name + " = " + val);
			}

		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			log.info("endElement " + localName);

		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			log.warn("characters: " + length + " " + ch.length);

		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			log.info("ignorableWhitespace: " + length);

		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {
			log.info("processingInstruction");

		}

		@Override
		public void skippedEntity(String name) throws SAXException {
			log.info("skippedEntity");

		}
	}

	@Test
	public void test() throws IOException, SAXException, TikaException {
		// detecting the file type
		// BodyContentHandler handler = new BodyContentHandler();
		Handler handler = new Handler();
		Metadata metadata = new Metadata();
		try (FileInputStream inputstream = new FileInputStream(new File("D:/Flat pics/1.jpg"));) {
			ParseContext pcontext = new ParseContext();

			// Jpeg Parse
			JpegParser JpegParser = new JpegParser();
			JpegParser.parse(inputstream, handler, metadata, pcontext);
			// System.out.println("Contents of the document:" + handler.toString());
			System.out.println("Metadata of the document:");
			String[] metadataNames = metadata.names();

			for (String name : metadataNames) {
				System.out.println(name + ": " + metadata.get(name));
			}
		}
	}
}
