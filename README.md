[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)
[![Java CI](https://github.com/VAlgoLang/VAlgoLang/workflows/Java%20CI/badge.svg?branch=master)](https://github.com/VAlgoLang/VAlgoLang/actions?query=workflow%3A%22Java+CI%22)
[![](https://img.shields.io/badge/docs-readthedocs.svg)](https://valgolang.github.io)

## VAlgoLang

VAlgoLang (formerly known as ManimDSL) is a domain specific language using [Manim](https://github.com/3b1b/manim) to create animations for
data structures and algorithms.

## Table of Contents:
- [Installation](#installation)
- [Web Editor](#web-editor)
- [Building VAlgoLang Locally](#building-valgolang-locally)
- [Usage](#usage)
- [Documentation](#documentation)
- [Contributing](#contributing)

## Installation

VAlgoLang produces Python code that needs to run on Manim to generate the mp4 files. Please follow the
[documentation](https://manimce.readthedocs.io/en/latest/installation.html) to install manim
and follow the instructions according to your operating system.

Once manim is installed there are multiple ways to get VAlgoLang.

#### Mac OSX

VAlgoLang is avaliable on Homebrew using the instructions below:
```
    brew tap VAlgoLang/homebrew-valgolang
    brew install valgolang
```

Typing `valgolang` in your terminal should show give you access to the compiler.

#### Debian Based Systems

```
curl -sLO https://github.com/VAlgoLang/VAlgoLang/releases/download/latest/valgolang_1.0.SNAPSHOT-1_all.deb && sudo dpkg -i valgolang_1.0.SNAPSHOT-1_all.deb
```

## Web Editor

If you do not want to install all the dependencies, you can use the [web editor](http://valgolang.netlify.app/) to write and compile code in VAlgoLang.

## Building VAlgoLang Locally

To get started on your local machine, please do the following:

1. Fork and clone the repository and open in your favourite editor. Since the interpreter is written in Kotlin, our choice is [IntelliJ](https://www.jetbrains.com/idea/).
2. Install [Manim and its dependencies](https://docs.manim.community/en/latest/installation.html).
3. Make changes to the compiler as you wish.
4. Run the following command to build the JAR file for the compiler:

```
./gradlew build -x test
```

Remember that you'll need to do this every time you make a change and would like to see its effect.

5. Run the following on a `.val` file to see whether your changes are working as expected:

```
./compile <your-file-name>.val
```

During development, it might be easier for you to read through the `.py` output file each time you make a change than to wait for Manim to generate a video. In this case, we recommend using the `-p` flag during compilation. For more on the command line arguments for VAlgoLang, see [here](https://valgolang.github.io/usage.html#command-line-arguments).

## Usage

Here is an example on how to use VAlgoLang:

```js
    let stack = Stack<number>();
    stack.push(1);
    let x = stack.pop();
```

Save this file with the .val extension and run the compiler on it:

```
    java -jar valgolang.jar <your-file-name>.val out.mp4
```

This should save the animation to `out.mp4`, animating the stack with 1 being pushed and popped off the stack.


## Documentation
Documentation is in progress at [valgolang.github.io](https://valgolang.github.io/).

## Contributing
We welcome all contributions! If you would like to contribute, please see the corresponding [guidelines][contributing]. By contributing, you are agreeing to our [code of conduct][code-of-conduct].

[contributing]: https://github.com/VAlgoLang/VAlgoLang/blob/master/CONTRIBUTING.md
[code-of-conduct]: https://github.com/VAlgoLang/VAlgoLang/blob/master/CODE_OF_CONDUCT.md
