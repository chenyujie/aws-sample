import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class S3 {
	private static AmazonS3 s3 = null;
	private static String bucketName = "s3-bucket-" + UUID.randomUUID();
	private static String key = "MyObjectKey";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if(s3 != null) {
			return;
		}
		
		System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");

		AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();

        s3 = new AmazonS3Client(credentials);
        s3.setRegion(Region.getRegion(Regions.US_WEST_2));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		try {
			for(S3ObjectSummary obj : s3.listObjects(bucketName).getObjectSummaries()) {
		        s3.deleteObject(obj.getBucketName(), obj.getKey());
			}
			s3.deleteBucket(bucketName);
		} catch (Exception e) {
			System.out.printf("Warning: bucket %s unavailable when destroy class\n", bucketName);
		}
	}

	@Test
	public void t010CreateBucket() {
        /*
         * Create a new S3 bucket - Amazon S3 bucket names are globally unique,
         * so once a bucket name has been taken by any user, you can't create
         * another bucket with that same name.
         *
         * You can optionally specify a location for your bucket if you want to
         * keep your data closer to your applications or users.
         */
        System.out.println("Creating bucket " + bucketName + "\n");
        
        assertNotNull(s3.createBucket(bucketName));
	}

	@Test
	public void t011ListBucket() {
        /*
         * List the buckets in your account
         */
		boolean found = false;
        System.out.println("Listing buckets");
        for (Bucket bucket : s3.listBuckets()) {
            System.out.println(" - " + bucket.getName());
            if(bucket.getName().contentEquals(bucketName)) {
            	found = true;
            }
        }
        System.out.println();
        org.junit.Assert.assertTrue(found);
	}

	@Test
	public void t012UploadFile() throws Exception {
	    /*
	     * Upload an object to your bucket - You can easily upload a file to
	     * S3, or upload directly an InputStream if you know the length of
	     * the data in the stream. You can also specify your own metadata
	     * when uploading to S3, which allows you set a variety of options
	     * like content-type and content-encoding, plus additional metadata
	     * specific to your applications.
	     */
	    System.out.println("Uploading a new object to S3 from a file\n");
	    s3.putObject(new PutObjectRequest(bucketName, key, createSampleFile()));
	}
	
	@Test
	public void t013ListFile() {
        /*
         * List objects in your bucket by prefix - There are many options for
         * listing the objects in your bucket.  Keep in mind that buckets with
         * many objects might truncate their results when listing their objects,
         * so be sure to check if the returned object listing is truncated, and
         * use the AmazonS3.listNextBatchOfObjects(...) operation to retrieve
         * additional results.
         */
        System.out.println("Listing objects");
        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix("My"));
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
            System.out.println(" - " + objectSummary.getKey() + "  " + "(size = " + objectSummary.getSize() + ")");
        }
        System.out.println();
	}
	
	@Test
	public void t014DownloadFile() throws Exception {
        /*
         * Download an object - When you download an object, you get all of
         * the object's metadata and a stream from which to read the contents.
         * It's important to read the contents of the stream as quickly as
         * possibly since the data is streamed directly from Amazon S3 and your
         * network connection will remain open until you read all the data or
         * close the input stream.
         *
         * GetObjectRequest also supports several other options, including
         * conditional downloading of objects based on modification times,
         * ETags, and selectively downloading a range of an object.
         */
        System.out.println("Downloading an object");
        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
        System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType());
        displayTextInputStream(object.getObjectContent());
	}
	
	@Test
	public void t015DeleteFile() {
        /*
         * Delete an object - Unless versioning has been turned on for your bucket,
         * there is no way to undelete an object, so use caution when deleting objects.
         */
        System.out.println("Deleting an object\n");
        s3.deleteObject(bucketName, key);
    }
	
	@Test
	public void t016DeleteBucket() {
        /*
         * Delete a bucket - A bucket must be completely empty before it can be
         * deleted, so remember to delete any objects from your buckets before
         * you try to delete them.
         */
        System.out.println("Deleting bucket " + bucketName + "\n");
        s3.deleteBucket(bucketName);
	}


	/**
     * Creates a temporary file with text data to demonstrate uploading a file
     * to Amazon S3
     *
     * @return A newly created temporary file with text data.
     *
     * @throws IOException
     */
    private static File createSampleFile() throws IOException {
        File file = File.createTempFile("aws-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("01234567890112345678901234\n");
        writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
        writer.write("01234567890112345678901234\n");
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.close();

        return file;
    }

    /**
     * Displays the contents of the specified input stream as text.
     *
     * @param input
     *            The input stream to display as text.
     *
     * @throws IOException
     */
    private static void displayTextInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null) break;

            System.out.println("    " + line);
        }
        System.out.println();
    }
}