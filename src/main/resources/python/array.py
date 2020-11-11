class Array:
    def __init__(self, values, title, boundaries, color=BLUE, text_color=WHITE, padding=True):
        self.values = values
        boundary_width = boundaries[1][0] - boundaries[0][0]
        title_width = 1 if title != "" else 0
        width_per_element = (boundary_width - title_width) / len(values)
        padding = 0.15 if padding else 0
        square_dim = min((boundaries[0][1] - boundaries[3][1] - padding), width_per_element)
        self.array_elements = [
            Rectangle_block(str(val), color=color, text_color=text_color, width=square_dim, height=square_dim) for val
            in self.values]
        offset = 0
        if ((square_dim * len(values)) + title_width) < boundary_width:
            offset = (boundary_width - ((square_dim * len(values)) + title_width)) / 2
        self.title = VGroup(Text(title).set_width(title_width))
        self.title.move_to(
            np.array([boundaries[0][0] + (title_width / 2) + offset, (boundaries[0][1] + boundaries[3][1]) / 2, 0]))
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

class Array2D:
    def __init__(self, values, title, boundaries, color=BLUE, text_color=WHITE):
        self.values = values
        boundary_width = boundaries[1][0] - boundaries[0][0]
        title_width = 1 if title != "" else 0
        width_per_element = (boundary_width - title_width) / len(values[0])
        boundary_height = boundaries[0][1] - boundaries[3][1]
        square_dim = min((boundary_height - 0.5) / len(values), width_per_element)
        self.rows = []
        offset_from_bottom = (boundary_height - square_dim * len(values)) / 2
        for i in range(len(values) - 1, -1, -1):
            new_ll = boundaries[2][0] + title_width, boundaries[2][1] + (i * square_dim) + offset_from_bottom
            new_boundaries = [(new_ll[0], new_ll[1] + square_dim),
                              (new_ll[0] + boundary_width, new_ll[1] + square_dim), new_ll,
                              (new_ll[0] + square_dim, new_ll[1])]
            self.rows.append(Array(values[len(values) - 1 - i], "",new_boundaries, color=color, text_color=text_color, padding=False).build())
        self.title = VGroup(Text(title).set_width(title_width))
        self.title.move_to(
            np.array([boundaries[0][0] + (title_width / 2), (boundaries[0][1] + boundaries[3][1]) / 2, 0]))
        self.color = color

    def build(self):
        animations = []
        for i in range(len(self.rows)):
            animations += [FadeIn(array_elem.all) for array_elem in self.rows[i].array_elements]
        return animations


