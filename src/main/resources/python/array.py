class ArrayElem:
    def __init__(self, val, scale, color, text_color, width):
        self.val = val
        self.rectangle = Rectangle_block(str(self.val), width=width, height=width, color=color, text_color=text_color)
        self.group = self.rectangle.all


class Array:
    def __init__(self, values, title, boundaries, color=BLUE, text_color=WHITE):
        self.values = values
        max = 1.5
        size_of_square = (boundaries[1][0] - boundaries[0][0] - 1) / len(values)
        square_width = min(max, size_of_square)
        self.array_elements = [ArrayElem(val, 1, color, text_color, square_width) for val in self.values]
        self.title = VGroup(Text(title).set_width(0.7))
        self.title.move_to(np.array([boundaries[0][0] + 0.35, (boundaries[0][1] + boundaries[3][1]) / 2, 0]))
        self.color = color

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
        o1 = self.array_elements[i1].rectangle.text
        o2 = self.array_elements[i2].rectangle.text
        o1_copy = deepcopy(o1)
        o2_copy = deepcopy(o2)
        o1_copy.move_to(o2.get_center())
        o2_copy.move_to(o1.get_center())
        return [
            CounterclockwiseTransform(o1, o1_copy),
            CounterclockwiseTransform(o2, o2_copy)
        ]
