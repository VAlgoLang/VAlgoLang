class Init_structure:
    def __init__(self, text, angle, length=1.5, color=WHITE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.text = text
        self.angle = angle
        self.length = length
        self.color = color
        self.text_color = text_color
        self.text_weight = text_weight
        self.font = font

    def build(self):
        line = Line(color=self.color)
        line.set_length(self.length)
        line.set_angle(self.angle)
        label = Text(self.text, color=self.text_color, weight=self.text_weight, font=self.font)
        label.next_to(line, DOWN, SMALL_BUFF)
        group = VGroup(label, line)
        return group
