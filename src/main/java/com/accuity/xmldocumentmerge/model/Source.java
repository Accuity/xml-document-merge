package com.accuity.xmldocumentmerge.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Source implements Comparable<Source> {

    @XmlAttribute
    private String name;

    @XmlAttribute
    private Float trust;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Source)) {
            return false;
        }
        Source src = (Source) obj;
        return src.getName().equals(this.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(Source otherSrc) {
        int compare = Float.compare(otherSrc.trust, this.trust);
        if (compare == 0) {
            compare = this.getName().compareTo(otherSrc.getName());
        }
        return compare;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getTrust() {
        return trust;
    }

    public void setTrust(Float trust) {
        this.trust = trust;
    }
}