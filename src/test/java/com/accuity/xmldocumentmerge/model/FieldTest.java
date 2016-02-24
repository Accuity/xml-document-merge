package com.accuity.xmldocumentmerge.model;

import org.junit.Assert;
import org.junit.Test;

public class FieldTest {

    @Test
    public void testIsStop() throws Exception {
        Field field = new Field();
        field.setStop(true);
        Assert.assertEquals(true, field.isStop());

        field.setStop(false);
        Assert.assertEquals(false, field.isStop());

        field.setStop(null);
        Assert.assertEquals("isStop returns false when stop property is null", false, field.isStop());
    }

    @Test
    public void testIsCoalesce() throws Exception {

        Field field = new Field();
        field.setCoalesce(true);
        Assert.assertEquals(true, field.isCoalesce());

        field.setCoalesce(false);
        Assert.assertEquals(false, field.isCoalesce());

        field.setCoalesce(null);
        Assert.assertEquals("isCoalesce returns false when coalesce property is null", false, field.isCoalesce());

    }

}