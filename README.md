# M321 Order Service

Spring-Boot-Service zur Demonstration von synchronem REST, asynchroner
Verarbeitung mit RabbitMQ sowie IAM, OAuth 2.0 und OpenID Connect mit Keycloak.
Ein autorisierter Client erstellt eine Bestellung und erhält sofort
`202 Accepted`; die Verarbeitung läuft danach unabhängig über RabbitMQ.

## Architektur

```text
User / Client
     |
     | Login (OIDC) / Access Token (OAuth2)
     v
Keycloak :8080  ---- Rollen USER / ADMIN
     |
     | signiertes JWT im Authorization-Header
     v
Spring Boot Order Service :8081
     |
     | autorisierte Bestellung
     v
orders.processing (RabbitMQ Work Queue / Round Robin)
     |
  +--+--+
  |     |
Worker1 Worker2
  |     |
  +--+--+
     v
orders.processed (Fanout Exchange)
     |
  +--+-----------------+
  |                    |
NotificationConsumer   AuditConsumer
```

OAuth2/OIDC schützt den externen REST-Zugriff. Zwischen Order Service und
RabbitMQ wird für diese Demo weiterhin die vorhandene AMQP-Verbindung verwendet;
das Client-JWT wird nicht als RabbitMQ-Nachricht weitergereicht.

## IAM, OAuth2, OIDC und JWT

### IAM

Identity and Access Management verwaltet digitale Identitäten und deren
Berechtigungen. Keycloak ist hier das lokale IAM-System beziehungsweise der
Identity Provider. Es verwaltet Benutzer, Passwörter und Rollen, authentifiziert
Benutzer und stellt signierte Tokens aus.

### OAuth 2.0

OAuth2 behandelt **Authorization**, also den delegierten Zugriff auf eine
Ressource. Der Client erhält von Keycloak ein Access Token und sendet es bei
jeder API-Anfrage als `Authorization: Bearer <token>`. Der Order Service ist ein
OAuth2 Resource Server und akzeptiert nur ein gültiges Token des konfigurierten
Issuers.

### OpenID Connect

OIDC ergänzt OAuth2 um **Authentication**, also die Identität des angemeldeten
Benutzers. Der Keycloak-Client verwendet das Protokoll `openid-connect`; über
Claims wie `sub` und `preferred_username` kann die API Subject und Username
auslesen. `GET /api/admin` gibt diese beiden Claims zur Demonstration zurück.

Kurz gesagt:

- Authentication: Wer bist du?
- Authorization: Was darfst du?
- Keycloak: IAM-System und Identity Provider.
- OAuth2: regelt den API-Zugriff.
- OIDC: ergänzt die Benutzeridentität auf Basis von OAuth2.
- JWT: signiertes Token, das der Client an die API sendet.

### JWT-Validierung

Das Access Token enthält unter anderem Issuer (`iss`), Subject (`sub`),
Username, Ablaufzeit und unter `realm_access.roles` die Keycloak-Realm-Rollen.
Spring Security lädt die öffentlichen Schlüssel vom konfigurierten JWK-Endpunkt
und prüft Signatur, Ablaufzeit und Issuer. Der private Signaturschlüssel bleibt
in Keycloak.

`SecurityConfig` wandelt Keycloak-Rollen um:

```text
realm_access.roles: ["USER"]  -> ROLE_USER
realm_access.roles: ["ADMIN"] -> ROLE_ADMIN
```

Es werden keine veralteten Keycloak-Spring-Adapter verwendet.

## Rollen und geschützte Endpoints

| Endpoint | Zugriff | Ergebnis bei Erfolg |
|---|---|---|
| `POST /api/orders` | `USER` oder `ADMIN` | `202 Accepted` |
| `GET /api/admin` | nur `ADMIN` | `200 OK` |
| `POST /api/consumers/start` | gültiges Token | `200 OK` |
| `POST /api/consumers/stop` | gültiges Token | `200 OK` |
| `/swagger-ui/**`, `/v3/api-docs/**` | öffentlich | Dokumentation |

Ohne oder mit ungültigem Token antwortet eine geschützte Route mit `401`. Ein
gültig authentifizierter Benutzer ohne erforderliche Rolle erhält `403`.

## Lokale Development-Konfiguration

Die Datei `docker-compose.yml` startet RabbitMQ und Keycloak. Beim ersten Start
importiert Keycloak automatisch `keycloak/m321-realm.json` mit:

- Realm: `m321`
- Client: `m321-order-client` (public client, Authorization Code + PKCE)
- Rollen: `USER`, `ADMIN`
- Benutzer `demo-user` / `user123` mit `USER`
- Benutzer `demo-admin` / `admin123` mit `ADMIN`
- Keycloak-Administrator `admin` / `admin`

Diese Passwörter sind absichtlich einfache **Development-Defaults für die
Schul-Demo** und nicht für Produktion geeignet. Admin-Zugangsdaten lassen sich
mit `KEYCLOAK_ADMIN_USERNAME` und `KEYCLOAK_ADMIN_PASSWORD` überschreiben.

Spring-Einstellungen in `application.properties` können ebenfalls über
Umgebungsvariablen überschrieben werden:

```properties
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/m321
KEYCLOAK_JWK_SET_URI=http://localhost:8080/realms/m321/protocol/openid-connect/certs
KEYCLOAK_CLIENT_ID=m321-order-client
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

## Start und URLs

Docker Desktop starten und im Projektverzeichnis ausführen:

```powershell
docker compose up -d
docker compose ps
```

Danach die Anwendung starten:

```powershell
.\mvnw.cmd spring-boot:run
```

Wichtige URLs:

- Keycloak Admin UI: http://localhost:8080/admin/
- Realm Discovery: http://localhost:8080/realms/m321/.well-known/openid-configuration
- API: http://localhost:8081
- Swagger UI: http://localhost:8081/swagger-ui.html
- generiertes OpenAPI: http://localhost:8081/v3/api-docs
- RabbitMQ Management UI: http://localhost:15672 (`guest` / `guest`)
- RabbitMQ AMQP: `localhost:5672`

Keycloak importiert ein Realm nur, wenn es noch nicht existiert. Änderungen an
der JSON-Datei können alternativ in der Admin UI nachvollzogen oder nach dem
bewussten Löschen des Keycloak-Development-Volumes frisch importiert werden.
Das RabbitMQ-Volume sollte dabei erhalten bleiben.

## Keycloak in der Admin UI nachvollziehen

Die Konfiguration ist bereits automatisiert, kann für die Präsentation aber
Schritt für Schritt gezeigt werden:

1. http://localhost:8080/admin/ öffnen und mit `admin` / `admin` anmelden.
2. Oben links vom `master` Realm zum Realm `m321` wechseln.
3. Unter **Clients** den Client `m321-order-client` öffnen.
4. Prüfen: Client authentication aus, Standard flow und Direct access grants an.
5. Unter **Realm roles** die Rollen `USER` und `ADMIN` zeigen.
6. Unter **Users** `demo-user` öffnen und unter Role mapping `USER` prüfen.
7. `demo-admin` öffnen und die Rolle `ADMIN` prüfen.

Für eine manuelle Konfiguration wären genau diese Schritte erforderlich:
Realm erstellen, Public-OIDC-Client mit Redirect URI
`http://localhost:8081/swagger-ui/oauth2-redirect.html` anlegen, Realm-Rollen
erstellen, Benutzer anlegen und Rollen zuweisen.

## Access Tokens für die Demo

Die folgenden Password-Grant-Aufrufe sind nur als kompakte lokale Schul-Demo
aktiviert. Für echte Anwendungen wird der interaktive Authorization-Code-Flow
mit PKCE verwendet, wie ihn der importierte Client und Swagger UI unterstützen.

USER-Token holen:

```powershell
$userTokenResponse = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/m321/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id  = "m321-order-client"
    username   = "demo-user"
    password   = "user123"
    scope      = "openid"
  }
$userToken = $userTokenResponse.access_token
```

ADMIN-Token holen:

```powershell
$adminTokenResponse = Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/realms/m321/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id  = "m321-order-client"
    username   = "demo-admin"
    password   = "admin123"
    scope      = "openid"
  }
$adminToken = $adminTokenResponse.access_token
```

Alternativ in Swagger UI **Authorize** öffnen. Ein vorhandenes Access Token kann
bei `bearerJwt` eingefügt werden; die OIDC-Discovery ist ebenfalls in der
OpenAPI-Beschreibung definiert.

## USER-/ADMIN-Demo Schritt für Schritt

1. RabbitMQ und Keycloak mit `docker compose up -d` starten.
2. Spring Boot auf Port 8081 starten.
3. Keycloak Admin UI öffnen und importierten Realm, Client, Rollen und Benutzer
   wie oben beschrieben zeigen.
4. Ohne Token eine Bestellung senden:

```powershell
$body = @{ product = "Keyboard"; quantity = 2 } | ConvertTo-Json
try {
  Invoke-WebRequest -Method Post `
    -Uri "http://localhost:8081/api/orders" `
    -ContentType "application/json" -Body $body
} catch {
  $_.Exception.Response.StatusCode.value__  # 401
}
```

5. `$userToken` wie oben holen und Bestellung autorisiert senden:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/orders" `
  -Headers @{ Authorization = "Bearer $userToken" } `
  -ContentType "application/json" -Body $body
```

Die Antwort ist `202 Accepted` und enthält `orderId`, `product`, `quantity` und
`createdAt`.

6. Mit USER auf den Admin-Endpoint zugreifen:

```powershell
try {
  Invoke-WebRequest -Uri "http://localhost:8081/api/admin" `
    -Headers @{ Authorization = "Bearer $userToken" }
} catch {
  $_.Exception.Response.StatusCode.value__  # 403
}
```

7. `$adminToken` holen und denselben Endpoint aufrufen:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/admin" `
  -Headers @{ Authorization = "Bearer $adminToken" }
```

Die Antwort ist `200 OK` und zeigt `subject`, `username` und die Erfolgsmeldung.

8. In den Spring-Logs beziehungsweise in RabbitMQ unter **Queues and Streams**
   zeigen, dass die autorisierte Bestellung weiterhin asynchron verarbeitet
   wird: ein Worker erhält sie, anschließend bekommen Notification- und
   Audit-Consumer je eine Kopie des verarbeiteten Events.

## RabbitMQ-Demo

`OrderProducer` publiziert JSON in die durable Queue `orders.processing`.
`OrderWorker1` und `OrderWorker2` konkurrieren um Nachrichten derselben Queue.
Nach der Verarbeitung publiziert der Worker ein `OrderProcessedEvent` auf den
Fanout Exchange `orders.processed`, der Kopien an `orders.notification` und
`orders.audit` verteilt.

Unter **Queues and Streams -> orders.processing** bedeuten:

- **Ready:** wartet auf einen Worker.
- **Unacked:** wird gerade verarbeitet.
- **Total:** Ready plus Unacked.

Die Consumer-Steuerung benötigt nun ebenfalls ein Access Token:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/consumers/stop" `
  -Headers @{ Authorization = "Bearer $adminToken" }

Invoke-RestMethod -Method Post -Uri "http://localhost:8081/api/consumers/start" `
  -Headers @{ Authorization = "Bearer $adminToken" }
```

Der Demo-Scheduler erzeugt standardmäßig zusätzlich automatisch Bestellungen
und ruft intern direkt den Producer auf. Diese interne Demonstrationsfunktion
ist kein externer REST-Zugriff und benötigt deshalb kein JWT.

```properties
messaging.auto-producer.enabled=true
messaging.auto-producer.interval=1800
messaging.consumer.processing-time=3000
spring.rabbitmq.listener.simple.prefetch=1
```

## API- und Event-Dokumentation

- `openapi.yaml` dokumentiert REST-Routen, Bearer JWT, OIDC Discovery und
  `401`/`403`.
- springdoc erzeugt die laufende Beschreibung und Swagger UI.
- `asyncapi.yaml` beschreibt weiterhin die RabbitMQ-Channels und Events.

## Tests und Build

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Die Security-Integrationstests prüfen mindestens:

- Bestellung ohne JWT ergibt `401`.
- USER darf eine Bestellung erstellen.
- USER erhält am Admin-Endpoint `403`.
- ADMIN erhält am Admin-Endpoint `200` und die JWT-Identität.
