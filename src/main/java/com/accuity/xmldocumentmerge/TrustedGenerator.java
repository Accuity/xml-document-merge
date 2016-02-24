package com.accuity.xmldocumentmerge;

import com.accuity.xmldocumentmerge.model.Rules;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Map;
import java.util.logging.Logger;


public class TrustedGenerator {
    private final static Logger LOG = Logger.getLogger(TrustedGenerator.class.getName());

    private RuleProcessor ruleProcessor;

    public TrustedGenerator(RuleProcessor ruleProcessor) {
        this.ruleProcessor = ruleProcessor;
    }

    public Document generateTrustedDocument(Rules trustMatrixRules, Map<String, Document> sourceDocuments) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document trustedDocument = null;
        if (trustMatrixRules.getRule() != null) {
            try {
                DocumentBuilder db = dbf.newDocumentBuilder();
                trustedDocument = db.newDocument();
                trustedDocument = ruleProcessor.processRule(trustMatrixRules.getRule(), trustedDocument, sourceDocuments);
                if (trustedDocument.getDocumentElement() != null) {
                    trustedDocument.getDocumentElement().setAttribute("source", "trusted");
                } else {
                    LOG.info("Trust Generator generated an empty document");
                    trustedDocument = null;
                }
            } catch (ParserConfigurationException e) {
                throw new RuntimeException("error initializing DocumentBuilder", e);
            }
        } else {
            LOG.warning("missing docroot rule");
        }
        return trustedDocument;
    }


}