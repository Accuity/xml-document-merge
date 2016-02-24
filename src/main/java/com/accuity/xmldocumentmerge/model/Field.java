package com.accuity.xmldocumentmerge.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Field implements Serializable {

    private static final long serialVersionUID = -4937775518257931292L;

    @XmlAttribute
    private Boolean stop;

    @XmlAttribute
    private Boolean coalesce;

    @XmlElement(name = "id")
    private List<Id> ids;

    public boolean isStop() {
        return stop != null ? stop : false;
    }

    public void setStop(Boolean stop) {
        this.stop = stop;
    }

    public boolean isCoalesce() {
        return coalesce != null ? coalesce : false;
    }

    public void setCoalesce(Boolean coalesce) {
        this.coalesce = coalesce;
    }

    public List<Id> getIds() {
        return ids;
    }

    public void setIds(List<Id> ids) {
        this.ids = ids;
    }
}
