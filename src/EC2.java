import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;


public class EC2 {
	private static AmazonS3 s3 = null;
	private static String bucketName = null;

	@BeforeClass
	public void setUp() throws Exception {
		if(s3 != null) {
			return;
		}

		AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        s3 = new AmazonS3Client(credentials);
        s3.setRegion(Region.getRegion(Regions.US_WEST_2));

        bucketName = "my-first-s3-bucket-" + UUID.randomUUID();
	}

	@AfterClass
	public static void tearDown() throws Exception {
        /*
         * Delete a bucket - A bucket must be completely empty before it can be
         * deleted, so remember to delete any objects from your buckets before
         * you try to delete them.
         */
        System.out.println("Deleting bucket " + bucketName + "\n");
        s3.deleteBucket(bucketName);
	}

	@Test
	public void AllTest() {
		fail();
	}

	@Test
	public void ListBucket() {
        /*
         * List the buckets in your account
         */
        System.out.println("Listing buckets");
        for (Bucket bucket : s3.listBuckets()) {
            System.out.println(" - " + bucket.getName());
        }
        System.out.println();
	}
}
