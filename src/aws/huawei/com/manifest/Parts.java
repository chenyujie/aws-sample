package aws.huawei.com.manifest;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Parts", propOrder = { "part" })
public class Parts {

	@XmlElement(required = true)
	protected List<Part> part;

	@XmlAttribute
	protected Integer count;

	public List<Part> getPart() {
		if (this.part == null) {
			this.part = new ArrayList<Part>();
		}
		return this.part;
	}

	public Integer getCount() {
		return this.count;
	}

	public void setCount(Integer value) {
		this.count = value;
	}
}
