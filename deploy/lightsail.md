# Sentry op AWS Lightsail

Doel: één 8 GB-instance in eu-west-1, Docker Compose (Portal + PostgreSQL + Caddy). SNS blijft in het bestaande AWS-account. Geen DynamoDB, geen Elastic Beanstalk.

1. Lightsail Linux 8 GB, regio `eu-west-1`.
2. Docker + Compose installeren.
3. Repo clonen, `Sentry__Sns__TopicArn` en DNS (`stubbe.`, `stt.`, `weerts.`) zetten.
4. `docker compose up -d --build`.
5. TLS via Caddy of Lightsail-loadbalancer.
6. Collector in de fabriek: alleen outbound HTTPS, API-key per site, geen IAM-keys in de service.

Deze map bevat geen live credentials. Productienaam Sentry is tijdelijk.
