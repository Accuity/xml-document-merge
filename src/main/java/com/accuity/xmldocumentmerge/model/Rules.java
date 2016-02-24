package com.accuity.xmldocumentmerge.model;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "rules")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"context", "rule"})
public class Rules {


    @XmlAttribute
    private String context;


    @XmlElement(name = "rule")
    private Rule rule;

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }
}
