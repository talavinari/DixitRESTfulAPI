package rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CardPickedNotifyDTO {
    public CardPickedNotifyDTO() {
    }

    public CardPickedNotifyDTO(BasicRequestDTO requestBasicDTO, int cardNumberRequest) {
        this.basicInfo = requestBasicDTO;
        this.cardNumberRequest = cardNumberRequest;
    }

    @XmlElement
    BasicRequestDTO basicInfo;

    @XmlElement
    int cardNumberRequest;

}
