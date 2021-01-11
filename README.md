[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)
[![Java CI](https://github.com/VAlgoLang/VAlgoLang/workflows/Java%20CI/badge.svg?branch=master)](https://github.com/VAlgoLang/VAlgoLang/actions?query=workflow%3A%22Java+CI%22)
[![](https://img.shields.io/badge/docs-readthedocs.svg)](https://valgolang.github.io)

## VAlgoLang

VAlgoLang (formerly known as ManimDSL) is a domain specific language using [Manim](https://github.com/3b1b/manim) to create animations for
data structures and algorithms.

## Table of Contents:
- [Installation](#installation)
- [Web Editor](#web-editor)
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
    brew tap ManimDSL/homebrew-manimdsl
    brew install manimdsl
```

Typing `valgolang` in your terminal should show give you access to the compiler.

#### Debian Based Systems

```
curl -sLO https://github.com/VAlgoLang/VAlgoLang/releases/download/latest/valgolang_1.0.SNAPSHOT-1_all.deb && sudo dpkg -i valgolang_1.0.SNAPSHOT-1_all.deb
```

## Web Editor

If you do not want to install all the dependencies, you can use the [web editor](http://valgolang.netlify.app/) to write and compile code in VAlgoLang.

## Usage

Here is an example on how to use VAlgoLang:

```js
    let stack = Stack();
    stack.push(1);
    stack.pop();
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
