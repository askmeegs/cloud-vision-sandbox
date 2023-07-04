# cloud-vision-sandbox 

Java + Cloud Vision demo for Cloud Run 


Sources:
- https://cloud.google.com/vision/docs/detect-labels-image-client-libraries
- https://cloud.google.com/docs/authentication/provide-credentials-adc#local-dev (using service account)
- https://spring.io/guides/tutorials/rest/
- https://cloud.google.com/firestore/docs/create-database-server-client-library
- https://cloud.google.com/firestore/docs/manage-data/add-data#java


Running locally:

```
export GOOGLE_APPLICATION_CREDENTIALS="/Users/mokeefe/dev/cloud-vision-sandbox/src/main/resources/static/sa-key.json"
./mvnw spring-boot:run
```


Building + pushing container image to AR: 
```
export DOCKER_DEFAULT_PLATFORM=linux/amd64
export REPO="us-central1-docker.pkg.dev/mokeefe-test-4/visiondemo/image-identifier"
export TAG="v0.0.2"
docker build -t $REPO:$TAG .
docker push $REPO:$TAG
```