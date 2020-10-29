class Rectangle_block:
    def __init__(self, text, target=None, height=0.75, width=1.5, color=BLUE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.shape = Rectangle(height=height, width=width, color=color)
        self.all = VGroup(self.text, self.shape)
        self.text.set_width(7/10 * width)
        self.width = width
        self.text_color = text_color
        self.font = font
        if target:
            self.owner = target
            self.all.scale(max(target.empty.submobjects[1].get_height() / self.shape.get_height(), target.empty.get_width() / self.shape.get_width()))

    def replace_text(self, new_text):
        new_text_obj = Text(new_text, color=self.text_color, font=self.font)
        new_text_obj.set_width(self.width * 7/10)
        return (Transform(self.text, new_text_obj.move_to(self.all.get_center())))
