class SubtitleBlock:
    def __init__(self, end_time, boundaries, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.text = Text("", color=text_color, weight=text_weight, font=font)
        self.text_color = text_color
        self.text_weight = text_weight
        self.boundaries = boundaries
        self.font = font
        self.end_time = end_time
        self.width = boundaries[1][0] - boundaries[0][0]
        self.height = boundaries[0][1] - boundaries[3][1]
        self.position = np.array(
            [(boundaries[0][0] + boundaries[1][0]) / 2, (boundaries[0][1] + boundaries[3][1]) / 2, 0])
        self.text.move_to(self.position)
        self.showing = False

    def change_text(self, text):
        self.text = Text(text, color=self.text_color, weight=self.text_weight, font=self.font)
        if self.text.get_height() > self.height:
            self.text.scale(self.height / self.text.get_height())
        if self.text.get_width() > self.width:
            self.text.scale(self.width / self.text.get_width())

    def display(self, text, end_time):
        self.change_text(text)
        self.text.move_to(self.position)
        self.end_time = end_time
        self.showing = True
        return ShowCreation(self.text)

    def clear(self):
        self.showing = False
        return Uncreate(self.text)

    def action(self):
        return self.clear()
