package com.accuity.xmldocumentmerge;


import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DocumentNodeMergerTest {

    @Test
    public void testMergeNodeChildren() throws Exception {
        DocumentNodeMerger merger = new DocumentNodeMerger();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();

        Document doc1 = documentBuilder.parse(new ByteArrayInputStream("<a><b><c><d>1</d></c></b><bb>1</bb><bbb>1</bbb></a>".getBytes(StandardCharsets.UTF_8)));
        Document doc2 = documentBuilder.parse(new ByteArrayInputStream("<a><b><c><d>2</d><dd>2</dd></c><cc>2</cc></b><bb></bb></a>".getBytes(StandardCharsets.UTF_8)));
        Document expected = documentBuilder.parse(new ByteArrayInputStream("<a><b><c><d>1</d><dd>2</dd></c><cc>2</cc></b><bb>1</bb><bbb>1</bbb></a>".getBytes(StandardCharsets.UTF_8)));

        merger.mergeNodeChildren(doc1.getDocumentElement(), doc2.getDocumentElement());

        Diff diff = XMLUnit.compareXML(expected, doc1);

        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());

    }

    @Test
    public void testMergeNodeChildrenMultipleSameElements() throws Exception {
        DocumentNodeMerger merger = new DocumentNodeMerger();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();

        Document doc1 = documentBuilder.parse(new ByteArrayInputStream("<a><b><c>c1</c></b><b><d>d1</d></b></a>".getBytes(StandardCharsets.UTF_8)));
        Document doc2 = documentBuilder.parse(new ByteArrayInputStream("<a><b><d>d2</d></b><b><c>c2</c></b></a>".getBytes(StandardCharsets.UTF_8)));
        Document expected = documentBuilder.parse(new ByteArrayInputStream("<a><b><c>c1</c><d>d2</d></b><b><d>d1</d><c>c2</c></b></a>".getBytes(StandardCharsets.UTF_8)));

        merger.mergeNodeChildren(doc1.getDocumentElement(), doc2.getDocumentElement());

        Diff diff = XMLUnit.compareXML(expected, doc1);

        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());

    }

    @Test
    public void testMergeNodeChildrenExtraCopies() throws Exception {
        DocumentNodeMerger merger = new DocumentNodeMerger();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();

        Document doc1 = documentBuilder.parse(new ByteArrayInputStream("<a><b><c>doc1num1</c></b></a>".getBytes(StandardCharsets.UTF_8)));
        Document doc2 = documentBuilder.parse(new ByteArrayInputStream("<a><b><c>doc2num1</c></b><b><c>doc2num2</c></b></a>".getBytes(StandardCharsets.UTF_8)));
        Document expected = documentBuilder.parse(new ByteArrayInputStream("<a><b><c>doc1num1</c></b></a>".getBytes(StandardCharsets.UTF_8)));

        merger.mergeNodeChildren(doc1.getDocumentElement(), doc2.getDocumentElement());

        Diff diff = XMLUnit.compareXML(expected, doc1);

        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());

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


    @Test
    /**
     * Tests that when the source node has multiple nodes with the same name, and the destination has none of that name,
     * all instances of that node get appended to the destination
     */
    public void testMergeNodeChildrenMultiple() throws Exception {
        DocumentNodeMerger merger = new DocumentNodeMerger();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();

        Document doc1 = documentBuilder.parse(new ByteArrayInputStream("<a></a>".getBytes(StandardCharsets.UTF_8)));
        Document doc2 = documentBuilder.parse(new ByteArrayInputStream("<a><b>1</b><b>2</b><b>3</b></a>".getBytes(StandardCharsets.UTF_8)));
        Document expected = documentBuilder.parse(new ByteArrayInputStream("<a><b>1</b><b>2</b><b>3</b></a>".getBytes(StandardCharsets.UTF_8)));

        merger.mergeNodeChildren(doc1.getDocumentElement(), doc2.getDocumentElement());

        Diff diff = XMLUnit.compareXML(expected, doc1);

        Assert.assertTrue(buildXmlDiffMessage(diff), diff.similar());

    }
}