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
import java.util.UUID;

import io.grpc.Context.Storage;
import org.threeten.bp.Duration;

import java.io.IOException;
import java.util.concurrent.Callable;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
// import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import com.google.cloud.aiplatform.v1beta1.EndpointName;
import com.google.cloud.aiplatform.v1beta1.PredictResponse;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class CloudVisionSandboxController {
	@GetMapping("/imagenclient")
	public VisionResponse imagenClientResp() {
		try {
			String instance = "{ \"prompt\": " + "\"rocky road cookies\"}";
			String parameters = "{\n"
					+ " \"temperature\": 0.2,\n"
					+ " \"maxOutputTokens\": 1024,\n"
					+ " \"topP\": 0.95,\n"
					+ " \"topK\": 40\n"
					+ "}";
			// String parameters = "{}";
			String project = "mokeefe-test-4";
			String location = "us-central1";
			String publisher = "google";
			String model = "imagegeneration";

			// String rawEndpoint =
			// "https://us-central1-aiplatform.googleapis.com/v1/projects/mokeefe-test-4/locations/us-central1/publishers/google/models/imagegeneration:predict";
			String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
			// create predictionServiceSettings and set retry settings to 60 seconds
			PredictionServiceSettings.Builder predictionServiceSettingsBuilder = PredictionServiceSettings.newBuilder();
			predictionServiceSettingsBuilder
					.predictSettings()
					.setRetrySettings(
							predictionServiceSettingsBuilder
									.predictSettings()
									.getRetrySettings()
									.toBuilder()
									.setTotalTimeout(Duration.ofSeconds(30))
									.build());
			PredictionServiceSettings predictionServiceSettings = predictionServiceSettingsBuilder.setEndpoint(endpoint)
					.build();

			// Initialize client that will be used to send requests. This client only needs
			// to be created
			// once, and can be reused for multiple requests.
			PredictionServiceClient predictionServiceClient = PredictionServiceClient.create(predictionServiceSettings);
			final EndpointName endpointName = EndpointName.ofProjectLocationPublisherModelName(project, location,
					publisher, model);

			// Initialize client that will be used to send requests. This client only needs
			// to be created
			// once, and can be reused for multiple requests.
			Value.Builder instanceValue = Value.newBuilder();
			JsonFormat.parser().merge(instance, instanceValue);
			List<Value> instances = new ArrayList<>();
			instances.add(instanceValue.build());

			// Use Value.Builder to convert instance to a dynamically typed value that can
			// be
			// processed by the service.
			Value.Builder parameterValueBuilder = Value.newBuilder();
			JsonFormat.parser().merge(parameters, parameterValueBuilder);
			Value parameterValue = parameterValueBuilder.build();

			PredictResponse predictResponse = predictionServiceClient.predict(endpointName, instances, parameterValue);
			// how many predictions in predictResponse?
			// print length of predictions 
			System.out.println("🍓 PREDICTIONS LENGTH: " + predictResponse.getPredictionsCount());



			predictResponse.getPredictionsList().forEach(prediction -> {
				// increment i 
				String index = UUID.randomUUID().toString(); 
				// get first 6 chars of uuid 
				index = index.substring(0, 6);
				// print prediction
				System.out.println("🍓 NEW PREDICTION BELOW");
				System.out.println(prediction);
				// get field bytesBase64Encoded string from prediction
				String bytesBase64Encoded = prediction.getStructValue().getFieldsMap().get("bytesBase64Encoded")
						.getStringValue();
				try {
					base64ToImage(bytesBase64Encoded, "image-" + index + ".png");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new VisionResponse("imagen client response here");
	}

	@GetMapping("/imagen")
	public VisionResponse imagenResp() {
		try {
			// To get token info:
			// curl "https://oauth2.googleapis.com/tokeninfo?access_token=$TOKEN"

			// docker run -p 8080:8080 -v ./imagen.json:/tmp/imagen.json hello:latest

			String accessToken = "placeholder";
			try {
				runBashCommand("gcloud auth activate-service-account --key-file=/tmp/imagen.json");
				accessToken = runBashCommand(
						"gcloud auth print-access-token --impersonate-service-account=imagen@mokeefe-test-4.iam.gserviceaccount.com");
			} catch (Exception e) {
				e.printStackTrace();
			}
			// strip trailing and leading whitespace from accesstoken
			accessToken = accessToken.trim();
			System.out.println("⭐️ accessToken: " + accessToken);

			// gcloud auth activate-service-account --key-file=imagen.json
			// gcloud auth print-access-token
			// --impersonate-service-account=imagen@mokeefe-test-4.iam.gserviceaccount.com

			// String hardcodedToken =
			// "ya29.c.b0Aaekm1IS__WfN8H7lTLXLeCRFDJtS1uuOM4vl_HoF_ju2Fb16ldomKRcQxbEgi1H16hBmooO7TJvjeN5yc2QfqkjdJvdU66LaOluZSpCsD2ASYstZgAFuTrblDEQb2YYTAU_D3i5U7TY5ptXgephEawAxOUS4iwo7famA_7n3CVD_Pt5Ai20l1I4X55iZAtuJTZVzrqSDwB3koW0z_RflHhI6ftfmsoAky3uJdSJGGtNFE6i-PI_rnuI0v1WOKGW-fzSX4jrZgVGzuRJ0AWVwtx79YFgUTPNuZbfrVCVpm3tPe2sWoKBaLYQcPgMeki1wzSLZEOQlZmu7SKWnb9JLS-o96-jX2ilzllN3gPnkp7skdo1vPQCTC-I71anl7aDSN7X9bRl2RAdInTSQZs9UHsKydfurxY_PgHrxVp-iPE-b5FaBh2I0TvlKE_V8kwelzvkM74Vyb0gSYbvjy5X3gOUNN_9E8q2OQAsZ4nE3EkA2igJTl5n4DSnIkzwQ4Q00Ci0FWSll-6bVFY9-BHhgXTFSB-LJwbOt1NCtTApIh-qGu3awgmVhLjkKZV5yXNcG585KkWJhXZa8Mz4X5zfg1kFXR_s6721I4pj8JmOgn3sJth8crXZc3_Is8pX2_3FUfl-7yZeBboubd9nrizfem8zUUqy0WzgVBVpdxRiBl8Zafyz43lQqQsSO0Uazjehn79uMWIYqjtF_kIWiz972xWsiIgs3kud0j5opkiBc3R85UbI7ja5FJYYOpJoI3nSWckrh--2m5gIwl8sWhsptzz-hFi4aeue_yfd1dZSUSQRzz75O5O05lQ5s4O7Ut467OmVX-yYtQMp5UXaM32_bUiF5xnOXgBkua6oXof1qS2dI7FX4qXy1ViJ6-jBVUmcFs01hbQyRgadJ25FhpYY0bmQYu8lf0VyfRyOqYo6MerJbz6jRQYo-xk6hzvXItX7YksXzvjswBaJWSm2wzuhg4oUdUmZF4c9f2v5zOIopuBdMOnq5rdqhbM";

			// GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new
			// FileInputStream("imagen.json"))
			// .createScoped(
			// "https://www.googleapis.com/auth/cloud-platform",
			// "https://www.googleapis.com/auth/sqlservice.login",
			// "https://www.googleapis.com/auth/compute",
			// "https://www.googleapis.com/auth/appengine.admin",
			// "https://www.googleapis.com/auth/userinfo.email", "openid");
			// credentials.refresh();
			// System.out.println("⭐️ credentials: " + credentials);
			// AccessToken accessToken = credentials.getAccessToken();
			// // print accessToken
			// System.out.println("🎄 accessToken: " + accessToken.getTokenValue());

			// 🏁 END AUTH 🏁 ----------------------------------------
			// Create the HTTP connection.
			String url = "https://us-central1-aiplatform.googleapis.com/v1/projects/mokeefe-test-4/locations/us-central1/publishers/google/models/imagegeneration:predict";

			String authorizationHeader = "Bearer " + accessToken;
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Authorization", authorizationHeader);
			connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			String requestBody = getRequestBody("rocky road cookie ice cream sandwich");

			// Send the request.
			connection.setDoOutput(true);
			byte[] requestBytes = requestBody.getBytes("UTF-8");
			connection.getOutputStream().write(requestBytes);

			// Get the response.
			int responseCode = connection.getResponseCode();
			String responseMessage = connection.getResponseMessage();
			StringBuilder responseBody = new StringBuilder();
			if (responseCode == 200) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(connection.getInputStream()));
				while (reader.read() != -1) {
					responseBody.append(reader.readLine());
				}
			}

			processResponseBody(responseBody.toString());

			// Print the response.
			System.out.println("Response code: " + responseCode);
			System.out.println("Response message: " + responseMessage);
			System.out.println("Response body: " + responseBody);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new VisionResponse("imagen response here");
	}

	private static String runBashCommand(String cmd) throws IOException, InterruptedException {
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(cmd);
		// pipe output and return
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(System.getProperty("line.separator"));
		}
		return builder.toString();
	}

	private static String getRequestBody(String prompt) throws JsonProcessingException {
		// Create the request body.
		ObjectMapper objectMapper = new ObjectMapper();

		// Create a JSON object
		ObjectNode requestBodyNode = objectMapper.createObjectNode();

		ArrayNode instances = requestBodyNode.putArray("instances");
		ObjectNode parameters = requestBodyNode.putObject("parameters");

		ObjectNode promptNode = objectMapper.createObjectNode();
		promptNode.put("prompt", prompt);
		instances.add(promptNode);

		parameters.put("sampleCount", 4);

		// Convert the JSON object to a JSON string
		return objectMapper.writeValueAsString(requestBodyNode);
	}

	public static void processResponseBody(String response) throws IOException {
		JsonNode productNode = new ObjectMapper().readTree("{" + response + "}");
		JsonNode predictions = productNode.path("predictions");

		for (int i = 0; i < predictions.size(); i++) {
			JsonNode encodedImage = predictions.get(i).path("bytesBase64Encoded");
			base64ToImage(encodedImage.textValue(), "image-" + i);
		}
	}

	private static void base64ToImage(String base64String, String fileName) throws IOException {
		byte[] decodedBytes = Base64.getDecoder().decode(base64String);
		// File imageFile = new File("image.png");
		String imageFile = fileName + ".png";
		try (OutputStream outputStream = new FileOutputStream(imageFile)) {
			outputStream.write(decodedBytes);
		}
	}

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
