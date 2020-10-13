## Manim DSL Compiler

ManimDSL is a domain specific language using Manim to create animations for
data structures and algorithms.

## Table of Contents:
- [Installation](#installation)
- [Usage](#usage)
- [Documentation](#documentation)
- [Help with Manim](#help-with-manim)
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
We welcome all contributions. In particular, we are hoping to extend the language as much as we can to produce more complex animations. For guidelines please see the
[documentation](https://manimdsl.github.io).

