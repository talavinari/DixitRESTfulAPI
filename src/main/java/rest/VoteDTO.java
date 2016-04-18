package rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class VoteDTO {
    @XmlElement
    BasicRequestDTO basicInfo;

    @XmlElement
    String card;
}
