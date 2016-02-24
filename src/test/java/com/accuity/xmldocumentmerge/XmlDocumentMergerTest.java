package com.accuity.xmldocumentmerge;

import com.accuity.xmldocumentmerge.model.Rule;
import com.accuity.xmldocumentmerge.model.Rules;
import com.accuity.xmldocumentmerge.model.Source;
import com.accuity.xmldocumentmerge.model.Weighting;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlDocumentMergerTest {

    @Test
    public void testGenerateTrustedDocumentDocRoot() throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        // data
        Rules rules = new Rules();
        Rule docRoot = new Rule();
        docRoot.setContext("country");
        Weighting weighting = new Weighting();
        Source sourceB = new Source();
        sourceB.setName("sourceB");
        sourceB.setTrust(0.9f);
        Source sourceA = new Source();
        sourceA.setName("sourceA");
        sourceA.setTrust(1f);

        List<Source> sourceList = new ArrayList<>();
        sourceList.add(sourceB);
        sourceList.add(sourceA);
        weighting.setSources(sourceList);
        docRoot.setWeighting(weighting);
        rules.setRule(docRoot);

        RuleProcessor rp = Mockito.mock(RuleProcessor.class);

        XmlDocumentMerger trustedGenerator = new XmlDocumentMerger(rp);

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("sourceB", db.parse(new ByteArrayInputStream("<country source=\"sourceB\"></country>".getBytes(StandardCharsets.UTF_8))));
        sourceDocuments.put("sourceA", db.parse(new ByteArrayInputStream("<country source=\"sourceA\"></country>".getBytes(StandardCharsets.UTF_8))));


        Mockito.when(rp.processRule(Mockito.any(Rule.class), Mockito.any(Document.class), Mockito.anyMap())).thenReturn(db.parse(new ByteArrayInputStream("<country source=\"trusted\"></country>".getBytes(StandardCharsets.UTF_8))));

        Document trusted = trustedGenerator.mergeDocuments(rules, sourceDocuments);

        Assert.assertEquals("trusted", trusted.getDocumentElement().getAttribute("source"));

    }

    /**
     * if the root rule object is null, the generator should return null.
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    @Test
    public void testGenerateTrustedDocumentChecksNullDocRoot() throws IOException, SAXException, ParserConfigurationException {
        RuleProcessor rp = Mockito.mock(RuleProcessor.class);

        XmlDocumentMerger trustedGenerator = new XmlDocumentMerger(rp);

        Rules rules = new Rules();
        rules.setContext("/");

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
        sourceDocuments.put("zeus", DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());


        Mockito.when(rp.processRule(Mockito.any(Rule.class), Mockito.any(Document.class), Mockito.anyMap())).thenReturn(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());

        Document trusted = trustedGenerator.mergeDocuments(rules, sourceDocuments);

        Assert.assertNull(trusted);

    }

    /**
     * Tests that mergeDocuments returns Null when RulesProcessor Returns An Empty Document.
     * For example, this would occur when there are no sources which are trusted > 0
     *
     * @throws ParserConfigurationException
     */
    @Test
    public void generateTrustedDocumentReturnsNullWhenRulesProcessorReturnsAnEmptyDocument() throws ParserConfigurationException {

        // mocks
        Rule docRootRule = new Rule();
        Rules rules = new Rules();
        rules.setRule(docRootRule);
        Map<String, Document> documentMap = new HashMap<>();
        RuleProcessor ruleProcessor = Mockito.mock(RuleProcessor.class);
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Mockito.when(ruleProcessor.processRule(Mockito.any(Rule.class), Mockito.any(Document.class), Mockito.anyMap())).thenReturn(db.newDocument());

        // instance
        XmlDocumentMerger generator = new XmlDocumentMerger(ruleProcessor);

        // run code
        Document trustedDocument = generator.mergeDocuments(rules, documentMap);

        // asserts
        Assert.assertNull("Generated document should be null", trustedDocument);
    }
}