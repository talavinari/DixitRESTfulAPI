package rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AssociationNotifyDTO {
    @XmlElement
    BasicRequestDTO basicInfo;

    @XmlElement
    String winningCard;

    @XmlElement
    String association;
}
