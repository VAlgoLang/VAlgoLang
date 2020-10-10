FROM gradle:jdk11
COPY . /src/
WORKDIR /src
RUN ./gradlew build --no-daemon -x test
ENTRYPOINT ["java", "-jar", "build/libs/ManimDSLCompiler-1.0-SNAPSHOT.jar"]