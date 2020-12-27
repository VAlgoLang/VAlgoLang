# Object representing the visualised variables on the top left hand side of the screen
class Variable_block:
    def __init__(self, variables, boundaries, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        group = VGroup()
        self.move_position = np.array(
            [(boundaries[0][0] + boundaries[1][0]) / 2, (boundaries[0][1] + boundaries[3][1]) / 2, 0])
        self.boundary_width = boundaries[1][0] - boundaries[0][0]
        for v in variables:
            text = Text(v, color=text_color, weight=text_weight, font=font)
            text.set_width(min(0.8 * self.boundary_width, text.get_width()))
            group.add(text)
        self.group = group
        self.text_color = text_color
        self.text_weight = text_weight
        self.font = font
        self.size = len(variables)

    def build(self):
        self.group.arrange(DOWN, aligned_edge=LEFT)
        return self.group.move_to(self.move_position)

    def update_variable(self, variables):
        # To avoid awkward replace transform
        if self.size == 0:
            self.group.scale_in_place(0)

        self.size = len(variables)
        if self.size == 0:
            return [FadeOut(self.group)]
        group = VGroup()
        for v in variables:
            text = Text(v, color=self.text_color, weight=self.text_weight, font=self.font)
            text.set_width(min(0.8 * self.boundary_width, text.get_width()))
            group.add(text)

        old_group = self.group
        self.group = group
        self.group.arrange(DOWN, aligned_edge=LEFT)
        self.group.move_to(self.move_position)
        return [
            ReplacementTransform(old_group, group)
        ]