class Rectangle_block:
    def __init__(self, text, target=None, height=0.75, width=1.5, color=BLUE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.shape = Rectangle(height=height, width=width, color=color)
        self.all = VGroup(self.text, self.shape)
        self.text.set_width(7/10 * width)
        self.height = height
        if(self.text.get_height() > 0.6 * height):
            self.text.scale(0.6 * height / self.text.get_height())
        self.width = width
        self.text_color = text_color
        self.font = font
        self.pointer = Triangle(color=color,fill_color=color,fill_opacity=1).flip(LEFT).scale(0.1)
        self.pointer.next_to(self.shape, TOP, 0.01)
        if target:
            self.owner = target
            self.all.scale(max(target.empty.get_height() / self.shape.get_height(), target.empty.get_width() / self.shape.get_width()))

    def replace_text(self, new_text, color=None):
        if not color:
            color = self.text_color
        new_text_obj = Text(new_text, color=color, font=self.font)
        new_text_obj.set_width(self.width * 7/10)
        if(new_text_obj.get_height() > 0.6 * self.height):
            new_text_obj.scale(0.6 * self.height / new_text_obj.get_height())
        return (Transform(self.text, new_text_obj.move_to(self.all.get_center())))

    def clean_up(self):
        return FadeOut(self.all)

