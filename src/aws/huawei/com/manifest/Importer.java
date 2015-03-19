package aws.huawei.com.manifest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Importer", propOrder = { "name", "version", "release" })
public class Importer {
	@XmlElement(required = true)
	protected String name;

	@XmlElement(required = true)
	protected String version;

	@XmlElement(required = true)
	protected String release;

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String value) {
		this.version = value;
	}

	public String getRelease() {
		return this.release;
	}

	public void setRelease(String value) {
		this.release = value;
	}
}
