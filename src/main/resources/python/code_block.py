class Code_block:
    def __init__(self, code, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        group = VGroup()
        fp = open("sample.re", "w")

        for c in code:
            for sc in c:
                fp.write(sc + "\n")

        fp.close()

        self.paragraph = Code("sample.re", style="inkpot", language="reasonml", line_spacing=0.2,
                              tab_width=2).code
        group.add(self.paragraph)
        group.set_width(5)
        self.code = code

    def build(self):
        return self.paragraph.arrange(DOWN, aligned_edge=LEFT, center=True)

    def get_line_at(self, line_number):
        idx = 0
        for i in range(line_number):
            idx += len(self.code[i])
        return self.paragraph[idx-1]