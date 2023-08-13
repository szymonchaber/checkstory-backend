### Building & deploying

Build Docker image for the backend service & push to the artifact store:

```shell
./gradlew buildFatJar

docker build --platform=linux/amd64 -t us-central1-docker.pkg.dev/checkstory-940fc/checkstory-images/checkstory-backend .
docker push us-central1-docker.pkg.dev/checkstory-940fc/checkstory-images/checkstory-backend
```

Copy the docker-compose to the backend server
There, run

```shell
docker compose up -d
```

### Deploying from scratch

https://mindsers.blog/post/https-using-nginx-certbot-docker/
(remember to comment out half of nginx/nginx.conf, then request a certificate, then restart with full nginx config)

The Postgres password cannot contain $ signs because
this https://stackoverflow.com/questions/40619582/how-can-i-escape-a-dollar-sign-in-a-docker-compose-file

### Backing up the data

We should consider buying a remote server to backup to

```shell
./scripts/backup.sh
```

### Restoring the backup

```shell
./scripts/restore_backup.sh
```
