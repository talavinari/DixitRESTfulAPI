package rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by tal on 3/25/2016.
 */
@XmlRootElement
public class VoteDTO {
    @XmlElement
    BasicRequestDTO basicInfo;

    @XmlElement
    String card;
}
