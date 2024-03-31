# Account service

## Usage

### Build Account service

`./gradlew build`

### Run tests

`/gradlew test -i`

Tests are integration tests, but they don't require the `background-security-check` service.

View test reports:  
`account-service\build\reports\tests\`

### Configuration (Optional)

You can use the following properties to configure the application:

| Property name                      | Default value                                   | Description                                        | 
|------------------------------------|-------------------------------------------------|----------------------------------------------------|
| bank.account.prefix                | 12345000                                        | Prefix for account number. Only digits are allowed |
| bank.background-security-check.url | http://localhost:9095/background-security-check | URL for the pre-screening service                  |
| server.servlet.context-path        | /account-service                                |                                                    |

### Start background security check service

Use any of below commands:

- `java -jar background-security-check-1.0.0.jar`
- `java -jar background-security-check-1.0.0.jar --spring.profiles.active=test`
- `java -jar background-security-check-1.0.0.jar --security.check.length.max=1001 --security.check.length.min=1000`

### Start account service

`java -jar build/libs/account-service.jar --bank.account.prefix=99887766`

Or run with
`./gradlew bootRun`

### Open Swagger

http://127.0.0.1:8080/account-service/swagger-ui/index.html

### Query the database (Optional)

Visit: http://127.0.0.1:8080/account-service/h2-console/ and use the login information in application.properties, 
username: 'sa' and no password