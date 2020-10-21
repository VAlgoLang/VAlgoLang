class Init_structure:
    def __init__(self, text, angle, length=1.5, color=WHITE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.shape = Line(color=color)
        self.shape.set_length(length)
        self.shape.set_angle(angle)
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.text.next_to(self.shape, DOWN, SMALL_BUFF)
        self.all = VGroup(self.text, self.shape)
