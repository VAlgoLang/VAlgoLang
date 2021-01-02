FROM gradle:jdk11

RUN apt-get update && apt-get install -y texlive texlive-latex-extra texlive-fonts-extra \
                                         texlive-latex-recommended texlive-science texlive-fonts-extra tipa

# Install python3 + all dependancies for manim
RUN apt-get update && apt-get install -y python3 python3-pip sox ffmpeg libcairo2 libcairo2-dev

# Update pip for opencv dependancy
RUN pip3 install --upgrade pip

# Install manim
RUN pip3 install manimlib

COPY . /src/
WORKDIR /src

# Build jar file using gradle
RUN gradle build -x test

ENTRYPOINT ["java", "-jar", "build/libs/manimdsl-1.0-SNAPSHOT.jar"]