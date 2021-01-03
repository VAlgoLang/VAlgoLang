class Array:
    def __init__(self, values, title, boundaries, color=BLUE, text_color=WHITE, padding=True):
        self.values = values
        self.boundary_width = boundaries[1][0] - boundaries[0][0] - 0.1
        self.boundary_height = boundaries[0][1] - boundaries[3][1]
        self.boundaries = boundaries

        title_width = 1 if title != "" else 0
        self.title_width = title_width
        width_per_element = (self.boundary_width - title_width) / len(values)
        self.padding = 0.2 if padding else 0

        square_dim = min((boundaries[0][1] - boundaries[3][1] - self.padding), width_per_element)

        self.array_elements = [
            Rectangle_block(str(val), color=color, text_color=text_color, width=square_dim, height=square_dim) for val
            in self.values]
        offset = 0
        if ((square_dim * len(values)) + title_width) < self.boundary_width:
            offset = (self.boundary_width - ((square_dim * len(values)) + title_width)) / 2

        self.title = VGroup(Text(title).set_width(title_width))
        if title_width != 0 and self.title.get_height() > 0.5 * self.boundary_height:
            self.title.scale(0.5 * self.boundary_height / self.title.get_height())

        self.title.move_to(
            np.array([boundaries[0][0] + (title_width / 2) + offset, (boundaries[0][1] + boundaries[3][1]) / 2, 0]))
        self.color = color
        self.text_color = text_color

        self.all = VGroup(self.title, *[rect.all for rect in self.array_elements])


    def build(self, coord=None):
        previous = self.title if coord is None else coord
        buff = self.padding
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

    def clean_up(self):
        animations = [FadeOut(self.title)]
        animations.extend([elem.clean_up() for elem in self.array_elements])
        return animations

    def update_element(self, idx, v, color=None):
        self.values[idx] = v
        return self.array_elements[idx].replace_text(v, color=color)

    def update_array_elements(self):
        width_per_element = (self.boundary_width - self.title_width - self.padding) / len(self.values)
        square_dim = min((self.boundary_height - self.padding), width_per_element)
        self.array_elements = [
            Rectangle_block(str(val), color=self.color, text_color=self.text_color, width=square_dim, height=square_dim)
            for val in self.values]
        self.all = VGroup(self.title, *[rect.all for rect in self.array_elements])
        offset = 0
        if ((square_dim * len(self.values)) + self.title_width) < self.boundary_width:
            offset = (self.boundary_width - ((square_dim * len(self.values)) + self.title_width)) / 2
        coord = np.array([self.boundaries[0][0] + (self.title_width / 2) + offset,
                          (self.boundaries[0][1] + self.boundaries[3][1]) / 2, 0])
        return ApplyMethod(self.title.move_to, coord), deepcopy(coord)

    def append(self, v):
        animations = [elem.clean_up() for elem in self.array_elements]
        self.values.append(v)
        move_title, coord = self.update_array_elements()
        animations.append(move_title)
        coord[0] += 0.5
        self.build(coord)
        animations.extend([FadeIn(array_elem.all, run_time=1.0) for array_elem in self.array_elements])
        return animations

class Array2D:
    def __init__(self, values, title, boundaries, color=BLUE, text_color=WHITE):
        self.values = values
        boundary_width = boundaries[1][0] - boundaries[0][0]
        title_width = 1 if title != "" else 0
        width_per_element = (boundary_width - title_width - 0.2) / len(values[0])
        boundary_height = boundaries[0][1] - boundaries[3][1]
        square_dim = min((boundary_height - 1) / len(values), width_per_element)
        self.rows = []
        offset_from_bottom = (boundary_height - square_dim * len(values)) / 2
        sub_array_width = (square_dim * len(values[0]))
        for i in range(len(values) - 1, -1, -1):
            new_ll = boundaries[2][0] + ((boundary_width - sub_array_width) /2) , boundaries[2][1] + (i * square_dim) + offset_from_bottom - 0.25
            new_boundaries = [(new_ll[0], new_ll[1] + square_dim),
                              (new_ll[0] + sub_array_width, new_ll[1] + square_dim), new_ll,
                              (new_ll[0] + sub_array_width, new_ll[1])]
            self.rows.append(Array(values[len(values) - 1 - i], "",new_boundaries, color=color, text_color=text_color, padding=False).build())
        self.title = VGroup(Text(title).set_width(title_width))
        if title_width != 0 and self.title.get_height() > (boundary_height - square_dim * len(values)) / 2:
                    self.title.scale((boundary_height - square_dim * len(values)) / 2 / self.title.get_height())
        self.title.move_to(
            np.array([boundaries[0][0] + (boundary_width/ 2), (boundaries[0][1] - 0.5), 0]))
        self.color = color
        self.text_color = text_color

    def build(self, creation_style=None):
        if not creation_style:
            creation_style = "FadeIn"
        creation_transform = globals()[creation_style]
        animations = []
        for i in range(len(self.rows)):
            animations += [creation_transform(array_elem.all) for array_elem in self.rows[i].array_elements]
        return animations

    def replace_row(self, row_index, new_values):
        return [self.rows[row_index].array_elements[i].replace_text(str(v)) for i, v in enumerate(new_values)]

    def swap_mobjects(self, i1, j1, i2, j2):
        # Animations for fading to grey and fading back to original color
        fade_to_grey_animations = []
        fade_to_original_animations = []
        for i in range(len(self.rows)):
            for j in range(len(self.rows[0].array_elements)):
                if (i != i1 or j != j1) and (i != i2 or j != j2):
                    fade_to_grey_animations.append(FadeToColor(self.rows[i].array_elements[j].text, GREY))
                    fade_to_original_animations.append(FadeToColor(self.rows[i].array_elements[j].text, self.text_color))

        # Swapping elements
        o1 = self.rows[i1].array_elements[j1].text
        o2 = self.rows[i2].array_elements[j2].text
        o1_copy = deepcopy(o1)
        o2_copy = deepcopy(o2)
        o1_copy.move_to(o2.get_center())
        o2_copy.move_to(o1.get_center())
        self.rows[i1].array_elements[j1].text = o2
        self.rows[i2].array_elements[j2].text = o1
        swap_animations = [CounterclockwiseTransform(o1, o1_copy), CounterclockwiseTransform(o2, o2_copy)]

        return [fade_to_grey_animations, swap_animations, fade_to_original_animations]

    def clean_up(self):
        animations = [FadeOut(self.title)]
        animations.extend([animation for elem in self.rows for animation in elem.clean_up()])
        return animations
