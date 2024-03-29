# Account service

## Usage

Build Account service: `./gradlew build`

Start background security check service: 
`java -jar background-security-check-1.0.0.jar`
`java -jar background-security-check-1.0.0.jar --spring.profiles.active=test`
`java -jar background-security-check-1.0.0.jar --security.check.length.max=1001 --security.check.length.min=1000`
Start account service: `java -jar build/libs/account-service.jar`

Or run with 
`./gradlew bootRun`

To query the database, visit: http://127.0.0.1:8080/h2-console
and use the login information