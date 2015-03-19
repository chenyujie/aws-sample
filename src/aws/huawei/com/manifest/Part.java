package aws.huawei.com.manifest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Part", propOrder = { "byteRange", "key", "headUrl", "getUrl", "deleteUrl" })
public class Part {
	@XmlElement(name = "byte-range", required = true)
	protected ByteRange byteRange;

	@XmlElement(required = true)
	protected String key;

	@XmlElement(name = "head-url", required = true)
	@XmlSchemaType(name = "anyURI")
	protected String headUrl;

	@XmlElement(name = "get-url", required = true)
	@XmlSchemaType(name = "anyURI")
	protected String getUrl;

	@XmlElement(name = "delete-url")
	@XmlSchemaType(name = "anyURI")
	protected String deleteUrl;

	@XmlAttribute
	protected Integer index;

	public ByteRange getByteRange() {
		return this.byteRange;
	}

	public void setByteRange(ByteRange value) {
		this.byteRange = value;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String value) {
		this.key = value;
	}

	public String getHeadUrl() {
		return this.headUrl;
	}

	public void setHeadUrl(String value) {
		this.headUrl = value;
	}

	public String getGetUrl() {
		return this.getUrl;
	}

	public void setGetUrl(String value) {
		this.getUrl = value;
	}

	public String getDeleteUrl() {
		return this.deleteUrl;
	}

	public void setDeleteUrl(String value) {
		this.deleteUrl = value;
	}

	public Integer getIndex() {
		return this.index;
	}

	public void setIndex(Integer value) {
		this.index = value;
	}
}
