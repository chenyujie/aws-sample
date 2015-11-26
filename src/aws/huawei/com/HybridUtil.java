package aws.huawei.com;

import static com.amazonaws.SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import aws.huawei.com.manifest.ByteRange;
import aws.huawei.com.manifest.Import;
import aws.huawei.com.manifest.Importer;
import aws.huawei.com.manifest.Manifest;
import aws.huawei.com.manifest.Part;
import aws.huawei.com.manifest.Parts;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.regions.ServiceAbbreviations;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.ArchitectureValues;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.CreateInstanceExportTaskRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.DiskImage;
import com.amazonaws.services.ec2.model.DiskImageDetail;
import com.amazonaws.services.ec2.model.ExportEnvironment;
import com.amazonaws.services.ec2.model.ExportToS3TaskSpecification;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.ImportInstanceLaunchSpecification;
import com.amazonaws.services.ec2.model.ImportInstanceRequest;
import com.amazonaws.services.ec2.model.ImportVolumeRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceAssociation;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.VolumeDetail;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.EmailAddressGrantee;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Grantee;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.amazonaws.services.s3.transfer.Upload;

public class HybridUtil {
	static {
		System.setProperty(DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
	}
	
	public static String endpoint(Regions region) {
		return Region.getRegion(region).getServiceEndpoint(ServiceAbbreviations.EC2);
	}
	
	public static AWSCredentials getCredentials() {
		return new ProfileCredentialsProvider().getCredentials();
	}
	
	public static AWSCredentials getCredentials(String access, String secret) {
		return new BasicAWSCredentials(access, secret);
	}
	
	public static void saveCredentials(String credentials) throws IOException {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".aws");
        dir.mkdir();
        File file = new File(dir, "credentials");
        FileWriter writer = new FileWriter(file, false); // append=false
        writer.write(credentials);
        writer.close();
	}
	
	public static ClientConfiguration getConfig() {
		ClientConfiguration cfg = new ClientConfiguration();
		String host = System.getProperty("proxy.host", null);
		if(host != null) {
			cfg.setProxyHost(host);
			cfg.setProxyPort(Integer.parseInt(System.getProperty("proxy.port", "8080")));
	        cfg.setProxyDomain(System.getProperty("proxy.domain", null));
	        cfg.setProxyWorkstation(System.getProperty("proxy.ws", null));
	        cfg.setProxyUsername(System.getProperty("proxy.user", null));
	        cfg.setProxyPassword(System.getProperty("proxy.pass", null));
		}
        return cfg;
	}
	
	public static AmazonEC2 getConnection(Regions region) {
		AmazonEC2 ec2 = new AmazonEC2Client(getCredentials(), getConfig());
		ec2.setRegion(Region.getRegion(region));
		return ec2;
	}
	
	public static List<Image> getImages(AmazonEC2 ec2) {
		return ec2.describeImages(new DescribeImagesRequest().withOwners("self")).getImages();
	}
	
	public static Instance getInstance(AmazonEC2 ec2, String id) {
		List<Reservation> groups = ec2.describeInstances(new DescribeInstancesRequest().withInstanceIds(id)).getReservations();
		return (groups.size()>0)?groups.get(0).getInstances().get(0):null;
	}
	
	public static List<Instance> getInstances(AmazonEC2 ec2, List<String> ids) {
		DescribeInstancesRequest filter = new DescribeInstancesRequest();
		if(ids != null) {
			filter.withInstanceIds(ids);
		}
		List<Reservation> groups = ec2.describeInstances(filter).getReservations();
		List<Instance> insts = new ArrayList<Instance>();
		for (Reservation group : groups) {
			for (Instance instance : group.getInstances()) {
				insts.add(instance);
			}
		}
		return insts;
	}
	
	public static String createVpc(AmazonEC2 ec2, String name, String cidr) {
		String id = ec2.createVpc(new CreateVpcRequest(cidr)).getVpc().getVpcId();
		ec2.createTags(new CreateTagsRequest().withResources(id).withTags(new Tag("Name", name)));
		return id;
	}
	
	public static void rename(AmazonEC2 ec2, String id, String name) {
		try { ec2.deleteTags(new DeleteTagsRequest().withResources(id).withTags(new Tag("Name"))); } catch(AmazonServiceException e) { e.printStackTrace(); }
		ec2.createTags(new CreateTagsRequest().withResources(id).withTags(new Tag("Name", name)));
	}
	
	public static String createSubnet(AmazonEC2 ec2, String name, String vpcid, String subnet) {
		String id = ec2.createSubnet(new CreateSubnetRequest(vpcid, subnet)).getSubnet().getSubnetId();
		ec2.createTags(new CreateTagsRequest().withResources(id).withTags(new Tag("Name", name)));
		return id;
	}
	
	public static String createKeyPair(AmazonEC2 ec2, String keyn) {
		return ec2.createKeyPair(new CreateKeyPairRequest(keyn)).getKeyPair().getKeyMaterial();
	}
	
	public static String bind(AmazonEC2 ec2, String id) {
		String ip = ec2.allocateAddress().getPublicIp();
		DescribeInstanceStatusRequest req = new DescribeInstanceStatusRequest().withInstanceIds(id);
		int count = 10;
		for(;;) {
			try {
				List<InstanceStatus> insts = ec2.describeInstanceStatus(req).getInstanceStatuses();
				if( ((insts.size() > 0 ) && (insts.get(0).getInstanceState().getCode() != 0)) || (--count <= 0) ) {
					break;
				}
				Thread.sleep(1000*10);
			} catch (Exception e) { }
		}
		ec2.associateAddress(new AssociateAddressRequest().withPublicIp(ip).withInstanceId(id));
		return ip;
	}
	
	public static List<Image> getImage(AmazonEC2 ec2, String name) {
		return ec2.describeImages(new DescribeImagesRequest().withFilters(new Filter("tag:Name").withValues(name))).getImages();
	}
	
	public static List<Subnet> getSubnet(AmazonEC2 ec2, String name) {
		return ec2.describeSubnets(new DescribeSubnetsRequest().withFilters(new Filter("tag:Name").withValues(name))).getSubnets();
	}
	
	public static List<Instance> createVm(AmazonEC2 ec2, String prefix, int count, String imageid, String subnetid, String keyn, String userdata) {
		Image img = ec2.describeImages(new DescribeImagesRequest().withImageIds(imageid)).getImages().get(0);
		InstanceType type = (img.getVirtualizationType().equals("hvm"))?InstanceType.T2Micro:InstanceType.T1Micro;
		RunInstancesRequest req = new RunInstancesRequest(imageid, count, count).withInstanceType(type).withKeyName(keyn)
				.withSubnetId(subnetid).withUserData(userdata);
		List<Instance> insts = ec2.runInstances(req).getReservation().getInstances();
		for(Instance inst:insts) {
			String name = (prefix!=null)?(prefix + "-aws-" + (System.currentTimeMillis()%9973)):("aws-" + System.currentTimeMillis());
			ec2.createTags(new CreateTagsRequest().withResources(inst.getInstanceId()).withTags(new Tag("Name", name)));
			inst.getTags().add(new Tag("Name", name));
		}
		return insts;
	}
	
	public static void deleteVm(AmazonEC2 ec2, String id) {
		Instance inst = getInstance(ec2, id);
		try { ec2.deleteKeyPair(new DeleteKeyPairRequest(inst.getKeyName())); } catch(AmazonServiceException e) { e.printStackTrace(); }
		List<InstanceNetworkInterface> interfs = inst.getNetworkInterfaces();
		for(InstanceNetworkInterface interf: interfs) {
			InstanceNetworkInterfaceAssociation assoc = interf.getAssociation();
			if(!assoc.getIpOwnerId().equals("amazon")) {
				try {
					ec2.disassociateAddress(new DisassociateAddressRequest(assoc.getPublicIp()));
					ec2.releaseAddress(new ReleaseAddressRequest(assoc.getPublicIp()));
				} catch(AmazonServiceException e) { e.printStackTrace(); }
			}
		}
		ec2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(id));
	}
	
	public static int nInstances(AmazonEC2 ec2, String vpc) {
		DescribeInstancesRequest filter = new DescribeInstancesRequest();
		if(!vpc.equals("-1")) {
			filter.withFilters(new Filter("vpc-id").withValues(vpc));
		}
		List<Reservation> groups = ec2.describeInstances(filter).getReservations();
		int n = 0;
		for (Reservation group : groups) {
			n += group.getInstances().size();
		}
		return n;
	}
	
	public static AmazonCloudWatchClient getMonitor(Regions region) {
	    AmazonCloudWatchClient cw = new AmazonCloudWatchClient(getCredentials(), getConfig());
	    cw.setRegion(Region.getRegion(region));
	    return cw;
	}
	
	public static List<Datapoint> meter(AmazonCloudWatchClient cw, String id, String metric, long start, long end, int period) {
		Dimension dimension = new Dimension();
		dimension.setName("InstanceId");
		dimension.setValue(id);
		Date startTime = new Date(start);
		Date endTime = new Date(end);
		GetMetricStatisticsRequest meter = new GetMetricStatisticsRequest().withMetricName(metric).withDimensions(dimension)
				.withStartTime(startTime).withEndTime(endTime).withPeriod(period).withStatistics("Average").withNamespace("AWS/EC2");
		return cw.getMetricStatistics(meter).getDatapoints();
	}

	public static Datapoint latest(List<Datapoint> points) {
		java.util.Collections.sort(points, new java.util.Comparator<Datapoint>() {
			public int compare(Datapoint o1, Datapoint o2) {
				if (null == o1 || null == o2) {
					return 0;
				} else if(o1.getTimestamp().after(o2.getTimestamp())) {
					return -1;
				} else if(o1.getTimestamp().before(o2.getTimestamp())) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		return points.get(0);
	}

	public static AmazonS3Client getStorage(Regions region) {
	    return getStorage(Region.getRegion(region));
	}
	
	public static AmazonS3Client getStorage(Region region) {
		AmazonS3Client s3 = new AmazonS3Client(getCredentials(), getConfig());
	    s3.setRegion(region);
	    return s3;
	}
	
	protected static DiskImageDetail createImage(AmazonS3Client s3, String bucket, String key, long volG) throws Exception {
		String mpath = UUID.randomUUID().toString() + "manifest.xml";

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, 7);
		Date expire = cal.getTime();

		Importer importer = new Importer();
		importer.setName("ec2-import");
		importer.setVersion("1.0.0");
		importer.setRelease("2014-10-15");

		String format = "RAW";
		String lowcase = key.toLowerCase();
		if(lowcase.endsWith("vmdk")) {
			format = "VMDK";
		} else if(lowcase.endsWith("vhd")) {
			format = "VHD";
		}

		Manifest manifest = new Manifest();
		manifest.setVersion("2014-10-15");
		manifest.setFileFormat(format);
		manifest.setImporter(importer);
		manifest.setSelfDestructUrl(s3.generatePresignedUrl(bucket, mpath, expire, HttpMethod.DELETE).toString());

		Parts parts = new Parts();
		parts.setCount(1);

		Part part = new Part();
		parts.getPart().add(part);
		part.setIndex(0);

		long bytes = s3.getObject(new GetObjectRequest(bucket, key)).getObjectMetadata().getContentLength();
		ByteRange byteRange = new ByteRange();
		byteRange.setStart(0L);
		byteRange.setEnd(bytes - 1L);
		part.setByteRange(byteRange);

		part.setKey(key);
		part.setHeadUrl(s3.generatePresignedUrl(bucket, key, expire, HttpMethod.HEAD).toString());
		part.setGetUrl(s3.generatePresignedUrl(bucket, key, expire, HttpMethod.GET).toString());

		Import imp = new Import();
		imp.setSize(bytes);
		imp.setVolumeSize(volG);
		imp.setParts(parts);

		manifest.setImport(imp);

		byte[] m = toXML(manifest).getBytes();		
		Upload upload = HybridUtil.upload(s3, bucket, mpath, new ByteArrayInputStream(m), m.length);

		URL manifestURL = s3.generatePresignedUrl(bucket, mpath, expire);

		upload.waitForCompletion();

		return new DiskImageDetail().withImportManifestUrl(manifestURL.toString()).withFormat(format).withBytes(bytes);
	}

	protected static String toXML(Object obj) throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Marshaller marshaller = null;
		JAXBContext context = null;
		Map<String, JAXBContext> map = new ConcurrentHashMap<String, JAXBContext>();
		Class<?> clazz = obj.getClass();
		context = map.get(clazz.toString());
		if (context == null) {
			context = JAXBContext.newInstance(clazz);
			map.put(clazz.toString(), context);
		}
		marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.marshal(obj, os);
		return os.toString();
	}
	
	public static String migrate(AmazonEC2 ec2, String az, AmazonS3Client s3, String bucket, String key, long size) throws Exception {
		DiskImageDetail image = HybridUtil.createImage(s3, bucket, key, size);
		ImportVolumeRequest req = new ImportVolumeRequest().withImage(image).withVolume(new VolumeDetail().withSize(size)).withAvailabilityZone(az);
		return ec2.importVolume(req).getConversionTask().getConversionTaskId();
	}
	
	public static String migrate(AmazonEC2 ec2, String subnet, String sg, String platform, InstanceType type, ArchitectureValues arch, AmazonS3Client s3, String bucket, String key, long size) throws Exception {
		DiskImageDetail image = HybridUtil.createImage(s3, bucket, key, size);
		DiskImage diskImages = new DiskImage().withImage(image).withVolume(new VolumeDetail().withSize(size));
		ImportInstanceLaunchSpecification spec = new ImportInstanceLaunchSpecification().withInstanceType(type)
				.withArchitecture(arch).withMonitoring(false).withSubnetId(subnet).withGroupNames(sg);
		ImportInstanceRequest req = new ImportInstanceRequest().withDiskImages(diskImages).withPlatform(platform).withLaunchSpecification(spec);
		return ec2.importInstance(req).getConversionTask().getConversionTaskId();
	}
	
	public static String export(AmazonEC2 ec2, String inst, String bucket, String prefix, ExportEnvironment target) {
		ExportToS3TaskSpecification exportToS3Task = new ExportToS3TaskSpecification().withS3Bucket(bucket);
		if(prefix != null) {
			exportToS3Task.setS3Prefix(prefix);
		}
		CreateInstanceExportTaskRequest req = new CreateInstanceExportTaskRequest().withExportToS3Task(exportToS3Task).withInstanceId(inst);
		if(target != null) {
			req.setTargetEnvironment(target);
		} else {
			req.setTargetEnvironment(ExportEnvironment.Vmware);
		}
		return ec2.createInstanceExportTask(req).getExportTask().getExportTaskId();
	}

	protected static Map<AmazonS3Client, TransferManager> transfers = new HashMap<AmazonS3Client, TransferManager>();
	
	public static synchronized TransferManager getTransfer(AmazonS3Client s3) {
		TransferManager mgr = transfers.get(s3);
		if(mgr == null) {
			mgr = new TransferManager(s3);
			transfers.put(s3, mgr);
		}
		return mgr;
	}

	public static Upload upload(AmazonS3Client s3, String bucket, String path, File file) {
		return getTransfer(s3).upload(new PutObjectRequest(bucket, path + "/" + file.getName(), file));
	}

	public static Upload upload(AmazonS3Client s3, String bucket, String key, InputStream is, long len) {
		ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(len);
		PutObjectRequest request = new PutObjectRequest(bucket, key, is, metadata);
		return getTransfer(s3).upload(request);
	}
	
	public static void upload(AmazonS3Client s3, String bucket, String path, File file, long piece) {
		TransferManagerConfiguration cfg = new TransferManagerConfiguration();
		cfg.setMinimumUploadPartSize(piece);
		cfg.setMultipartUploadThreshold(piece);
		TransferManager mgr = getTransfer(s3);
		mgr.setConfiguration(cfg);
		Upload upload = mgr.upload(bucket, path + "/" + file.getName(), file);
		try {
			upload.waitForCompletion();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void download(AmazonS3Client s3, String bucket, String key, File out, int retries) throws Exception {
		long len = s3.getObjectMetadata(bucket, key).getContentLength();
		byte[] b = new byte[1024*1024];
		OutputStream os = new FileOutputStream(out, false);
		File file = File.createTempFile("tmp", null);
		for(long pos=0; pos<len;) {
			GetObjectRequest req = new GetObjectRequest(bucket, key).withRange(pos, (pos+1024*1024>len)?(len-1):(pos+1024*1024-1));
			for(int loop=0; loop<retries; loop++) try {
				Download download = getTransfer(s3).download(req, file);
				download.waitForCompletion();
				FileInputStream is = new FileInputStream(file);
				int n = is.read(b);
				pos += n;
				os.write(b, 0, n);
				is.close();
				break;
			} catch(Exception e) { }
		}
		os.flush();
		os.close();
		file.delete();
	}
	
	public static Download download(AmazonS3Client s3, String bucket, String key, File out) {
		return getTransfer(s3).download(bucket, key, out);
	}
	
	public static void grant(AmazonS3Client s3, String bucket, String prefix, String mail, Permission... permissions) {
		AccessControlList acl = s3.getBucketAcl(bucket);
		Grantee grantee = new EmailAddressGrantee(mail);
		for(Permission permission : permissions) {
			acl.grantPermission(grantee, permission);
		}
		if(prefix != null) {
			s3.setObjectAcl(bucket, prefix, acl);
		} else {
			s3.setBucketAcl(bucket, acl);
		}
	}
}
