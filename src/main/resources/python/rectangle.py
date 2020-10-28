class Rectangle_block:
    def __init__(self, text, target, height=0.75, width=1.5, color=BLUE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.shape = Rectangle(height=height, width=width, color=color)
        self.text.scale(self.shape.get_height() * 0.75 / self.text.get_height())
        self.all = VGroup(self.text, self.shape)
        self.all.scale(max(target.submobjects[1].get_height() / self.shape.get_height(), target.get_width() / self.shape.get_width()))