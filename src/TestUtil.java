import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import sun.misc.BASE64Encoder;
import aws.huawei.com.HybridUtil;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.ArchitectureValues;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.CancelConversionTaskRequest;
import com.amazonaws.services.ec2.model.ConversionTask;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.ExportEnvironment;
import com.amazonaws.services.ec2.model.ExportTask;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class TestUtil {
	protected Regions region = Regions.AP_SOUTHEAST_1;
	
	@Test
	public void t000listaz() {
		List<AvailabilityZone> azs = HybridUtil.getConnection(region).describeAvailabilityZones().getAvailabilityZones();
		System.out.printf(">> No of AZs %d\n", azs.size());
		for (AvailabilityZone az : azs) {
			System.out.printf(">>>>  %s:%s-%s\n", az.getZoneName(), az.getState(), az.toString());
		}
		System.out.println();
	}

	@Test
	public void t003ListInstances() {
		List<Reservation> groups = HybridUtil.getConnection(region).describeInstances().getReservations();
		System.out.printf(">> No of instances %d\n", groups.size());
		for (Reservation group : groups) {
			// System.out.println(">>>  " + node.getReservationId());
			for (Instance instance : group.getInstances()) {
				System.out.printf(">>>>  %s:%s\n", instance.getInstanceId(), instance.getState().getName());
			}
		}
		System.out.println();
	}

	@Test
	public void t001bind() {
		AmazonEC2 ec2 = HybridUtil.getConnection(region);
		System.out.println(HybridUtil.bind(ec2,
				HybridUtil.createVm(ec2, null, 1, "ami-e8da80ba", "subnet-839e5be6", "key-b11096cb-5984-4a0e-b41e-a4e3f4e634ec", null).get(0).getInstanceId()));
	}

	@Test
	public void t002format() {
		System.out.println(String.format("%.2f", 12.4512345));
	}

	@Test
	public void t003list() {
		Instance inst = HybridUtil.getInstance(HybridUtil.getConnection(region), "i-ddf24d11");
		System.out.printf("%s: %s-%s", inst.getPlatform(), (inst.getPlatform() != null) ? "Windows" : "Linux", inst);
	}

	@Test
	public void t004createbyname() {
		AmazonEC2 ec2 = HybridUtil.getConnection(region);
		System.out.println(HybridUtil
				.createVm(
						ec2,
						"web",
						1,
						HybridUtil.getImage(ec2, "scaling").get(0).getImageId(),
						HybridUtil.getSubnet(ec2, "chenyujie").get(0).getSubnetId(),
						"key-2b88ae98-2e01-415c-86d5-826affb59f7e",
						new BASE64Encoder()
								.encode("#!/bin/bash -v\nsed -i s/localhost/172.31.89.101/ /var/www/html/wordpress/wp-config.php\nservice apache2 restart"
										.getBytes())).get(0).getInstanceId());
	}

	@Test
	public void t000credential() {
		try {
			HybridUtil.saveCredentials("[default]\naws_access_key_id=AKIAJTDWDULWGLXPL3VQ\naws_secret_access_key=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");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void t006meter() {
		AmazonCloudWatchClient cw = HybridUtil.getMonitor(region);
		System.out.println(HybridUtil.meter(cw, "i-f06f89db", "CPUUtilization", System.currentTimeMillis() - 2 * 60 * 1000, System.currentTimeMillis(), 60));
	}

	@Test
	public void t007listTask() {
		AmazonEC2 ec2 = HybridUtil.getConnection(region);
		List<ConversionTask> tasks = ec2.describeConversionTasks().getConversionTasks();
		for (ConversionTask task : tasks) {
			System.out.printf(">>>> (%9s)%s: %s\n", task.getState(), task.getConversionTaskId(), task);
		}
	}

	@Test
	public void t007cancelTask() {
		AmazonEC2 ec2 = HybridUtil.getConnection(region);
		List<ConversionTask> tasks = ec2.describeConversionTasks().getConversionTasks();
		for (ConversionTask task : tasks) {
			if (task.getState().equals("active")) {
				System.out.printf(">>>> Cancelling %s: %s\n", task.getConversionTaskId(), task);
				ec2.cancelConversionTask(new CancelConversionTaskRequest().withConversionTaskId(task.getConversionTaskId()).withReasonMessage("just cancel"));
			}
		}
	}

	@Test
	public void t008latest() {
		AmazonCloudWatchClient cw = HybridUtil.getMonitor(region);
		List<Datapoint> result = HybridUtil.meter(cw, "i-d336d6f8", "CPUUtilization", System.currentTimeMillis() - 15 * 60 * 1000, System.currentTimeMillis(),
				60);
		for (Datapoint p : result) {
			System.out.printf(">>>> %s:%s\n", p.getTimestamp().toString(), p.getAverage().toString());
		}
		Datapoint p = HybridUtil.latest(result);
		System.out.printf(">>>> latest : %s:%s\n", p.getTimestamp().toString(), p.getAverage().toString());
	}

	@Test
	public void t009import() throws Exception {
		AmazonEC2 ec2 = HybridUtil.getConnection(region);
		AmazonS3Client s3 = HybridUtil.getStorage(region);
		String bucket = "hybridaas";
		String key = "cascading-template-08-disk1.vmdk";
		System.out.printf(">>>> Importing %s/%s\n", bucket, key);
		System.out.println(HybridUtil.migrate(ec2, "ap-southeast-1b", s3, bucket, key, 200));
	}

	@Test
	public void t010import() throws Exception {
		AmazonEC2 ec2 = HybridUtil.getConnection(region);
		AmazonS3Client s3 = HybridUtil.getStorage(region);
		String bucket = "chenyujie-singapore";
		String key = "template-vpn-chenyujie-disk1.vmdk";
		System.out.printf(">>>> Importing %s/%s", bucket, key);
		System.out.printf(">> task id: %s\n", HybridUtil.migrate(ec2, "subnet-944587f1", null, "Linux", InstanceType.T2Micro, ArchitectureValues.X86_64, s3, bucket, key, 8));
	}

	@Test
	public void t011upload() throws Exception {
		AmazonS3Client s3 = HybridUtil.getStorage(region);
		String bucket = "hybridbucket"; // "s3-upload-sdk-sample-" + HybridUtil.getCredentials().getAWSAccessKeyId().toLowerCase();
		if (!s3.doesBucketExist(bucket)) {
			s3.createBucket(bucket);
		}
		File file = new File("E:/clean/kubernetes-disk1.vmdk");
		HybridUtil.upload(s3, bucket, file.getName(), file,  10 * 1024 * 1024);
	}

	@Test
	public void t012listbuckets() {
		AmazonS3Client s3 = HybridUtil.getStorage(region);
		for (Bucket bucket : s3.listBuckets()) {
			String location = s3.getBucketLocation(bucket.getName());
			System.out.printf("- %s(%s)\n", bucket.getName(), location);
			s3.setRegion(Region.fromValue(location).toAWSRegion());
			List<S3ObjectSummary> list = s3.listObjects(bucket.getName()).getObjectSummaries();
			for (S3ObjectSummary item : list) {
				System.out.printf("-- %s(%d bytes)\n", item.getKey(), item.getSize());
			}
		}
	}

	@Test
	public void t013download() throws Exception {
		AmazonS3Client s3 = HybridUtil.getStorage(region);
		String bucket = "chenyujie";
		File file = new File("d:/export.vhd");
		HybridUtil.download(s3, bucket, "inst1/export-i-fh3bnqjb.vhd", file, 3);
	}

	@Test
	public void t014export() {
		String bucket = "new-bucket-739ee590";
		String prefix = "";
		String inst = "i-ddf24d11";

		AmazonS3Client s3 = HybridUtil.getStorage(region);
		HybridUtil.grant(s3, bucket, prefix, "vm-import-export@amazon.com", Permission.ReadAcp, Permission.Write);

		AmazonEC2 ec2 = HybridUtil.getConnection(region);
		System.out.printf(">> task id: %s", HybridUtil.export(ec2, inst, bucket, prefix, ExportEnvironment.Microsoft));
	}
	
	@Test
	public void t015listExport() {
		AmazonEC2 ec2 = HybridUtil.getConnection(region);
		List<ExportTask> tasks = ec2.describeExportTasks().getExportTasks();
		for(ExportTask task:tasks) {
			System.out.printf("%s: (%s)\n", task.getExportTaskId(), task.toString());
		}
	}

	@Test
	public void t119ListImages() {
		AmazonEC2 ec2 = HybridUtil.getConnection(region);
		List<Image> images = ec2.describeImages(new DescribeImagesRequest().withOwners("self")).getImages();
		System.out.printf(">> No of images %d\n", images.size());
		for (Image image : images) {
			System.out.printf(">>>>  %s(%s): %s-%s\n", image.getName(), image.getImageId(), image.getHypervisor(), image.toString());
		}
	}
}
