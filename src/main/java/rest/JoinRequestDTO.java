package rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by tal on 3/12/2016.
 */
@XmlRootElement
public class JoinRequestDTO {
    @XmlElement
    public String nickName;

    @XmlElement
    public String roomName;
}
