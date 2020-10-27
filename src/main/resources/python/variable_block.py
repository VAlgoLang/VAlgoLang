# Object representing the visualised variables on the top left hand side of the screen
class Variable_block:
    def __init__(self, variables, variable_frame, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        group = VGroup()
        for v in variables:
            group.add(Text(v, color=text_color, weight=text_weight, font=font))
        self.group = group
        self.variable_frame = variable_frame
        self.text_color = text_color
        self.text_weight = text_weight
        self.font = font

    def build(self):
        self.group.arrange(DOWN, aligned_edge=LEFT)
        return self.group.move_to(self.variable_frame)

    def update_variable(self, variables):
        group = VGroup()
        for v in variables:
            group.add(Text(v, color=self.text_color, weight=self.text_weight, font=self.font))

        old_group = self.group
        self.group = group
        self.group.arrange(DOWN, aligned_edge=LEFT)
        self.group.move_to(self.variable_frame)
        return [
            ReplacementTransform(old_group, group)
        ]