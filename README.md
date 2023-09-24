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

### Putting the server in maintenance mode:

```shell
ssh backend "touch ~/nginx/maintenance.enable"
```

### Putting the server out of maintenance mode:

```shell
ssh backend "rm ~/nginx/maintenance.enable"
```

### Rebuilding a projection:

1. Put the server in maintenance mode per instructions above
2. We should kill the backend entirely
3. Drop tables relevant to the projection on Postgres
4. Set the checkpoint stream to nothing in EventStore
5. Fire up the backend again - it should start rebuilding the projection automatically
6. When it's done (how do we tell?), stop maintenance mode per instructions above
