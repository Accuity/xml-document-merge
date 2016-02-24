@XmlSchema(
        namespace="http://accuity.com/apo/trust-matrix",
        elementFormDefault = XmlNsForm.QUALIFIED,
        attributeFormDefault = XmlNsForm.QUALIFIED,
        xmlns={
                @XmlNs(prefix="tm", namespaceURI="http://accuity.com/apo/trust-matrix")
        }
)

package com.accuity.xmldocumentmerge.model;

import javax.xml.bind.annotation.*;