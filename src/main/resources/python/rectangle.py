class Rectangle_block:
    def __init__(self, text, height=0.75, width=1.5, color=BLUE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        textWidth = 7/10 * width
        self.text.set_width(textWidth)
        self.shape = Rectangle(height=height, width=width, color=color)
        self.all = VGroup(self.text, self.shape)
