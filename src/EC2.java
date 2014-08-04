import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.runners.MethodSorters;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DeleteVpcRequest;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Vpc;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EC2 {
	private static AmazonEC2 ec2 = null;
	private static String cidr_vpc = "10.123.0.0/16";
	private static String cidr_sub = "10.123.1.0/24";
	private static String vpcid;
	private static String subnetid;
	private static String sgn = "ec2-sg-" + UUID.randomUUID();
	private static String sgid;
	private static String keyn = "key-" + UUID.randomUUID();
	private static String imageid = "ami-d13845e1";
	private static List<Instance> instances;

	@BeforeClass
	public static void setUpBeforClass() throws Exception {
		if (ec2 != null) {
			return;
		}

		AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();

		ec2 = new AmazonEC2Client(credentials);
		ec2.setRegion(Region.getRegion(Regions.US_WEST_2));

		java.util.List<com.amazonaws.services.ec2.model.Region> regions = ec2.describeRegions().getRegions();
		System.out.printf(">> No of regions %d%n", regions.size());
		for (com.amazonaws.services.ec2.model.Region region : regions) {
			System.out.printf(">>>>  %s:%s\n", region.getRegionName(), region.getEndpoint());
		}
		System.out.println();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void t011ListVPCs() {
		List<Vpc> vpcs = ec2.describeVpcs().getVpcs();
		System.out.printf(">> No of VPCs %d\n", vpcs.size());
		for (Vpc vpc : vpcs) {
			System.out.println(">>>>  " + vpc.getVpcId());
		}
		System.out.println();
	}

	@Test
	public void t012ListSGs() {
		List<SecurityGroup> sgs = ec2.describeSecurityGroups().getSecurityGroups();
		System.out.printf(">> No of SGs %d\n", sgs.size());
		for (SecurityGroup sg : sgs) {
			System.out.printf(">>>>  %s(%s):%s\n", sg.getGroupName(), sg.getGroupId(), sg.getDescription());
		}
		System.out.println();
	}

	@Test
	public void t024ListInstances() {
		List<Reservation> groups = ec2.describeInstances().getReservations();
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
	@Ignore
	public void t119ListImages() {
		List<Image> images = ec2.describeImages().getImages();
		System.out.printf(">> No of images %d\n", images.size());
		for (Image image : images) {
			System.out.printf(">>>>  %s(%s): %s\n", image.getName(), image.getImageId(), image.getHypervisor());
		}
	}

	@Test
	public void t020createVPC() {
		vpcid = ec2.createVpc(new CreateVpcRequest(cidr_vpc)).getVpc().getVpcId();
		System.out.println(String.format("VPC created: [%s]\n", vpcid));
	}

	@Test
	public void t021createSubnet() {
		subnetid = ec2.createSubnet(new CreateSubnetRequest(vpcid, cidr_sub)).getSubnet().getSubnetId();
		System.out.println(String.format("Subnet created: [%s]\n", subnetid));
	}

	@Test
	public void t021createSG() {
		// Create a new security group.
		try {
			CreateSecurityGroupRequest sgr = new CreateSecurityGroupRequest(sgn, "Getting Started Security Group").withVpcId(vpcid);
			sgid = ec2.createSecurityGroup(sgr).getGroupId();
			System.out.println(String.format("Security group created: [%s]", sgid));
		} catch (AmazonServiceException ase) {
			// Likely this means that the group is already created, so ignore.
			System.out.println(ase.getMessage());
		}

		String ipAddr = "0.0.0.0/0";

		// Get the IP of the current host, so that we can limit the Security Group
		// by default to the ip range associated with your subnet.
		try {
			InetAddress addr = InetAddress.getLocalHost();

			// Get IP Address
			ipAddr = addr.getHostAddress() + "/10";
		} catch (UnknownHostException e) {
		}

		// Create a range that you would like to populate.
		List<String> ipRanges = Collections.singletonList(ipAddr);

		// Open up port 23 for TCP traffic to the associated IP from above (e.g. ssh traffic).
		IpPermission ipPermission = new IpPermission().withIpProtocol("tcp").withFromPort(new Integer(22)).withToPort(new Integer(22))
				.withIpRanges(ipRanges);

		List<IpPermission> ipPermissions = Collections.singletonList(ipPermission);

		try {
			// Authorize the ports to the used.
			AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest().withIpPermissions(ipPermissions)
					.withGroupId(sgid);
			ec2.authorizeSecurityGroupIngress(ingressRequest);
			System.out.println(String.format("Ingress port authroized: [%s]", ipPermissions.toString()));
		} catch (AmazonServiceException ase) {
			// Ignore because this likely means the zone has already been authorized.
			System.out.println(ase.getMessage());
		}
		System.out.println();
	}

	@Test
	public void t022createKeyPair() {
		String rsa = ec2.createKeyPair(new CreateKeyPairRequest(keyn)).getKeyPair().getKeyMaterial();
		System.out.println(String.format("Key Pair created: (%s)\n", rsa));
	}

	@Test
	public void t023createInstance() {
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest(imageid, 1, 1).withInstanceType(InstanceType.T2Micro).withKeyName(keyn)
				.withSubnetId(subnetid);
		instances = ec2.runInstances(runInstancesRequest).getReservation().getInstances();
		System.out.println(String.format("Instances created: (%s)\n", instances));
	}

	@Test
	public void t030destroyInstance() {
		List<String> insts = new ArrayList<String>();
		for (Instance inst : instances) {
			insts.add(inst.getInstanceId());
		}
		ec2.terminateInstances(new TerminateInstancesRequest(insts));
		System.out.println(String.format("Instances deleted: (%s)\n", insts));
	}

	@Test
	public void t031deleteKeyPair() {
		ec2.deleteKeyPair(new DeleteKeyPairRequest(keyn));
		System.out.println(String.format("Key Pair deleted: [%s]\n", keyn));
	}

	@Test
	public void t032deleteSG() {
		ec2.deleteSecurityGroup(new DeleteSecurityGroupRequest().withGroupId(sgid));
		System.out.println(String.format("Security Group deleted: (%s)\n", sgid));
	}

	@Test
	public void t033deleteSubnet() throws Exception {
		Thread.sleep(5000);
		ec2.deleteSubnet(new DeleteSubnetRequest(subnetid));
		System.out.println(String.format("Subnet deleted: (%s)\n", subnetid));
	}

	@Test
	public void t034deleteVPC() {
		ec2.deleteVpc(new DeleteVpcRequest(vpcid));
		System.out.println(String.format("VPC deleted: (%s)\n", vpcid));
	}
}
