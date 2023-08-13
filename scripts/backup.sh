ssh backend 'docker compose -f ~/checkstory-backend-ktor/docker-compose.yml down'
ssh backend 'tar czf - -C /var/lib/docker/volumes . '> ../checkstory-backup.tar.gz
ssh backend 'docker compose -f ~/checkstory-backend-ktor/docker-compose.yml up -d'
