package com.accuity.xmldocumentmerge.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SourceTest {

    /**
     * Test that the compareTo method will cause a list of sources to be sorted from most trusted to least
     *
     * @throws Exception
     */
    @Test
    public void testCompareTo() throws Exception {

        List<Source> sources = new ArrayList<>();

        Source source1 = new Source();
        source1.setTrust(.9f);
        source1.setName("fdb");


        Source source2 = new Source();
        source2.setTrust(1f);
        source2.setName("zeus");

        sources.add(source1);
        sources.add(source2);

        Collections.sort(sources);

        Assert.assertEquals("zeus", sources.get(0).getName());
    }
}