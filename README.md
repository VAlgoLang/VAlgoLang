## Manim DSL Compiler

ManimDSL is a domain specific language using [Manim](https://github.com/3b1b/manim) to create animations for
data structures and algorithms.

## Table of Contents:
- [Installation](#installation)
- [Usage](#usage)
- [Documentation](#documentation)
- [Contributing](#contributing)

## Installation

ManimDSL produces Python code that needs to run on Manim to generate the mp4 files. Please  follow the
[documentation](https://manimce.readthedocs.io/en/latest/installation.html)
and follow the instructions according to your operating system.

## Usage

Here is an example on how to use ManimDSL:

```js
    let stack = new Stack;
    stack.push(1);
    stack.pop();
```

Save this file with the manimdsl extension and run the compiler on it:

```
    java -jar manimdsl.jar <your-file-name>.manimdsl out.mp4
```

This should save the animation to `out.mp4`, animating the stack with 1 being pushed and popped off the stack.


## Documentation
Documentation is in progress at [manimdsl.github.io](https://manimdsl.github.io/).

## Contributing
We welcome all contributions! If you would like to contribute, please see the corresponding [guidelines][contributing]. By contributing, you are agreeing to our [code of conduct][code-of-conduct].

[contributing]: https://github.com/ManimDSL/ManimDSLCompiler/blob/master/CONTRIBUTING.md
[code-of-conduct]: https://github.com/ManimDSL/ManimDSLCompiler/blob/master/CODE_OF_CONDUCT.md
