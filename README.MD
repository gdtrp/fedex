# Aggregation microservice
## How to build
Run command: mvn clean package, executable jar will be in {PROJECT_FOLDER}/target folder
## How to run
1. Run from maven: mvn clean spring-boot:run
2. Run as jar: java -jar {PROJECT_FOLDER}/target/assessment-{version}.jar
Property file can be passed with command line parameter -Dspring.config.location={PROPERTY_FILE_PATH}
## Common properties
server.port - port where aggregation service will be running  
service.bulk.enabled - enable bulk process(default true). If true all requests will be accumulating until {service.bulk.size} will be reached  
service.bulk.size - minimum limit for bulk process (default 5)  
service.bulk.flushInterval - interval for accumulating requests in bulk process before sending to API in seconds (default 5 sec, if 0 then flush will be disabled).  
service.host - path to external service  
service.connectionTimeout  - connection timeout for external service in seconds (default 30 sec)  
service.readTimeout - read timeout for external service in seconds  (default 30 sec)  
