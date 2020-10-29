class Code_block:
    def __init__(self, code, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        group = VGroup()
        for c in code:
            for sc in c:
                text = Text(sc, color=text_color, weight=text_weight, font=font)
                group.add(text)
        group.set_width(5)
        self.all = group
        self.code = code

    def build(self):
        return self.all.arrange(DOWN, aligned_edge=LEFT, center=True)

    def get_line_at(self, line_number):
        idx = 0
        for i in range(line_number):
            idx += len(self.code[i])
        return self.all[idx-1]
