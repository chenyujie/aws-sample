package aws.huawei.com.manifest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ByteRange")
public class ByteRange {
	@XmlAttribute
	protected Long start;

	@XmlAttribute
	protected Long end;

	public Long getStart() {
		return this.start;
	}

	public void setStart(Long value) {
		this.start = value;
	}

	public Long getEnd() {
		return this.end;
	}

	public void setEnd(Long value) {
		this.end = value;
	}
}
