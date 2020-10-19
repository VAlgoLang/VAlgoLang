class Rectangle_block:
    def __init__(self, text, height=0.75, width=1.5, color=BLUE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.text   = text
        self.height = height
        self.width  = width
        self.color  = color
        self.text_color  = text_color
        self.text_weight = text_weight
        self.font   = font

    def build(self):
        inside_text = Text(self.text, color=self.text_color, weight=self.text_weight, font=self.font)
        rectangle   = Rectangle(height=self.height, width=self.width, color=self.color)
        group       = VGroup(inside_text, rectangle)
        return group
