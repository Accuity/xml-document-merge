package com.accuity.xmldocumentmerge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"field", "weighting", "rules"})
public class Rule {


    @XmlAttribute
    private String context;

    @XmlAttribute
    private String filter;


    @XmlElement
    private Field field;


    @XmlElement(name = "weightings")
    private Weighting weighting;


    @XmlElement(name = "rule")
    private List<Rule> rules;


    @XmlTransient
    @JsonIgnore
    private Rule parentRule;


    @XmlTransient
    @JsonIgnore
    private String fullContextXPath;

    public void setParentRulesForChildren() {
        fullContextXPath = calculateXPath();
        if (rules != null) {
            for (Rule rule : rules) {
                rule.setParentRule(this);
                rule.setParentRulesForChildren();
            }

        }
    }

    private String calculateXPath() {
        List<String> contextItems = new ArrayList<>();

        Rule rulePointer = this;
        while (rulePointer != null) {
            String contextItem = "/" + rulePointer.getContext();
            if (rulePointer.getFilter() != null && !rulePointer.getFilter().trim().isEmpty()) {
                contextItem += rulePointer.getFilter();
            }
            contextItems.add(contextItem);
            rulePointer = rulePointer.getParentRule();
        }

        //Go backwards through the contextItems list and build the xpath
        StringBuilder sb = new StringBuilder();
        for (int i = contextItems.size() - 1; i >= 0; i--) {
            sb.append(contextItems.get(i));
        }
        return sb.toString();
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Weighting getWeighting() {
        return weighting;
    }

    public void setWeighting(Weighting weighting) {
        this.weighting = weighting;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public Rule getParentRule() {
        return parentRule;
    }

    public void setParentRule(Rule parentRule) {
        this.parentRule = parentRule;
    }

    public String getFullContextXPath() {
        return fullContextXPath;
    }

    public void setFullContextXPath(String fullContextXPath) {
        this.fullContextXPath = fullContextXPath;
    }
}
