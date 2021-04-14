FROM gradle:jdk11

RUN apt-get update && apt-get install -y texlive texlive-latex-extra texlive-fonts-extra \
                                         texlive-latex-recommended texlive-science texlive-fonts-extra tipa

# Install python3 + all dependancies for manim
RUN apt-get update && apt-get install -y python3 python3-pip sox ffmpeg libcairo2 libcairo2-dev dos2unix

# Update pip for opencv dependancy
RUN pip3 install --upgrade pip

# Install manim
RUN pip3 install manimlib

COPY . /src/
WORKDIR /src

# Convert line endings for windows
RUN dos2unix antlr_config/antlrBuild

# Build jar file using gradle
RUN gradle build -x test

ENTRYPOINT ["java", "-jar", "build/libs/valgolang-1.0-SNAPSHOT.jar"]