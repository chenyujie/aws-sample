package aws.huawei.com.manifest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Import", propOrder = { "size", "volumeSize", "parts" })
public class Import {
	protected long size;

	@XmlElement(name = "volume-size")
	protected long volumeSize;

	@XmlElement(required = true)
	protected Parts parts;

	public long getSize() {
		return this.size;
	}

	public void setSize(long value) {
		this.size = value;
	}

	public long getVolumeSize() {
		return this.volumeSize;
	}

	public void setVolumeSize(long value) {
		this.volumeSize = value;
	}

	public Parts getParts() {
		return this.parts;
	}

	public void setParts(Parts value) {
		this.parts = value;
	}
}
