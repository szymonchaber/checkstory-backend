### Main process

- CQRS is the main architecture
- The mobile app provides commands describing what actions did the user take for a given checklist or template
- The commands are ingested in configureSynchronization and the result is stored in a PostgreSQL database
- Then, the app is notified via UserDataChangeNotifier which uses Firebase Messaging to fetch new data
- Also, each user is known to be subscribed (or not), so the app won't ask to synchronize for non-paid users

### Other notables

- We use Docker Compose with automatic Github Actions deployment to the DigitalOcean server
- Wa have some manual backup processes described below
- We use nginx reverse proxy with certbot

### Environment variables

TODO

### Building & deploying

Local build in preparation for Dockerfile:
```shell
./gradlew app:buildFatJar
```

Push to master & run relevant Github workflow

### Deploying from scratch

https://mindsers.blog/post/https-using-nginx-certbot-docker/
(remember to comment out half of nginx/nginx.conf, then request a certificate, then restart with full nginx config)

The Postgres password cannot contain $ signs because
this https://stackoverflow.com/questions/40619582/how-can-i-escape-a-dollar-sign-in-a-docker-compose-file

### Backing up the data

We should consider buying a remote server to back up to

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
