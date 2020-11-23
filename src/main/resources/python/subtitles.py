class Subtitle_block:
    def __init__(self, text='', boundaries=[2.5, -3.5, 0], text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        subtitle = Text(text, color=text_color, weight=text_weight, font=font)
        self.text = subtitle
        self.text_color = text_color
        self.text_weight = text_weight
        self.boundaries = boundaries
        self.font = font
        self.position = [boundaries[0][0] + (title_width / 2) + offset, (boundaries[0][1] + boundaries[3][1]) / 2, 0]

    def build(self):
        self.text.move_to(self.position)
        return self.text.arrange(DOWN, center=True)

    def change_text(self, text):
        self.text = Text(text, color=self.text_color, weight=self.text_weight, font=self.font)

    def display(self, text):
        self.change_text(text)
        self.text.move_to(self.position)
        return ShowCreation(self.text)

    def clear(self):
        return Uncreate(self.text)