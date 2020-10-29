class ArrayElem:
    def __init__(self, val, scale, init_color, width):
        self.val = val
        self.group = Rectangle_block(str(self.val), width=width, height=width).all
        self.group.set_color(init_color)


class Array:
    def __init__(self, values, title, boundaries, init_color=BLUE):
        self.values = values
        max = 1.5
        size_of_square = (boundaries[1][0] - boundaries[0][0] - 1) / len(values)
        square_width = min(max, size_of_square)
        self.array_elements = [ArrayElem(val, 1, init_color, square_width) for val in self.values]
        self.title = VGroup(Text(title).set_width(0.7))
        self.title.move_to(np.array([boundaries[0][0] + 0.35, (boundaries[0][1] + boundaries[3][1]) / 2, 0]))
        self.init_color = init_color

    def build(self):
        previous = self.title
        buff = 0.2
        for array_elem in self.array_elements:
            group = array_elem.group
            group.next_to(previous, RIGHT, buff)
            previous = group
            buff = 0
        return self

    def swap_mobjects(self, i1: int, i2: int):
        o1 = self.array_elements[i1].text
        o2 = self.array_elements[i2].text
        o1_copy = deepcopy(o1)
        o2_copy = deepcopy(o2)
        o1_copy.move_to(o2.get_center())
        o2_copy.move_to(o1.get_center())
        return [
            CounterclockwiseTransform(o1, o1_copy),
            CounterclockwiseTransform(o2, o2_copy)
        ]
