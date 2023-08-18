ssh backend 'docker compose -f ~/docker-compose.yml down'
ssh backend 'tar xzf - -C /var/lib/docker/volumes' < ../checkstory-backup.tar.gz
ssh backend 'docker compose -f ~/docker-compose.yml up -d'
