class Rectangle_block:
    def __init__(self, text, height=0.75, width=1.5, color=BLUE, text_color=WHITE):
        self.text   = text
        self.height = height
        self.width  = width
        self.color  = color
        self.text_color = text_color

    def build(self):
        inside_text = TextMobject(self.text, color=self.text_color)
        rectangle   = Rectangle(height=self.height, width=self.width, color=self.color)
        group       = VGroup(inside_text, rectangle)
        return group
