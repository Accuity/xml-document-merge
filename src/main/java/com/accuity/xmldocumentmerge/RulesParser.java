package com.accuity.xmldocumentmerge;

import com.accuity.xmldocumentmerge.model.Rules;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.InputStream;

/**
 * Parses an inputStream into the Rules model to be passed into the XmlDocumentMerger
 */
public class RulesParser {
    Rules parse(InputStream inputStream) {

        Rules rules = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Rules.class);
            rules = (Rules) jaxbContext.createUnmarshaller().unmarshal(inputStream);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return rules;
    }
}
