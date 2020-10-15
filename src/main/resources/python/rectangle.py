class Rectangle_block:
    def __init__(self, text, height=0.75, width=1.5, color=BLUE):
        self.text   = text
        self.height = height
        self.width  = width
        self.color  = color

    def build(self):
        inside_text = TextMobject(self.text)
        rectangle   = Rectangle(height=self.height, width=self.width, color=self.color)
        group       = VGroup(inside_text, rectangle)
        return group
