package rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by tal on 3/23/2016.
 */
@XmlRootElement
public class CardRequestDTO {
    @XmlElement
    BasicRequestDTO requestBasicDTO;

    @XmlElement
    int cardNumberRequest;

}
