# Object representing the visualised variables on the top left hand side of the screen
class Variable_block:
    def __init__(self, variables, variable_frame, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        group = VGroup()
        for v in variables:
            text = Text(v, color=text_color, weight=text_weight, font=font)
            text.set_width(min(0.8 * variable_frame.get_width(), text.get_width()))
            group.add(text)
        self.group = group
        self.variable_frame = variable_frame
        self.text_color = text_color
        self.text_weight = text_weight
        self.font = font

    def build(self):
        self.group.arrange(DOWN, aligned_edge=LEFT)
        return self.group.move_to(self.variable_frame)

    def update_variable(self, variables):
        if len(variables) == 0:
            return [FadeOut(self.group)]
        group = VGroup()
        for v in variables:
            text = Text(v, color=self.text_color, weight=self.text_weight, font=self.font)
            text.set_width(min(0.8 * self.variable_frame.get_width(), text.get_width()))
            group.add(text)

        old_group = self.group
        self.group = group
        self.group.arrange(DOWN, aligned_edge=LEFT)
        self.group.move_to(self.variable_frame)
        return [
            ReplacementTransform(old_group, group)
        ]