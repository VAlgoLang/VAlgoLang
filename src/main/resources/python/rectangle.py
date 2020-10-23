class Rectangle_block:
    def __init__(self, text, height=0.75, width=1.5, color=BLUE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.color = color
        self.shape_text = text
        self.text_color = text_color
        self.text_weight = text_weight
        self.font = font
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.shape = Rectangle(height=height, width=width, color=color)
        self.all = VGroup(self.text, self.shape)
