package rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class BasicRequestDTO {
    @XmlElement
    public String nickName;

    @XmlElement
    public String roomName;
}



