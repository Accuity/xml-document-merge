package com.accuity.xmldocumentmerge;

import com.accuity.xmldocumentmerge.model.Rules;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class RulesParserTest {

    @Test
    public void testParse() throws Exception {
        String fileContent = "<tm:rules tm:context=\"/\" xmlns:tm=\"http://accuity.com/apo/trust-matrix\">" +
                "  <tm:rule tm:context=\"rootElement\">" +
                "    <tm:weightings>\n" +
                "    <tm:source tm:name=\"sourceC\" tm:trust=\"1.0\"></tm:source>\n" +
                "    <tm:source tm:name=\"sourceB\" tm:trust=\"0.9\"></tm:source>\n" +
                "  </tm:weightings>\n" +
                "  <tm:rule tm:context=\"subElement/collection/collectionElement\" tm:filter=\"[not(type = 'typeA')]\">" +
                "    <tm:field tm:coalesce=\"true\" tm:stop=\"true\"><tm:id tm:path=\"type\"></tm:id></tm:field>" +
                "    <tm:weightings>\n" +
                "      <tm:source tm:name=\"sourceA\" tm:trust=\"1.0\"></tm:source>\n" +
                "      <tm:source tm:name=\"sourceB\" tm:trust=\"0.5\"></tm:source>\n" +
                "    </tm:weightings>\n" +
                "  </tm:rule>\n" +
                "  <tm:rule tm:context=\"subElement/collection/collectionElement\" tm:filter=\"[type = 'typeA']\">" +
                "    <tm:field tm:coalesce=\"true\" tm:stop=\"true\"><tm:id tm:path=\"type\"></tm:id></tm:field>\n" +
                "      <tm:weightings>\n" +
                "        <tm:source tm:name=\"sourceB\" tm:trust=\"1.0\"></tm:source>\n" +
                "      </tm:weightings>\n" +
                "    </tm:rule>\n" +
                "  </tm:rule>\n" +
                "</tm:rules>";

        RulesParser rulesParser = new RulesParser();
        Rules result = rulesParser.parse(new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals("/", result.getContext());
        Assert.assertEquals("rootElement", result.getRule().getContext());
    }
}