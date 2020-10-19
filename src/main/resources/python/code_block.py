class Code_block:
    def __init__(self, code, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        group = VGroup()
        for c in code:
            group.add(Text(c, color=text_color, weight=text_weight, font=font))
        self.group = group

    def build(self):
        return self.group.arrange(DOWN, aligned_edge=LEFT)

    def get_line_at(self, line_number):
        return self.group[line_number - 1]
