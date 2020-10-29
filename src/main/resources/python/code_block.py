class Code_block:
    def __init__(self, code, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        group = VGroup()
        for c in code:
            sgroup = VGroup()
            for sc in c:
                text = Text(sc, color=text_color, weight=text_weight, font=font)
                text.next_to(sgroup,DOWN)
                text.align_to(sgroup,LEFT)
                sgroup.add(text)
            group.add(sgroup)
        group.set_width(5)
        self.all = group

    def build(self):
        return self.all.arrange(DOWN, aligned_edge=LEFT)

    def get_line_at(self, line_number):
        return self.all[line_number - 1][0]
