### Main process

- EventSourcing is main architecture
- The mobile app provides evetns describing what actions did the user take for a given checklist or template
- TemplateProjection & ChecklistProjection subscribe to those events & store updated data in a PostgreSQL database
- Then, the app is notified via UserDataChangeNotifier which uses Firebase Messaging to fetch new data
- Also, each user is known to be subscribed (or not), so the app won't ask to synchronize for non-paid users

### Other notables

- We use Docker Compose with automatic Github Actions deployment to the DigitalOcean server
- Wa have some manual backup processes described below
- We use nginx reverse proxy with certbot
- We use EventStoreDB in a clustered setup
- We cannot yet fake external dependencies - we have to work on that
- We also haven't run the containers locally - we should update that situation

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
2. Stop the server entirely:
   ```shell
   ssh backend "docker stop checkstory-backend-backend-1"
   ```
3. Drop tables relevant to the projection on Postgres via IntelliJ or using psql.\
   To be able to connect with the database from your computer, use:
      ```shell
      ssh -L 5432:localhost:5432 backend
      ```
4. Set the checkpoint stream to nothing in EventStore
   1. ```shell
      ssh -L 2113:localhost:2113 backend
      ```
   2. Open [localhost:2113](http://localhost:2113/web/index.html#/streams/checkpoint_default)
   3. Click on any event within
   4. Add a new event to stream "checkpoint_default" using "add similar" option with position "0"
5. Fire up the backend again - it should start rebuilding the projection automatically
   ```shell
      ssh backend "docker start checkstory-backend-backend-1"
      ```
6. Monitor the projection rebuild with this command:
   ```shell
   ssh backend "docker logs checkstory-backend-backend-1 --follow --since 10m"
   ```
7. When it's done - no new INSERTS since 1 minute:
   1. Stop maintenance mode per instructions above
