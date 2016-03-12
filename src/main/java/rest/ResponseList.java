package rest;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by tal on 3/12/2016.
 */
@XmlRootElement(name = "responseList")
public class ResponseList {

    private List<String> list;

    public ResponseList() {
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }
}
