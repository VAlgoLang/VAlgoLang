class Array:
    def __init__(self, values, title, boundaries, color=BLUE, text_color=WHITE):
        self.values = values
        width_per_element = (boundaries[1][0] - boundaries[0][0] - 1) / len(values)
        square_dim = min((boundaries[0][1] - boundaries[3][1] - 0.15),width_per_element)
        self.array_elements = [Rectangle_block(str(val), color=color, text_color=text_color, width=square_dim, height=square_dim) for val in self.values]
        self.title = VGroup(Text(title).set_width(1))
        self.title.move_to(np.array([boundaries[0][0] + 0.5, (boundaries[0][1] + boundaries[3][1]) / 2, 0]))
        self.color = color

    def build(self):
        previous = self.title
        buff = 0.1
        for array_elem in self.array_elements:
            group = array_elem.all
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
        self.array_elements[i1].text = o2
        self.array_elements[i2].text = o1
        return [
            CounterclockwiseTransform(o1, o1_copy),
            CounterclockwiseTransform(o2, o2_copy)
        ]

    def clone_and_swap(self, i1, i2):
        elem1_copy = deepcopy(self.array_elements[i1].text)
        elem2_copy = deepcopy(self.array_elements[i2].text)
        elem2_copy2 = deepcopy(self.array_elements[i2].text)
        elem2_copy2.move_to(self.array_elements[i1].text.get_center())
        elem1_copy2 = deepcopy(elem1_copy)
        elem1_copy2.move_to(self.array_elements[i2].text.get_center())
        return elem1_copy, elem2_copy, [[ApplyMethod(elem1_copy.next_to, self.array_elements[i1].all, np.array([0, 0.4, 0]))],
                                        [ClockwiseTransform(elem2_copy, elem2_copy2), FadeOut(self.array_elements[i1].text)],
                                        [ClockwiseTransform(elem1_copy, elem1_copy2), FadeOut(self.array_elements[i2].text)]]