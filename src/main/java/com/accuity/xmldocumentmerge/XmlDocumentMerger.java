package com.accuity.xmldocumentmerge;

import com.accuity.xmldocumentmerge.model.Rules;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This is the main class to use.
 */
public class XmlDocumentMerger {
    private final static Logger LOG = Logger.getLogger(XmlDocumentMerger.class.getName());

    private RuleProcessor ruleProcessor;

    public XmlDocumentMerger(RuleProcessor ruleProcessor) {
        this.ruleProcessor = ruleProcessor;
    }

    /**
     *
     * @param trustMatrixRules The rules model
     * @param sourceDocuments a map of documents to combine. The keys are the source names used in the rules
     * @return the combined document
     */
    public Document mergeDocuments(Rules trustMatrixRules, Map<String, Document> sourceDocuments) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document trustedDocument = null;
        if (trustMatrixRules.getRule() != null) {
            try {
                DocumentBuilder db = dbf.newDocumentBuilder();
                trustedDocument = db.newDocument();
                trustedDocument = ruleProcessor.processRule(trustMatrixRules.getRule(), trustedDocument, sourceDocuments);
                if (trustedDocument.getDocumentElement() == null) {
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