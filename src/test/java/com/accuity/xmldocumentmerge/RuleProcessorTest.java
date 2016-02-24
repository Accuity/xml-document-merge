package com.accuity.xmldocumentmerge;


import com.accuity.xmldocumentmerge.model.*;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RuleProcessorTest {
    private final static Logger LOG = Logger.getLogger(RuleProcessorTest.class.getName());

    @Before
    public void setup() {
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
    }

    /**
     * tests docroot
     *
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    @Test
    public void testDocRoot() throws ParserConfigurationException, IOException, SAXException {
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement>test</stringElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);

        Document trusted = documentBuilder.newDocument();

        Rule rule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        rule.setField(field);
        rule.setContext("test");
        rule.setWeighting(buildFdbFirstWeighting());

        ruleProcessor.processRule(rule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement>test</stringElement></test>".getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * tests that no trusted doc will be generated when there are no weights on the docroot rule
     * test created to verify HAPI-388
     *
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    @Test
    public void testDocRootHasNoWeightingSources() throws ParserConfigurationException, IOException, SAXException {
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement>test</stringElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);

        Document trusted = documentBuilder.newDocument();

        Rule rule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        rule.setField(field);
        rule.setContext("test");
        rule.setWeighting(new Weighting());

        ruleProcessor.processRule(rule, trusted, sourceDocuments);

        Document expected = documentBuilder.newDocument();

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * tests that a rule with a context of a leaf element (not root element) will merge in that element's value
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleSimpleFieldWithoutChildren() throws Exception {
        LOG.info("testProcessRuleSimpleFieldWithoutChildren");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement>test</stringElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);

        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement></stringElement></test>".getBytes(StandardCharsets.UTF_8)));


        Rule rule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(true);
        rule.setField(field);
        rule.setContext("stringElement");
        rule.setFullContextXPath("/test/stringElement");
        rule.setWeighting(buildFdbFirstWeighting());
        rule.setParentRule(new Rule()); // prevent docroot

        ruleProcessor.processRule(rule, trusted, sourceDocuments);

        // expected
        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement>test</stringElement></test>".getBytes(StandardCharsets.UTF_8)));

        // assertions
        Diff diff = XMLUnit.compareXML(expected, trusted);
        LOG.info(toString(trusted));
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * tests that a rule with a context of an element (not root element) will merge in that element
     * with 1 of each child element from all sources (in order of trust weight
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleSimpleFieldWithChildrenAndNoStop() throws Exception {
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement><childElement>test</childElement><childElement2>test</childElement2></stringElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><stringElement><childElement>zeus</childElement></stringElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        // simulate that the docroot rule has run with a trusted source of fdb
        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement></stringElement></test>".getBytes(StandardCharsets.UTF_8)));

        Rule rule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        rule.setField(field);
        rule.setContext("stringElement");
        rule.setWeighting(buildZeusFirstWeighting());


        ruleProcessor.processRule(rule, trusted, sourceDocuments);

        // assertions
        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement><childElement>zeus</childElement><childElement2>test</childElement2></stringElement></test>".getBytes(StandardCharsets.UTF_8)));


        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * tests that when finding the place to insert nodes in the trusted document, if there are none found and the rule has a filter, then remove the filter and look again.
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleFindsParentWithoutFilter() throws Exception {
        LOG.info("testProcessRuleFindsParentWithoutFilter");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><stringElement type=\"A\">fdb</stringElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><stringElement type=\"B\">zeus</stringElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        // simulate that the docroot rule has run with a trusted source of fdb
        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><stringElement type=\"A\">fdb</stringElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));


        Rule docRootRule = new Rule();
        docRootRule.setFullContextXPath("/test");
        docRootRule.setWeighting(buildFdbFirstWeighting());

        Rule rule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        rule.setField(field);
        rule.setContext("complexElement/stringElement");
        rule.setFilter("[@type = 'B']");
        rule.setWeighting(buildZeusFirstWeighting());
        rule.setParentRule(docRootRule);
        rule.setFullContextXPath("/test/complexElement/stringElement[@type = 'B']");


        ruleProcessor.processRule(rule, trusted, sourceDocuments);

        // assertions
        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><stringElement type=\"A\">fdb</stringElement><stringElement type=\"B\">zeus</stringElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));


        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * tests that rules nested in other rules will override the parent rule
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleSimpleFieldWithChildrenNestedRules() throws Exception {
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><a><b><source>fdb</source><c><d>fdb</d></c></b></a></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><a><b><source>zeus</source><c><d>zeus</d></c></b></a></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><a></a></test>".getBytes(StandardCharsets.UTF_8)));

        Rule docRootRule = new Rule();
        docRootRule.setContext("test");
        docRootRule.setFullContextXPath("/test");

        Rule rule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        rule.setContext("a");
        rule.setWeighting(buildFdbFirstWeighting());
        rule.setField(field);
        rule.setParentRule(docRootRule); // avoid docroot


        Rule subRule = new Rule();
        field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        subRule.setField(field);
        subRule.setContext("b/c");
        subRule.setWeighting(buildZeusFirstWeighting());

        List<Rule> subRules = new ArrayList<>();
        subRules.add(subRule);

        rule.setRules(subRules);

        rule.setParentRulesForChildren();

        ruleProcessor.processRule(rule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><a><b><source>fdb</source><c><d>zeus</d></c></b></a></test>".getBytes(StandardCharsets.UTF_8)));


        LOG.info(toString(trusted));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }


    /**
     * Tests that stop = false performs a subtree merge
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleSimpleFieldWithoutStop() throws Exception {
        LOG.info("testProcessRuleSimpleFieldWithoutStop");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement>fdb</stringElement><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><stringElement>zeus</stringElement><complexElement><childElement>zeus</childElement><zeusElement>zeus</zeusElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        // simulate what the trusted doc will be after doc root rule is applied
        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><stringElement>fdb</stringElement><complexElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));


        Rule subRule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        subRule.setField(field);
        subRule.setContext("complexElement");
        subRule.setFullContextXPath("/test/complexElement");
        subRule.setWeighting(buildFdbFirstWeighting());
        subRule.setParentRule(new Rule());


        ruleProcessor.processRule(subRule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><stringElement>fdb</stringElement><complexElement><childElement>fdb</childElement><zeusElement>zeus</zeusElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));


        LOG.info(toString(trusted));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * Tests that stop = true performs a subtree copy rather than a merge
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleSimpleFieldWithStop() throws Exception {
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement>zeus</childElement><zeusElement>zeus</zeusElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><stringElement>fdb</stringElement><complexElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));


        Rule subRule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(true);
        subRule.setField(field);
        subRule.setContext("complexElement");
        subRule.setFullContextXPath("/test/complexElement");
        subRule.setWeighting(buildFdbFirstWeighting());
        subRule.setParentRule(new Rule()); // prevent docroot


        ruleProcessor.processRule(subRule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><stringElement>fdb</stringElement><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * Tests that rules with a filter work correctly
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleSubFieldRule() throws Exception {
        LOG.info("testProcessRuleSubFieldRule");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement type=\"typea\">fdb</childElement><childElement type=\"typeb\">fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement type=\"typea\">zeus</childElement><childElement type=\"typeb\">zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><complexElement><childElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);

        Rule complexRule = new Rule();
        complexRule.setFullContextXPath("/test/complexElement");
        complexRule.setContext("complexElement");
        complexRule.setWeighting(buildFdbFirstWeighting());
        complexRule.setParentRule(new Rule()); // prevent docroot
        complexRule.setField(field);

        Rule subRule = new Rule();

        subRule.setField(field);
        subRule.setContext("childElement");
        subRule.setFilter("[@type = 'typea']");
        subRule.setFullContextXPath("/test/complexElement/childElement[@type = 'typea']");
        subRule.setWeighting(buildFdbFirstWeighting());
        subRule.setParentRule(complexRule); // prevent docroot

        Rule typeBRule = new Rule();
        typeBRule.setField(field);
        typeBRule.setContext("childElement");
        subRule.setFilter("[@type = 'typeb']");
        typeBRule.setFullContextXPath("/test/complexElement/childElement[@type = 'typeb']");
        typeBRule.setWeighting(buildZeusFirstWeighting());
        typeBRule.setParentRule(complexRule);


        List<Rule> subFieldRules = new ArrayList<>();
        subFieldRules.add(subRule);
        subFieldRules.add(typeBRule);

        complexRule.setRules(subFieldRules);


        ruleProcessor.processRule(complexRule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><complexElement><childElement type=\"typea\">fdb</childElement><childElement type=\"typeb\">zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * Tests that coalesce rules work correctly
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleCoalesceRule() throws Exception {
        LOG.info("testProcessRuleSubFieldRule");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement>zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><complexElement><childElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Field field = new Field();
        field.setCoalesce(true);
        field.setStop(false);

        Rule complexRule = new Rule();
        complexRule.setFullContextXPath("/test/complexElement/childElement");
        complexRule.setContext("childElement");
        complexRule.setWeighting(buildFdbFirstWeighting());
        complexRule.setParentRule(new Rule()); // prevent docroot
        complexRule.setField(field);

        ruleProcessor.processRule(complexRule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><complexElement><childElement>fdb</childElement><childElement>zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * Tests that coalesce rules work correctly with matching rules
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleCoalesceWithMatchingRule() throws Exception {
        LOG.info("testProcessRuleCoalesceWithMatchingRule");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement type=\"typea\">fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement type=\"typea\">zeus</childElement><childElement type=\"typeb\">zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><complexElement><childElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        List<Id> ids = new ArrayList<>();
        Id typeId = new Id();
        typeId.setPath("@type");
        ids.add(typeId);

        Field field = new Field();
        field.setCoalesce(true);
        field.setStop(false);
        field.setIds(ids);

        Rule complexRule = new Rule();
        complexRule.setFullContextXPath("/test/complexElement/childElement");
        complexRule.setContext("childElement");
        complexRule.setWeighting(buildFdbFirstWeighting());
        complexRule.setParentRule(new Rule()); // prevent docroot
        complexRule.setField(field);

        ruleProcessor.processRule(complexRule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><complexElement><childElement type=\"typea\">fdb</childElement><childElement type=\"typeb\">zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * Tests that coalesce rules work correctly with matching rules
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleCoalesceWithSelfMatchingRule() throws Exception {
        LOG.info("testProcessRuleCoalesceWithSelfMatchingRule");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement>zeus</childElement><childElement>zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><complexElement><childElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        List<Id> ids = new ArrayList<>();
        Id typeId = new Id();
        typeId.setPath(".");
        ids.add(typeId);

        Field field = new Field();
        field.setCoalesce(true);
        field.setStop(false);
        field.setIds(ids);

        Rule complexRule = new Rule();
        complexRule.setFullContextXPath("/test/complexElement/childElement");
        complexRule.setContext("childElement");
        complexRule.setWeighting(buildFdbFirstWeighting());
        complexRule.setParentRule(new Rule()); // prevent docroot
        complexRule.setField(field);

        ruleProcessor.processRule(complexRule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><complexElement><childElement>fdb</childElement><childElement>zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * Tests that sources with a trust value <= 0 will not get merged in
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleWillNotMergeUntrustedData() throws Exception {
        LOG.info("testProcessRuleWillNotMergeUntrustedData");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement>fdb</stringElement><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><stringElement>zeus</stringElement><complexElement><childElement>zeus</childElement><zeusElement>zeus</zeusElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        // simulate what the trusted doc will be after doc root rule is applied
        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><stringElement>fdb</stringElement><complexElement><childElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));


        Rule subRule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        subRule.setField(field);
        subRule.setContext("childElement");
        subRule.setFullContextXPath("/test/complexElement/childElement");
        subRule.setWeighting(buildFdbFirstWeighting());
        subRule.setParentRule(new Rule());

        subRule.getWeighting().getSources().get(0).setTrust(-1f);
        subRule.getWeighting().getSources().get(1).setTrust(-.5f);


        ruleProcessor.processRule(subRule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><stringElement>fdb</stringElement><complexElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * tests that when there are no trusted sources at the docroot rule, then an empty document is returned
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleWillNotCreateTrustDocWhenDocRootHasNoTrustedSources() throws Exception {
        LOG.info("testProcessRuleWillNotMergeUntrustedData");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement>fdb</stringElement><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><stringElement>zeus</stringElement><complexElement><childElement>zeus</childElement><zeusElement>zeus</zeusElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        // simulate what the trusted doc will be after doc root rule is applied
        Document trusted = documentBuilder.newDocument();


        Rule subRule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        subRule.setField(field);
        subRule.setContext("test");
        subRule.setFullContextXPath("/test");
        subRule.setWeighting(buildFdbFirstWeighting());

        subRule.getWeighting().getSources().get(0).setTrust(0f);
        subRule.getWeighting().getSources().get(1).setTrust(-.5f);


        ruleProcessor.processRule(subRule, trusted, sourceDocuments);

        Document expected = documentBuilder.newDocument();

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * Tests that rules that have no sources will use the parent's sources
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleWillUseParentSourcesWhenRuleHasNoWeight() throws Exception {
        LOG.info("testProcessRuleWillNotMergeUntrustedData");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><stringElement>fdb</stringElement><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><stringElement>zeus</stringElement><complexElement><childElement>zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        // simulate what the trusted doc will be after doc root rule is applied
        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><stringElement>fdb</stringElement><complexElement><childElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));


        Rule parentRule = new Rule();
        parentRule.setWeighting(buildFdbFirstWeighting());

        Rule subRule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        subRule.setField(field);
        subRule.setContext("childElement");
        subRule.setFullContextXPath("/test/complexElement/childElement");
        subRule.setParentRule(parentRule);

        ruleProcessor.processRule(subRule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><stringElement>fdb</stringElement><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    @Test
    public void testBuildKeySelf() throws IOException, SAXException, XPathExpressionException, ParserConfigurationException {
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        List<Id> ids = new ArrayList<>();
        Id typeId = new Id();
        typeId.setPath(".");
        ids.add(typeId);


        String key = ruleProcessor.buildKeyForNode(fdbDocument.getDocumentElement().getFirstChild().getFirstChild(), ids);

        Assert.assertEquals("fdb;", key);
    }


    @Test
    public void testBuildKeyAttribute() throws IOException, SAXException, XPathExpressionException, ParserConfigurationException {
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement type=\"fdb\"></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        List<Id> ids = new ArrayList<>();
        Id typeId = new Id();
        typeId.setPath("@type");
        ids.add(typeId);


        String key = ruleProcessor.buildKeyForNode(fdbDocument.getDocumentElement().getFirstChild().getFirstChild(), ids);

        Assert.assertEquals("fdb;", key);
    }

    @Test
    public void testBuildKeyElement() throws IOException, SAXException, XPathExpressionException, ParserConfigurationException {
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement><type>fdb</type><value>sdfsd</value></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        List<Id> ids = new ArrayList<>();
        Id typeId = new Id();
        typeId.setPath("type");
        ids.add(typeId);


        String key = ruleProcessor.buildKeyForNode(fdbDocument.getDocumentElement().getFirstChild().getFirstChild(), ids);

        Assert.assertEquals("fdb;", key);
    }


    @Test
    public void testSimpleFieldRuleDeletesNodeWhenItDoesntExistInAnyTrustedSources() throws IOException, SAXException, ParserConfigurationException {
        LOG.info("testSimpleFieldRuleDeletesNodeWhenItDoesntExistInAnyTrustedSources");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement>fdb</childElement><c>fdb</c></complexElement><complexElement><childElement>fdb</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement>zeus</childElement></complexElement><complexElement><childElement>zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        // simulate what the trusted doc will be after doc root rule is applied
        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><complexElement><childElement>zeus</childElement><c>fdb</c></complexElement><complexElement><childElement>zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));


        Rule parentRule = new Rule();
        parentRule.setWeighting(buildZeusFirstWeighting());

        Rule subRule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(true);
        subRule.setField(field);
        subRule.setContext("c");
        subRule.setFullContextXPath("/test/complexElement/c");
        subRule.setParentRule(parentRule);
        Source zeusSource = new Source();
        zeusSource.setName("zeus");
        zeusSource.setTrust(1f);
        List<Source> sources = new ArrayList<>();
        sources.add(zeusSource);
        Weighting weighting = new Weighting();
        weighting.setSources(sources);
        subRule.setWeighting(weighting);

        ruleProcessor.processRule(subRule, trusted, sourceDocuments);

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"trusted\"><complexElement><childElement>zeus</childElement></complexElement><complexElement><childElement>zeus</childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }


    private String toString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    private Weighting buildZeusFirstWeighting() {
        Source zeusSource = new Source();
        zeusSource.setName("zeus");
        zeusSource.setTrust(1f);

        Source fdbSource = new Source();
        fdbSource.setName("fdb");
        fdbSource.setTrust(.5f);


        List<Source> sources = new ArrayList<>();
        sources.add(zeusSource);
        sources.add(fdbSource);

        Weighting weighting = new Weighting();
        weighting.setSources(sources);
        return weighting;
    }

    private Weighting buildFdbFirstWeighting() {
        Source zeusSource = new Source();
        zeusSource.setName("zeus");
        zeusSource.setTrust(.5f);

        Source fdbSource = new Source();
        fdbSource.setName("fdb");
        fdbSource.setTrust(1f);


        List<Source> sources = new ArrayList<>();
        sources.add(zeusSource);
        sources.add(fdbSource);

        Weighting weighting = new Weighting();
        weighting.setSources(sources);
        return weighting;
    }

    private String buildXmlDiffMessage(Diff diff) throws IOException, SAXException {
        DetailedDiff detailedDiff = new DetailedDiff(diff);
        StringBuilder sb = new StringBuilder();
        for (Object o : detailedDiff.getAllDifferences()) {
            Difference difference = (Difference) o;
            // compare bigdecimal based on value instead of string
            if (difference.getControlNodeDetail().getValue().equals("1000.00")) {
                if (new BigDecimal(difference.getControlNodeDetail().getValue()).compareTo(new BigDecimal(difference.getTestNodeDetail().getValue())) != 0) {
                    sb.append(o.toString() + "\n");
                }
            }
            //ignore time-zone for dates
            else if (difference.getControlNodeDetail().getValue().endsWith(":00")) {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    if (!df.parse(difference.getControlNodeDetail().getValue()).equals(df.parse(difference.getTestNodeDetail().getValue()))) {
                        sb.append(o.toString() + "\n");
                    }
                } catch (ParseException e) {
                    throw new RuntimeException("Attempted date parsing error", e);
                }
            } else {
                sb.append(o.toString() + "\n");
            }

        }
        return sb.toString();
    }

    /**
     * Tests when processing a rule, and there are no source documents, and the rule has stop = true, and the rule has children.
     * This should not happen, but bug HAPI-375 is caused by this scenario not being handled gracefully.
     *
     * @throws Exception
     */
    @Test
    public void testProcessStopRuleWithChildrenWithNoSourceDocs() throws Exception {
        LOG.info("testProcessStopRuleWithChildrenWithNoSourceDocs");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Map<String, Document> sourceDocuments = new HashMap<>();

        // simulate what the trusted doc will be after doc root rule is applied (empty when there are no source docs)
        Document trusted = documentBuilder.newDocument();


        Rule subRule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(true);
        subRule.setField(field);
        subRule.setContext("complexElement");
        subRule.setFullContextXPath("/test/complexElement");
        subRule.setWeighting(buildFdbFirstWeighting());
        subRule.setParentRule(new Rule());
        subRule.setRules(new ArrayList<Rule>());


        ruleProcessor.processRule(subRule, trusted, sourceDocuments);

        Document expected = documentBuilder.newDocument();


        LOG.info(toString(trusted));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    /**
     * tests that a rule with an xpath which has no matches in the must trusted document,
     * will find a match in the next most trusted document
     *
     * @throws Exception
     */
    @Test
    public void testProcessRuleWillFallBackToNextMostTrusted() throws Exception {
        LOG.info("testProcessRuleWillFallBackToNextMostTrusted");

        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement>test</childElement><childElement2>test</childElement2></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        // simulate that the docroot rule has run with a trusted source of fdb
        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement>test</childElement><childElement2>test</childElement2></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Rule docRootRule = new Rule();
        docRootRule.setContext("test");
        docRootRule.setWeighting(buildZeusFirstWeighting());

        Rule rule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(true);
        rule.setField(field);
        rule.setContext("complexElement");
        rule.setParentRule(docRootRule);
        rule.setFullContextXPath("/test/complexElement");


        ruleProcessor.processRule(rule, trusted, sourceDocuments);

        // assertions
        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement>test</childElement><childElement2>test</childElement2></complexElement></test>".getBytes(StandardCharsets.UTF_8)));


        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }

    @Test
    public void testSimpleRuleWillIgnoreExtraNodes() throws Exception {
        LOG.info("testProcessRuleWillFallBackToNextMostTrusted");

        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"fdb\"><complexElement><childElement><stringElement>test1</stringElement></childElement><childElement><stringElement>test2</stringElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));
        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement><stringElement>zeus</stringElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        // simulate that the docroot rule has run with a trusted source of fdb
        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement><stringElement>zeus</stringElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Rule rule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(true);
        rule.setField(field);
        rule.setContext("complexElement");
        rule.setParentRule(new Rule());
        rule.setWeighting(buildZeusFirstWeighting());
        rule.setFullContextXPath("/test/complexElement/childElement");

        ruleProcessor.processRule(rule, trusted, sourceDocuments);

        // assertions

        Document expected = documentBuilder.parse(new ByteArrayInputStream("<test source=\"zeus\"><complexElement><childElement><stringElement>zeus</stringElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());

    }

    /**
     * Tests that when a rule matches multiple nodes in the most trusted document, all will be added to the trusted document
     *
     * @throws Exception
     */
    @Test
    public void testSimpleRuleMultipleNodesFromMostTrustedSource() throws Exception {
        LOG.info("testSimpleRuleMultipleNodesFromMostTrustedSource");

        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document zeusDocument = documentBuilder.parse(new ByteArrayInputStream(("<test source=\"zeus\">" +
                "<complexElement>" +
                "<childElement><stringElement>zeus</stringElement></childElement>" +
                "<childElement><stringElement>zeus2</stringElement></childElement>" +
                "</complexElement>" +
                "</test>").getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        //   sourceDocuments.put("fdb", fdbDocument);
        sourceDocuments.put("zeus", zeusDocument);

        // simulate that the docroot rule has run with a trusted source of other
        Document trusted = documentBuilder.parse(new ByteArrayInputStream("<test source=\"other\"><complexElement><childElement></childElement></complexElement></test>".getBytes(StandardCharsets.UTF_8)));

        Rule rule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        rule.setField(field);
        rule.setContext("complexElement");
        rule.setParentRule(new Rule());
        rule.setWeighting(buildZeusFirstWeighting());
        rule.setFullContextXPath("/test/complexElement/childElement");

        ruleProcessor.processRule(rule, trusted, sourceDocuments);

        // assertions

        Document expected = documentBuilder.parse(new ByteArrayInputStream(("<test source=\"other\">" +
                "<complexElement>" +
                "<childElement><stringElement>zeus</stringElement></childElement>" +
                "<childElement><stringElement>zeus2</stringElement></childElement>" +
                "</complexElement>" +
                "</test>").getBytes(StandardCharsets.UTF_8)));

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());

    }

    /**
     * tests that an empty document will be produced when there are no source documents with trust > 0 at the docroot level
     * At one point, this caused a NPE when there were subrules.
     *
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    @Test
    public void testDocRootWithNoTrustedSourcesAndSubRules() throws ParserConfigurationException, IOException, SAXException {
        LOG.info("testDocRootWithNoTrustedSourcesAndSubRules");
        RuleProcessor ruleProcessor = new RuleProcessor();

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document fdbDocument = documentBuilder.parse(new ByteArrayInputStream("<test source=\"other\"><stringElement>test</stringElement></test>".getBytes(StandardCharsets.UTF_8)));

        Map<String, Document> sourceDocuments = new HashMap<>();
        sourceDocuments.put("other", fdbDocument);

        Document trusted = documentBuilder.newDocument();

        Rule rule = new Rule();
        Field field = new Field();
        field.setCoalesce(false);
        field.setStop(false);
        rule.setField(field);
        rule.setContext("test");
        rule.setWeighting(buildFdbFirstWeighting());

        Rule subRule = new Rule();
        subRule.setParentRule(rule);
        subRule.setWeighting(buildZeusFirstWeighting());
        subRule.getWeighting().getSources().get(0).setName("other");
        subRule.setContext("stringElement");
        subRule.setFullContextXPath("/test/stringElement");
        List<Rule> subRules = new ArrayList<>();
        subRules.add(subRule);

        rule.setRules(subRules);

        ruleProcessor.processRule(rule, trusted, sourceDocuments);


        Document expected = documentBuilder.newDocument();

        Diff diff = XMLUnit.compareXML(expected, trusted);
        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());
    }
}