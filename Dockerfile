FROM gradle:jdk11

# Install python3 + all dependancies for manim
RUN apt-get update && apt-get install -y python3  python3-pip libcairo2-dev ffmpeg texlive texlive-latex-extra texlive-fonts-extra \
                                         texlive-latex-recommended texlive-science texlive-fonts-extra tipa

# Install manim (community)
RUN pip3 install manimce

COPY . /src/
WORKDIR /src

# Build jar file using gradle
RUN gradle build -x test

ENTRYPOINT ["java", "-jar", "build/libs/ManimDSLCompiler-1.0-SNAPSHOT.jar"]