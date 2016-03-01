package com.accuity.xmldocumentmerge;

import com.accuity.xmldocumentmerge.model.Rule;
import com.accuity.xmldocumentmerge.model.Rules;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SerializationTest {
    @Test
    public void testJson() throws JsonProcessingException {
        Rules rules = new Rules();

        Rule rule1 = new Rule();
        rule1.setContext("test");
        rule1.setFilter("");

        Rule rule2 = new Rule();
        rule2.setContext("test2");

        List<Rule> rule1Subrules = new ArrayList<>();
        rule1Subrules.add(rule2);
        rule1.setRules(rule1Subrules);
        rules.setRule(rule1);
        rules.setContext("/");

        rules.getRule().setParentRulesForChildren();

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(rules);

        JSONObject jsonObject = new JSONObject(json);

        Assert.assertEquals("/",jsonObject.getString("context"));
    }
}
