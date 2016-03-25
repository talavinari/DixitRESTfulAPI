package rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by tal on 3/23/2016.
 */
@XmlRootElement
public class CardRequestDTO {
    public CardRequestDTO() {
    }

    public CardRequestDTO(BasicRequestDTO requestBasicDTO, int cardNumberRequest) {
        this.basicInfo = requestBasicDTO;
        this.cardNumberRequest = cardNumberRequest;
    }

    @XmlElement
    BasicRequestDTO basicInfo;

    @XmlElement
    int cardNumberRequest;

}
