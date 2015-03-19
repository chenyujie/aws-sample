package aws.huawei.com.manifest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "version", "fileFormat", "importer", "selfDestructUrl", "_import" })
@XmlRootElement(name = "manifest")
public class Manifest {
	@XmlElement(required = true)
	protected String version;

	@XmlElement(name = "file-format", required = true)
	protected String fileFormat;

	@XmlElement(required = true)
	protected Importer importer;

	@XmlElement(name = "self-destruct-url", required = true)
	@XmlSchemaType(name = "anyURI")
	protected String selfDestructUrl;

	@XmlElement(name = "import", required = true)
	protected Import _import;

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String value) {
		this.version = value;
	}

	public String getFileFormat() {
		return this.fileFormat;
	}

	public void setFileFormat(String value) {
		this.fileFormat = value;
	}

	public Importer getImporter() {
		return this.importer;
	}

	public void setImporter(Importer value) {
		this.importer = value;
	}

	public String getSelfDestructUrl() {
		return this.selfDestructUrl;
	}

	public void setSelfDestructUrl(String value) {
		this.selfDestructUrl = value;
	}

	public Import getImport() {
		return this._import;
	}

	public void setImport(Import value) {
		this._import = value;
	}
}
