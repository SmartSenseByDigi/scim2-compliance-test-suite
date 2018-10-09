FROM andreptb/maven:3.3.9-jdk8-alpine as builder
ADD . /build
WORKDIR /build
RUN mvn clean install

FROM tomcat:9.0.12-jre8-alpine
RUN apk add --update ca-certificates
COPY --from=builder /build/target/scimproxycompliance.war /usr/local/tomcat/webapps

