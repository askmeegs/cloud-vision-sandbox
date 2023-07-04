package com.example.cloudvisionsandbox;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

@RestController
public class CloudVisionSandboxController {

	@GetMapping("/")
	public VisionResponse visionResp() {
		// First, identify the image
		List<AnnotateImageResponse> responses = new ArrayList<>();
		try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {

			// The path to the image file to annotate
			String fileName = "/opt/app/wakeupcat.jpg";

			// Reads the image file into memory
			Path path = Paths.get(fileName);
			byte[] data = Files.readAllBytes(path);

			ByteString imgBytes = ByteString.copyFrom(data);

			// Builds the image annotation request
			List<AnnotateImageRequest> requests = new ArrayList<>();
			Image img = Image.newBuilder().setContent(imgBytes).build();
			Feature feat = Feature.newBuilder().setType(Type.LABEL_DETECTION).build();
			AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
			requests.add(request);

			// Performs label detection on the image file
			BatchAnnotateImagesResponse response = vision.batchAnnotateImages(requests);
			responses = response.getResponsesList();

			for (AnnotateImageResponse res : responses) {
				if (res.hasError()) {
					System.out.format("Error: %s%n", res.getError().getMessage());
					return new VisionResponse("Error: " + res.getError().getMessage());
				}

				for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
					annotation
							.getAllFields()
							.forEach((k, v) -> System.out.format("%s : %s%n", k, v.toString()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new VisionResponse("Error: " + e.getMessage());
		}

		// Then write a timestamp and the annotations to Cloud Firestore
		try {
			FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
					.setProjectId("mokeefe-test-4")
					.setCredentials(GoogleCredentials.getApplicationDefault())
					.build();
			Firestore db = firestoreOptions.getService();
			Map<String, Object> docData = new HashMap<>();

			Map<String, Object> data = new HashMap<>();
			data.put("timestamp", new java.util.Date());
			data.put("annotations", responses.toString());

			ApiFuture<DocumentReference> addedDocRef = db.collection("visionresp").add(data);
			System.out.println("✅ Firestore: added document with ID: " + addedDocRef.get().getId());

		} catch (Exception e) {
			e.printStackTrace();
			return new VisionResponse("Error: " + e.getMessage());
		}
		return new VisionResponse(responses.toString());
	}
}
